package com.pitahmzalendo.kejafy.listing.util;

import java.math.BigDecimal;

/**
 * Haversine great-circle distance. Good enough at neighbourhood/city scale
 * (the error versus a more exact ellipsoidal formula is negligible over the
 * few-kilometre radii this feature deals with) and, unlike PostGIS, needs
 * no DB extension — matches ListingRepository.search, which reimplements
 * this same formula in raw SQL for the radius WHERE clause and ORDER BY.
 * If the two ever drift apart, distances shown in the response and the
 * rows actually returned could disagree — keep them in sync.
 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
    }

    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Null-safe convenience for response mapping: returns null (rather
     * than throwing or returning 0) whenever either point is missing, e.g.
     * a plain (non-geo) feed/search where the caller gave no lat/lng.
     * Rounded to 0.1km — no legitimate use case needs more precision than
     * that for a "how far is this" display, and it avoids implying false
     * accuracy from a straight-line estimate.
     */
    public static Double distanceKmOrNull(Double lat1, Double lng1, BigDecimal lat2, BigDecimal lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return null;
        }
        double km = distanceKm(lat1, lng1, lat2.doubleValue(), lng2.doubleValue());
        return Math.round(km * 10.0) / 10.0;
    }
}
