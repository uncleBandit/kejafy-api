package com.pitahmzalendo.kejafy.listing.entity;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "listing_videos",
        indexes = {
                @Index(name = "idx_listing_videos_listing", columnList = "listing_id")
        }
)
public class ListingVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_listing_video_listing"))
    private Listing listing;

    // Client uploads to object storage / a video processing pipeline
    // first and sends back the resulting URL — same upload-then-reference
    // pattern used everywhere else (avatarUrl, KYC documents). This is
    // expected to be a processed/streamable URL (e.g. HLS manifest or an
    // MP4 transcode), not the raw upload.
    @Column(name = "video_url", nullable = false, length = 2048)
    private String videoUrl;

    @Column(name = "thumbnail_url", length = 2048)
    private String thumbnailUrl;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    // Order videos play in within a listing (agent-controlled, e.g.
    // exterior first, then rooms). Lower = earlier.
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ListingVideo() {
        // JPA
    }

    public ListingVideo(String videoUrl, String thumbnailUrl, Integer durationSeconds, int displayOrder) {
        this.videoUrl = videoUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.durationSeconds = durationSeconds;
        this.displayOrder = displayOrder;
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

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}