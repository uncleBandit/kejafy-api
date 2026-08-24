package com.pitahmzalendo.kejafy.report.dto.request;

import com.pitahmzalendo.kejafy.report.entity.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body for both POST /api/v1/reports/listings/{id} and
 * /api/v1/reports/agents/{id} — the target itself comes from the path,
 * not this payload, so the same shape covers both without a targetType
 * field a client could get out of sync with the URL it's calling.
 */
public record CreateReportRequest(
        @NotNull(message = "reason is required")
        ReportReason reason,

        @Size(max = 1000, message = "details is too long")
        String details
) {
}
