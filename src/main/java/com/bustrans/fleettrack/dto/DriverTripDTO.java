package com.bustrans.fleettrack.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverTripDTO {
    private Integer tripId;
    private String tripDate;
    private String startTime;
    private String eta;
    private String pickupName;
    private String dropName;
    private String busRegistration;
    private String tripStatus;
    private String latestStatusType; // nullable — most recent Trip_Status log entry
}
