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
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;

@Controller
@RequestMapping("/feedback")
public class FeedbackController {
    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @InitBinder("feedback")
    public void bindForm(WebDataBinder binder) {
        binder.setAllowedFields("subject", "comments");
        binder.setAutoGrowNestedPaths(false);
    }

    @GetMapping
    public String listFeedback(Model model, Authentication authentication) {
        model.addAttribute("feedbackList", feedbackService.getAllFeedback(userId(authentication)));
        return "feedback/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model, Authentication authentication) {
        userId(authentication);
        model.addAttribute("feedback", new FeedbackForm());
        return formPage(model, null);
    }

    @PostMapping("/save")
    public String createFeedback(@ModelAttribute("feedback") FeedbackForm form,
                                BindingResult errors, Model model, HttpServletRequest request, RedirectAttributes redirect,
                                Authentication authentication) {
        return submit(form, errors, model, null, request, redirect, userId(authentication));
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model, Authentication authentication) {
        Feedback record = feedbackService.getFeedbackById(id, userId(authentication));
        FeedbackForm form = new FeedbackForm();
        form.setSubject(record.getSubject());
        form.setComments(record.getComments());
        model.addAttribute("feedback", form);
        return formPage(model, id);
    }

    @PostMapping("/edit/{id}")
    public String updateFeedback(@PathVariable Integer id, @ModelAttribute("feedback") FeedbackForm form,
                                BindingResult errors, Model model, HttpServletRequest request, RedirectAttributes redirect,
                                Authentication authentication) {
        Long userId = userId(authentication);
        feedbackService.getFeedbackById(id, userId);
        return submit(form, errors, model, id, request, redirect, userId);
    }

    private String submit(FeedbackForm form, BindingResult errors, Model model, Integer id,
                          HttpServletRequest request, RedirectAttributes redirect, Long userId) {
        // URI variables are also offered to the binder; a body/query id is never allowed.
        boolean unexpected = Arrays.stream(errors.getSuppressedFields())
                .anyMatch(field -> !(id != null && field.equals("id") && !request.getParameterMap().containsKey("id")));
        if (unexpected) {
            errors.reject("unexpectedFields", "Unexpected form fields were submitted. Reload the form and try again.");
        }
        if (!errors.hasErrors()) {
            try {
                if (id == null) {
                    feedbackService.createFeedback(form, userId);
                } else {
                    feedbackService.updateFeedback(id, form, userId);
                }
                redirect.addFlashAttribute("successMessage", "Feedback record saved.");
                return "redirect:/feedback";
            } catch (IllegalArgumentException exception) {
                errors.reject("invalidFeedback", exception.getMessage());
            }
        }
        return formPage(model, id);
    }

    private String formPage(Model model, Integer id) {
        model.addAttribute("feedbackId", id);
        FeedbackForm form = (FeedbackForm) model.getAttribute("feedback");
        model.addAttribute("oversizedComments", form != null && form.getComments() != null
                && form.getComments().length() > 1000);
        return "feedback/form";
    }

    private Long userId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("A valid authenticated user is required");
        }
        try {
            long id = Long.parseLong(authentication.getName());
            if (id > 0 && id <= Integer.MAX_VALUE) return id;
        } catch (NumberFormatException ignored) {
            // Never accept an owner from a request parameter as a fallback.
        }
        throw new AccessDeniedException("A valid authenticated user is required");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> denied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access is denied");
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> missingRecord(NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @GetMapping("/delete/{id}")
    public String deleteFeedback(@PathVariable Integer id, RedirectAttributes redirect, Authentication authentication) {
        feedbackService.deleteFeedback(id, userId(authentication));
        redirect.addFlashAttribute("successMessage", "Feedback record deleted.");
        return "redirect:/feedback";
    }
}

