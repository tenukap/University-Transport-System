package com.bustrans.fleettrack.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentPaymentDTO(
        Long paymentId,
        BigDecimal amount,
        LocalDate paymentDate,
        String status,
        String reviewNote,
        boolean hasSlip,
        LocalDateTime submittedAt
) {}
