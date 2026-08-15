package com.pitahmzalendo.kejafy.listing.controller;

import com.pitahmzalendo.kejafy.config.security.KejafyOidcUser;
import com.pitahmzalendo.kejafy.listing.dto.request.AddListingVideoRequest;
import com.pitahmzalendo.kejafy.listing.dto.request.CreateListingRequest;
import com.pitahmzalendo.kejafy.listing.dto.request.UpdateListingRequest;
import com.pitahmzalendo.kejafy.listing.dto.response.ListingDetailResponse;
import com.pitahmzalendo.kejafy.listing.service.ListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Everything here operates on the caller's own listings — an agent
 * managing their inventory, not the public-facing feed (see
 * ListingFeedController for that). @PreAuthorize hasRole('AGENT') means
 * a plain CUSTOMER can't reach any of this even before ownership checks
 * in ListingService kick in.
 */
@RestController
@RequestMapping("/api/v1/agent/listings")
@Tag(name = "Agent Listings", description = "Agent-side listing management")
@SecurityRequirement(name = "sessionCookie")
@PreAuthorize("hasRole('AGENT')")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @PostMapping
    @Operation(summary = "Create a new listing (starts as DRAFT)")
    public ResponseEntity<ListingDetailResponse> create(
            @AuthenticationPrincipal KejafyOidcUser principal,
            @Valid @RequestBody CreateListingRequest request
    ) {
        var listing = listingService.createDraft(principal.getDomainUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ListingDetailResponse.from(listing));
    }

    @GetMapping
    @Operation(summary = "List the caller's own listings, any status")
    public ResponseEntity<Page<ListingDetailResponse>> getMine(
            @AuthenticationPrincipal KejafyOidcUser principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ListingDetailResponse> page = listingService
                .getMyListings(principal.getDomainUser(), pageable)
                .map(ListingDetailResponse::from);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of the caller's own listings")
    public ResponseEntity<ListingDetailResponse> getOne(
            @PathVariable Long id,
            @AuthenticationPrincipal KejafyOidcUser principal
    ) {
        var listing = listingService.getOwned(id, principal.getDomainUser());
        return ResponseEntity.ok(ListingDetailResponse.from(listing));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Edit a listing — only while DRAFT or REJECTED")
    public ResponseEntity<ListingDetailResponse> update(
            @PathVariable Long id,
            @AuthenticationPrincipal KejafyOidcUser principal,
            @Valid @RequestBody UpdateListingRequest request
    ) {
        var listing = listingService.updateDraft(id, principal.getDomainUser(), request);
        return ResponseEntity.ok(ListingDetailResponse.from(listing));
    }

    @PostMapping("/{id}/videos")
    @Operation(summary = "Attach a video to a listing (client uploads the file first, sends back the URL)")
    public ResponseEntity<ListingDetailResponse> addVideo(
            @PathVariable Long id,
            @AuthenticationPrincipal KejafyOidcUser principal,
            @Valid @RequestBody AddListingVideoRequest request
    ) {
        var listing = listingService.addVideo(id, principal.getDomainUser(), request);
        return ResponseEntity.ok(ListingDetailResponse.from(listing));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit for admin review — moves the listing to PENDING_REVIEW")
    public ResponseEntity<ListingDetailResponse> submit(
            @PathVariable Long id,
            @AuthenticationPrincipal KejafyOidcUser principal
    ) {
        var listing = listingService.submitForReview(id, principal.getDomainUser());
        return ResponseEntity.ok(ListingDetailResponse.from(listing));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Take a listing down (e.g. property let/sold)")
    public ResponseEntity<ListingDetailResponse> archive(
            @PathVariable Long id,
            @AuthenticationPrincipal KejafyOidcUser principal
    ) {
        var listing = listingService.archiveOwn(id, principal.getDomainUser());
        return ResponseEntity.ok(ListingDetailResponse.from(listing));
    }
}
