package com.pitahmzalendo.kejafy.listing.controller;

import com.pitahmzalendo.kejafy.config.security.KejafyOidcUser;
import com.pitahmzalendo.kejafy.listing.dto.request.ListingSearchFilter;
import com.pitahmzalendo.kejafy.listing.dto.response.ListingFeedItemResponse;
import com.pitahmzalendo.kejafy.listing.entity.ListingPurpose;
import com.pitahmzalendo.kejafy.listing.entity.ListingSearchSort;
import com.pitahmzalendo.kejafy.listing.entity.PropertyType;
import com.pitahmzalendo.kejafy.listing.service.ListingFeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

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
@Validated
public class ListingFeedController {

    // A radius search this wide stops being "find a house near me" and
    // starts being "return the whole city" — capped mainly so a mistyped
    // or malicious radiusKm can't turn the WHERE-clause distance filter
    // into a full table scan across every published listing.
    private static final int MAX_RADIUS_KM = 100;

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

    @GetMapping("/search")
    @Operation(summary = "Search the published feed by area/radius, price, bedrooms, property type and purpose",
            description = "lat+lng+radiusKm together scope results to a circle around a point (e.g. a map "
                    + "pin or the center of a neighbourhood) — radiusKm without both lat and lng, or "
                    + "sort=DISTANCE without both, is rejected with 400. All other filters are optional "
                    + "and combine with AND; omit a param to not filter on it.")
    public ResponseEntity<Page<ListingFeedItemResponse>> search(
            @AuthenticationPrincipal KejafyOidcUser principal,
            @Parameter(description = "Search center latitude, e.g. from a map pin")
            @RequestParam(required = false) @DecimalMin("-90") @DecimalMax("90") Double lat,
            @Parameter(description = "Search center longitude")
            @RequestParam(required = false) @DecimalMin("-180") @DecimalMax("180") Double lng,
            @Parameter(description = "Radius in kilometres around lat/lng; requires both to be set. Capped at " + MAX_RADIUS_KM + "km")
            @RequestParam(required = false) @Positive @DecimalMax("" + MAX_RADIUS_KM) Double radiusKm,
            @RequestParam(required = false) PropertyType propertyType,
            @RequestParam(required = false) ListingPurpose purpose,
            @RequestParam(required = false) @Positive BigDecimal minPrice,
            @RequestParam(required = false) @Positive BigDecimal maxPrice,
            @RequestParam(required = false) Integer minBedrooms,
            @RequestParam(required = false) Integer minBathrooms,
            @Parameter(description = "Case-insensitive substring match against the agent-entered area name")
            @RequestParam(required = false) String neighbourhood,
            @RequestParam(defaultValue = "NEWEST") ListingSearchSort sort,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        var filter = new ListingSearchFilter(lat, lng, radiusKm, propertyType, purpose,
                minPrice, maxPrice, minBedrooms, minBathrooms, neighbourhood, sort);
        var page = listingFeedService.search(principal.getDomainUser(), filter, pageable);
        return ResponseEntity.ok(page);
    }
}