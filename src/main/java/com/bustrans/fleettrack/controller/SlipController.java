package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.entity.Payment;
import com.bustrans.fleettrack.repository.PaymentRepository;
import com.bustrans.fleettrack.service.SlipStorageService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Streams bank slip files.  Access rules enforced in code:
 * - STUDENT: may only fetch their own invoice's slip.
 * - FINANCE_OFFICER, ADMIN: unrestricted.
 * Slips are never served as static files.
 */
@RestController
@RequestMapping("/api/slips")
public class SlipController {

    private final PaymentRepository paymentRepository;
    private final SlipStorageService slipStorageService;

    public SlipController(PaymentRepository paymentRepository,
                          SlipStorageService slipStorageService) {
        this.paymentRepository = paymentRepository;
        this.slipStorageService = slipStorageService;
    }

    @GetMapping("/{paymentId}")
    public void downloadSlip(@PathVariable Long paymentId,
                              Authentication auth,
                              HttpServletResponse response) throws IOException {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slip not found"));

        if (payment.getSlipFileName() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No slip attached to this payment");
        }

        // Students may only download a slip belonging to their own invoice
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority())
                .orElse("");
        if ("ROLE_STUDENT".equals(role)) {
            Long callerId = Long.parseLong(auth.getPrincipal().toString());
            Integer ownerStudentId = payment.getInvoice().getStudentUserId();
            // Return 404 (not 403) so a student cannot discover other invoices exist
            if (ownerStudentId == null || !callerId.equals(ownerStudentId.longValue())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Slip not found");
            }
        }

        Path file = slipStorageService.load(payment.getSlipFileName());
        if (!Files.exists(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Slip file not found on disk");
        }

        String contentType = slipStorageService.contentType(payment.getSlipFileName());
        String displayName = payment.getSlipOriginalName() != null
                ? payment.getSlipOriginalName() : payment.getSlipFileName();

        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "inline; filename=\"" + displayName + "\"");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setContentLengthLong(Files.size(file));

        try (OutputStream out = response.getOutputStream()) {
            Files.copy(file, out);
        }
    }
}
