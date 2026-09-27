package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.entity.Payment;
import com.bustrans.fleettrack.form.PaymentForm;
import com.bustrans.fleettrack.service.InvoiceService;
import com.bustrans.fleettrack.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;

import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.NoSuchElementException;
import java.time.LocalDate;
import java.util.List;
import java.util.Arrays;

@Controller
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;
    private final InvoiceService invoiceService;

    public PaymentController(PaymentService paymentService, InvoiceService invoiceService) {
        this.paymentService = paymentService;
        this.invoiceService = invoiceService;
    }

    @InitBinder("payment")
    public void bindPaymentForm(WebDataBinder binder) {
        binder.setAllowedFields("invoiceId", "amount", "paymentDate", "paymentStatus");
        binder.setAutoGrowNestedPaths(false);
    }

    @GetMapping
    public String listPayments(Model model) {
        model.addAttribute("payments", paymentService.getAllPayments());
        return "payment/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("payment", new PaymentForm());
        return formPage(model, null);
    }

    @PostMapping("/save")
    public String createPayment(@ModelAttribute("payment") PaymentForm form,
                                BindingResult errors, Model model, HttpServletRequest request, RedirectAttributes redirect) {
        return submit(form, errors, model, null, request, redirect);
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Payment payment = paymentService.getPaymentById(id);
        PaymentForm form = new PaymentForm();
        form.setInvoiceId(payment.getInvoice().getInvoiceId());
        form.setAmount(payment.getAmount());
        form.setPaymentDate(payment.getPaymentDate());
        form.setPaymentStatus(payment.getPaymentStatus());
        model.addAttribute("payment", form);
        return formPage(model, id);
    }

    @PostMapping("/edit/{id}")
    public String updatePayment(@PathVariable Long id,
                                @ModelAttribute("payment") PaymentForm form,
                                BindingResult errors, Model model, HttpServletRequest request, RedirectAttributes redirect) {
        // Missing targets must not become a create or an editable error form.
        paymentService.getPaymentById(id);
        return submit(form, errors, model, id, request, redirect);
    }

    private String submit(PaymentForm form, BindingResult errors, Model model, Long id,
                          HttpServletRequest request, RedirectAttributes redirect) {
        // MVC also offers URI variables to the binder. Only the route-only id is exempt;
        // an id supplied as a request parameter is still rejected and never bound.
        boolean unexpectedFields = Arrays.stream(errors.getSuppressedFields())
                .anyMatch(field -> !(id != null && field.equals("id") && !request.getParameterMap().containsKey("id")));
        if (unexpectedFields) {
            errors.reject("unexpectedFields", "Unexpected form fields were submitted. Reload the payment form and try again.");
        }
        if (!errors.hasErrors()) {
            try {
                if (id == null) {
                    paymentService.createPayment(form);
                } else {
                    paymentService.updatePayment(id, form);
                }
                redirect.addFlashAttribute("successMessage", "Payment record saved.");
                return "redirect:/payments";
            } catch (IllegalArgumentException exception) {
                errors.reject("invalidPayment", exception.getMessage());
            }
        }
        return formPage(model, id);
    }

    private String formPage(Model model, Long id) {
        model.addAttribute("paymentId", id);
        model.addAttribute("today", LocalDate.now());
        var invoices = invoiceService.getAllInvoices();
        model.addAttribute("invoices", invoices);
        // Keep invalid or stale selections visible rather than silently choosing another value.
        BindingResult errors = (BindingResult) model.getAttribute(BindingResult.MODEL_KEY_PREFIX + "payment");
        if (errors != null) {
            Object rawInvoice = errors.getFieldValue("invoiceId");
            if (rawInvoice != null && !rawInvoice.toString().isBlank()
                    && invoices.stream().noneMatch(invoice -> invoice.getInvoiceId().toString().equals(rawInvoice.toString()))) {
                model.addAttribute("unavailableInvoice", rawInvoice.toString());
            }
            Object rawStatus = errors.getFieldValue("paymentStatus");
            if (rawStatus != null && !rawStatus.toString().isBlank()
                    && !List.of("PAID", "PENDING", "FAILED").contains(rawStatus.toString())) {
                model.addAttribute("unavailableStatus", rawStatus.toString());
            }
        }
        return "payment/form";
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> missingPayment(NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @GetMapping("/delete/{id}")
    public String deletePayment(@PathVariable Long id, RedirectAttributes redirect) {
        paymentService.deletePayment(id);
        redirect.addFlashAttribute("successMessage", "Payment record deleted.");
        return "redirect:/payments";
    }
}

