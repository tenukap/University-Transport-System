package com.transport.uni_transport_system.service;

import com.transport.uni_transport_system.entity.Payment;
import com.transport.uni_transport_system.entity.PaymentCancellation;
import com.transport.uni_transport_system.repository.PaymentCancellationRepository;
import com.transport.uni_transport_system.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentService {

    @Autowired private PaymentRepository paymentRepo;
    @Autowired private PaymentCancellationRepository cancelRepo;

    public List<Payment> getAllPayments() {
        List<Payment> payments = paymentRepo.findAll();

        // Auto-sync: Check cancellation history and update status if needed
        for (Payment payment : payments) {
            boolean hasCancellation = cancelRepo.existsByPaymentId(payment.getPaymentId());
            if (hasCancellation && !"Cancelled".equals(payment.getStatus())) {
                payment.setStatus("Cancelled");
                paymentRepo.save(payment);  // Persist the fix
            }
        }
        return payments;
    }

    public void cancelPayment(int paymentId, String reason) {
        Payment payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // 1. Update the payment status
        payment.setStatus("Cancelled");
        paymentRepo.save(payment);

        // 2. Log the cancellation record
        PaymentCancellation record = new PaymentCancellation();
        record.setPaymentId(paymentId);
        record.setReason(reason);
        record.setCancelledAt(LocalDateTime.now());
        cancelRepo.save(record);
    }
}
