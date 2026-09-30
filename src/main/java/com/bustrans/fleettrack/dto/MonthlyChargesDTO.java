package com.bustrans.fleettrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyChargesDTO {

    private int month;
    private int year;
    private BigDecimal total;
    private int count;
    private List<ChargeItem> items;
    private List<MonthSummary> months;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChargeItem {
        private Long bookingId;
        private String tripDate;
        private String pickup;
        private String dropoff;
        private Integer seatNumber;
        private BigDecimal fareAmount;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthSummary {
        private int month;
        private int year;
        private BigDecimal total;
    }
}
