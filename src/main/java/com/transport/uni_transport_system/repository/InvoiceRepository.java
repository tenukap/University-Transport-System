package com.transport.uni_transport_system.repository;

import com.transport.uni_transport_system.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
}