package com.pitahmzalendo.kejafy.listing.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Partial update, same PATCH semantics as UpdateListingRequest — null
 * fields are left unchanged, so an agent can set just floodRiskLevel
 * today and fill in the rest later. See EnvironmentProfile for what
 * each 1–5 score means and which direction is "good".
 */
public record UpdateEnvironmentProfileRequest(
        @Min(value = 1, message = "noiseLevel must be between 1 and 5")
        @Max(value = 5, message = "noiseLevel must be between 1 and 5")
        Integer noiseLevel,

        @Min(value = 1, message = "floodRiskLevel must be between 1 and 5")
        @Max(value = 5, message = "floodRiskLevel must be between 1 and 5")
        Integer floodRiskLevel,

        @Min(value = 1, message = "roadAccessQuality must be between 1 and 5")
        @Max(value = 5, message = "roadAccessQuality must be between 1 and 5")
        Integer roadAccessQuality,

        @Min(value = 1, message = "securityLevel must be between 1 and 5")
        @Max(value = 5, message = "securityLevel must be between 1 and 5")
        Integer securityLevel,

        @Min(value = 1, message = "waterReliability must be between 1 and 5")
        @Max(value = 5, message = "waterReliability must be between 1 and 5")
        Integer waterReliability,

        @Min(value = 1, message = "powerReliability must be between 1 and 5")
        @Max(value = 5, message = "powerReliability must be between 1 and 5")
        Integer powerReliability
) {
}
