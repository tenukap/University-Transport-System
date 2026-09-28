package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.entity.T_R_Payment;
import com.bustrans.fleettrack.entity.T_R_PaymentCancellation;
import com.bustrans.fleettrack.repository.T_R_PaymentCancellationRepository;
import com.bustrans.fleettrack.repository.T_R_PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class T_R_PaymentService {

    @Autowired private T_R_PaymentRepository paymentRepo;
    @Autowired private T_R_PaymentCancellationRepository cancelRepo;

    public List<T_R_Payment> getAllPayments() {
        List<T_R_Payment> payments = paymentRepo.findAll();

        // Auto-sync: Check cancellation history and update status if needed
        for (T_R_Payment payment : payments) {
            boolean hasCancellation = cancelRepo.existsByPaymentId(payment.getPaymentId());
            if (hasCancellation && !"Cancelled".equals(payment.getStatus())) {
                payment.setStatus("Cancelled");
                paymentRepo.save(payment);  // Persist the fix
            }
        }
        return payments;
    }

    public void cancelPayment(int paymentId, String reason) {
        T_R_Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // 1. Update the payment status
        payment.setStatus("Cancelled");
        paymentRepo.save(payment);

        // 2. Log the cancellation record
        T_R_PaymentCancellation record = new T_R_PaymentCancellation();
        record.setPaymentId(paymentId);
        record.setReason(reason);
        record.setCancelledAt(LocalDateTime.now());
        cancelRepo.save(record);
    }
}
