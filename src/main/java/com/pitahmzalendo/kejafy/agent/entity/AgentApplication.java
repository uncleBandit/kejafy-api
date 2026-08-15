package com.pitahmzalendo.kejafy.agent.entity;


import com.pitahmzalendo.kejafy.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A CUSTOMER's request to become an AGENT, gated on admin-reviewed KYC.
 * A user can have more than one row here over time (e.g. rejected, fixes
 * their documents, re-applies) so this is a history, not a 1:1 profile —
 * "the current application" is whichever PENDING/APPROVED row is most
 * recent, which is what the repository queries expose.
 */
@Entity
@Table(
        name = "agent_applications",
        indexes = {
                @Index(name = "idx_agent_app_applicant", columnList = "applicant_id"),
                @Index(name = "idx_agent_app_status", columnList = "status")
        }
)
public class AgentApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_agent_app_applicant"))
    private User applicant;

    // National ID / passport number as typed by the applicant. Stored
    // alongside the document image so admins can cross-check it without
    // opening the file, and so it's searchable if the same document is
    // reused for a duplicate/fraudulent application.
    @Column(name = "id_number", nullable = false, length = 50)
    private String idNumber;

    // Object storage URLs (S3/Cloudinary/etc) — client uploads the image
    // separately and sends back the resulting URL, same pattern as
    // User.avatarUrl.
    @Column(name = "id_document_front_url", nullable = false, length = 2048)
    private String idDocumentFrontUrl;

    @Column(name = "id_document_back_url", length = 2048)
    private String idDocumentBackUrl;

    // Optional selfie for basic liveness / face-match against the ID —
    // not enforced at the schema level since not every KYC flow needs it,
    // but leaving room for it avoids a migration later.
    @Column(name = "selfie_url", length = 2048)
    private String selfieUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AgentApplicationStatus status = AgentApplicationStatus.PENDING;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by", foreignKey = @ForeignKey(name = "fk_agent_app_reviewer"))
    private User reviewedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    protected AgentApplication() {
        // JPA
    }

    public AgentApplication(User applicant, String idNumber, String idDocumentFrontUrl,
                            String idDocumentBackUrl, String selfieUrl) {
        this.applicant = applicant;
        this.idNumber = idNumber;
        this.idDocumentFrontUrl = idDocumentFrontUrl;
        this.idDocumentBackUrl = idDocumentBackUrl;
        this.selfieUrl = selfieUrl;
    }

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
    }

    public void approve(User admin) {
        this.status = AgentApplicationStatus.APPROVED;
        this.reviewedBy = admin;
        this.reviewedAt = LocalDateTime.now();
        this.rejectionReason = null;
    }

    public void reject(User admin, String reason) {
        this.status = AgentApplicationStatus.REJECTED;
        this.reviewedBy = admin;
        this.reviewedAt = LocalDateTime.now();
        this.rejectionReason = reason;
    }

    public boolean isPending() {
        return status == AgentApplicationStatus.PENDING;
    }

    public Long getId() {
        return id;
    }

    public User getApplicant() {
        return applicant;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public String getIdDocumentFrontUrl() {
        return idDocumentFrontUrl;
    }

    public String getIdDocumentBackUrl() {
        return idDocumentBackUrl;
    }

    public String getSelfieUrl() {
        return selfieUrl;
    }

    public AgentApplicationStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }
}