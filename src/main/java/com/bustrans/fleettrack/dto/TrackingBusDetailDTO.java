package com.bustrans.fleettrack.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackingBusDetailDTO {
    private Long busId;
    private String registration;
    private Integer capacity;
    private String busStatus;
    private List<TripWithHistory> trips;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripWithHistory {
        private Integer tripId;
        private String tripDate;
        private String startTime;
        private String eta;
        private String pickupName;
        private String dropName;
        private String tripStatus;
        private List<StatusEntry> statusLog;   // newest first
        private List<PingEntry> recentPings;   // newest first, up to 10
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusEntry {
        private String statusType;
        private String time;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PingEntry {
        private String lat;
        private String lng;
        private String time;
    }
}
