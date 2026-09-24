package com.transport.uni_transport_system.repository;

import com.transport.uni_transport_system.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByInvoice_InvoiceId(Long invoiceId);
}
