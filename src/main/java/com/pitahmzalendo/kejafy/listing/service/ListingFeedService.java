package com.pitahmzalendo.kejafy.listing.service;

import com.pitahmzalendo.kejafy.listing.dto.response.ListingFeedItemResponse;
import com.pitahmzalendo.kejafy.listing.entity.Listing;
import com.pitahmzalendo.kejafy.listing.entity.ListingStatus;
import com.pitahmzalendo.kejafy.listing.exception.ListingException;
import com.pitahmzalendo.kejafy.listing.repository.ListingLikeRepository;
import com.pitahmzalendo.kejafy.listing.repository.ListingRepository;
import com.pitahmzalendo.kejafy.subscription.service.SubscriptionService;
import com.pitahmzalendo.kejafy.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * The public feed — every published listing, gated on the viewer's
 * subscription status. This is the enforcement point for the platform's
 * whole selling point: DO NOT add another read path that returns
 * latitude/longitude/description to a non-subscriber. If a new endpoint
 * needs listing data for a customer-facing screen, it should call
 * through here, not ListingRepository/ListingDetailResponse directly.
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

    public Page<ListingFeedItemResponse> getFeed(User viewer, Pageable pageable) {
        Page<Listing> page = listingRepository.findByStatusOrderByPublishedAtDesc(
                ListingStatus.PUBLISHED, pageable);

        boolean unlocked = subscriptionService.hasActiveSubscription(viewer);
        Set<Long> likedListingIds = likedListingIds(viewer, page.getContent());

        return page.map(listing -> {
            boolean likedByMe = likedListingIds.contains(listing.getId());
            return unlocked
                    ? ListingFeedItemResponse.unlocked(listing, likedByMe)
                    : ListingFeedItemResponse.locked(listing, likedByMe);
        });
    }

    /** Single-listing view — also records a view (feed scrolling doesn't; opening a listing does). */
    @Transactional
    public ListingFeedItemResponse getListing(Long listingId, User viewer) {
        Listing listing = listingRepository.findById(listingId)
                .filter(l -> l.getStatus() == ListingStatus.PUBLISHED)
                .orElseThrow(() -> new ListingException("Listing not found."));

        listing.incrementViewCount();
        listingRepository.save(listing);

        boolean unlocked = subscriptionService.hasActiveSubscription(viewer);
        boolean likedByMe = listingLikeRepository.existsByListingAndUser(listing, viewer);

        return unlocked
                ? ListingFeedItemResponse.unlocked(listing, likedByMe)
                : ListingFeedItemResponse.locked(listing, likedByMe);
    }

    private Set<Long> likedListingIds(User viewer, java.util.List<Listing> listings) {
        if (listings.isEmpty()) {
            return Set.of();
        }
        return listingLikeRepository.findByUserAndListingIn(viewer, listings).stream()
                .map(like -> like.getListing().getId())
                .collect(Collectors.toSet());
    }
}
