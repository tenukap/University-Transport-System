package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.entity.Payment;
import com.bustrans.fleettrack.entity.Invoice;
import com.bustrans.fleettrack.form.PaymentForm;
import com.bustrans.fleettrack.repository.InvoiceRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.Locale;
import java.util.NoSuchElementException;
import org.junit.jupiter.params.provider.NullSource;
import com.bustrans.fleettrack.repository.PaymentRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private final PaymentRepository repository = mock(PaymentRepository.class);
    private final InvoiceRepository invoices = mock(InvoiceRepository.class);
    private final PaymentService service = new PaymentService(repository, invoices);

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "1", "12.34", "12.3400", "1E+3", "99999999.99", "99999999.9900"})
    void savesRepresentableAmountsWithoutChangingTheirValue(String input) {
        PaymentForm record = validRecord(new BigDecimal(input));
        Invoice invoice = new Invoice(); invoice.setInvoiceId(1L);
        when(invoices.findById(1L)).thenReturn(Optional.of(invoice));
        when(repository.save(any(Payment.class))).thenAnswer(call -> call.getArgument(0));

        Payment saved = service.createPayment(record);
        assertSame(invoice, saved.getInvoice());
        assertNull(saved.getPaymentId());
        assertEquals(record.getAmount(), saved.getAmount());

        assertEquals(new BigDecimal(input), record.getAmount());
        verify(repository).save(any(Payment.class));
        verifyNoMoreInteractions(repository);
    }

    @ParameterizedTest
    @CsvSource({
        "NULL, Payment amount is required",
        "-0.01, Payment amount must be greater than zero",
        "0, Payment amount must be greater than zero",
        "0.000, Payment amount must be greater than zero",
        "0.001, Payment amount must have at most two decimal places",
        "0.009, Payment amount must have at most two decimal places",
        "12.345, Payment amount must have at most two decimal places",
        "12.3401, Payment amount must have at most two decimal places",
        "100000000, Payment amount cannot exceed 99999999.99",
        "100000000.00, Payment amount cannot exceed 99999999.99"
    })
    void rejectsInvalidAmountsBeforePersistence(String input, String expectedMessage) {
        BigDecimal amount = input.equals("NULL") ? null : new BigDecimal(input);
        PaymentForm record = validRecord(amount);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.createPayment(record));

        assertEquals(expectedMessage, error.getMessage());
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @CsvSource({"PAID,PAID", "PENDING,PENDING", "FAILED,FAILED", "' paid ',PAID", "pending,PENDING", "failed,FAILED"})
    void normalizesManualStatuses(String input, String expected) {
        PaymentForm form = validRecord(BigDecimal.ONE);
        form.setPaymentStatus(input);
        when(invoices.findById(1L)).thenReturn(Optional.of(new Invoice()));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        assertEquals(expected, service.createPayment(form).getPaymentStatus());
    }

    @Test
    void normalizationDoesNotDependOnTurkishLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            normalizesManualStatuses("paid", "PAID");
        } finally {
            Locale.setDefault(previous);
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t", "SUCCESS", "CANCELLED"})
    void rejectsMissingAndUnsupportedStatuses(String status) {
        PaymentForm form = validRecord(BigDecimal.ONE);
        form.setPaymentStatus(status);
        assertThrows(IllegalArgumentException.class, () -> service.createPayment(form));
        verifyNoInteractions(repository, invoices);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void rejectsInvalidInvoiceIds(Long id) {
        PaymentForm form = validRecord(BigDecimal.ONE);
        form.setInvoiceId(id);
        assertThrows(IllegalArgumentException.class, () -> service.createPayment(form));
        verifyNoInteractions(repository, invoices);
    }

    @Test
    void rejectsNonexistentInvoice() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createPayment(validRecord(BigDecimal.ONE)));
        verify(invoices).findById(1L);
        verifyNoInteractions(repository);
    }

    @Test
    void updateUsesLoadedTargetAndAllowsInvoiceAndManualStatusChanges() {
        Payment existing = existingPayment();
        Invoice selected = new Invoice();
        selected.setInvoiceId(1L);
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(invoices.findById(1L)).thenReturn(Optional.of(selected));
        when(repository.save(existing)).thenReturn(existing);
        PaymentForm form = validRecord(new BigDecimal("12.34"));
        form.setPaymentStatus(" paid ");
        assertSame(existing, service.updatePayment(7L, form));
        assertEquals(7L, existing.getPaymentId());
        assertSame(selected, existing.getInvoice());
        assertEquals(new BigDecimal("12.34"), existing.getAmount());
        assertEquals("PAID", existing.getPaymentStatus());
        assertEquals(form.getPaymentDate(), existing.getPaymentDate());
    }

    @ParameterizedTest
    @ValueSource(strings = {"amount", "date", "status", "invoice"})
    void invalidUpdateDoesNotMutateExistingPayment(String invalidField) {
        Payment existing = existingPayment();
        Invoice originalInvoice = existing.getInvoice();
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        PaymentForm form = validRecord(new BigDecimal("12.34"));
        switch (invalidField) {
            case "amount" -> form.setAmount(new BigDecimal("1.234"));
            case "date" -> form.setPaymentDate(LocalDate.now().plusDays(1));
            case "status" -> form.setPaymentStatus("INVALID");
            // Empty repository result represents an invoice deleted before submission.
            case "invoice" -> form.setInvoiceId(99L);
        }
        assertThrows(IllegalArgumentException.class, () -> service.updatePayment(7L, form));
        assertSame(originalInvoice, existing.getInvoice());
        assertEquals(new BigDecimal("5.00"), existing.getAmount());
        assertEquals("FAILED", existing.getPaymentStatus());
        assertEquals(LocalDate.of(2020, 2, 1), existing.getPaymentDate());
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1, 999})
    void missingUpdateTargetCannotBecomeAnInsert(Long id) {
        assertThrows(NoSuchElementException.class,
                () -> service.updatePayment(id, validRecord(BigDecimal.ONE)));
        verify(repository, never()).save(any());
        verifyNoInteractions(invoices);
    }

    @Test
    void omittedDateStillDefaultsToToday() {
        PaymentForm form = validRecord(BigDecimal.ONE);
        form.setPaymentDate(null);
        when(invoices.findById(1L)).thenReturn(Optional.of(new Invoice()));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        LocalDate before = LocalDate.now();
        Payment saved = service.createPayment(form);
        assertFalse(saved.getPaymentDate().isBefore(before));
        assertFalse(saved.getPaymentDate().isAfter(LocalDate.now()));
        assertNull(form.getPaymentDate());
    }

    @Test
    void deletesLoadedPaymentRecord() {
        Payment existing = existingPayment();
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        service.deletePayment(7L);
        verify(repository).delete(existing);
        verifyNoInteractions(invoices);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1, 999})
    void missingPaymentCannotBeDeleted(Long id) {
        assertThrows(NoSuchElementException.class, () -> service.deletePayment(id));
        verify(repository, never()).delete(any());
        verifyNoInteractions(invoices);
    }

    private Payment existingPayment() {
        Payment payment = new Payment();
        payment.setPaymentId(7L);
        payment.setInvoice(new Invoice());
        payment.setAmount(new BigDecimal("5.00"));
        payment.setPaymentStatus("FAILED");
        payment.setPaymentDate(LocalDate.of(2020, 2, 1));
        return payment;
    }

    private PaymentForm validRecord(BigDecimal amount) {
        PaymentForm record = new PaymentForm();
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(1L);
        record.setInvoiceId(invoice.getInvoiceId());
        record.setPaymentDate(LocalDate.of(2020, 1, 1));
        record.setPaymentStatus("PENDING");
        record.setAmount(amount);
        return record;
    }
}


