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

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0;

    @Column(name = "like_count", nullable = false)
    private long likeCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime submittedAt;

    private LocalDateTime publishedAt;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<ListingVideo> videos = new ArrayList<>();

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
}
