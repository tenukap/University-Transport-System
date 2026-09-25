package com.transport.uni_transport_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "tripcancellation")
@Data
public class TripCancellation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CancellationId")
    private int cancellationId;

    @Column(name = "TripId")
    private int tripId;

    @Column(name = "Reason")
    private String reason;

    @Column(name = "CancelledAt")
    private LocalDateTime cancelledAt;
}