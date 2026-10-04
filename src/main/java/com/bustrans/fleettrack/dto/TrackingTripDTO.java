package com.bustrans.fleettrack.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackingTripDTO {
    private Integer tripId;
    private String tripDate;
    private String startTime;
    private String eta;
    private String pickupName;
    private String dropName;
    private Long busId;
    private String busRegistration;
    private String driverName;
    private String tripStatus;
    private String latestStatusType;
    private String latestStatusAt;
    private String latestLat;
    private String latestLng;
    private String latestLocationAt;
}
