package com.pitahmzalendo.kejafy.listing.entity;


/**
 * DRAFT       - agent is still building it (adding videos, editing fields); not visible to anyone else.
 * PENDING_REVIEW - agent submitted it; sitting in the admin moderation queue.
 * PUBLISHED   - admin-approved and live in the public feed.
 * REJECTED    - admin declined it; agent can edit and resubmit (back to PENDING_REVIEW).
 * ARCHIVED    - agent took it down (e.g. property let/sold) or an admin unpublished it; no longer in the feed.
 */
public enum ListingStatus {
    DRAFT,
    PENDING_REVIEW,
    PUBLISHED,
    REJECTED,
    ARCHIVED
}