package com.pitahmzalendo.kejafy.report.dto.request;

import com.pitahmzalendo.kejafy.report.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * POST /api/v1/admin/reports/{id}/review. status must be ACTION_TAKEN or
 * DISMISSED — ReportService rejects PENDING here, since "review" only
 * makes sense as a terminal decision, not a way to reset one.
 *
 * takeDownListing lets an admin resolve a LISTING report and archive the
 * listing in the same request, instead of resolving here then hopping
 * to AdminListingController separately. Ignored (and should be false)
 * for AGENT reports — ReportService validates that combination.
 */
public record ReviewReportRequest(
        @NotNull(message = "status is required")
        ReportStatus status,

        @Size(max = 1000, message = "resolutionNotes is too long")
        String resolutionNotes,

        boolean takeDownListing
) {
}
