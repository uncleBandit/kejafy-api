package com.pitahmzalendo.kejafy.listing.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A single nearby point of interest attached to a listing — "Greenview
 * Primary School, 0.4km away". Deliberately NOT the exact GPS pin of the
 * amenity itself, just a name + type + distance: precise enough to be
 * useful, coarse enough that it can never substitute for the listing's
 * own gated latitude/longitude (see Listing class javadoc). That's also
 * why ListingAmenity is safe to show on a LOCKED feed card — see
 * ListingFeedItemResponse, which includes amenities/environmentProfile
 * on both locked() and unlocked(): "there's a hospital 600m away" is
 * exactly the kind of teaser that should make a non-subscriber want to
 * unlock the exact pin, not something that needs gating itself.
 *
 * externalPlaceId is reserved for a future places-API enrichment job
 * (see AmenitySource.PLACES_API): the natural dedupe key against that
 * provider's own id for the place, so a re-run of the job can upsert
 * rather than create duplicate rows. Null for AGENT_ENTERED rows.
 */
@Entity
@Table(
        name = "listing_amenities",
        indexes = {
                @Index(name = "idx_listing_amenities_listing", columnList = "listing_id")
        }
)
public class ListingAmenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_listing_amenity_listing"))
    private Listing listing;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AmenityType type;

    // Agent-entered display name, e.g. "Greenview Primary School" — the
    // type alone ("PRIMARY_SCHOOL") is enough to render an icon/filter
    // on, but a real name is what makes the claim checkable/credible.
    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "distance_km", precision = 5, scale = 2)
    private java.math.BigDecimal distanceKm;

    @Column(name = "walking_minutes")
    private Integer walkingMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AmenitySource source;

    @Column(name = "external_place_id", length = 255)
    private String externalPlaceId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ListingAmenity() {
        // JPA
    }

    public ListingAmenity(AmenityType type, String name, java.math.BigDecimal distanceKm,
                          Integer walkingMinutes, AmenitySource source) {
        this.type = type;
        this.name = name;
        this.distanceKm = distanceKm;
        this.walkingMinutes = walkingMinutes;
        this.source = source;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Listing getListing() {
        return listing;
    }

    void setListing(Listing listing) {
        this.listing = listing;
    }

    public AmenityType getType() {
        return type;
    }

    public AmenityCategory getCategory() {
        return type.category();
    }

    public String getName() {
        return name;
    }

    public java.math.BigDecimal getDistanceKm() {
        return distanceKm;
    }

    public Integer getWalkingMinutes() {
        return walkingMinutes;
    }

    public AmenitySource getSource() {
        return source;
    }

    public String getExternalPlaceId() {
        return externalPlaceId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
