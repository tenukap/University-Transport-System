package com.transport.uni_transport_system.repository;

import com.transport.uni_transport_system.entity.PaymentCancellation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCancellationRepository extends JpaRepository<PaymentCancellation, Integer> {
    // Custom query method - Spring generates the SQL automatically
    boolean existsByPaymentId(int paymentId);
}