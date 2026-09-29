package com.bustrans.fleettrack.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "paymentcancellation")
@Data
public class PaymentCancelation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CancellationId")
    private int cancellationId;

    @Column(name = "PaymentId")
    private Long paymentId;

    @Column(name = "Reason")
    private String reason;

    @Column(name = "CancelledAt")
    private LocalDateTime cancelledAt;
}