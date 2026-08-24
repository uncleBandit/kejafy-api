package com.pitahmzalendo.kejafy.listing.entity;

/**
 * Sort options for GET /api/v1/feed/search. DISTANCE only makes sense
 * when the caller supplied a lat/lng center — ListingFeedService rejects
 * that combination before it ever reaches the repository.
 */
public enum ListingSearchSort {
    NEWEST,
    PRICE_ASC,
    PRICE_DESC,
    DISTANCE
}
