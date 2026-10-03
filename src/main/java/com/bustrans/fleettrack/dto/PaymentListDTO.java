package com.bustrans.fleettrack.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentListDTO(
        Long id,
        Long invoiceId,
        String invoiceLabel,
        BigDecimal amount,
        LocalDate paymentDate,
        String status,
        // Extended fields — null for manually created payments; omitted from JSON by non_null setting
        String studentName,
        Boolean hasSlip,
        String submittedByName,
        String reviewedByName,
        LocalDateTime reviewedAt,
        String reviewNote
) {}
