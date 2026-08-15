package com.pitahmzalendo.kejafy.listing.dto.request;

import jakarta.validation.constraints.Size;

public record ListingReviewRequest(
        @Size(max = 500, message = "reason is too long")
        String reason
) {
}
