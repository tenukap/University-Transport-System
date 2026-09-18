package com.transport.uni_transport_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationResponseDTO {

    private Integer locationId;
    private String locationName;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
