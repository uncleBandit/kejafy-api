package com.pitahmzalendo.kejafy.listing.entity;


import com.pitahmzalendo.kejafy.user.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A property listing owned by an AGENT. Location is split deliberately:
 * {@code neighbourhood} is a coarse, human-written area name (e.g.
 * "Kilimani, Nairobi") that's shown to everyone as a teaser, while
 * {@code latitude}/{@code longitude} is the exact GPS pin — the paid
 * selling point — and must only ever be exposed by the feed layer to
 * users with an active subscription. See ListingFeedService; don't add
 * a getter/DTO path that leaks lat/lng around that gate.
 */
@Entity
@Table(
        name = "listings",
        indexes = {
                @Index(name = "idx_listings_agent", columnList = "agent_id"),
                @Index(name = "idx_listings_status", columnList = "status")
        }
)
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_listing_agent"))
    private User agent;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PropertyType propertyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ListingPurpose purpose;

    @Column(name = "price_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal priceAmount;

    @Column(name = "price_currency", nullable = false, length = 3)
    private String priceCurrency = "KES";

    private Integer bedrooms;

    private Integer bathrooms;

    // Approximate area/neighbourhood, e.g. "Kilimani, Nairobi" — free to
    // show to non-subscribers. Never derive this from lat/lng on the fly
    // in a way that could leak precision; it's an agent-entered string.
    @Column(nullable = false, length = 150)
    private String neighbourhood;

    // Exact GPS pin — the paid selling point. See class javadoc: gate this
    // behind SubscriptionService.hasActiveSubscription at the read side,
    // never return it from a public/unauthenticated endpoint.
    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ListingStatus status = ListingStatus.DRAFT;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    // Admin-confirmed trust signal, separate from `status`: PUBLISHED
    // only means "passed content moderation", not that anyone checked
    // the property is real. See ReportService/AdminReportController —
    // admins toggle this after some out-of-band confirmation (site
    // visit, phone verification, etc). Shown as a badge on unlocked
    // feed cards; never implied by status alone.
    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    private LocalDateTime verifiedAt;

    // Fraud-reporting trust gate: set by ReportService once a listing's
    // pending report count crosses ReportService.AUTO_FLAG_THRESHOLD.
    // A flagged listing is pulled out of the public feed (see
    // ListingRepository.findByStatus...FlaggedForReviewFalse... and the
    // native search query) even though `status` is still PUBLISHED —
    // this way an admin's later decision (dismiss vs take down) doesn't
    // need to guess or reconstruct what status the listing had before.
    @Column(name = "flagged_for_review", nullable = false)
    private boolean flaggedForReview = false;

    private LocalDateTime flaggedAt;

    @Column(name = "report_count", nullable = false)
    private long reportCount = 0;

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0;

    @Column(name = "like_count", nullable = false)
    private long likeCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime submittedAt;

    private LocalDateTime publishedAt;

    @Column(name = "comment_count", nullable = false)
    private long commentCount = 0;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<ListingVideo> videos = new ArrayList<>();

    // Nearby points of interest (schools, hospitals, matatu stage, etc).
    // Same cascade/orphanRemoval pattern as videos — deleting the
    // listing deletes its amenity claims, and removing one from this
    // list (see removeAmenity) deletes the orphaned row rather than
    // leaving it dangling. Shown on both locked and unlocked feed
    // cards — see ListingAmenity class javadoc for why that's safe.
    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("distanceKm ASC")
    private List<ListingAmenity> amenities = new ArrayList<>();

    // "What's it like to actually live here" — noise, flood risk, road
    // access, security, utility reliability. Embedded rather than its
    // own table (see EnvironmentProfile javadoc); starts empty
    // (isEmpty() true) until an agent or admin fills it in via
    // ListingService.updateEnvironmentProfile. Shown unlocked on the
    // feed for the same reason amenities are — it's a decision factor,
    // not the paid selling point.
    @Embedded
    private EnvironmentProfile environmentProfile = new EnvironmentProfile();

    protected Listing() {
        // JPA
    }

    public Listing(User agent, String title, String description, PropertyType propertyType,
                   ListingPurpose purpose, BigDecimal priceAmount, Integer bedrooms, Integer bathrooms,
                   String neighbourhood, BigDecimal latitude, BigDecimal longitude) {
        this.agent = agent;
        this.title = title;
        this.description = description;
        this.propertyType = propertyType;
        this.purpose = purpose;
        this.priceAmount = priceAmount;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.neighbourhood = neighbourhood;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addVideo(ListingVideo video) {
        videos.add(video);
        video.setListing(this);
    }

    public void addAmenity(ListingAmenity amenity) {
        amenities.add(amenity);
        amenity.setListing(this);
    }

    /**
     * @return true if an amenity with this id was found (and removed)
     * on this listing — false lets the caller distinguish "not found"
     * from "found and removed" without a separate lookup.
     */
    public boolean removeAmenity(Long amenityId) {
        return amenities.removeIf(a -> a.getId() != null && a.getId().equals(amenityId));
    }

    public void submitForReview() {
        this.status = ListingStatus.PENDING_REVIEW;
        this.submittedAt = LocalDateTime.now();
    }

    public void publish() {
        this.status = ListingStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        this.rejectionReason = null;
    }

    public void reject(String reason) {
        this.status = ListingStatus.REJECTED;
        this.rejectionReason = reason;
    }

    public void archive() {
        this.status = ListingStatus.ARCHIVED;
    }

    public void verify() {
        this.verified = true;
        this.verifiedAt = LocalDateTime.now();
    }

    public void unverify() {
        this.verified = false;
        this.verifiedAt = null;
    }

    /** Pulls the listing out of the public feed without touching `status`. See field javadoc. */
    public void flagForReview() {
        this.flaggedForReview = true;
        this.flaggedAt = LocalDateTime.now();
    }

    public void clearFlag() {
        this.flaggedForReview = false;
        this.flaggedAt = null;
    }

    public void incrementReportCount() {
        this.reportCount++;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public boolean isOwnedBy(User user) {
        return agent != null && agent.getId() != null && agent.getId().equals(user.getId());
    }

    public Long getId() {
        return id;
    }

    public User getAgent() {
        return agent;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PropertyType getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(PropertyType propertyType) {
        this.propertyType = propertyType;
    }

    public ListingPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(ListingPurpose purpose) {
        this.purpose = purpose;
    }

    public BigDecimal getPriceAmount() {
        return priceAmount;
    }

    public void setPriceAmount(BigDecimal priceAmount) {
        this.priceAmount = priceAmount;
    }

    public String getPriceCurrency() {
        return priceCurrency;
    }

    public void setPriceCurrency(String priceCurrency) {
        this.priceCurrency = priceCurrency;
    }

    public Integer getBedrooms() {
        return bedrooms;
    }

    public void setBedrooms(Integer bedrooms) {
        this.bedrooms = bedrooms;
    }

    public Integer getBathrooms() {
        return bathrooms;
    }

    public void setBathrooms(Integer bathrooms) {
        this.bathrooms = bathrooms;
    }

    public String getNeighbourhood() {
        return neighbourhood;
    }

    public void setNeighbourhood(String neighbourhood) {
        this.neighbourhood = neighbourhood;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public boolean isVerified() {
        return verified;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public boolean isFlaggedForReview() {
        return flaggedForReview;
    }

    public LocalDateTime getFlaggedAt() {
        return flaggedAt;
    }

    public long getReportCount() {
        return reportCount;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public long getViewCount() {
        return viewCount;
    }

    public long getLikeCount() {
        return likeCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public List<ListingVideo> getVideos() {
        return videos;
    }

    public List<ListingAmenity> getAmenities() {
        return amenities;
    }

    public EnvironmentProfile getEnvironmentProfile() {
        return environmentProfile;
    }

    public long getCommentCount() {
        return commentCount;
    }

    public void incrementCommentCount() {
        this.commentCount++;
    }

    public void decrementCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }


}