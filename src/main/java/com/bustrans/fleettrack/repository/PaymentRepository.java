package com.bustrans.fleettrack.repository;

import com.bustrans.fleettrack.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
}