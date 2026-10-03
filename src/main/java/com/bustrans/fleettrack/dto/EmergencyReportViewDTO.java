package com.bustrans.fleettrack.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyReportViewDTO {
    private Integer id;
    private String title;
    private String type;
    private String description;
    private String reporterName;
    private String reporterRole;
    private String tripLabel;      // "Pickup -> Drop, 10 Oct 07:30" or null
    private String busRegistration; // null when report has no trip
    private String status;
    private String timestamp;
}
