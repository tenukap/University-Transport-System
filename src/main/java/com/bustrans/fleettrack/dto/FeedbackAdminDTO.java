package com.bustrans.fleettrack.dto;

import java.time.LocalDateTime;

public class FeedbackAdminDTO {
    private Integer id;
    private String studentName;
    private String studentIndex;    // userId used as student index
    private String tripLabel;
    private String busRegistration;
    private Integer rating;
    private String comments;
    private String status;
    private LocalDateTime submittedAt;
    private String adminResponse;
    private LocalDateTime reviewedAt;
    private String reviewedByName;

    public FeedbackAdminDTO(Integer id, String studentName, String studentIndex,
                            String tripLabel, String busRegistration,
                            Integer rating, String comments, String status,
                            LocalDateTime submittedAt, String adminResponse,
                            LocalDateTime reviewedAt, String reviewedByName) {
        this.id = id;
        this.studentName = studentName;
        this.studentIndex = studentIndex;
        this.tripLabel = tripLabel;
        this.busRegistration = busRegistration;
        this.rating = rating;
        this.comments = comments;
        this.status = status;
        this.submittedAt = submittedAt;
        this.adminResponse = adminResponse;
        this.reviewedAt = reviewedAt;
        this.reviewedByName = reviewedByName;
    }

    public Integer getId() { return id; }
    public String getStudentName() { return studentName; }
    public String getStudentIndex() { return studentIndex; }
    public String getTripLabel() { return tripLabel; }
    public String getBusRegistration() { return busRegistration; }
    public Integer getRating() { return rating; }
    public String getComments() { return comments; }
    public String getStatus() { return status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public String getAdminResponse() { return adminResponse; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public String getReviewedByName() { return reviewedByName; }
}
