package com.bustrans.fleettrack.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "Feedback")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FeedbackId")
    private Integer feedbackId;

    @Column(name = "UserId", nullable = false)
    private Long userId;

    // The completed booking this feedback is about. Nullable so the generic
    // Feedback table can still hold non-booking feedback if ever needed.
    @Column(name = "BookingId")
    private Long bookingId;

    @Column(name = "Subject", nullable = false, length = 150)
    private String subject;

    @Column(name = "Message", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String message;

    @Column(name = "Rating")
    private Integer rating;

    @Builder.Default
    @Column(name = "Status", length = 20)
    private String status = "Pending";

    // Populated by the DB default (GETDATE()); never written from the app.
    @Column(name = "SubmittedAt", insertable = false, updatable = false)
    private LocalDateTime submittedAt;
}
