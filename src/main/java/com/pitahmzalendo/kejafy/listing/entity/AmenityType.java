package com.pitahmzalendo.kejafy.listing.entity;

/**
 * A specific kind of nearby point of interest an agent (or a places-API
 * enrichment job, see AmenitySource) can attach to a listing — "there's
 * a primary school 400m away", not "this unit has a balcony". On-unit
 * features belong on Listing/description, not here: this enum is only
 * for things a prospective tenant would search a map for around the
 * property, which is why every value maps to a category() a search
 * filter can group on.
 */
public enum AmenityType {
    PRIMARY_SCHOOL(AmenityCategory.EDUCATION),
    SECONDARY_SCHOOL(AmenityCategory.EDUCATION),
    UNIVERSITY(AmenityCategory.EDUCATION),
    HOSPITAL(AmenityCategory.HEALTHCARE),
    CLINIC(AmenityCategory.HEALTHCARE),
    PHARMACY(AmenityCategory.HEALTHCARE),
    MATATU_STAGE(AmenityCategory.TRANSPORT),
    BUS_STOP(AmenityCategory.TRANSPORT),
    TRAIN_STATION(AmenityCategory.TRANSPORT),
    TARMAC_ROAD_ACCESS(AmenityCategory.TRANSPORT),
    SUPERMARKET(AmenityCategory.SHOPPING),
    SHOPPING_MALL(AmenityCategory.SHOPPING),
    MARKET(AmenityCategory.SHOPPING),
    POLICE_STATION(AmenityCategory.SECURITY),
    GATED_ESTATE_SECURITY(AmenityCategory.SECURITY),
    PARK(AmenityCategory.RECREATION),
    GYM(AmenityCategory.RECREATION),
    RECREATION_CENTER(AmenityCategory.RECREATION),
    BANK(AmenityCategory.FINANCIAL),
    ATM(AmenityCategory.FINANCIAL),
    RESTAURANT(AmenityCategory.DINING),
    PLACE_OF_WORSHIP(AmenityCategory.WORSHIP),
    PETROL_STATION(AmenityCategory.FUEL),
    OTHER(AmenityCategory.OTHER);

    private final AmenityCategory category;

    AmenityType(AmenityCategory category) {
        this.category = category;
    }

    public AmenityCategory category() {
        return category;
    }
}
