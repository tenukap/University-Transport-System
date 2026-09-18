package com.transport.uni_transport_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponseDTO {

    private Long id;
    private Long studentId;
    private String studentName;
    private Integer tripId;
    private Integer pickupLocId;
    private Integer dropoffLocId;
    private String status;
    private LocalDateTime createdAt;
    private Integer seatNumber;
}
