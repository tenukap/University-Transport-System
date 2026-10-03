package com.bustrans.fleettrack.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdatePaymentRequest {
    private Long invoiceId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String status;

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
