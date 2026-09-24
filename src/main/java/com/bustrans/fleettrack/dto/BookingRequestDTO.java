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
public class BookingRequestDTO {

    private Integer tripId;
    private Integer pickupLocId;
    private Integer dropoffLocId;
    private Integer seatNumber;
    private BigDecimal fareAmount;
}
