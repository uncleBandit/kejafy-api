package com.pitahmzalendo.kejafy.listing.repository;


import com.pitahmzalendo.kejafy.listing.entity.Listing;
import com.pitahmzalendo.kejafy.listing.entity.ListingStatus;
import com.pitahmzalendo.kejafy.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    Page<Listing> findByAgentOrderByCreatedAtDesc(User agent, Pageable pageable);

    Page<Listing> findByStatusOrderByPublishedAtDesc(ListingStatus status, Pageable pageable);

    Page<Listing> findByStatusOrderBySubmittedAtAsc(ListingStatus status, Pageable pageable);

    // Flagged listings (see Listing.flaggedForReview) are pulled out of
    // every public-facing read even while status is still PUBLISHED —
    // renamed from findByStatusOrderByPublishedAtDesc when the fraud
    // flag was added; update ListingFeedService's call site if you
    // rename this again.
    Page<Listing> findByStatusAndFlaggedForReviewFalseOrderByPublishedAtDesc(
            ListingStatus status, Pageable pageable);

    // Admin moderation queue for ReportService's auto-flag outcome —
    // oldest flag first, so the listing that's been sitting flagged
    // longest gets reviewed first.
    Page<Listing> findByFlaggedForReviewTrueOrderByFlaggedAtAsc(Pageable pageable);


    /**
     * Geo/radius + attribute search over PUBLISHED listings, for
     * GET /api/v1/feed/search (ListingFeedService.search).
     *
     * Every filter is optional and short-circuits via
     * "(:param IS NULL OR ...)" so a null simply drops that condition —
     * one query handles "just area + price", "just radius", or every
     * filter at once, instead of building the WHERE clause in Java.
     *
     * Distance itself is plain Haversine, done in raw SQL (acos/cos/sin/
     * radians are standard in Postgres, MySQL and H2, so this needs no
     * PostGIS/spatial extension) rather than PostGIS ST_DWithin. LEAST/
     * GREATEST clamp the acos() argument to [-1, 1] — floating-point
     * rounding can nudge the cosine sum a hair past 1.0 for a point
     * that's essentially on top of the search center, which makes
     * acos() return NaN instead of ~0. GeoUtils.distanceKm does the same
     * formula in Java to populate ListingFeedItemResponse.distanceKm for
     * display — if you change one, change the other.
     *
     * lat/lng/radiusKm are only applied when radiusKm is non-null;
     * ListingFeedService rejects a radiusKm with no center before this
     * is ever called, so lat/lng being null here always means "no geo
     * filtering requested" rather than a malformed request.
     *
     * sort is a plain string switch (NEWEST/PRICE_ASC/PRICE_DESC/DISTANCE)
     * matching ListingSearchSort.name() — each CASE WHEN branch is its
     * own ORDER BY column, so an inactive branch evaluates to NULL for
     * every row and has no effect on ordering; published_at DESC is the
     * always-on tiebreaker.
     *
     * flagged_for_review is excluded here the same way
     * findByStatusAndFlaggedForReviewFalseOrderByPublishedAtDesc excludes
     * it for the plain feed — status alone isn't enough once a listing's
     * been auto-flagged (see Listing.flaggedForReview javadoc), and this
     * query used to miss that, which meant a flagged listing could still
     * surface through search while hidden from the main feed.
     *
     * The env_* min/max params compare directly against the flat columns
     * EnvironmentProfile embeds onto this table (see that class) — a
     * threshold that's non-null only ever matches a listing that HAS a
     * score for that column; an unscored (null) column never satisfies
     * either a min or a max comparison in SQL, which is what we want
     * (see ListingSearchFilter javadoc: unscored isn't "not excluded").
     *
     * requireAmenityType is an EXISTS against listing_amenities rather
     * than a join, so it can't multiply result rows if a listing has
     * more than one amenity of a type search doesn't even care about.
     */
    @Query(
            value = """
                    SELECT l.* FROM listings l
                    WHERE l.status = 'PUBLISHED'
                      AND l.flagged_for_review = false
                      AND (:propertyType IS NULL OR l.property_type = :propertyType)
                      AND (:purpose IS NULL OR l.purpose = :purpose)
                      AND (:minPrice IS NULL OR l.price_amount >= :minPrice)
                      AND (:maxPrice IS NULL OR l.price_amount <= :maxPrice)
                      AND (:minBedrooms IS NULL OR l.bedrooms >= :minBedrooms)
                      AND (:minBathrooms IS NULL OR l.bathrooms >= :minBathrooms)
                      AND (:neighbourhood IS NULL OR lower(l.neighbourhood) LIKE lower(concat('%', :neighbourhood, '%')))
                      AND (:maxNoiseLevel IS NULL OR l.env_noise_level <= :maxNoiseLevel)
                      AND (:maxFloodRiskLevel IS NULL OR l.env_flood_risk_level <= :maxFloodRiskLevel)
                      AND (:minRoadAccessQuality IS NULL OR l.env_road_access_quality >= :minRoadAccessQuality)
                      AND (:minSecurityLevel IS NULL OR l.env_security_level >= :minSecurityLevel)
                      AND (:minWaterReliability IS NULL OR l.env_water_reliability >= :minWaterReliability)
                      AND (:minPowerReliability IS NULL OR l.env_power_reliability >= :minPowerReliability)
                      AND (
                            :requireAmenityType IS NULL OR EXISTS (
                                SELECT 1 FROM listing_amenities la
                                WHERE la.listing_id = l.id AND la.type = :requireAmenityType
                            )
                          )
                      AND (
                            :radiusKm IS NULL OR
                            (6371 * acos(least(1.0, greatest(-1.0,
                                cos(radians(:lat)) * cos(radians(l.latitude)) *
                                cos(radians(l.longitude) - radians(:lng)) +
                                sin(radians(:lat)) * sin(radians(l.latitude))
                            )))) <= :radiusKm
                          )
                    ORDER BY
                      CASE WHEN :sort = 'PRICE_ASC' THEN l.price_amount END ASC,
                      CASE WHEN :sort = 'PRICE_DESC' THEN l.price_amount END DESC,
                      CASE WHEN :sort = 'DISTANCE' THEN
                        (6371 * acos(least(1.0, greatest(-1.0,
                            cos(radians(:lat)) * cos(radians(l.latitude)) *
                            cos(radians(l.longitude) - radians(:lng)) +
                            sin(radians(:lat)) * sin(radians(l.latitude))
                        ))))
                      END ASC,
                      l.published_at DESC
                    """,
            countQuery = """
                    SELECT count(*) FROM listings l
                    WHERE l.status = 'PUBLISHED'
                      AND l.flagged_for_review = false
                      AND (:propertyType IS NULL OR l.property_type = :propertyType)
                      AND (:purpose IS NULL OR l.purpose = :purpose)
                      AND (:minPrice IS NULL OR l.price_amount >= :minPrice)
                      AND (:maxPrice IS NULL OR l.price_amount <= :maxPrice)
                      AND (:minBedrooms IS NULL OR l.bedrooms >= :minBedrooms)
                      AND (:minBathrooms IS NULL OR l.bathrooms >= :minBathrooms)
                      AND (:neighbourhood IS NULL OR lower(l.neighbourhood) LIKE lower(concat('%', :neighbourhood, '%')))
                      AND (:maxNoiseLevel IS NULL OR l.env_noise_level <= :maxNoiseLevel)
                      AND (:maxFloodRiskLevel IS NULL OR l.env_flood_risk_level <= :maxFloodRiskLevel)
                      AND (:minRoadAccessQuality IS NULL OR l.env_road_access_quality >= :minRoadAccessQuality)
                      AND (:minSecurityLevel IS NULL OR l.env_security_level >= :minSecurityLevel)
                      AND (:minWaterReliability IS NULL OR l.env_water_reliability >= :minWaterReliability)
                      AND (:minPowerReliability IS NULL OR l.env_power_reliability >= :minPowerReliability)
                      AND (
                            :requireAmenityType IS NULL OR EXISTS (
                                SELECT 1 FROM listing_amenities la
                                WHERE la.listing_id = l.id AND la.type = :requireAmenityType
                            )
                          )
                      AND (
                            :radiusKm IS NULL OR
                            (6371 * acos(least(1.0, greatest(-1.0,
                                cos(radians(:lat)) * cos(radians(l.latitude)) *
                                cos(radians(l.longitude) - radians(:lng)) +
                                sin(radians(:lat)) * sin(radians(l.latitude))
                            )))) <= :radiusKm
                          )
                    """,
            nativeQuery = true
    )
    Page<Listing> search(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radiusKm") Double radiusKm,
            @Param("propertyType") String propertyType,
            @Param("purpose") String purpose,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minBedrooms") Integer minBedrooms,
            @Param("minBathrooms") Integer minBathrooms,
            @Param("neighbourhood") String neighbourhood,
            @Param("maxNoiseLevel") Integer maxNoiseLevel,
            @Param("maxFloodRiskLevel") Integer maxFloodRiskLevel,
            @Param("minRoadAccessQuality") Integer minRoadAccessQuality,
            @Param("minSecurityLevel") Integer minSecurityLevel,
            @Param("minWaterReliability") Integer minWaterReliability,
            @Param("minPowerReliability") Integer minPowerReliability,
            @Param("requireAmenityType") String requireAmenityType,
            @Param("sort") String sort,
            Pageable pageable
    );
}