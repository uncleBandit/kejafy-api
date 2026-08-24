package com.pitahmzalendo.kejafy.report.controller;

import com.pitahmzalendo.kejafy.config.security.KejafyOidcUser;
import com.pitahmzalendo.kejafy.listing.dto.response.ListingDetailResponse;
import com.pitahmzalendo.kejafy.report.dto.request.ReviewReportRequest;
import com.pitahmzalendo.kejafy.report.dto.response.ReportResponse;
import com.pitahmzalendo.kejafy.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/reports")
@Tag(name = "Admin: Reports", description = "Fraud/abuse report review queue")
@SecurityRequirement(name = "sessionCookie")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final ReportService reportService;

    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/pending")
    @Operation(summary = "List PENDING reports, oldest first")
    public ResponseEntity<Page<ReportResponse>> getPending(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ReportResponse> page = reportService.getPending(pageable).map(ReportResponse::from);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/flagged-listings")
    @Operation(summary = "Listings auto-flagged by the report threshold, oldest flag first")
    public ResponseEntity<Page<ListingDetailResponse>> getFlaggedListings(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ListingDetailResponse> page = reportService
                .getFlaggedListings(pageable)
                .map(ListingDetailResponse::from);
        return ResponseEntity.ok(page);
    }

    @PostMapping("/{id}/review")
    @Operation(summary = "Resolve a report as ACTION_TAKEN or DISMISSED, optionally taking the listing down")
    public ResponseEntity<ReportResponse> review(
            @PathVariable Long id,
            @AuthenticationPrincipal KejafyOidcUser principal,
            @Valid @RequestBody ReviewReportRequest request
    ) {
        var report = reportService.resolve(id, principal.getDomainUser(), request);
        return ResponseEntity.ok(ReportResponse.from(report));
    }

    @PostMapping("/listings/{listingId}/verify")
    @Operation(summary = "Mark a listing as admin-verified (e.g. after a site visit or phone check)")
    public ResponseEntity<ListingDetailResponse> verifyListing(@PathVariable Long listingId) {
        var listing = reportService.verifyListing(listingId);
        return ResponseEntity.ok(ListingDetailResponse.from(listing));
    }

    @PostMapping("/listings/{listingId}/unverify")
    @Operation(summary = "Revoke a listing's verified badge")
    public ResponseEntity<ListingDetailResponse> unverifyListing(@PathVariable Long listingId) {
        var listing = reportService.unverifyListing(listingId);
        return ResponseEntity.ok(ListingDetailResponse.from(listing));
    }
}
