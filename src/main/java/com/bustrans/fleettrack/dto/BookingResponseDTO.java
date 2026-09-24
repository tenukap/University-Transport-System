package com.bustrans.fleettrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponseDTO {

    private Long id;
    private Long userId;
    private Integer tripId;
    private Integer pickupLocId;
    private Integer dropoffLocId;
    private Integer seatNumber;
    private BigDecimal fareAmount;
    private String status;
    private LocalDateTime createdAt;
}
