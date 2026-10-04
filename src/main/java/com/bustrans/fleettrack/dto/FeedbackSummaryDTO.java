package com.bustrans.fleettrack.dto;

public class FeedbackSummaryDTO {
    private long total;
    private long pending;
    private long reviewed;
    private Double averageRating;   // null when no rated feedback exists

    public FeedbackSummaryDTO(long total, long pending, long reviewed, Double averageRating) {
        this.total = total;
        this.pending = pending;
        this.reviewed = reviewed;
        this.averageRating = averageRating;
    }

    public long getTotal() { return total; }
    public long getPending() { return pending; }
    public long getReviewed() { return reviewed; }
    public Double getAverageRating() { return averageRating; }
}
