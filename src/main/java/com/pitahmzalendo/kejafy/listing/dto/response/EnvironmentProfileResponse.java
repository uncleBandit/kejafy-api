package com.pitahmzalendo.kejafy.listing.dto.response;

import com.pitahmzalendo.kejafy.listing.entity.EnvironmentProfile;
import com.pitahmzalendo.kejafy.listing.entity.EnvironmentSource;

import java.time.LocalDateTime;

/**
 * A listing's "what's it like to live here" scores, 1–5 each. Not every
 * field points the same way (see EnvironmentProfile):
 * <ul>
 *   <li>noiseLevel, floodRiskLevel: higher = worse.</li>
 *   <li>roadAccessQuality, securityLevel, waterReliability,
 *       powerReliability: higher = better.</li>
 * </ul>
 * A null score means "not assessed", never a default value.
 */
public record EnvironmentProfileResponse(
        Integer noiseLevel,
        Integer floodRiskLevel,
        Integer roadAccessQuality,
        Integer securityLevel,
        Integer waterReliability,
        Integer powerReliability,
        EnvironmentSource source,
        LocalDateTime assessedAt
) {
    /**
     * Returns null when the listing hasn't been assessed yet, so the
     * client gets {@code "environmentProfile": null} rather than an object
     * full of nulls. Hibernate also hands back a null embeddable when
     * every one of its columns is null, so both cases end up here.
     */
    public static EnvironmentProfileResponse from(EnvironmentProfile profile) {
        if (profile == null || profile.isEmpty()) {
            return null;
        }
        return new EnvironmentProfileResponse(
                profile.getNoiseLevel(),
                profile.getFloodRiskLevel(),
                profile.getRoadAccessQuality(),
                profile.getSecurityLevel(),
                profile.getWaterReliability(),
                profile.getPowerReliability(),
                profile.getSource(),
                profile.getAssessedAt()
        );
    }
}
