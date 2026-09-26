package com.bustrans.fleettrack.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "paymentcancellation")
@Data
public class PaymentCancellation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CancellationId")
    private int cancellationId;

    @Column(name = "PaymentId")
    private int paymentId;

    @Column(name = "Reason")
    private String reason;

    @Column(name = "CancelledAt")
    private LocalDateTime cancelledAt;
}