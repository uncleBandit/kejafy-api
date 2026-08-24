package com.pitahmzalendo.kejafy.report.entity;

/**
 * Deliberately specific rather than a generic "inappropriate" catch-all —
 * each value maps to a real, common house-hunting scam pattern (fake
 * listings, viewing-fee/deposit scams, unlicensed people posing as
 * agents) so both the reporter and the admin reviewing the queue know
 * exactly what's being alleged without reading free text first.
 */
public enum ReportReason {
    FAKE_OR_NONEXISTENT_PROPERTY,
    ALREADY_SOLD_OR_RENTED,
    DUPLICATE_LISTING,
    MISLEADING_PRICE_OR_DETAILS,
    UPFRONT_PAYMENT_REQUEST,
    IMPERSONATING_AGENT,
    OFFENSIVE_OR_ABUSIVE,
    OTHER
}
