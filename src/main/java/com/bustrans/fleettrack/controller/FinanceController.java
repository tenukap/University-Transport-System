package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.dto.*;
import com.bustrans.fleettrack.service.FinanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping("/invoices")
    public List<InvoiceListDTO> listInvoices() {
        return financeService.listInvoices();
    }

    @PutMapping("/invoices/{id}")
    public InvoiceListDTO updateInvoiceDueDate(@PathVariable Long id,
                                                @RequestBody UpdateInvoiceDueDateRequest req) {
        return financeService.updateInvoiceDueDate(id, req);
    }

    @GetMapping("/payments")
    public List<PaymentListDTO> listPayments() {
        return financeService.listPayments();
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentListDTO> createPayment(@RequestBody CreatePaymentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createPayment(req));
    }

    @PutMapping("/payments/{id}")
    public PaymentListDTO updatePayment(@PathVariable Long id,
                                         @RequestBody UpdatePaymentRequest req) {
        return financeService.updatePayment(id, req);
    }

    @DeleteMapping("/payments/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id) {
        financeService.deletePayment(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/payments/{id}/approve")
    public PaymentListDTO approvePayment(@PathVariable Long id, Authentication auth) {
        return financeService.approvePayment(id, userId(auth));
    }

    @PutMapping("/payments/{id}/reject")
    public PaymentListDTO rejectPayment(@PathVariable Long id,
                                         @RequestBody ReviewDecisionRequest req,
                                         Authentication auth) {
        return financeService.rejectPayment(id, req.getReason(), userId(auth));
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private Long userId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        try {
            return Long.parseLong(auth.getPrincipal().toString());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication token");
        }
    }
}
