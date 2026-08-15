package com.pitahmzalendo.kejafy.listing.dto.response;

import com.pitahmzalendo.kejafy.listing.entity.Listing;
import com.pitahmzalendo.kejafy.listing.entity.ListingPurpose;
import com.pitahmzalendo.kejafy.listing.entity.PropertyType;

import java.math.BigDecimal;
import java.util.List;

/**
 * The public feed card. This is THE enforcement point for the
 * location-gating selling point: a non-subscriber gets {@code locked=true}
 * and every field below {@code videos}/{@code likeCount}/{@code likedByMe}
 * comes back null — no title, description, price, neighbourhood, or GPS.
 * They can still watch and like. A subscriber gets everything, including
 * the exact pin, via {@link #unlocked}.
 *
 * Deliberately NOT built from ListingDetailResponse or by nulling fields
 * on a shared object — two separate factory methods make it obvious at a
 * glance which fields are safe for a non-subscriber and prevent a future
 * edit from accidentally wiring a gated field into the locked path.
 */
public record ListingFeedItemResponse(
        Long id,
        boolean locked,
        List<ListingVideoResponse> videos,
        long likeCount,
        boolean likedByMe,
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
        BigDecimal longitude
) {
    public static ListingFeedItemResponse locked(Listing listing, boolean likedByMe) {
        return new ListingFeedItemResponse(
                listing.getId(),
                true,
                listing.getVideos().stream().map(ListingVideoResponse::from).toList(),
                listing.getLikeCount(),
                likedByMe,
                null, null, null, null, null, null, null, null, null, null, null
        );
    }

    public static ListingFeedItemResponse unlocked(Listing listing, boolean likedByMe) {
        return new ListingFeedItemResponse(
                listing.getId(),
                false,
                listing.getVideos().stream().map(ListingVideoResponse::from).toList(),
                listing.getLikeCount(),
                likedByMe,
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
                listing.getLongitude()
        );
    }
}
