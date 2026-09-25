package com.transport.uni_transport_system.util;

import java.time.LocalTime;

public class EtaCalculator {

    // Average city bus speed in km/h (adjust for realism)
    private static final double AVG_SPEED_KMH = 35.0;

    /**
     * Calculate distance between two coordinates using the Haversine formula.
     * Returns distance in kilometers.
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth's radius in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Calculate ETA based on current position, destination, and current time.
     * Returns the estimated arrival time as LocalTime.
     */
    public static LocalTime calculateEta(double currentLat, double currentLng,
                                         double destLat, double destLng) {
        double distanceKm = calculateDistance(currentLat, currentLng, destLat, destLng);
        double hours = distanceKm / AVG_SPEED_KMH;
        long minutesToAdd = Math.round(hours * 60);

        return LocalTime.now().plusMinutes(minutesToAdd);
    }
}
