package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.entity.Invoice;
import com.bustrans.fleettrack.entity.Payment;
import com.bustrans.fleettrack.form.PaymentForm;
import com.bustrans.fleettrack.repository.InvoiceRepository;
import com.bustrans.fleettrack.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
public class PaymentService {
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;

    public PaymentService(PaymentRepository paymentRepository, InvoiceRepository invoiceRepository) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Payment getPaymentById(Long id) {
        if (id == null || id <= 0) {
            throw new NoSuchElementException("Payment not found");
        }
        return paymentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Payment not found with ID: " + id));
    }

    @Transactional
    public Payment createPayment(PaymentForm form) {
        ValidatedPayment values = validate(form);
        return save(new Payment(), values);
    }

    @Transactional
    public Payment updatePayment(Long id, PaymentForm form) {
        Payment existing = getPaymentById(id);
        ValidatedPayment values = validate(form);
        return save(existing, values);
    }

    private ValidatedPayment validate(PaymentForm form) {
        if (form.getInvoiceId() == null || form.getInvoiceId() <= 0) {
            throw new IllegalArgumentException("Select a valid invoice");
        }
        BigDecimal amount = form.getAmount();
        if (amount == null) {
            throw new IllegalArgumentException("Payment amount is required");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Payment amount cannot exceed 99999999.99");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Payment amount must have at most two decimal places");
        }
        LocalDate date = form.getPaymentDate() == null ? LocalDate.now() : form.getPaymentDate();
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Payment date cannot be in the future");
        }
        if (form.getPaymentStatus() == null || form.getPaymentStatus().isBlank()) {
            throw new IllegalArgumentException("Select a payment status");
        }
        String status = form.getPaymentStatus().trim().toUpperCase(Locale.ROOT);
        if (!List.of("PAID", "PENDING", "FAILED").contains(status)) {
            throw new IllegalArgumentException("Payment status must be PAID, PENDING, or FAILED");
        }
        Invoice invoice = invoiceRepository.findById(form.getInvoiceId())
                .orElseThrow(() -> new IllegalArgumentException("Selected invoice no longer exists. Select another invoice."));
        return new ValidatedPayment(invoice, amount, date, status);
    }

    // Apply values only after every validation and lookup has succeeded.
    private Payment save(Payment payment, ValidatedPayment values) {
        payment.setInvoice(values.invoice());
        payment.setAmount(values.amount());
        payment.setPaymentDate(values.date());
        payment.setPaymentStatus(values.status());
        return paymentRepository.save(payment);
    }

    private record ValidatedPayment(Invoice invoice, BigDecimal amount, LocalDate date, String status) {}

    @Transactional
    public void deletePayment(Long id) {
        paymentRepository.delete(getPaymentById(id));
    }
}

