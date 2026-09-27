package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
}