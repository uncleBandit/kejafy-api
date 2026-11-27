package com.pitahmzalendo.kejafy.listing.entity;

/**
 * Where a {@link ListingAmenity} row came from — shown to the reader
 * (see ListingAmenityResponse) so "school 300m away" from an unverified
 * agent claim can be told apart from the same claim confirmed against a
 * places API or an admin site visit. Distinct from Listing.verified,
 * which is a trust signal on the listing as a whole rather than on any
 * one surrounding-amenity claim.
 */
public enum AmenitySource {
    // Agent typed this in by hand when creating/editing the listing.
    // Cheapest to collect, weakest trust signal.
    AGENT_ENTERED,

    // Populated or corroborated by an automated places-API enrichment
    // job keyed off the listing's lat/lng (see class javadoc on
    // ListingAmenity for the dedupe key that job should use).
    PLACES_API,

    // An admin confirmed this claim directly (e.g. during the same
    // out-of-band check that sets Listing.verified).
    ADMIN_VERIFIED
}
