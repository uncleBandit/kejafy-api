package com.pitahmzalendo.kejafy.listing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDateTime;

/**
 * Structured "what's it like to actually live here" score, embedded
 * directly on {@link Listing} (flat columns, same pattern as bedrooms/
 * bathrooms) rather than a separate table — there's exactly one profile
 * per listing and every field needs to be filterable/sortable in
 * ListingRepository.search without a join, the same reason bedrooms and
 * bathrooms aren't their own table.
 *
 * Every score is 1–5, but the fields are NOT all oriented the same way —
 * this is deliberate rather than an oversight, so read each field's
 * javadoc rather than assuming "5 is always good":
 * <ul>
 *   <li>{@code *Level}/{@code *Risk} fields (noiseLevel, floodRiskLevel):
 *       higher = worse.</li>
 *   <li>{@code *Quality}/{@code *Reliability} fields (roadAccessQuality,
 *       securityLevel, waterReliability, powerReliability): higher = better.</li>
 * </ul>
 * Do not average these into a single "environment score" without
 * inverting the risk-oriented fields first (e.g. {@code 6 - noiseLevel})
 * — averaging them as-is silently rewards high noise and flood risk.
 *
 * A listing with no scores set yet has every field null rather than
 * defaulting to a value — null must mean "not assessed", not "assessed
 * as 0/average", since the two are very different claims to show a
 * prospective tenant. {@link #isEmpty()} is how callers tell an
 * unassessed profile apart from one worth serializing.
 */
@Embeddable
public class EnvironmentProfile {

    /** Ambient noise near the property. 1 = very quiet, 5 = very noisy. */
    @Column(name = "env_noise_level")
    private Integer noiseLevel;

    /** Flood exposure. 1 = none observed, 5 = severe/recurring. */
    @Column(name = "env_flood_risk_level")
    private Integer floodRiskLevel;

    /** Road surface and access into the property. 1 = poor (e.g. impassable in rain), 5 = excellent (tarmac, well maintained). */
    @Column(name = "env_road_access_quality")
    private Integer roadAccessQuality;

    /** General safety of the area. 1 = poor, 5 = excellent. */
    @Column(name = "env_security_level")
    private Integer securityLevel;

    /** Consistency of piped/borehole water supply. 1 = poor (frequent shortages), 5 = excellent (always available). */
    @Column(name = "env_water_reliability")
    private Integer waterReliability;

    /** Consistency of electricity supply. 1 = poor (frequent blackouts), 5 = excellent (rarely out). */
    @Column(name = "env_power_reliability")
    private Integer powerReliability;

    @Enumerated(EnumType.STRING)
    @Column(name = "env_source", length = 30)
    private EnvironmentSource source;

    @Column(name = "env_assessed_at")
    private LocalDateTime assessedAt;

    public EnvironmentProfile() {
        // JPA
    }

    /** True when nothing has been assessed yet — every score is null. */
    public boolean isEmpty() {
        return noiseLevel == null && floodRiskLevel == null && roadAccessQuality == null
                && securityLevel == null && waterReliability == null && powerReliability == null;
    }

    /**
     * Applies non-null fields from a partial update, same PATCH semantics
     * as ListingService.updateDraft, and stamps source/assessedAt to
     * reflect who just touched it and when.
     */
    public void applyUpdate(Integer noiseLevel, Integer floodRiskLevel, Integer roadAccessQuality,
                            Integer securityLevel, Integer waterReliability, Integer powerReliability,
                            EnvironmentSource source) {
        if (noiseLevel != null) this.noiseLevel = noiseLevel;
        if (floodRiskLevel != null) this.floodRiskLevel = floodRiskLevel;
        if (roadAccessQuality != null) this.roadAccessQuality = roadAccessQuality;
        if (securityLevel != null) this.securityLevel = securityLevel;
        if (waterReliability != null) this.waterReliability = waterReliability;
        if (powerReliability != null) this.powerReliability = powerReliability;
        this.source = source;
        this.assessedAt = LocalDateTime.now();
    }

    public Integer getNoiseLevel() {
        return noiseLevel;
    }

    public Integer getFloodRiskLevel() {
        return floodRiskLevel;
    }

    public Integer getRoadAccessQuality() {
        return roadAccessQuality;
    }

    public Integer getSecurityLevel() {
        return securityLevel;
    }

    public Integer getWaterReliability() {
        return waterReliability;
    }

    public Integer getPowerReliability() {
        return powerReliability;
    }

    public EnvironmentSource getSource() {
        return source;
    }

    public LocalDateTime getAssessedAt() {
        return assessedAt;
    }
}
