package com.bustrans.fleettrack.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StudentInvoiceDTO(
        Long id,
        int billingMonth,
        int billingYear,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        BigDecimal pendingAmount,
        BigDecimal balance,
        String status,
        List<StudentPaymentDTO> payments
) {}
