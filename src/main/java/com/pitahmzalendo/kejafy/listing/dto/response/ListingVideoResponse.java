package com.pitahmzalendo.kejafy.listing.dto.response;

import com.pitahmzalendo.kejafy.listing.entity.ListingVideo;

public record ListingVideoResponse(
        Long id,
        String videoUrl,
        String thumbnailUrl,
        Integer durationSeconds,
        int displayOrder
) {
    public static ListingVideoResponse from(ListingVideo video) {
        return new ListingVideoResponse(
                video.getId(),
                video.getVideoUrl(),
                video.getThumbnailUrl(),
                video.getDurationSeconds(),
                video.getDisplayOrder()
        );
    }
}
