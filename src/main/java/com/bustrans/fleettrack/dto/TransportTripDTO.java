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
public class TransportTripDTO {

    private Integer tripId;
    private String  tripDate;
    private String  startTime;
    private String  eta;
    private String  tripStatus;
    private BigDecimal operatingCost;

    // Pickup / drop — flattened so no nested Location object is exposed.
    private Integer pickupLocationId;
    private String  pickupLocationName;
    private Integer dropLocationId;
    private String  dropLocationName;

    // Bus info — flattened; busLabel is "NC-5678 (40 seats)".
    private Long   busId;
    private String busLabel;

    // Driver info — driverName is looked up from Users at mapping time.
    private Integer driverUserId;
    private String  driverName;
}
