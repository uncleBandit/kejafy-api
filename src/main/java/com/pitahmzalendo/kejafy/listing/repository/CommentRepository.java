package com.pitahmzalendo.kejafy.listing.repository;

import com.pitahmzalendo.kejafy.listing.entity.Comment;
import com.pitahmzalendo.kejafy.listing.entity.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Oldest first, like a chat/Q&A thread reads top-to-bottom. Includes
    // soft-deleted rows (needed so a reply chain doesn't lose its parent
    // reference in the response) — CommentService renders those as
    // "[deleted]" rather than filtering them out of the page entirely.
    Page<Comment> findByListingOrderByCreatedAtAsc(Listing listing, Pageable pageable);

    long countByListingAndDeletedFalse(Listing listing);
}
