package com.transport.uni_transport_system.service;

import com.transport.uni_transport_system.entity.Feedback;
import com.transport.uni_transport_system.form.FeedbackForm;
import com.transport.uni_transport_system.repository.FeedbackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FeedbackServiceTest {
    private final FeedbackRepository repository = mock(FeedbackRepository.class);
    private final FeedbackService service = new FeedbackService(repository);

    @ParameterizedTest
    @ValueSource(strings = {"x", "  Keep my spaces  ", "First line\nSecond line"})
    void createPreservesTextAndSetsServerDate(String text) {
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        LocalDate before = LocalDate.now();
        Feedback saved = service.createFeedback(form(text));
        assertNull(saved.getFeedbackId());
        assertEquals(text, saved.getComments());
        assertFalse(saved.getFeedbackDate().isBefore(before));
        assertFalse(saved.getFeedbackDate().isAfter(LocalDate.now()));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t\n", "\u2003"})
    void rejectsEmptyCommentsWithoutMutation(String text) {
        assertInvalid(text);
    }

    @Test
    void enforcesLengthWithoutTruncating() {
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        String maximum = "x".repeat(1000);
        assertEquals(maximum, service.createFeedback(form(maximum)).getComments());
        clearInvocations(repository);
        assertInvalid("x".repeat(1001));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2000-01-01", "2020-02-29", "2099-01-01"})
    void editsPreserveStoredDateEvenForUnusualHistoricalData(String originalDate) {
        Feedback existing = existing(LocalDate.parse(originalDate));
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        assertSame(existing, service.updateFeedback(7L, form("  Edited\ntext  ")));
        assertEquals(7L, existing.getFeedbackId());
        assertEquals(LocalDate.parse(originalDate), existing.getFeedbackDate());
        assertEquals("  Edited\ntext  ", existing.getComments());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1, 999})
    void missingRecordsCannotBeUpdatedOrDeleted(Long id) {
        assertThrows(NoSuchElementException.class, () -> service.getFeedbackById(id));
        assertThrows(NoSuchElementException.class, () -> service.updateFeedback(id, form("Valid")));
        assertThrows(NoSuchElementException.class, () -> service.deleteFeedback(id));
        verify(repository, never()).save(any());
        verify(repository, never()).delete(any());
    }

    @Test
    void deletesLoadedRecord() {
        Feedback existing = existing(LocalDate.of(2020, 1, 1));
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        service.deleteFeedback(7L);
        verify(repository).delete(existing);
    }

    private void assertInvalid(String text) {
        assertThrows(IllegalArgumentException.class, () -> service.createFeedback(form(text)));
        Feedback existing = existing(LocalDate.of(2020, 1, 1));
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        assertThrows(IllegalArgumentException.class, () -> service.updateFeedback(7L, form(text)));
        assertEquals("Original", existing.getComments());
        assertEquals(LocalDate.of(2020, 1, 1), existing.getFeedbackDate());
        verify(repository, never()).save(any());
    }

    private Feedback existing(LocalDate date) {
        Feedback feedback = new Feedback();
        feedback.setFeedbackId(7L);
        feedback.setFeedbackDate(date);
        feedback.setComments("Original");
        return feedback;
    }

    private FeedbackForm form(String text) {
        FeedbackForm form = new FeedbackForm();
        form.setComments(text);
        return form;
    }
}
