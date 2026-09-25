package com.transport.uni_transport_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Data
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PaymentId")
    private int paymentId;

    @Column(name = "StudentId")
    private int studentId;

    @Column(name = "Amount")
    private BigDecimal amount;

    @Column(name = "Status")
    private String status;
}