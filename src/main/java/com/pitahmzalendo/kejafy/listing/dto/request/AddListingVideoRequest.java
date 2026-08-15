package com.pitahmzalendo.kejafy.listing.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddListingVideoRequest(
        @NotBlank(message = "videoUrl is required")
        @Size(max = 2048, message = "videoUrl is too long")
        String videoUrl,

        @Size(max = 2048, message = "thumbnailUrl is too long")
        String thumbnailUrl,

        @Min(value = 1, message = "durationSeconds must be positive")
        Integer durationSeconds
) {
}
