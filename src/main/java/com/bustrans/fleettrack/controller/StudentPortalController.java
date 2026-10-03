package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.dto.StudentInvoiceDTO;
import com.bustrans.fleettrack.dto.StudentPaymentDTO;
import com.bustrans.fleettrack.service.FinanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Student-facing invoice and payment endpoints.
 * All identity comes from the JWT principal; no student-id path parameter.
 */
@RestController
@RequestMapping("/api/student")
public class StudentPortalController {

    private final FinanceService financeService;

    public StudentPortalController(FinanceService financeService) {
        this.financeService = financeService;
    }

    /** Returns the authenticated student's invoices, syncing from bookings first. */
    @GetMapping("/invoices")
    public List<StudentInvoiceDTO> getMyInvoices(Authentication auth) {
        return financeService.listStudentInvoices(userId(auth));
    }

    /** Upload a bank slip against an invoice.  Student id always comes from the JWT. */
    @PostMapping(value = "/invoices/{invoiceId}/payments",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StudentPaymentDTO> submitSlip(
            @PathVariable Long invoiceId,
            @RequestParam BigDecimal amount,
            @RequestParam MultipartFile slip,
            Authentication auth) {
        StudentPaymentDTO dto = financeService.submitSlip(invoiceId, userId(auth), amount, slip);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
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
