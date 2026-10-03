package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.dto.*;
import com.bustrans.fleettrack.entity.*;
import com.bustrans.fleettrack.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FinanceService {

    private final StudentRepository studentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final SlipStorageService slipStorageService;

    public FinanceService(StudentRepository studentRepository,
                          InvoiceRepository invoiceRepository,
                          PaymentRepository paymentRepository,
                          BookingRepository bookingRepository,
                          UserRepository userRepository,
                          SlipStorageService slipStorageService) {
        this.studentRepository = studentRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.slipStorageService = slipStorageService;
    }

    // ── Invoice sync ──────────────────────────────────────────────────────────

    /**
     * Syncs invoices for all students before the finance officer list is served.
     * The invoice is a live view of bookings, so it is refreshed whenever someone looks at it.
     */
    void syncAllStudentInvoices() {
        studentRepository.findAll().forEach(s -> syncStudentInvoices(s.getUserId()));
    }

    /**
     * Ensures exactly one invoice exists per (student, billing_month, billing_year) that
     * has at least one non-CANCELLED booking.  Creates, updates-total, or deletes invoices
     * as needed.  Never overwrites dueDate on an existing invoice (finance officer may have edited it).
     *
     * Called inside a @Transactional method so it participates in the caller's transaction.
     */
    void syncStudentInvoices(Long studentUserId) {
        // Load all non-CANCELLED bookings for this student (trip date drives the billing month)
        List<Booking> bookings = bookingRepository.findByUser_UserIdAndStatusNot(studentUserId, "CANCELLED");

        // Group by billing year|month
        Map<String, List<Booking>> byMonthYear = bookings.stream()
                .filter(b -> b.getBusTrip() != null && b.getBusTrip().getTripDate() != null)
                .collect(Collectors.groupingBy(b -> {
                    LocalDate d = b.getBusTrip().getTripDate();
                    return d.getYear() + "|" + d.getMonthValue();
                }));

        // Existing invoices for this student, keyed the same way
        Map<String, Invoice> existingByKey = invoiceRepository
                .findByStudentUserIdOrderByInvoiceIdDesc(studentUserId.intValue())
                .stream()
                .collect(Collectors.toMap(
                        inv -> inv.getBillingYear() + "|" + inv.getBillingMonth(),
                        inv -> inv,
                        (a, b) -> a)); // keep first on duplicate (DB unique index prevents it)

        Set<String> seen = new HashSet<>();

        for (Map.Entry<String, List<Booking>> entry : byMonthYear.entrySet()) {
            String key = entry.getKey();
            seen.add(key);
            String[] parts = key.split("\\|");
            int year  = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            BigDecimal total = sumFares(entry.getValue());

            Invoice inv = existingByKey.get(key);
            if (inv == null) {
                // Natural due date: last day of billing month + 14 days.
                // If that is before today (past month), use today so chk_invoice_dates passes.
                LocalDate naturalDue = YearMonth.of(year, month).atEndOfMonth().plusDays(14);
                LocalDate dueDate = naturalDue.isBefore(LocalDate.now()) ? LocalDate.now() : naturalDue;

                inv = new Invoice();
                inv.setStudentUserId(studentUserId.intValue());
                inv.setBillingMonth(month);
                inv.setBillingYear(year);
                inv.setIssueDate(LocalDate.now());
                inv.setDueDate(dueDate);
                inv.setTotalAmount(total);
                invoiceRepository.save(inv);
            } else if (inv.getTotalAmount().compareTo(total) != 0) {
                // Only update total; finance officer may have edited dueDate — leave it alone.
                // A PAID invoice intentionally reverts to PENDING if new bookings raise the total.
                inv.setTotalAmount(total);
                invoiceRepository.save(inv);
            }
        }

        // Invoices with no remaining billable bookings
        for (Map.Entry<String, Invoice> entry : existingByKey.entrySet()) {
            if (!seen.contains(entry.getKey())) {
                Invoice orphan = entry.getValue();
                List<Payment> orphanPayments = paymentRepository.findByInvoice_InvoiceId(orphan.getInvoiceId());
                if (orphanPayments.isEmpty()) {
                    invoiceRepository.delete(orphan);
                } else {
                    // Has payments (likely a credit from a booking that was later cancelled).
                    // Keep invoice at total 0 so amountPaid >= 0 == total => status PAID.
                    if (orphan.getTotalAmount().compareTo(BigDecimal.ZERO) != 0) {
                        orphan.setTotalAmount(BigDecimal.ZERO);
                        invoiceRepository.save(orphan);
                    }
                }
            }
        }
    }

    // ── Finance invoices ──────────────────────────────────────────────────────

    @Transactional
    public List<InvoiceListDTO> listInvoices() {
        syncAllStudentInvoices(); // Invoice is a live view of bookings; sync before serving the list
        List<Invoice> invoices = invoiceRepository.findAllByOrderByInvoiceIdDesc();
        Map<Long, BigDecimal> paidMap = buildPaidMap();
        Map<Long, Student> studentMap = buildStudentMap();
        return invoices.stream()
                .map(inv -> toInvoiceListDTO(inv, paidMap, studentMap))
                .collect(Collectors.toList());
    }

    @Transactional
    public InvoiceListDTO updateInvoiceDueDate(Long id, UpdateInvoiceDueDateRequest req) {
        Invoice invoice = requireInvoiceExists(id);
        if (req.getDueDate() == null || req.getDueDate().isBefore(invoice.getIssueDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Due date must not be before the issue date");
        }
        invoice.setDueDate(req.getDueDate());
        invoice = invoiceRepository.save(invoice);
        return toInvoiceListDTO(invoice, buildPaidMap(), buildStudentMap());
    }

    // ── Finance payments ──────────────────────────────────────────────────────

    public List<PaymentListDTO> listPayments() {
        List<Payment> payments = paymentRepository.findAllByOrderByPaymentIdDesc();
        Map<Long, Student> studentMap = buildStudentMap();
        return payments.stream()
                .map(p -> toPaymentListDTO(p, studentMap))
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentListDTO createPayment(CreatePaymentRequest req) {
        Invoice invoice = requireInvoiceByIdOrBadRequest(req.getInvoiceId());
        BigDecimal amount = validateAmount(req.getAmount());
        LocalDate paymentDate = validatePaymentDate(req.getPaymentDate());
        String status = validateStatus(req.getStatus());
        if ("PAID".equals(status)) {
            checkOverpayment(invoice, amount, null);
        }
        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(amount);
        payment.setPaymentDate(paymentDate);
        payment.setPaymentStatus(status);
        payment = paymentRepository.save(payment);
        return toPaymentListDTO(payment, buildStudentMap());
    }

    @Transactional
    public PaymentListDTO updatePayment(Long id, UpdatePaymentRequest req) {
        Payment payment = requirePaymentExists(id);
        // Slip-backed payments must go through approve/reject, not direct edit
        if (payment.getSlipFileName() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Use approve or reject for student submissions");
        }
        Invoice invoice = requireInvoiceByIdOrBadRequest(req.getInvoiceId());
        BigDecimal amount = validateAmount(req.getAmount());
        LocalDate paymentDate = validatePaymentDate(req.getPaymentDate());
        String status = validateStatus(req.getStatus());
        if ("PAID".equals(status)) {
            checkOverpayment(invoice, amount, id);
        }
        payment.setInvoice(invoice);
        payment.setAmount(amount);
        payment.setPaymentDate(paymentDate);
        payment.setPaymentStatus(status);
        payment = paymentRepository.save(payment);
        return toPaymentListDTO(payment, buildStudentMap());
    }

    @Transactional
    public void deletePayment(Long id) {
        Payment payment = requirePaymentExists(id);
        if ("PAID".equals(payment.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Paid payments are kept for the record");
        }
        // Payments with a slip are kept for the audit trail regardless of status
        if (payment.getSlipFileName() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Payments with a slip are kept for the record");
        }
        paymentRepository.delete(payment);
    }

    // ── Finance review ────────────────────────────────────────────────────────

    @Transactional
    public PaymentListDTO approvePayment(Long paymentId, Long reviewerUserId) {
        Payment payment = requirePaymentExists(paymentId);
        if (!"PENDING".equals(payment.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only PENDING payments can be approved");
        }
        // Re-run overpayment guard: this payment is PENDING so it is not yet in the PAID sum;
        // passing null includes all existing PAID payments, then checks adding this amount.
        checkOverpayment(payment.getInvoice(), payment.getAmount(), null);

        payment.setPaymentStatus("PAID");
        payment.setReviewedByUserId(reviewerUserId.intValue());
        payment.setReviewedAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);
        return toPaymentListDTO(payment, buildStudentMap());
    }

    @Transactional
    public PaymentListDTO rejectPayment(Long paymentId, String reason, Long reviewerUserId) {
        Payment payment = requirePaymentExists(paymentId);
        if (!"PENDING".equals(payment.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only PENDING payments can be rejected");
        }
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Rejection reason is required");
        }
        if (reason.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Rejection reason must not exceed 255 characters");
        }
        payment.setPaymentStatus("FAILED");
        payment.setReviewNote(reason);
        payment.setReviewedByUserId(reviewerUserId.intValue());
        payment.setReviewedAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);
        return toPaymentListDTO(payment, buildStudentMap());
    }

    // ── Student invoices ──────────────────────────────────────────────────────

    @Transactional
    public List<StudentInvoiceDTO> listStudentInvoices(Long studentUserId) {
        syncStudentInvoices(studentUserId); // Invoice is a live view; sync on every read
        List<Invoice> invoices = invoiceRepository.findByStudentUserIdOrderByInvoiceIdDesc(studentUserId.intValue());
        return invoices.stream().map(inv -> {
            List<Payment> pmts = paymentRepository.findByInvoice_InvoiceId(inv.getInvoiceId());
            BigDecimal amountPaid    = sumByStatus(pmts, "PAID");
            BigDecimal pendingAmount = sumByStatus(pmts, "PENDING");
            // Balance clamped to 0: if amountPaid exceeds total (credit case), show 0 not negative
            BigDecimal balance = inv.getTotalAmount().subtract(amountPaid).max(BigDecimal.ZERO);
            String status = computeStatus(inv.getTotalAmount(), amountPaid, inv.getDueDate());

            List<StudentPaymentDTO> paymentDtos = pmts.stream()
                    .map(p -> new StudentPaymentDTO(
                            p.getPaymentId(), p.getAmount(), p.getPaymentDate(),
                            p.getPaymentStatus(), p.getReviewNote(),
                            p.getSlipFileName() != null,
                            p.getReviewedAt())) // reusing reviewedAt as an indication of when reviewed; submittedAt not stored separately
                    .collect(Collectors.toList());

            return new StudentInvoiceDTO(inv.getInvoiceId(),
                    inv.getBillingMonth(), inv.getBillingYear(),
                    inv.getIssueDate(), inv.getDueDate(),
                    inv.getTotalAmount(), amountPaid, pendingAmount, balance, status,
                    paymentDtos);
        }).collect(Collectors.toList());
    }

    @Transactional
    public StudentPaymentDTO submitSlip(Long invoiceId, Long studentUserId,
                                        BigDecimal amount, MultipartFile slip) {
        // Load invoice; 404 if it belongs to a different student (do not reveal it exists)
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
        if (invoice.getStudentUserId() == null
                || !invoice.getStudentUserId().equals(studentUserId.intValue())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than 0");
        }

        List<Payment> existing = paymentRepository.findByInvoice_InvoiceId(invoiceId);

        // Only one PENDING slip submission allowed at a time per invoice
        boolean hasPending = existing.stream()
                .anyMatch(p -> "PENDING".equals(p.getPaymentStatus()));
        if (hasPending) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You already have a payment waiting for review");
        }

        BigDecimal amountPaid    = sumByStatus(existing, "PAID");
        BigDecimal pendingAmount = sumByStatus(existing, "PENDING"); // 0 since no PENDING above
        BigDecimal availableBalance = invoice.getTotalAmount().subtract(amountPaid).subtract(pendingAmount);

        if (amount.compareTo(availableBalance) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Amount exceeds the outstanding balance, including slips awaiting review");
        }

        slipStorageService.validate(slip);

        String storedName;
        try {
            storedName = slipStorageService.store(slip);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not save the slip file. Please try again.");
        }

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(amount);
        payment.setPaymentDate(LocalDate.now());
        payment.setPaymentStatus("PENDING");
        payment.setSlipFileName(storedName);
        payment.setSlipOriginalName(slip.getOriginalFilename());
        payment.setSubmittedByUserId(studentUserId.intValue());
        payment = paymentRepository.save(payment);

        return new StudentPaymentDTO(payment.getPaymentId(), payment.getAmount(),
                payment.getPaymentDate(), payment.getPaymentStatus(),
                null, true, null);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private BigDecimal sumFares(List<Booking> bookings) {
        return bookings.stream()
                .map(Booking::getFareAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumByStatus(List<Payment> payments, String status) {
        return payments.stream()
                .filter(p -> status.equals(p.getPaymentStatus()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String computeStatus(BigDecimal total, BigDecimal amountPaid, LocalDate dueDate) {
        if (amountPaid.compareTo(total) >= 0) return "PAID";
        if (LocalDate.now().isAfter(dueDate))  return "OVERDUE";
        return "PENDING";
    }

    private Invoice requireInvoiceExists(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
    }

    private Invoice requireInvoiceByIdOrBadRequest(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice is required");
        }
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice not found"));
    }

    private Payment requirePaymentExists(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
    }

    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than 0");
        }
        return amount;
    }

    private LocalDate validatePaymentDate(LocalDate date) {
        if (date == null) date = LocalDate.now();
        if (date.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment date cannot be in the future");
        }
        return date;
    }

    private String validateStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
        }
        String s = status.trim().toUpperCase(Locale.ROOT);
        if (!List.of("PAID", "PENDING", "FAILED").contains(s)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be PAID, PENDING, or FAILED");
        }
        return s;
    }

    /**
     * Guards against PAID payments pushing the invoice total over its face value.
     * excludePaymentId: exclude that payment from the existing PAID sum (used when editing).
     */
    private void checkOverpayment(Invoice invoice, BigDecimal amount, Long excludePaymentId) {
        BigDecimal existingPaid = paymentRepository.findByInvoice_InvoiceId(invoice.getInvoiceId())
                .stream()
                .filter(p -> "PAID".equals(p.getPaymentStatus()))
                .filter(p -> excludePaymentId == null || !p.getPaymentId().equals(excludePaymentId))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (existingPaid.add(amount).compareTo(invoice.getTotalAmount()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment exceeds the invoice balance");
        }
    }

    private Map<Long, BigDecimal> buildPaidMap() {
        return paymentRepository.findAll().stream()
                .filter(p -> "PAID".equals(p.getPaymentStatus()))
                .collect(Collectors.groupingBy(
                        p -> p.getInvoice().getInvoiceId(),
                        Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)));
    }

    private Map<Long, Student> buildStudentMap() {
        return studentRepository.findAll().stream()
                .collect(Collectors.toMap(Student::getUserId, s -> s));
    }

    private String lookupUserName(Integer userId) {
        if (userId == null) return null;
        return userRepository.findById(userId.longValue())
                .map(User::getFullName)
                .orElse(null);
    }

    private InvoiceListDTO toInvoiceListDTO(Invoice inv, Map<Long, BigDecimal> paidMap,
                                             Map<Long, Student> studentMap) {
        BigDecimal amountPaid = paidMap.getOrDefault(inv.getInvoiceId(), BigDecimal.ZERO);
        // Clamp to 0: if amountPaid exceeds total (credit from cancelled bookings), show 0 not negative
        BigDecimal balance = inv.getTotalAmount().subtract(amountPaid).max(BigDecimal.ZERO);
        String status = computeStatus(inv.getTotalAmount(), amountPaid, inv.getDueDate());

        String studentName = "";
        String studentIndex = "";
        if (inv.getStudentUserId() != null) {
            Student s = studentMap.get(inv.getStudentUserId().longValue());
            if (s != null) {
                studentName  = s.getUser() != null ? s.getUser().getFullName()  : "Unknown";
                studentIndex = s.getStudentIndex() != null ? s.getStudentIndex() : "";
            }
        }

        return new InvoiceListDTO(inv.getInvoiceId(), studentName, studentIndex,
                inv.getBillingMonth(), inv.getBillingYear(),
                inv.getIssueDate(), inv.getDueDate(),
                inv.getTotalAmount(), amountPaid, balance, status);
    }

    private PaymentListDTO toPaymentListDTO(Payment p, Map<Long, Student> studentMap) {
        Invoice inv = p.getInvoice();
        String label = buildInvoiceLabel(inv, studentMap);

        String studentName = null;
        if (inv.getStudentUserId() != null) {
            Student s = studentMap.get(inv.getStudentUserId().longValue());
            studentName = s != null && s.getUser() != null ? s.getUser().getFullName() : null;
        }

        return new PaymentListDTO(
                p.getPaymentId(), inv.getInvoiceId(), label,
                p.getAmount(), p.getPaymentDate(), p.getPaymentStatus(),
                studentName,
                p.getSlipFileName() != null,
                lookupUserName(p.getSubmittedByUserId()),
                lookupUserName(p.getReviewedByUserId()),
                p.getReviewedAt(),
                p.getReviewNote());
    }

    private String buildInvoiceLabel(Invoice invoice, Map<Long, Student> studentMap) {
        String name = "Unknown";
        if (invoice.getStudentUserId() != null) {
            Student s = studentMap.get(invoice.getStudentUserId().longValue());
            if (s != null && s.getUser() != null) name = s.getUser().getFullName();
        }
        return name + " – " + String.format("%02d/%d",
                invoice.getBillingMonth(), invoice.getBillingYear());
    }
}
