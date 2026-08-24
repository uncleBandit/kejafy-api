package com.pitahmzalendo.kejafy.report.dto.response;

import com.pitahmzalendo.kejafy.report.entity.Report;
import com.pitahmzalendo.kejafy.report.entity.ReportReason;
import com.pitahmzalendo.kejafy.report.entity.ReportStatus;
import com.pitahmzalendo.kejafy.report.entity.ReportTargetType;

import java.time.LocalDateTime;

public record ReportResponse(
        Long id,
        ReportTargetType targetType,
        Long reportedListingId,
        String reportedListingTitle,
        Long reportedAgentId,
        String reportedAgentName,
        Long reporterId,
        String reporterName,
        ReportReason reason,
        String details,
        ReportStatus status,
        String resolutionNotes,
        Long reviewedById,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {
    public static ReportResponse from(Report report) {
        var listing = report.getReportedListing();
        var agent = report.getReportedAgent();
        var reviewedBy = report.getReviewedBy();

        return new ReportResponse(
                report.getId(),
                report.getTargetType(),
                listing != null ? listing.getId() : null,
                listing != null ? listing.getTitle() : null,
                agent != null ? agent.getId() : null,
                agent != null ? agent.getFirstName() + " " + agent.getLastName() : null,
                report.getReporter().getId(),
                report.getReporter().getFirstName() + " " + report.getReporter().getLastName(),
                report.getReason(),
                report.getDetails(),
                report.getStatus(),
                report.getResolutionNotes(),
                reviewedBy != null ? reviewedBy.getId() : null,
                report.getCreatedAt(),
                report.getReviewedAt()
        );
    }
}
