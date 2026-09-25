package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.entity.Feedback;
import com.bustrans.fleettrack.form.FeedbackForm;
import com.bustrans.fleettrack.service.FeedbackService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;

import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.NoSuchElementException;

@Controller
@RequestMapping("/feedback")
public class FeedbackController {
    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @InitBinder("feedback")
    public void bindForm(WebDataBinder binder) {
        binder.setAllowedFields("comments");
        binder.setAutoGrowNestedPaths(false);
    }

    @GetMapping
    public String listFeedback(Model model) {
        model.addAttribute("feedbackList", feedbackService.getAllFeedback());
        return "feedback/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("feedback", new FeedbackForm());
        return formPage(model, null);
    }

    @PostMapping("/save")
    public String createFeedback(@ModelAttribute("feedback") FeedbackForm form,
                                BindingResult errors, Model model, HttpServletRequest request, RedirectAttributes redirect) {
        return submit(form, errors, model, null, request, redirect);
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Feedback record = feedbackService.getFeedbackById(id);
        FeedbackForm form = new FeedbackForm();
        form.setComments(record.getComments());
        model.addAttribute("feedback", form);
        return formPage(model, id);
    }

    @PostMapping("/edit/{id}")
    public String updateFeedback(@PathVariable Long id, @ModelAttribute("feedback") FeedbackForm form,
                                BindingResult errors, Model model, HttpServletRequest request, RedirectAttributes redirect) {
        feedbackService.getFeedbackById(id);
        return submit(form, errors, model, id, request, redirect);
    }

    private String submit(FeedbackForm form, BindingResult errors, Model model, Long id,
                          HttpServletRequest request, RedirectAttributes redirect) {
        // URI variables are also offered to the binder; a body/query id is never allowed.
        boolean unexpected = Arrays.stream(errors.getSuppressedFields())
                .anyMatch(field -> !(id != null && field.equals("id") && !request.getParameterMap().containsKey("id")));
        if (unexpected) {
            errors.reject("unexpectedFields", "Unexpected form fields were submitted. Reload the form and try again.");
        }
        if (!errors.hasErrors()) {
            try {
                if (id == null) {
                    feedbackService.createFeedback(form);
                } else {
                    feedbackService.updateFeedback(id, form);
                }
                redirect.addFlashAttribute("successMessage", "Feedback record saved.");
                return "redirect:/feedback";
            } catch (IllegalArgumentException exception) {
                errors.reject("invalidFeedback", exception.getMessage());
            }
        }
        return formPage(model, id);
    }

    private String formPage(Model model, Long id) {
        model.addAttribute("feedbackId", id);
        return "feedback/form";
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> missingRecord(NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @GetMapping("/delete/{id}")
    public String deleteFeedback(@PathVariable Long id, RedirectAttributes redirect) {
        feedbackService.deleteFeedback(id);
        redirect.addFlashAttribute("successMessage", "Feedback record deleted.");
        return "redirect:/feedback";
    }
}

