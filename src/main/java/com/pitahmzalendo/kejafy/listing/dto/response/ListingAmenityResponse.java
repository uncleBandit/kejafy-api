package com.pitahmzalendo.kejafy.listing.dto.response;

import com.pitahmzalendo.kejafy.listing.entity.AmenityCategory;
import com.pitahmzalendo.kejafy.listing.entity.AmenitySource;
import com.pitahmzalendo.kejafy.listing.entity.AmenityType;
import com.pitahmzalendo.kejafy.listing.entity.ListingAmenity;

import java.math.BigDecimal;

/**
 * One nearby point of interest on a listing — safe on both locked and
 * unlocked feed cards (see ListingAmenity / ListingFeedItemResponse): it
 * carries a name, type and distance, never coordinates.
 *
 * source is included so a client can tell an agent's unverified claim
 * apart from one confirmed by a places API or an admin (see AmenitySource).
 * externalPlaceId is deliberately left out: it's an internal dedupe key
 * for the enrichment job, not something a reader needs.
 */
public record ListingAmenityResponse(
        Long id,
        AmenityType type,
        AmenityCategory category,
        String name,
        BigDecimal distanceKm,
        Integer walkingMinutes,
        AmenitySource source
) {
    public static ListingAmenityResponse from(ListingAmenity amenity) {
        return new ListingAmenityResponse(
                amenity.getId(),
                amenity.getType(),
                amenity.getCategory(),
                amenity.getName(),
                amenity.getDistanceKm(),
                amenity.getWalkingMinutes(),
                amenity.getSource()
        );
    }
}
