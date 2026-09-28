package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.T_R_PaymentCancellation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface T_R_PaymentCancellationRepository extends JpaRepository<T_R_PaymentCancellation, Integer> {
    // Custom query method - Spring generates the SQL automatically
    boolean existsByPaymentId(int paymentId);
}