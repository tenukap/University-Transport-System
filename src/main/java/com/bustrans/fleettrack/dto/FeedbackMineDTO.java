package com.bustrans.fleettrack.dto;

import java.time.LocalDateTime;

public class FeedbackMineDTO {
    private Integer id;
    private String tripLabel;
    private Integer rating;
    private String comments;
    private String status;
    private LocalDateTime submittedAt;
    private String adminResponse;
    private LocalDateTime reviewedAt;

    public FeedbackMineDTO(Integer id, String tripLabel, Integer rating, String comments,
                           String status, LocalDateTime submittedAt,
                           String adminResponse, LocalDateTime reviewedAt) {
        this.id = id;
        this.tripLabel = tripLabel;
        this.rating = rating;
        this.comments = comments;
        this.status = status;
        this.submittedAt = submittedAt;
        this.adminResponse = adminResponse;
        this.reviewedAt = reviewedAt;
    }

    public Integer getId() { return id; }
    public String getTripLabel() { return tripLabel; }
    public Integer getRating() { return rating; }
    public String getComments() { return comments; }
    public String getStatus() { return status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public String getAdminResponse() { return adminResponse; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
}
