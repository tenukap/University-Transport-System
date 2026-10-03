package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByInvoice_InvoiceId(Long invoiceId);
    List<Payment> findAllByOrderByPaymentIdDesc();
    List<Payment> findByInvoice_InvoiceId(Long invoiceId);
}
