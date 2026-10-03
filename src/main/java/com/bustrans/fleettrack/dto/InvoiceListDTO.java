package com.bustrans.fleettrack.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceListDTO(
        Long id,
        String studentName,
        String studentIndex,
        int billingMonth,
        int billingYear,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        BigDecimal balance,
        String status
) {}
