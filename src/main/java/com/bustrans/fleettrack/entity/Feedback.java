package com.bustrans.fleettrack.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Nationalized;
import java.time.LocalDateTime;

@Entity
@Table(name = "Feedback", schema = "dbo")
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FeedbackId")
    private Integer feedbackId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "UserId", nullable = false, columnDefinition = "int")
    private User user;

    @Column(name = "Subject", nullable = false, length = 150)
    private String subject;

    @Nationalized
    @Column(name = "Message", nullable = false, columnDefinition = "nvarchar(max)")
    private String comments;

    @Column(name = "Rating")
    private Integer rating;

    @Column(name = "BookingId")
    private Long bookingId;

    @Column(name = "Status", length = 20)
    private String status;

    @Column(name = "SubmittedAt", columnDefinition = "datetime2")
    private LocalDateTime submittedAt;

    @Nationalized
    @Column(name = "admin_response", columnDefinition = "nvarchar(1000)")
    private String adminResponse;

    @Column(name = "reviewed_at", columnDefinition = "datetime2")
    private LocalDateTime reviewedAt;

    // Plain column — reviewer looked up by id when building admin DTO
    @Column(name = "reviewed_by_id")
    private Integer reviewedById;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id", insertable = false, updatable = false)
    private User reviewedBy;

    public Integer getFeedbackId() { return feedbackId; }
    public void setFeedbackId(Integer feedbackId) { this.feedbackId = feedbackId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public String getAdminResponse() { return adminResponse; }
    public void setAdminResponse(String adminResponse) { this.adminResponse = adminResponse; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public Integer getReviewedById() { return reviewedById; }
    public void setReviewedById(Integer reviewedById) { this.reviewedById = reviewedById; }
    public User getReviewedBy() { return reviewedBy; }
}
