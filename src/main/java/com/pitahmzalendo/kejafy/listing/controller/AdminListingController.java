package com.pitahmzalendo.kejafy.listing.controller;

import com.pitahmzalendo.kejafy.config.security.KejafyOidcUser;
import com.pitahmzalendo.kejafy.listing.dto.request.ListingReviewRequest;
import com.pitahmzalendo.kejafy.listing.dto.request.UpdateEnvironmentProfileRequest;
import com.pitahmzalendo.kejafy.listing.dto.response.ListingDetailResponse;
import com.pitahmzalendo.kejafy.listing.service.ListingService;
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
@RequestMapping("/api/v1/admin/listings")
@Tag(name = "Admin: Listings", description = "Moderation queue for listings pending review")
@SecurityRequirement(name = "sessionCookie")
@PreAuthorize("hasRole('ADMIN')")
public class AdminListingController {

    private final ListingService listingService;

    public AdminListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping("/pending")
    @Operation(summary = "List PENDING_REVIEW listings, oldest submission first")
    public ResponseEntity<Page<ListingDetailResponse>> getPending(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ListingDetailResponse> page = listingService
                .getPendingReview(pageable)
                .map(ListingDetailResponse::from);
        return ResponseEntity.ok(page);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve and publish a listing")
    public ResponseEntity<ListingDetailResponse> approve(@PathVariable Long id) {
        var listing = listingService.approve(id);
        return ResponseEntity.ok(ListingDetailResponse.from(listing));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a listing, optionally with a reason shown to the agent")
    public ResponseEntity<ListingDetailResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ListingReviewRequest request
    ) {
        String reason = request != null ? request.reason() : null;
        var listing = listingService.reject(id, reason);
        return ResponseEntity.ok(ListingDetailResponse.from(listing));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Force-unpublish a live listing (e.g. reported / policy violation)")
    public ResponseEntity<ListingDetailResponse> archive(@PathVariable Long id) {
        var listing = listingService.archiveAsAdmin(id);
        return ResponseEntity.ok(ListingDetailResponse.from(listing));
    }

    @PutMapping("/{id}/environment-profile")
    @Operation(
            summary = "Override/confirm a listing's environment scores, even while it's live",
            description = "Unlike the agent-side endpoint, this isn't restricted to DRAFT/REJECTED — an "
                    + "admin can correct a misleading score (e.g. after a report) without needing the "
                    + "listing unpublished first. Recorded with source=ADMIN_ASSESSED."
    )
    public ResponseEntity<ListingDetailResponse> updateEnvironmentProfile(
            @PathVariable Long id,
            @AuthenticationPrincipal KejafyOidcUser principal,
            @Valid @RequestBody UpdateEnvironmentProfileRequest request
    ) {
        var listing = listingService.updateEnvironmentProfile(id, principal.getDomainUser(), request);
        return ResponseEntity.ok(ListingDetailResponse.from(listing));
    }
}