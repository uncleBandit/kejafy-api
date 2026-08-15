package com.pitahmzalendo.kejafy.listing.repository;


import com.pitahmzalendo.kejafy.listing.entity.Listing;
import com.pitahmzalendo.kejafy.listing.entity.ListingStatus;
import com.pitahmzalendo.kejafy.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    Page<Listing> findByAgentOrderByCreatedAtDesc(User agent, Pageable pageable);

    Page<Listing> findByStatusOrderByPublishedAtDesc(ListingStatus status, Pageable pageable);

    Page<Listing> findByStatusOrderBySubmittedAtAsc(ListingStatus status, Pageable pageable);
}
