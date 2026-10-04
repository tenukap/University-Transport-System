package com.bustrans.fleettrack.dto;

public class EligibleTripDTO {
    private Long bookingId;
    private String tripLabel;   // "Pickup -> Drop, 10 Oct 07:30"
    private String busRegistration;

    public EligibleTripDTO(Long bookingId, String tripLabel, String busRegistration) {
        this.bookingId = bookingId;
        this.tripLabel = tripLabel;
        this.busRegistration = busRegistration;
    }

    public Long getBookingId() { return bookingId; }
    public String getTripLabel() { return tripLabel; }
    public String getBusRegistration() { return busRegistration; }
}
