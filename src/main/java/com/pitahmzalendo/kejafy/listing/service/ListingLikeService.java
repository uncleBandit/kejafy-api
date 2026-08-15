package com.pitahmzalendo.kejafy.listing.service;


import com.pitahmzalendo.kejafy.listing.entity.Listing;
import com.pitahmzalendo.kejafy.listing.entity.ListingLike;
import com.pitahmzalendo.kejafy.listing.repository.ListingLikeRepository;
import com.pitahmzalendo.kejafy.listing.repository.ListingRepository;
import com.pitahmzalendo.kejafy.listing.exception.ListingException;
import com.pitahmzalendo.kejafy.user.entity.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liking (the "double tap" gesture) is available to every authenticated
 * user regardless of subscription status — it's not gated. See
 * ListingFeedItemResponse for what IS gated.
 */
@Service
@Transactional
public class ListingLikeService {

    private final ListingLikeRepository listingLikeRepository;
    private final ListingRepository listingRepository;

    public ListingLikeService(ListingLikeRepository listingLikeRepository,
                              ListingRepository listingRepository) {
        this.listingLikeRepository = listingLikeRepository;
        this.listingRepository = listingRepository;
    }

    /** Idempotent: liking an already-liked listing is a no-op, not an error — matches a repeated double-tap gesture. */
    public boolean like(Long listingId, User user) {
        Listing listing = requireListing(listingId);

        if (listingLikeRepository.existsByListingAndUser(listing, user)) {
            return false;
        }

        try {
            listingLikeRepository.save(new ListingLike(listing, user));
        } catch (DataIntegrityViolationException e) {
            // Lost a race with a duplicate tap — already liked, nothing more to do.
            return false;
        }

        listing.incrementLikeCount();
        listingRepository.save(listing);
        return true;
    }

    public boolean unlike(Long listingId, User user) {
        Listing listing = requireListing(listingId);

        return listingLikeRepository.findByListingAndUser(listing, user)
                .map(like -> {
                    listingLikeRepository.delete(like);
                    listing.decrementLikeCount();
                    listingRepository.save(listing);
                    return true;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isLikedBy(Listing listing, User user) {
        return listingLikeRepository.existsByListingAndUser(listing, user);
    }

    private Listing requireListing(Long listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingException("Listing not found."));
    }
}
