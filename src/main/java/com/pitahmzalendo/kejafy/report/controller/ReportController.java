package com.pitahmzalendo.kejafy.report.controller;

import com.pitahmzalendo.kejafy.config.security.KejafyOidcUser;
import com.pitahmzalendo.kejafy.report.dto.request.CreateReportRequest;
import com.pitahmzalendo.kejafy.report.dto.response.ReportResponse;
import com.pitahmzalendo.kejafy.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Any authenticated user (customer, agent, admin) can file a report —
 * no subscription required, since letting a fraud report sit behind a
 * paywall would undermine the whole point of the trust layer. See
 * ReportService for the auto-flag threshold and duplicate-report guard.
 */
@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Fraud/abuse reports against listings and agents")
@SecurityRequirement(name = "sessionCookie")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/listings/{listingId}")
    @Operation(summary = "Report a listing (fake, already taken, misleading, payment scam, etc)")
    public ResponseEntity<ReportResponse> reportListing(
            @PathVariable Long listingId,
            @AuthenticationPrincipal KejafyOidcUser principal,
            @Valid @RequestBody CreateReportRequest request
    ) {
        var report = reportService.reportListing(principal.getDomainUser(), listingId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReportResponse.from(report));
    }

    @PostMapping("/agents/{agentId}")
    @Operation(summary = "Report an agent (impersonation, harassment, scam behaviour, etc)")
    public ResponseEntity<ReportResponse> reportAgent(
            @PathVariable Long agentId,
            @AuthenticationPrincipal KejafyOidcUser principal,
            @Valid @RequestBody CreateReportRequest request
    ) {
        var report = reportService.reportAgent(principal.getDomainUser(), agentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReportResponse.from(report));
    }

    @GetMapping("/mine")
    @Operation(summary = "Reports the caller has filed, newest first")
    public ResponseEntity<Page<ReportResponse>> getMine(
            @AuthenticationPrincipal KejafyOidcUser principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ReportResponse> page = reportService
                .getMyReports(principal.getDomainUser(), pageable)
                .map(ReportResponse::from);
        return ResponseEntity.ok(page);
    }
}
