package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.entity.Feedback;
import com.bustrans.fleettrack.form.FeedbackForm;
import com.bustrans.fleettrack.repository.FeedbackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.bustrans.fleettrack.entity.User;
import com.bustrans.fleettrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FeedbackServiceTest {
    private final FeedbackRepository repository = mock(FeedbackRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final FeedbackService service = new FeedbackService(repository, users);
    private final User owner = new User();
    @BeforeEach void setupUser() {
        owner.setUserId(11L);
        when(users.findById(11L)).thenReturn(Optional.of(owner));
    }

    @ParameterizedTest
    @ValueSource(strings = {"x", "  Keep my spaces  ", "First line\nSecond line"})
    void createPreservesTextAndSetsServerDate(String text) {
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        LocalDateTime before = LocalDateTime.now();
        Feedback saved = service.createFeedback(form(text), 11L);
        assertNull(saved.getFeedbackId());
        assertEquals(text, saved.getComments());
        assertEquals("Transport", saved.getSubject());
        assertSame(owner, saved.getUser());
        assertEquals("Pending", saved.getStatus());
        assertFalse(saved.getSubmittedAt().isBefore(before));
        assertFalse(saved.getSubmittedAt().isAfter(LocalDateTime.now()));
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
        assertEquals(maximum, service.createFeedback(form(maximum), 11L).getComments());
        clearInvocations(repository);
        assertInvalid("x".repeat(1001));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2000-01-01", "2020-02-29", "2099-01-01"})
    void editsPreserveStoredDateEvenForUnusualHistoricalData(String originalDate) {
        Feedback existing = existing(LocalDate.parse(originalDate));
        when(repository.findByFeedbackIdAndUser_UserId(7, 11L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        assertSame(existing, service.updateFeedback(7, form("  Edited\ntext  "), 11L));
        assertEquals(7, existing.getFeedbackId());
        assertEquals(LocalDate.parse(originalDate).atStartOfDay(), existing.getSubmittedAt());
        assertEquals("  Edited\ntext  ", existing.getComments());
        assertSame(owner, existing.getUser());
        assertEquals("Reviewed", existing.getStatus());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1, 999})
    void missingRecordsCannotBeUpdatedOrDeleted(Integer id) {
        assertThrows(NoSuchElementException.class, () -> service.getFeedbackById(id, 11L));
        assertThrows(NoSuchElementException.class, () -> service.updateFeedback(id, form("Valid"), 11L));
        assertThrows(NoSuchElementException.class, () -> service.deleteFeedback(id, 11L));
        verify(repository, never()).save(any());
        verify(repository, never()).delete(any());
    }

    @Test
    void deletesLoadedRecord() {
        Feedback existing = existing(LocalDate.of(2020, 1, 1));
        when(repository.findByFeedbackIdAndUser_UserId(7, 11L)).thenReturn(Optional.of(existing));
        service.deleteFeedback(7, 11L);
        verify(repository).delete(existing);
    }

    private void assertInvalid(String text) {
        assertThrows(IllegalArgumentException.class, () -> service.createFeedback(form(text), 11L));
        Feedback existing = existing(LocalDate.of(2020, 1, 1));
        when(repository.findByFeedbackIdAndUser_UserId(7, 11L)).thenReturn(Optional.of(existing));
        assertThrows(IllegalArgumentException.class, () -> service.updateFeedback(7, form(text), 11L));
        assertEquals("Original", existing.getComments());
        assertEquals(LocalDate.of(2020, 1, 1).atStartOfDay(), existing.getSubmittedAt());
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t\n", "\u2003"})
    void rejectsBlankSubjectsBeforeMutation(String subject) {
        FeedbackForm form = form("Valid");
        form.setSubject(subject);
        Feedback record = existing(LocalDate.of(2020, 1, 1));
        when(repository.findByFeedbackIdAndUser_UserId(7, 11L)).thenReturn(Optional.of(record));
        assertThrows(IllegalArgumentException.class, () -> service.createFeedback(form, 11L));
        assertThrows(IllegalArgumentException.class, () -> service.updateFeedback(7, form, 11L));
        assertEquals("Original subject", record.getSubject());
        assertEquals("Original", record.getComments());
        verify(repository, never()).save(any());
    }

    @Test
    void subjectBoundaryAndWhitespaceArePreserved() {
        FeedbackForm form = form("Valid");
        form.setSubject(" " + "x".repeat(148) + " ");
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        assertEquals(form.getSubject(), service.createFeedback(form, 11L).getSubject());
        clearInvocations(repository);
        form.setSubject("x".repeat(151));
        assertThrows(IllegalArgumentException.class, () -> service.createFeedback(form, 11L));
        verify(repository, never()).save(any());
    }

    @Test
    void preservesNullableAndPreciseHistoricalMetadata() {
        Feedback record = existing(LocalDate.of(2020, 1, 1));
        record.setStatus(null);
        record.setSubmittedAt(null);
        when(repository.findByFeedbackIdAndUser_UserId(7, 11L)).thenReturn(Optional.of(record));
        service.updateFeedback(7, form("Edited"), 11L);
        assertNull(record.getStatus());
        assertNull(record.getSubmittedAt());
        LocalDateTime timestamp = LocalDateTime.of(2000, 2, 3, 4, 5, 6, 123456700);
        record.setSubmittedAt(timestamp);
        service.updateFeedback(7, form("Edited again"), 11L);
        assertEquals(timestamp, record.getSubmittedAt());
        assertSame(owner, record.getUser());
    }

    @Test
    void oversizedHistoricalMessageIsNeverSilentlyTruncated() {
        Feedback record = existing(LocalDate.of(2020, 1, 1));
        String original = "x".repeat(1500);
        record.setComments(original);
        when(repository.findByFeedbackIdAndUser_UserId(7, 11L)).thenReturn(Optional.of(record));
        assertEquals(original, service.getFeedbackById(7, 11L).getComments());
        assertThrows(IllegalArgumentException.class, () -> service.updateFeedback(7, form(original), 11L));
        assertEquals(original, record.getComments());
        verify(repository, never()).save(any());
        service.updateFeedback(7, form("Explicit shorter edit"), 11L);
        assertEquals("Explicit shorter edit", record.getComments());
    }

    @Test
    void anotherOwnerCannotReadUpdateOrDelete() {
        assertThrows(NoSuchElementException.class, () -> service.getFeedbackById(7, 22L));
        assertThrows(NoSuchElementException.class, () -> service.updateFeedback(7, form("Attempt"), 22L));
        assertThrows(NoSuchElementException.class, () -> service.deleteFeedback(7, 22L));
        verify(repository, times(3)).findByFeedbackIdAndUser_UserId(7, 22L);
        verify(repository, never()).findById(anyInt());
        verify(repository, never()).save(any());
        verify(repository, never()).delete(any());
    }

    @Test
    void listingIsOwnerScoped() {
        service.getAllFeedback(11L);
        verify(repository).findByUser_UserId(11L);
        verify(repository, never()).findAll();
    }

    @Test
    void deletedAuthenticatedUserCannotCreateFeedback() {
        when(users.findById(11L)).thenReturn(Optional.empty());
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.createFeedback(form("Valid"), 11L));
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1, 2147483648L})
    void invalidOwnerNeverReachesPersistence(Long id) {
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.getAllFeedback(id));
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.getFeedbackById(7, id));
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.createFeedback(form("Valid"), id));
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.updateFeedback(7, form("Valid"), id));
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> service.deleteFeedback(7, id));
        verifyNoInteractions(repository);
    }

    private Feedback existing(LocalDate date) {
        Feedback feedback = new Feedback();
        feedback.setFeedbackId(7);
        feedback.setUser(owner);
        feedback.setSubject("Original subject");
        feedback.setStatus("Reviewed");
        feedback.setSubmittedAt(date.atStartOfDay());
        feedback.setComments("Original");
        return feedback;
    }

    private FeedbackForm form(String text) {
        FeedbackForm form = new FeedbackForm();
        form.setComments(text);
        form.setSubject("Transport");
        return form;
    }
}
