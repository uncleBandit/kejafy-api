package com.pitahmzalendo.kejafy.listing.service;

import com.pitahmzalendo.kejafy.listing.dto.request.AddListingVideoRequest;
import com.pitahmzalendo.kejafy.listing.dto.request.CreateListingRequest;
import com.pitahmzalendo.kejafy.listing.dto.request.UpdateListingRequest;
import com.pitahmzalendo.kejafy.listing.entity.Listing;
import com.pitahmzalendo.kejafy.listing.entity.ListingStatus;
import com.pitahmzalendo.kejafy.listing.entity.ListingVideo;
import com.pitahmzalendo.kejafy.listing.exception.ListingAccessException;
import com.pitahmzalendo.kejafy.listing.exception.ListingException;
import com.pitahmzalendo.kejafy.listing.repository.ListingRepository;
import com.pitahmzalendo.kejafy.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

/**
 * Agent-side listing lifecycle (create → add videos → submit for review)
 * plus the admin moderation actions (approve/reject/archive). Public,
 * gated reads live in ListingFeedService instead — this service always
 * returns the full entity and assumes the caller is already authorized
 * to see it (owning agent or admin).
 */
@Service
@Transactional
public class ListingService {

    private static final Logger log = LoggerFactory.getLogger(ListingService.class);

    // Editing is only safe before a listing has gone live or while it's
    // sitting on a rejection the agent needs to fix — not while it's
    // PENDING_REVIEW (would let an agent silently change what an admin
    // is mid-way through reviewing) or already PUBLISHED (edits there
    // should be a deliberate "unpublish, edit, resubmit" flow, not a
    // silent live change to something a subscriber might already see).
    private static final Set<ListingStatus> EDITABLE_STATUSES =
            EnumSet.of(ListingStatus.DRAFT, ListingStatus.REJECTED);

    private final ListingRepository listingRepository;

    public ListingService(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    public Listing createDraft(User agent, CreateListingRequest request) {
        Listing listing = new Listing(
                agent,
                request.title(),
                request.description(),
                request.propertyType(),
                request.purpose(),
                request.priceAmount(),
                request.bedrooms(),
                request.bathrooms(),
                request.neighbourhood(),
                request.latitude(),
                request.longitude()
        );
        listing = listingRepository.save(listing);
        log.info("Listing draft created, listingId={}, agentId={}", listing.getId(), agent.getId());
        return listing;
    }

    public Listing updateDraft(Long listingId, User agent, UpdateListingRequest request) {
        Listing listing = requireOwnedByAgent(listingId, agent);
        if (!EDITABLE_STATUSES.contains(listing.getStatus())) {
            throw new ListingException(
                    "Only DRAFT or REJECTED listings can be edited; this one is " + listing.getStatus() + ".");
        }

        if (request.title() != null) listing.setTitle(request.title());
        if (request.description() != null) listing.setDescription(request.description());
        if (request.propertyType() != null) listing.setPropertyType(request.propertyType());
        if (request.purpose() != null) listing.setPurpose(request.purpose());
        if (request.priceAmount() != null) listing.setPriceAmount(request.priceAmount());
        if (request.bedrooms() != null) listing.setBedrooms(request.bedrooms());
        if (request.bathrooms() != null) listing.setBathrooms(request.bathrooms());
        if (request.neighbourhood() != null) listing.setNeighbourhood(request.neighbourhood());
        if (request.latitude() != null) listing.setLatitude(request.latitude());
        if (request.longitude() != null) listing.setLongitude(request.longitude());

        return listingRepository.save(listing);
    }

    public Listing addVideo(Long listingId, User agent, AddListingVideoRequest request) {
        Listing listing = requireOwnedByAgent(listingId, agent);
        if (!EDITABLE_STATUSES.contains(listing.getStatus())) {
            throw new ListingException(
                    "Videos can only be added while the listing is DRAFT or REJECTED.");
        }

        ListingVideo video = new ListingVideo(
                request.videoUrl(),
                request.thumbnailUrl(),
                request.durationSeconds(),
                listing.getVideos().size()
        );
        listing.addVideo(video);
        return listingRepository.save(listing);
    }

    /** Goes to "pending review" — not live yet. See AdminListingController for the approve/reject step. */
    public Listing submitForReview(Long listingId, User agent) {
        Listing listing = requireOwnedByAgent(listingId, agent);
        if (listing.getStatus() != ListingStatus.DRAFT && listing.getStatus() != ListingStatus.REJECTED) {
            throw new ListingException(
                    "Only DRAFT or REJECTED listings can be submitted for review.");
        }
        if (listing.getVideos().isEmpty()) {
            throw new ListingException("Add at least one video before submitting for review.");
        }

        listing.submitForReview();
        Listing saved = listingRepository.save(listing);
        log.info("Listing submitted for review, listingId={}, agentId={}", listingId, agent.getId());
        return saved;
    }

    /** Agent taking their own listing down — e.g. the property has been let or sold. */
    public Listing archiveOwn(Long listingId, User agent) {
        Listing listing = requireOwnedByAgent(listingId, agent);
        listing.archive();
        return listingRepository.save(listing);
    }

    @Transactional(readOnly = true)
    public Listing getOwned(Long listingId, User agent) {
        return requireOwnedByAgent(listingId, agent);
    }

    @Transactional(readOnly = true)
    public Page<Listing> getMyListings(User agent, Pageable pageable) {
        return listingRepository.findByAgentOrderByCreatedAtDesc(agent, pageable);
    }

    // ---- Admin moderation ----

    @Transactional(readOnly = true)
    public Page<Listing> getPendingReview(Pageable pageable) {
        return listingRepository.findByStatusOrderBySubmittedAtAsc(ListingStatus.PENDING_REVIEW, pageable);
    }

    public Listing approve(Long listingId) {
        Listing listing = requirePendingReview(listingId);
        listing.publish();
        Listing saved = listingRepository.save(listing);
        log.info("Listing approved and published, listingId={}", listingId);
        return saved;
    }

    public Listing reject(Long listingId, String reason) {
        Listing listing = requirePendingReview(listingId);
        listing.reject(reason);
        Listing saved = listingRepository.save(listing);
        log.info("Listing rejected, listingId={}", listingId);
        return saved;
    }

    /** Admin taking a live listing down (e.g. reported / policy violation) without deleting its history. */
    public Listing archiveAsAdmin(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingException("Listing not found."));
        listing.archive();
        return listingRepository.save(listing);
    }

    private Listing requireOwnedByAgent(Long listingId, User agent) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingException("Listing not found."));
        if (!listing.isOwnedBy(agent)) {
            throw new ListingAccessException("You do not own this listing.");
        }
        return listing;
    }

    private Listing requirePendingReview(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingException("Listing not found."));
        if (listing.getStatus() != ListingStatus.PENDING_REVIEW) {
            throw new ListingException("This listing is not pending review.");
        }
        return listing;
    }
}
