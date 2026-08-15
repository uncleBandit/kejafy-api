package com.pitahmzalendo.kejafy.listing.dto.request;

import com.pitahmzalendo.kejafy.listing.entity.ListingPurpose;
import com.pitahmzalendo.kejafy.listing.entity.PropertyType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Partial update, same PATCH semantics as UpdateProfileRequest — null
 * fields are left unchanged. Only allowed while the listing is DRAFT or
 * REJECTED (see ListingService); a PUBLISHED or PENDING_REVIEW listing
 * can't be silently edited out from under the moderation queue.
 */
public record UpdateListingRequest(
        @Size(max = 150, message = "title is too long")
        String title,

        @Size(max = 4000, message = "description is too long")
        String description,

        PropertyType propertyType,

        ListingPurpose purpose,

        @DecimalMin(value = "0.0", inclusive = false, message = "priceAmount must be positive")
        BigDecimal priceAmount,

        @Min(value = 0, message = "bedrooms cannot be negative")
        Integer bedrooms,

        @Min(value = 0, message = "bathrooms cannot be negative")
        Integer bathrooms,

        @Size(max = 150, message = "neighbourhood is too long")
        String neighbourhood,

        @DecimalMin(value = "-90.0", message = "latitude out of range")
        @DecimalMax(value = "90.0", message = "latitude out of range")
        BigDecimal latitude,

        @DecimalMin(value = "-180.0", message = "longitude out of range")
        @DecimalMax(value = "180.0", message = "longitude out of range")
        BigDecimal longitude
) {
}
