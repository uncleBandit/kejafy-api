package com.pitahmzalendo.kejafy.listing.dto.request;

import com.pitahmzalendo.kejafy.listing.entity.ListingPurpose;
import com.pitahmzalendo.kejafy.listing.entity.PropertyType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Creates a listing in DRAFT status — it isn't submitted for review yet.
 * The agent adds videos with separate calls (a video is typically a
 * large async upload, not part of this JSON payload), then calls
 * submit-for-review when ready.
 */
public record CreateListingRequest(
        @NotBlank(message = "title is required")
        @Size(max = 150, message = "title is too long")
        String title,

        @NotBlank(message = "description is required")
        @Size(max = 4000, message = "description is too long")
        String description,

        @NotNull(message = "propertyType is required")
        PropertyType propertyType,

        @NotNull(message = "purpose is required")
        ListingPurpose purpose,

        @NotNull(message = "priceAmount is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "priceAmount must be positive")
        BigDecimal priceAmount,

        @Min(value = 0, message = "bedrooms cannot be negative")
        Integer bedrooms,

        @Min(value = 0, message = "bathrooms cannot be negative")
        Integer bathrooms,

        @NotBlank(message = "neighbourhood is required")
        @Size(max = 150, message = "neighbourhood is too long")
        String neighbourhood,

        @NotNull(message = "latitude is required")
        @DecimalMin(value = "-90.0", message = "latitude out of range")
        @DecimalMax(value = "90.0", message = "latitude out of range")
        BigDecimal latitude,

        @NotNull(message = "longitude is required")
        @DecimalMin(value = "-180.0", message = "longitude out of range")
        @DecimalMax(value = "180.0", message = "longitude out of range")
        BigDecimal longitude
) {
}
