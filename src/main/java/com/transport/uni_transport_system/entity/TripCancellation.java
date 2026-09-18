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
    private int cancellationId;

    @Column(name = "trip_id")
    private int tripId;

    private String reason;
    private LocalDateTime cancelledAt;
}
