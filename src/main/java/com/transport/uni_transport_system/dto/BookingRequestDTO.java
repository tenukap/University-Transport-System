package com.transport.uni_transport_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequestDTO {

    private Long studentId;
    private Integer tripId;
    private Integer pickupLocId;
    private Integer dropoffLocId;
}
