package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.entity.Feedback;
import com.bustrans.fleettrack.entity.User;
import com.bustrans.fleettrack.form.FeedbackForm;
import com.bustrans.fleettrack.repository.FeedbackRepository;
import com.bustrans.fleettrack.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    public FeedbackService(FeedbackRepository feedbackRepository, UserRepository userRepository) {
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
    }

    // userId comes from the authenticated principal, never a submitted form.
    public List<Feedback> getAllFeedback(Long userId) {
        requireUserId(userId);
        return feedbackRepository.findByUser_UserId(userId);
    }

    public Feedback getFeedbackById(Integer id, Long userId) {
        requireUserId(userId);
        if (id == null || id <= 0) throw new NoSuchElementException("Feedback not found");
        // Identical responses for missing and other users' records avoid ownership disclosure.
        return feedbackRepository.findByFeedbackIdAndUser_UserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("Feedback not found"));
    }

    @Transactional
    public Feedback createFeedback(FeedbackForm form, Long userId) {
        requireUserId(userId);
        validate(form);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user is unavailable"));
        Feedback feedback = new Feedback();
        feedback.setUser(user);
        feedback.setSubject(form.getSubject());
        feedback.setComments(form.getComments());
        feedback.setStatus("Pending");
        feedback.setSubmittedAt(LocalDateTime.now());
        return feedbackRepository.save(feedback);
    }

    @Transactional
    public Feedback updateFeedback(Integer id, FeedbackForm form, Long userId) {
        Feedback existing = getFeedbackById(id, userId);
        validate(form);
        existing.setSubject(form.getSubject());
        existing.setComments(form.getComments());
        // Preserve owner, status and the complete historical timestamp, including nulls.
        return feedbackRepository.save(existing);
    }

    private void validate(FeedbackForm form) {
        if (form.getSubject() == null || form.getSubject().isBlank()) {
            throw new IllegalArgumentException("Feedback subject cannot be empty");
        }
        if (form.getSubject().length() > 150) {
            throw new IllegalArgumentException("Feedback subject cannot exceed 150 characters");
        }
        if (form.getComments() == null || form.getComments().isBlank()) {
            throw new IllegalArgumentException("Feedback comments cannot be empty");
        }
        if (form.getComments().length() > 1000) {
            throw new IllegalArgumentException("Feedback cannot exceed 1000 characters. No changes were saved.");
        }
    }

    private void requireUserId(Long userId) {
        // Users.UserId is SQL INT; leave the shared Java Long mapping unchanged.
        if (userId == null || userId <= 0 || userId > Integer.MAX_VALUE) {
            throw new AccessDeniedException("A valid authenticated user is required");
        }
    }

    @Transactional
    public void deleteFeedback(Integer id, Long userId) {
        feedbackRepository.delete(getFeedbackById(id, userId));
    }
}
