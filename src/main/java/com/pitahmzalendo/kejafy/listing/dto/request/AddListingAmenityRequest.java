package com.pitahmzalendo.kejafy.listing.dto.request;

import com.pitahmzalendo.kejafy.listing.entity.AmenityType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Always AmenitySource.AGENT_ENTERED — there's no client-supplied source
 * field, since an agent can't self-attest to PLACES_API or
 * ADMIN_VERIFIED provenance. See ListingService.addAmenity.
 */
public record AddListingAmenityRequest(
        @NotNull(message = "type is required")
        AmenityType type,

        @NotBlank(message = "name is required")
        @Size(max = 150, message = "name is too long")
        String name,

        @NotNull(message = "distanceKm is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "distanceKm must be positive")
        @DecimalMax(value = "100.0", message = "distanceKm is implausibly large for a nearby amenity")
        BigDecimal distanceKm,

        @Min(value = 1, message = "walkingMinutes must be positive")
        Integer walkingMinutes
) {
}
