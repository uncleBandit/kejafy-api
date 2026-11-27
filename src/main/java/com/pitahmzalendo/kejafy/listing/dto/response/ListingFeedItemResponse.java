package com.pitahmzalendo.kejafy.listing.dto.response;

import com.pitahmzalendo.kejafy.listing.entity.Listing;
import com.pitahmzalendo.kejafy.listing.entity.ListingPurpose;
import com.pitahmzalendo.kejafy.listing.entity.PropertyType;

import java.math.BigDecimal;
import java.util.List;

/**
 * The public feed card. This is THE enforcement point for the
 * location-gating selling point: a non-subscriber gets {@code locked=true}
 * and every field below {@code videos}/{@code likeCount}/{@code likedByMe}/
 * {@code commentCount} comes back null — no title, description, price,
 * neighbourhood, or GPS. They can still watch, like, and read (though not
 * post) comments — see CommentService. A subscriber gets everything,
 * including the exact pin, via {@link #unlocked}.
 *
 * Deliberately NOT built from ListingDetailResponse or by nulling fields
 * on a shared object — two separate factory methods make it obvious at a
 * glance which fields are safe for a non-subscriber and prevent a future
 * edit from accidentally wiring a gated field into the locked path.
 *
 * distanceKm is populated only when the request was a geo search (see
 * ListingSearchFilter) — it's straight-line distance from the caller's
 * search center, not the listing's own coordinates, so it's safe to show
 * even on a locked card: knowing "this is 2.3km from the point I searched"
 * doesn't reveal the listing's actual pin the way latitude/longitude would.
 *
 * amenities/environmentProfile are populated on BOTH locked and unlocked
 * cards, deliberately outside the gate everything else here sits behind.
 * They describe the surroundings ("school 400m away", "flood risk: low"),
 * never the listing's own exact coordinates (see ListingAmenity /
 * EnvironmentProfile class javadocs) — they're the decision factors meant
 * to make a browsing non-subscriber want to unlock the pin, not something
 * that needs to be withheld itself.
 */
public record ListingFeedItemResponse(
        Long id,
        boolean locked,
        List<ListingVideoResponse> videos,
        long likeCount,
        boolean likedByMe,
        long commentCount,
        List<ListingAmenityResponse> amenities,
        EnvironmentProfileResponse environmentProfile,
        String title,
        String description,
        PropertyType propertyType,
        ListingPurpose purpose,
        BigDecimal priceAmount,
        String priceCurrency,
        Integer bedrooms,
        Integer bathrooms,
        String neighbourhood,
        BigDecimal latitude,
        BigDecimal longitude,
        Double distanceKm
) {
    public static ListingFeedItemResponse locked(Listing listing, boolean likedByMe) {
        return locked(listing, likedByMe, null);
    }

    public static ListingFeedItemResponse locked(Listing listing, boolean likedByMe, Double distanceKm) {
        return new ListingFeedItemResponse(
                listing.getId(),
                true,
                listing.getVideos().stream().map(ListingVideoResponse::from).toList(),
                listing.getLikeCount(),
                likedByMe,
                listing.getCommentCount(),
                listing.getAmenities().stream().map(ListingAmenityResponse::from).toList(),
                EnvironmentProfileResponse.from(listing.getEnvironmentProfile()),
                // title, description, propertyType, purpose, priceAmount,
                // priceCurrency, bedrooms, bathrooms, neighbourhood, latitude,
                // longitude — all gated.
                null, null, null, null, null, null, null, null, null, null, null,
                distanceKm
        );
    }

    public static ListingFeedItemResponse unlocked(Listing listing, boolean likedByMe) {
        return unlocked(listing, likedByMe, null);
    }

    public static ListingFeedItemResponse unlocked(Listing listing, boolean likedByMe, Double distanceKm) {
        return new ListingFeedItemResponse(
                listing.getId(),
                false,
                listing.getVideos().stream().map(ListingVideoResponse::from).toList(),
                listing.getLikeCount(),
                likedByMe,
                listing.getCommentCount(),
                listing.getAmenities().stream().map(ListingAmenityResponse::from).toList(),
                EnvironmentProfileResponse.from(listing.getEnvironmentProfile()),
                listing.getTitle(),
                listing.getDescription(),
                listing.getPropertyType(),
                listing.getPurpose(),
                listing.getPriceAmount(),
                listing.getPriceCurrency(),
                listing.getBedrooms(),
                listing.getBathrooms(),
                listing.getNeighbourhood(),
                listing.getLatitude(),
                listing.getLongitude(),
                distanceKm
        );
    }
}