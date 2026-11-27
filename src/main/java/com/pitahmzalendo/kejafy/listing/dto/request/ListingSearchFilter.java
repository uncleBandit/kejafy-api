package com.pitahmzalendo.kejafy.listing.dto.request;

import com.pitahmzalendo.kejafy.listing.entity.AmenityType;
import com.pitahmzalendo.kejafy.listing.entity.ListingPurpose;
import com.pitahmzalendo.kejafy.listing.entity.ListingSearchSort;
import com.pitahmzalendo.kejafy.listing.entity.PropertyType;

import java.math.BigDecimal;

/**
 * Parsed query params for GET /api/v1/feed/search. A search is a GET, so
 * its params live in the URL rather than a JSON body — ListingFeedController
 * collects the individual @RequestParam values into one of these and hands
 * it to ListingFeedService instead of passing a dozen loose arguments
 * around. Every field is optional; an all-null filter is just "browse
 * everything", same as the plain feed.
 *
 * lat/lng/radiusKm together define the search circle for the "precise
 * location" selling point — radiusKm without a center (or DISTANCE sort
 * without a center) is rejected by ListingFeedService.validate, not here,
 * since that's a cross-field domain rule, not a shape/type concern.
 */
public record ListingSearchFilter(
        Double lat,
        Double lng,
        Double radiusKm,
        PropertyType propertyType,
        ListingPurpose purpose,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer minBedrooms,
        Integer minBathrooms,
        String neighbourhood,
        Integer maxNoiseLevel,
        Integer maxFloodRiskLevel,
        Integer minRoadAccessQuality,
        Integer minSecurityLevel,
        Integer minWaterReliability,
        Integer minPowerReliability,
        AmenityType requireAmenityType,
        ListingSearchSort sort
) {
    public boolean hasCenter() {
        return lat != null && lng != null;
    }
}

