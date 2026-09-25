package com.bustrans.fleettrack.service;

import com.bustrans.fleettrack.entity.Feedback;
import com.bustrans.fleettrack.form.FeedbackForm;
import com.bustrans.fleettrack.repository.FeedbackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;

    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAll();
    }

    public Feedback getFeedbackById(Long id) {
        if (id == null || id <= 0) throw new NoSuchElementException("Feedback not found");
        return feedbackRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Feedback not found with ID: " + id));
    }

    @Transactional
    public Feedback createFeedback(FeedbackForm form) {
        validate(form);
        Feedback feedback = new Feedback();
        feedback.setComments(form.getComments());
        feedback.setFeedbackDate(LocalDate.now());
        return feedbackRepository.save(feedback);
    }

    @Transactional
    public Feedback updateFeedback(Long id, FeedbackForm form) {
        Feedback existing = getFeedbackById(id);
        validate(form);
        existing.setComments(form.getComments());
        // The original date is historical data, not an editable form field.
        return feedbackRepository.save(existing);
    }

    private void validate(FeedbackForm form) {
        if (form.getComments() == null || form.getComments().isBlank()) {
            throw new IllegalArgumentException("Feedback comments cannot be empty");
        }
        if (form.getComments().length() > 1000) {
            throw new IllegalArgumentException("Feedback cannot exceed 1000 characters");
        }
    }

    @Transactional
    public void deleteFeedback(Long id) {
        feedbackRepository.delete(getFeedbackById(id));
    }
}
