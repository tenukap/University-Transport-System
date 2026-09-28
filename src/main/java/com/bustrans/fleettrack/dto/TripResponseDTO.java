package com.bustrans.fleettrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripResponseDTO {

    private Integer tripId;
    private String tripDate;
    private String startTime;
    private String eta;
    private String tripStatus;
    private BigDecimal operatingCost;
    private Integer pickupLocationId;
    private String pickupLocationName;
    private Integer dropLocationId;
    private String dropLocationName;
}
