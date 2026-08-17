package com.pitahmzalendo.kejafy.listing.service;

import com.pitahmzalendo.kejafy.listing.dto.request.CreateCommentRequest;
import com.pitahmzalendo.kejafy.listing.entity.Comment;
import com.pitahmzalendo.kejafy.listing.entity.Listing;
import com.pitahmzalendo.kejafy.listing.entity.ListingStatus;
import com.pitahmzalendo.kejafy.listing.exception.CommentAccessException;
import com.pitahmzalendo.kejafy.listing.exception.CommentException;
import com.pitahmzalendo.kejafy.listing.exception.CommentNotFoundException;
import com.pitahmzalendo.kejafy.listing.exception.ListingNotFoundException;
import com.pitahmzalendo.kejafy.listing.repository.CommentRepository;
import com.pitahmzalendo.kejafy.listing.repository.ListingRepository;
import com.pitahmzalendo.kejafy.subscription.service.SubscriptionService;
import com.pitahmzalendo.kejafy.user.entity.User;
import com.pitahmzalendo.kejafy.user.entity.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reading a listing's comments is public — no subscription check, ever
 * (see getComments). Posting is gated: an active subscription, OR being
 * the listing's own agent (an agent must be able to answer questions on
 * their own listing without "subscribing" to their own inventory), OR
 * an admin. That's the one place the gate lives — don't duplicate the
 * check in the controller.
 */
@Service
@Transactional
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;
    private final ListingRepository listingRepository;
    private final SubscriptionService subscriptionService;

    public CommentService(CommentRepository commentRepository,
                          ListingRepository listingRepository,
                          SubscriptionService subscriptionService) {
        this.commentRepository = commentRepository;
        this.listingRepository = listingRepository;
        this.subscriptionService = subscriptionService;
    }

    public Comment addComment(Long listingId, User author, CreateCommentRequest request) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException("Listing not found."));

        if (listing.getStatus() != ListingStatus.PUBLISHED) {
            throw new CommentException("Comments are only open on published listings.");
        }

        if (!canPost(listing, author)) {
            throw new CommentAccessException(
                    "An active subscription is required to comment on listings.");
        }

        Comment parent = resolveParent(listing, request.parentCommentId());

        Comment comment = new Comment(listing, author, request.body(), parent);
        comment = commentRepository.save(comment);

        listing.incrementCommentCount();
        listingRepository.save(listing);

        log.info("Comment added, listingId={}, commentId={}, authorId={}",
                listingId, comment.getId(), author.getId());
        return comment;
    }

    @Transactional(readOnly = true)
    public Page<Comment> getComments(Long listingId, Pageable pageable) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException("Listing not found."));
        return commentRepository.findByListingOrderByCreatedAtAsc(listing, pageable);
    }

    /** Author of the comment, or an admin, can remove it. Soft-deleted so replies keep their parent. */
    public void deleteComment(Long commentId, User requester) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found."));

        boolean allowed = comment.isAuthoredBy(requester) || requester.getRole() == UserRole.ADMIN;
        if (!allowed) {
            throw new CommentAccessException("You can only delete your own comments.");
        }

        if (comment.isDeleted()) {
            return;
        }

        comment.softDelete();
        Listing listing = comment.getListing();
        listing.decrementCommentCount();
        listingRepository.save(listing);
    }

    private boolean canPost(Listing listing, User author) {
        return subscriptionService.hasActiveSubscription(author)
                || listing.isOwnedBy(author)
                || author.getRole() == UserRole.ADMIN;
    }

    private Comment resolveParent(Listing listing, Long parentCommentId) {
        if (parentCommentId == null) {
            return null;
        }

        Comment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new CommentNotFoundException("Parent comment not found."));

        if (!parent.getListing().getId().equals(listing.getId())) {
            throw new CommentException("Parent comment belongs to a different listing.");
        }
        if (!parent.isTopLevel()) {
            throw new CommentException("Replies can only be made to a top-level comment.");
        }

        return parent;
    }
}
