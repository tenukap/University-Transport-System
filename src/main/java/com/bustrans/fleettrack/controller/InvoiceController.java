package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.entity.Invoice;
import com.bustrans.fleettrack.form.InvoiceForm;
import com.bustrans.fleettrack.service.InvoiceService;
import com.bustrans.fleettrack.exception.InvoiceDeletionBlockedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;

import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.NoSuchElementException;

@Controller
@RequestMapping("/invoices")
public class InvoiceController {
    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @InitBinder("invoice")
    public void bindForm(WebDataBinder binder) {
        binder.setAllowedFields("billingMonth", "billingYear", "issueDate", "dueDate", "totalAmount");
        binder.setAutoGrowNestedPaths(false);
    }

    @GetMapping
    public String listInvoice(Model model) {
        model.addAttribute("invoices", invoiceService.getAllInvoices());
        return "invoice/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("invoice", new InvoiceForm());
        return formPage(model, null);
    }

    @PostMapping("/save")
    public String createInvoice(@ModelAttribute("invoice") InvoiceForm form,
                                BindingResult errors, Model model, HttpServletRequest request, RedirectAttributes redirect) {
        return submit(form, errors, model, null, request, redirect);
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Invoice record = invoiceService.getInvoiceById(id);
        InvoiceForm form = new InvoiceForm();
        form.setBillingMonth(record.getBillingMonth());
        form.setBillingYear(record.getBillingYear());
        form.setIssueDate(record.getIssueDate());
        form.setDueDate(record.getDueDate());
        form.setTotalAmount(record.getTotalAmount());
        model.addAttribute("invoice", form);
        return formPage(model, id);
    }

    @PostMapping("/edit/{id}")
    public String updateInvoice(@PathVariable Long id, @ModelAttribute("invoice") InvoiceForm form,
                                BindingResult errors, Model model, HttpServletRequest request, RedirectAttributes redirect) {
        invoiceService.getInvoiceById(id);
        return submit(form, errors, model, id, request, redirect);
    }

    private String submit(InvoiceForm form, BindingResult errors, Model model, Long id,
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
                    invoiceService.createInvoice(form);
                } else {
                    invoiceService.updateInvoice(id, form);
                }
                redirect.addFlashAttribute("successMessage", "Invoice record saved.");
                return "redirect:/invoices";
            } catch (IllegalArgumentException exception) {
                errors.reject("invalidInvoice", exception.getMessage());
            }
        }
        return formPage(model, id);
    }

    private String formPage(Model model, Long id) {
        model.addAttribute("invoiceId", id);
        return "invoice/form";
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> missingRecord(NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @GetMapping("/delete/{id}")
    public String deleteInvoice(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            invoiceService.deleteInvoice(id);
            redirect.addFlashAttribute("successMessage", "Invoice record deleted.");
        } catch (InvoiceDeletionBlockedException exception) {
            redirect.addFlashAttribute("deleteError", exception.getMessage());
        } catch (DataIntegrityViolationException exception) {
            // The service transaction has finished rolling back. Do not infer a
            // payment conflict from an arbitrary integrity constraint failure.
            redirect.addFlashAttribute("deleteError",
                    "This invoice could not be deleted because of a database integrity conflict. Refresh the list and try again.");
        }
        return "redirect:/invoices";
    }
}

