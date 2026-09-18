package com.transport.uni_transport_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripResponseDTO {

    private Integer tripId;
    private LocalDate tripDate;
    private LocalTime startTime;
    private LocalTime eta;
    private Integer pickupLocationId;
    private Integer dropLocationId;
    private String pickupLocationName;
    private String dropLocationName;
}
