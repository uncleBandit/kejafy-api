package com.pitahmzalendo.kejafy.listing.service;

import com.pitahmzalendo.kejafy.listing.dto.request.ListingSearchFilter;
import com.pitahmzalendo.kejafy.listing.dto.response.ListingFeedItemResponse;
import com.pitahmzalendo.kejafy.listing.entity.Listing;
import com.pitahmzalendo.kejafy.listing.entity.ListingLike;
import com.pitahmzalendo.kejafy.listing.entity.ListingStatus;
import com.pitahmzalendo.kejafy.listing.exception.ListingException;
import com.pitahmzalendo.kejafy.listing.exception.ListingNotFoundException;
import com.pitahmzalendo.kejafy.listing.repository.ListingLikeRepository;
import com.pitahmzalendo.kejafy.listing.repository.ListingRepository;
import com.pitahmzalendo.kejafy.listing.util.GeoUtils;
import com.pitahmzalendo.kejafy.subscription.service.SubscriptionService;
import com.pitahmzalendo.kejafy.user.entity.User;
import com.pitahmzalendo.kejafy.user.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * NOTE: this file was referenced by ListingFeedController and by
 * javadoc elsewhere (Listing, ListingFeedItemResponse) but wasn't
 * present in the uploaded zip. Reconstructed here from that contract —
 * getFeed/getListing behave the way the existing call sites and the
 * locked/unlocked split in ListingFeedItemResponse imply. If your real
 * project already has a different implementation, treat this as a
 * reference for the new search() method and diff the rest rather than
 * overwriting blind.
 *
 * The gate itself: PUBLISHED listings are visible to everyone, but only
 * a subscriber (or an ADMIN, same carve-out ChatService uses) gets the
 * unlocked card — title/description/price/exact location. Everyone else
 * gets locked(): id, videos, like/comment counts, and now optionally
 * distanceKm when the request was a geo search.
 */
@Service
@Transactional(readOnly = true)
public class ListingFeedService {

    private final ListingRepository listingRepository;
    private final ListingLikeRepository listingLikeRepository;
    private final SubscriptionService subscriptionService;

    public ListingFeedService(ListingRepository listingRepository,
                              ListingLikeRepository listingLikeRepository,
                              SubscriptionService subscriptionService) {
        this.listingRepository = listingRepository;
        this.listingLikeRepository = listingLikeRepository;
        this.subscriptionService = subscriptionService;
    }

    public Page<ListingFeedItemResponse> getFeed(User user, Pageable pageable) {
        Page<Listing> page = listingRepository.findByStatusAndFlaggedForReviewFalseOrderByPublishedAtDesc(
                ListingStatus.PUBLISHED, pageable);
        boolean unlocked = hasFullAccess(user);
        Set<Long> likedIds = likedListingIds(user, page);
        return page.map(listing -> toResponse(listing, unlocked, likedIds, null, null));
    }

    public ListingFeedItemResponse getListing(Long id, User user) {
        Listing listing = listingRepository.findById(id)
                .filter(l -> l.getStatus() == ListingStatus.PUBLISHED)
                .filter(l -> !l.isFlaggedForReview())
                .orElseThrow(() -> new ListingNotFoundException("Listing not found."));

        boolean unlocked = hasFullAccess(user);
        boolean likedByMe = listingLikeRepository.existsByListingAndUser(listing, user);
        return toResponse(listing, unlocked, likedByMe);
    }

    /**
     * Geo/radius + attribute search — GET /api/v1/feed/search. Same
     * locked/unlocked gating as getFeed; the filter itself (price range,
     * bedrooms, property type, radius) applies before the subscription
     * gate, so a non-subscriber can narrow down candidates by area and
     * budget and see locked cards, without ever getting the exact pin
     * for a listing they haven't paid to unlock.
     */
    public Page<ListingFeedItemResponse> search(User user, ListingSearchFilter filter, Pageable pageable) {
        validate(filter);

        Page<Listing> page = listingRepository.search(
                filter.lat(),
                filter.lng(),
                filter.radiusKm(),
                filter.propertyType() != null ? filter.propertyType().name() : null,
                filter.purpose() != null ? filter.purpose().name() : null,
                filter.minPrice(),
                filter.maxPrice(),
                filter.minBedrooms(),
                filter.minBathrooms(),
                blankToNull(filter.neighbourhood()),
                filter.maxNoiseLevel(),
                filter.maxFloodRiskLevel(),
                filter.minRoadAccessQuality(),
                filter.minSecurityLevel(),
                filter.minWaterReliability(),
                filter.minPowerReliability(),
                filter.requireAmenityType() != null ? filter.requireAmenityType().name() : null,
                (filter.sort() != null ? filter.sort() : com.pitahmzalendo.kejafy.listing.entity.ListingSearchSort.NEWEST).name(),
                pageable
        );

        boolean unlocked = hasFullAccess(user);
        Set<Long> likedIds = likedListingIds(user, page);
        return page.map(listing -> toResponse(listing, unlocked, likedIds, filter.lat(), filter.lng()));
    }

    // Kept in sync with ListingFeedController.MAX_RADIUS_KM (100). The
    // controller's @DecimalMax rejects an over-cap value before it's
    // even bound to a filter, but re-checking here means search() is
    // still safe against a full-feed-width table scan if it's ever
    // called from somewhere other than that one @RequestParam path.
    private static final double MAX_RADIUS_KM = 100;

    private void validate(ListingSearchFilter filter) {
        if (filter.radiusKm() != null && !filter.hasCenter()) {
            throw new ListingException("radiusKm requires both lat and lng.");
        }
        if (filter.radiusKm() != null && filter.radiusKm() <= 0) {
            throw new ListingException("radiusKm must be positive.");
        }
        if (filter.radiusKm() != null && filter.radiusKm() > MAX_RADIUS_KM) {
            throw new ListingException("radiusKm cannot exceed " + (int) MAX_RADIUS_KM + "km.");
        }
        if (filter.sort() == com.pitahmzalendo.kejafy.listing.entity.ListingSearchSort.DISTANCE && !filter.hasCenter()) {
            throw new ListingException("sort=DISTANCE requires both lat and lng.");
        }
        if (filter.minPrice() != null && filter.maxPrice() != null
                && filter.minPrice().compareTo(filter.maxPrice()) > 0) {
            throw new ListingException("minPrice cannot be greater than maxPrice.");
        }
    }

    private boolean hasFullAccess(User user) {
        return user.getRole() == UserRole.ADMIN || subscriptionService.hasActiveSubscription(user);
    }

    private Set<Long> likedListingIds(User user, Page<Listing> page) {
        return listingLikeRepository.findByUserAndListingIn(user, page.getContent()).stream()
                .map(ListingLike::getListing)
                .map(Listing::getId)
                .collect(Collectors.toSet());
    }

    private ListingFeedItemResponse toResponse(Listing listing, boolean unlocked,
                                               Set<Long> likedIds, Double centerLat, Double centerLng) {
        boolean likedByMe = likedIds.contains(listing.getId());
        Double distanceKm = GeoUtils.distanceKmOrNull(centerLat, centerLng, listing.getLatitude(), listing.getLongitude());
        return unlocked
                ? ListingFeedItemResponse.unlocked(listing, likedByMe, distanceKm)
                : ListingFeedItemResponse.locked(listing, likedByMe, distanceKm);
    }

    private ListingFeedItemResponse toResponse(Listing listing, boolean unlocked, boolean likedByMe) {
        return unlocked
                ? ListingFeedItemResponse.unlocked(listing, likedByMe)
                : ListingFeedItemResponse.locked(listing, likedByMe);
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}