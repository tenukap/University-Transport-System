package com.transport.uni_transport_system.entity;

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
    private int cancellationId;
    private int paymentId;
    private String reason;
    private LocalDateTime cancelledAt;
}
