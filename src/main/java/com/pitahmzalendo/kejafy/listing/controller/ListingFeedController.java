package com.pitahmzalendo.kejafy.listing.controller;

import com.pitahmzalendo.kejafy.config.security.KejafyOidcUser;
import com.pitahmzalendo.kejafy.listing.dto.response.ListingFeedItemResponse;
import com.pitahmzalendo.kejafy.listing.service.ListingFeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The public, video-first feed every logged-in user (any role) can
 * scroll — subscription status decides whether each card is locked
 * (videos + like only) or unlocked (full detail + exact location). See
 * ListingFeedItemResponse / ListingFeedService for the actual gate.
 */
@RestController
@RequestMapping("/api/v1/feed")
@Tag(name = "Listing Feed", description = "Public, subscription-gated listing feed")
@SecurityRequirement(name = "sessionCookie")
public class ListingFeedController {

    private final ListingFeedService listingFeedService;

    public ListingFeedController(ListingFeedService listingFeedService) {
        this.listingFeedService = listingFeedService;
    }

    @GetMapping
    @Operation(summary = "Scroll the published-listing feed, newest first")
    public ResponseEntity<Page<ListingFeedItemResponse>> getFeed(
            @AuthenticationPrincipal KejafyOidcUser principal,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        var page = listingFeedService.getFeed(principal.getDomainUser(), pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Open a single published listing")
    public ResponseEntity<ListingFeedItemResponse> getOne(
            @PathVariable Long id,
            @AuthenticationPrincipal KejafyOidcUser principal
    ) {
        var item = listingFeedService.getListing(id, principal.getDomainUser());
        return ResponseEntity.ok(item);
    }
}
