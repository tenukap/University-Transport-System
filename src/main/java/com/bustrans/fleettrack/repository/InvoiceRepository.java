package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findAllByOrderByInvoiceIdDesc();
    Optional<Invoice> findByStudentUserIdAndBillingMonthAndBillingYear(
            Integer studentUserId, Integer billingMonth, Integer billingYear);
    /** All invoices for a specific student, newest first — used by sync and student view. */
    List<Invoice> findByStudentUserIdOrderByInvoiceIdDesc(Integer studentUserId);
}