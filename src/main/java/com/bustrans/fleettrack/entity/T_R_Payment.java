package com.bustrans.fleettrack.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "payment")
@Data
public class T_R_Payment {
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