package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.PaymentCancelation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCancellationRepository extends JpaRepository<PaymentCancelation, Integer> {
    boolean existsByPaymentId(int paymentId);
}
