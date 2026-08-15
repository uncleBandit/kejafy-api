package com.pitahmzalendo.kejafy.listing.repository;


import com.pitahmzalendo.kejafy.listing.entity.Listing;
import com.pitahmzalendo.kejafy.listing.entity.ListingLike;
import com.pitahmzalendo.kejafy.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ListingLikeRepository extends JpaRepository<ListingLike, Long> {

    Optional<ListingLike> findByListingAndUser(Listing listing, User user);

    boolean existsByListingAndUser(Listing listing, User user);

    List<ListingLike> findByUserAndListingIn(User user, Collection<Listing> listings);
}
