package com.transport.uni_transport_system.service;

import com.transport.uni_transport_system.entity.Invoice;
import com.transport.uni_transport_system.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.transport.uni_transport_system.form.InvoiceForm;
import com.transport.uni_transport_system.repository.PaymentRepository;
import com.transport.uni_transport_system.exception.InvoiceDeletionBlockedException;
import java.util.NoSuchElementException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InvoiceService {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");

    private final InvoiceRepository invoiceRepository;

    private final PaymentRepository paymentRepository;
    public InvoiceService(InvoiceRepository invoiceRepository, PaymentRepository paymentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    // READ - Get all invoices
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    // READ - Get one invoice by ID
    public Invoice getInvoiceById(Long id) {
        if (id == null || id <= 0) throw new NoSuchElementException("Invoice not found");
        return invoiceRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Invoice not found with ID: " + id
                        ));
    }

    // CREATE / UPDATE invoice
    private void validate(InvoiceForm invoice) {

        // Validate billing month
        if (invoice.getBillingMonth() == null ||
                invoice.getBillingMonth() < 1 ||
                invoice.getBillingMonth() > 12) {

            throw new IllegalArgumentException(
                    "Billing month must be between 1 and 12"
            );
        }

        // Validate billing year
        if (invoice.getBillingYear() == null ||
                invoice.getBillingYear() < 2020) {

            throw new IllegalArgumentException(
                    "Billing year must be 2020 or later"
            );
        }

        // Validate issue date
        if (invoice.getIssueDate() == null) {
            throw new IllegalArgumentException(
                    "Issue date is required"
            );
        }

        // Validate due date
        if (invoice.getDueDate() == null) {
            throw new IllegalArgumentException(
                    "Due date is required"
            );
        }

        // Due date cannot be earlier than issue date
        if (invoice.getDueDate().isBefore(invoice.getIssueDate())) {

            throw new IllegalArgumentException(
                    "Due date cannot be before issue date"
            );
        }

        // Validate amount
        BigDecimal amount = invoice.getTotalAmount();
        if (amount == null) {
            throw new IllegalArgumentException("Total amount is required");
        }
        if (amount.signum() < 0) {

            throw new IllegalArgumentException(
                    "Total amount cannot be negative"
            );
        }

        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Total amount cannot exceed 99999999.99");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Total amount must have at most two decimal places");
        }


    }

    @Transactional
    public Invoice createInvoice(InvoiceForm form) {
        validate(form);
        return save(new Invoice(), form);
    }

    @Transactional
    public Invoice updateInvoice(Long id, InvoiceForm form) {
        Invoice existing = getInvoiceById(id);
        validate(form);
        return save(existing, form);
    }

    private Invoice save(Invoice invoice, InvoiceForm form) {
        invoice.setBillingMonth(form.getBillingMonth());
        invoice.setBillingYear(form.getBillingYear());
        invoice.setIssueDate(form.getIssueDate());
        invoice.setDueDate(form.getDueDate());
        invoice.setTotalAmount(form.getTotalAmount());
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public void deleteInvoice(Long id) {
        Invoice invoice = getInvoiceById(id);
        if (paymentRepository.existsByInvoice_InvoiceId(id)) {
            throw new InvoiceDeletionBlockedException();
        }
        invoiceRepository.delete(invoice);
        // Force constraint checks before returning. The controller handles conflicts
        // only after this transaction has rolled back.
        invoiceRepository.flush();
    }
}
