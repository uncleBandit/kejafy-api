package com.pitahmzalendo.kejafy.listing.controller;


import com.pitahmzalendo.kejafy.config.security.KejafyOidcUser;
import com.pitahmzalendo.kejafy.listing.service.ListingLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/listings/{listingId}/like")
@Tag(name = "Listing Likes", description = "Double-tap like — available regardless of subscription status")
@SecurityRequirement(name = "sessionCookie")
public class ListingLikeController {

    private final ListingLikeService listingLikeService;

    public ListingLikeController(ListingLikeService listingLikeService) {
        this.listingLikeService = listingLikeService;
    }

    @PostMapping
    @Operation(summary = "Like a listing (idempotent — repeat taps are a no-op)")
    public ResponseEntity<Void> like(
            @PathVariable Long listingId,
            @AuthenticationPrincipal KejafyOidcUser principal
    ) {
        listingLikeService.like(listingId, principal.getDomainUser());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Remove a like")
    public ResponseEntity<Void> unlike(
            @PathVariable Long listingId,
            @AuthenticationPrincipal KejafyOidcUser principal
    ) {
        listingLikeService.unlike(listingId, principal.getDomainUser());
        return ResponseEntity.noContent().build();
    }
}
