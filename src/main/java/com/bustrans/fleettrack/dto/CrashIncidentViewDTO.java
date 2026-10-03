package com.bustrans.fleettrack.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrashIncidentViewDTO {
    private Integer id;
    private String location;
    private String severity;
    private String description;
    private String driverName;
    private String busRegistration;
    private String tripLabel;  // "Pickup -> Drop, 10 Oct 07:30" or null
    private String status;
    private String timestamp;
}
