package com.pitahmzalendo.kejafy.listing.dto.response;

import com.pitahmzalendo.kejafy.listing.entity.Listing;
import com.pitahmzalendo.kejafy.listing.entity.ListingPurpose;
import com.pitahmzalendo.kejafy.listing.entity.ListingStatus;
import com.pitahmzalendo.kejafy.listing.entity.PropertyType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The full record, including the exact GPS pin — only ever built for:
 * the owning agent, an admin, or (via ListingFeedService) a subscriber.
 * Never return this from an endpoint an unauthenticated or
 * non-subscribed customer can reach; use ListingFeedItemResponse there.
 */
public record ListingDetailResponse(
        Long id,
        Long agentId,
        String agentName,
        String agentPhone,
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
        ListingStatus status,
        String rejectionReason,
        long viewCount,
        long likeCount,
        List<ListingVideoResponse> videos,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime publishedAt
) {
    public static ListingDetailResponse from(Listing listing) {
        return new ListingDetailResponse(
                listing.getId(),
                listing.getAgent().getId(),
                listing.getAgent().getFirstName() + " " + listing.getAgent().getLastName(),
                listing.getAgent().getPhoneNumber(),
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
                listing.getStatus(),
                listing.getRejectionReason(),
                listing.getViewCount(),
                listing.getLikeCount(),
                listing.getVideos().stream().map(ListingVideoResponse::from).toList(),
                listing.getCreatedAt(),
                listing.getUpdatedAt(),
                listing.getPublishedAt()
        );
    }
}
