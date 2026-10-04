package com.bustrans.fleettrack.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackingBusDTO {
    private Long busId;
    private String registration;
    private Integer capacity;
    private String busStatus;
    private TrackingTripDTO currentOrNextTrip; // null if no trips
    private Integer tripsToday;
}
