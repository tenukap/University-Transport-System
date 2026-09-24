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

    // Reuse the team's User entity; never cascade feedback operations to users.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "UserId", nullable = false, columnDefinition = "int")
    private User user;

    @Column(name = "Subject", nullable = false, length = 150)
    private String subject;

    @Nationalized
    @Column(name = "Message", nullable = false, columnDefinition = "nvarchar(max)")
    private String comments;

    @Column(name = "Status", length = 20)
    private String status;

    @Column(name = "SubmittedAt", columnDefinition = "datetime2")
    private LocalDateTime submittedAt;

    public Integer getFeedbackId() { return feedbackId; }
    public void setFeedbackId(Integer feedbackId) { this.feedbackId = feedbackId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
