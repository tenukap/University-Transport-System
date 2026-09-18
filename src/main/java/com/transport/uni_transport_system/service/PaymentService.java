package com.transport.uni_transport_system.service;

import com.transport.uni_transport_system.entity.Payment;
import com.transport.uni_transport_system.entity.PaymentCancellation;
import com.transport.uni_transport_system.repository.PaymentCancellationRepository;
import com.transport.uni_transport_system.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class PaymentService {
    @Autowired private PaymentRepository paymentRepo;
    @Autowired private PaymentCancellationRepository cancelRepo;

    public void cancelPayment(int paymentId, String reason) {
        Payment payment = paymentRepo.findById(paymentId).orElseThrow();
        payment.setStatus("Cancelled");
        paymentRepo.save(payment);

        PaymentCancellation record = new PaymentCancellation();
        record.setPaymentId(paymentId);
        record.setReason(reason);
        record.setCancelledAt(LocalDateTime.now());
        cancelRepo.save(record);
    }
}