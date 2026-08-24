package com.pitahmzalendo.kejafy.report.entity;

import com.pitahmzalendo.kejafy.listing.entity.Listing;
import com.pitahmzalendo.kejafy.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A user-filed fraud/abuse report against either a Listing or an AGENT
 * (never both — see {@link #forListing} / {@link #forAgent}, the only
 * two ways to construct one). This is the fraud-reporting half of the
 * trust layer; Listing.verified/verifiedAt is the other half (admin
 * confirms a listing is real, independent of anyone reporting it).
 *
 * Exactly one of reportedListing/reportedAgent is set — enforced by the
 * private constructor only being reachable through the two named
 * factories below, same pattern ListingFeedItemResponse uses for its
 * locked/unlocked split, so a future edit can't accidentally construct
 * a Report with both or neither target set.
 */
@Entity
@Table(
        name = "reports",
        indexes = {
                @Index(name = "idx_reports_listing", columnList = "reported_listing_id"),
                @Index(name = "idx_reports_agent", columnList = "reported_agent_id"),
                @Index(name = "idx_reports_status", columnList = "status"),
                @Index(name = "idx_reports_reporter", columnList = "reporter_id")
        }
)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_report_reporter"))
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ReportTargetType targetType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_listing_id",
            foreignKey = @ForeignKey(name = "fk_report_listing"))
    private Listing reportedListing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_agent_id",
            foreignKey = @ForeignKey(name = "fk_report_agent"))
    private User reportedAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ReportReason reason;

    @Column(length = 1000)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id",
            foreignKey = @ForeignKey(name = "fk_report_reviewer"))
    private User reviewedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    protected Report() {
        // JPA
    }

    private Report(User reporter, ReportTargetType targetType, Listing reportedListing,
                    User reportedAgent, ReportReason reason, String details) {
        this.reporter = reporter;
        this.targetType = targetType;
        this.reportedListing = reportedListing;
        this.reportedAgent = reportedAgent;
        this.reason = reason;
        this.details = details;
    }

    public static Report forListing(User reporter, Listing listing, ReportReason reason, String details) {
        return new Report(reporter, ReportTargetType.LISTING, listing, null, reason, details);
    }

    public static Report forAgent(User reporter, User agent, ReportReason reason, String details) {
        return new Report(reporter, ReportTargetType.AGENT, null, agent, reason, details);
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void resolve(User admin, ReportStatus resolution, String notes) {
        if (resolution == ReportStatus.PENDING) {
            throw new IllegalArgumentException("Cannot resolve a report back to PENDING.");
        }
        this.status = resolution;
        this.resolutionNotes = notes;
        this.reviewedBy = admin;
        this.reviewedAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return status == ReportStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public User getReporter() {
        return reporter;
    }

    public ReportTargetType getTargetType() {
        return targetType;
    }

    public Listing getReportedListing() {
        return reportedListing;
    }

    public User getReportedAgent() {
        return reportedAgent;
    }

    public ReportReason getReason() {
        return reason;
    }

    public String getDetails() {
        return details;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }
}
