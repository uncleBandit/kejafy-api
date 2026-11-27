package com.pitahmzalendo.kejafy.listing.entity;

/**
 * Broad grouping for {@link AmenityType}, used for filter UI (e.g. an
 * "Education" chip that covers both SCHOOL and UNIVERSITY) without the
 * client needing to know every individual type up front. Derived from
 * the type — never stored directly, see AmenityType.category().
 */
public enum AmenityCategory {
    EDUCATION,
    HEALTHCARE,
    TRANSPORT,
    SHOPPING,
    SECURITY,
    RECREATION,
    FINANCIAL,
    WORSHIP,
    DINING,
    FUEL,
    OTHER
}
