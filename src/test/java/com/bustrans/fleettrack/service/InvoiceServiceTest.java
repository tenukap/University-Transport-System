package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.entity.Invoice;
import com.bustrans.fleettrack.form.InvoiceForm;
import com.bustrans.fleettrack.repository.PaymentRepository;
import com.bustrans.fleettrack.exception.InvoiceDeletionBlockedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.Optional;
import java.util.NoSuchElementException;
import com.bustrans.fleettrack.repository.InvoiceRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvoiceServiceTest {

    private final InvoiceRepository repository = mock(InvoiceRepository.class);
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final InvoiceService service = new InvoiceService(repository, payments);

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.000", "0.01", "1", "12.34", "12.3400", "1E+3", "99999999.99", "99999999.9900"})
    void savesRepresentableAmountsWithoutChangingTheirValue(String input) {
        InvoiceForm record = validRecord(new BigDecimal(input));
        when(repository.save(any(Invoice.class))).thenAnswer(call -> call.getArgument(0));

        Invoice saved = service.createInvoice(record);
        assertNull(saved.getInvoiceId());
        assertEquals(record.getTotalAmount(), saved.getTotalAmount());

        assertEquals(new BigDecimal(input), record.getTotalAmount());
        verify(repository).save(any(Invoice.class));
        verifyNoMoreInteractions(repository);
    }

    @ParameterizedTest
    @CsvSource({
        "NULL, Total amount is required",
        "-0.01, Total amount cannot be negative",
        "0.001, Total amount must have at most two decimal places",
        "0.009, Total amount must have at most two decimal places",
        "12.345, Total amount must have at most two decimal places",
        "12.3401, Total amount must have at most two decimal places",
        "100000000, Total amount cannot exceed 99999999.99",
        "100000000.00, Total amount cannot exceed 99999999.99"
    })
    void rejectsInvalidAmountsBeforePersistence(String input, String expectedMessage) {
        BigDecimal amount = input.equals("NULL") ? null : new BigDecimal(input);
        InvoiceForm record = validRecord(amount);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.createInvoice(record));

        assertEquals(expectedMessage, error.getMessage());
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"monthNull", "monthZero", "month13", "yearNull", "year2019",
            "issueNull", "dueNull", "reversedDates", "amount"})
    void invalidCreateAndUpdateDoNotPersistOrMutate(String invalidField) {
        InvoiceForm form = validRecord(BigDecimal.ONE);
        switch (invalidField) {
            case "monthNull" -> form.setBillingMonth(null);
            case "monthZero" -> form.setBillingMonth(0);
            case "month13" -> form.setBillingMonth(13);
            case "yearNull" -> form.setBillingYear(null);
            case "year2019" -> form.setBillingYear(2019);
            case "issueNull" -> form.setIssueDate(null);
            case "dueNull" -> form.setDueDate(null);
            case "reversedDates" -> form.setDueDate(form.getIssueDate().minusDays(1));
            case "amount" -> form.setTotalAmount(new BigDecimal("1.234"));
        }
        assertThrows(IllegalArgumentException.class, () -> service.createInvoice(form));
        Invoice existing = existingInvoice();
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        assertThrows(IllegalArgumentException.class, () -> service.updateInvoice(7L, form));
        assertEquals(7L, existing.getInvoiceId());
        assertEquals(2, existing.getBillingMonth());
        assertEquals(2020, existing.getBillingYear());
        assertEquals(LocalDate.of(2020, 2, 1), existing.getIssueDate());
        assertEquals(LocalDate.of(2020, 2, 2), existing.getDueDate());
        assertEquals(new BigDecimal("5.00"), existing.getTotalAmount());
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 12})
    void acceptsMonthBoundariesYear2020AndFutureEqualDates(int month) {
        InvoiceForm form = validRecord(BigDecimal.ZERO);
        form.setBillingMonth(month);
        form.setBillingYear(2020);
        form.setIssueDate(LocalDate.now().plusYears(1));
        form.setDueDate(form.getIssueDate());
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        Invoice saved = service.createInvoice(form);
        assertEquals(month, saved.getBillingMonth());
        assertEquals(2020, saved.getBillingYear());
        assertEquals(form.getIssueDate(), saved.getIssueDate());
        assertEquals(form.getDueDate(), saved.getDueDate());
    }

    @Test
    void updateCopiesFieldsToLoadedTarget() {
        Invoice existing = existingInvoice();
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        InvoiceForm form = validRecord(new BigDecimal("12.34"));
        assertSame(existing, service.updateInvoice(7L, form));
        assertEquals(7L, existing.getInvoiceId());
        assertEquals(form.getBillingMonth(), existing.getBillingMonth());
        assertEquals(form.getBillingYear(), existing.getBillingYear());
        assertEquals(form.getIssueDate(), existing.getIssueDate());
        assertEquals(form.getDueDate(), existing.getDueDate());
        assertEquals(form.getTotalAmount(), existing.getTotalAmount());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1, 999})
    void missingTargetsAreNotInsertedOrDeleted(Long id) {
        assertThrows(NoSuchElementException.class, () -> service.getInvoiceById(id));
        assertThrows(NoSuchElementException.class, () -> service.updateInvoice(id, validRecord(BigDecimal.ONE)));
        assertThrows(NoSuchElementException.class, () -> service.deleteInvoice(id));
        verify(repository, never()).save(any());
        verify(repository, never()).delete(any());
        verifyNoInteractions(payments);
    }

    @Test
    void referencedInvoiceIsNotDeleted() {
        when(repository.findById(7L)).thenReturn(Optional.of(existingInvoice()));
        when(payments.existsByInvoice_InvoiceId(7L)).thenReturn(true);
        assertEquals("This invoice cannot be deleted because payment records reference it.",
                assertThrows(InvoiceDeletionBlockedException.class, () -> service.deleteInvoice(7L)).getMessage());
        verify(repository, never()).delete(any());
        verify(repository, never()).flush();
        verify(payments).existsByInvoice_InvoiceId(7L);
        verifyNoMoreInteractions(payments);
    }

    @Test
    void unreferencedInvoiceIsDeletedAndFlushedWithoutChangingPayments() {
        Invoice existing = existingInvoice();
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        service.deleteInvoice(7L);
        verify(repository).delete(existing);
        verify(repository).flush();
        verify(payments).existsByInvoice_InvoiceId(7L);
        verifyNoMoreInteractions(payments);
    }

    @Test
    void lateIntegrityFailurePropagatesOutOfTransaction() {
        when(repository.findById(7L)).thenReturn(Optional.of(existingInvoice()));
        DataIntegrityViolationException conflict = new DataIntegrityViolationException("constraint");
        doThrow(conflict).when(repository).flush();
        assertSame(conflict, assertThrows(DataIntegrityViolationException.class, () -> service.deleteInvoice(7L)));
        verify(payments).existsByInvoice_InvoiceId(7L);
        verifyNoMoreInteractions(payments);
    }

    private Invoice existingInvoice() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(7L);
        invoice.setBillingMonth(2);
        invoice.setBillingYear(2020);
        invoice.setIssueDate(LocalDate.of(2020, 2, 1));
        invoice.setDueDate(LocalDate.of(2020, 2, 2));
        invoice.setTotalAmount(new BigDecimal("5.00"));
        return invoice;
    }

    private InvoiceForm validRecord(BigDecimal amount) {
        InvoiceForm record = new InvoiceForm();
        record.setBillingMonth(9);
        record.setBillingYear(2026);
        record.setIssueDate(LocalDate.of(2026, 9, 1));
        record.setDueDate(LocalDate.of(2026, 9, 30));
        record.setTotalAmount(amount);
        return record;
    }
}


