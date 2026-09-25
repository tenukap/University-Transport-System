package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.entity.Invoice;
import com.bustrans.fleettrack.form.InvoiceForm;
import com.bustrans.fleettrack.service.InvoiceService;
import com.bustrans.fleettrack.exception.InvoiceDeletionBlockedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessResourceFailureException;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import com.bustrans.fleettrack.security.JwtAuthenticationFilter;
import com.bustrans.fleettrack.exception.ApiExceptionHandler;
import com.bustrans.fleettrack.exception.GlobalExceptionHandler;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = InvoiceController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {JwtAuthenticationFilter.class, ApiExceptionHandler.class, GlobalExceptionHandler.class}))
@AutoConfigureMockMvc(addFilters = false)
class InvoiceControllerTest {
    @Autowired MockMvc mvc;
    @MockBean InvoiceService service;

    @Test
    void relocatedStylesheetIsServedAtExistingUrl() throws Exception {
        mvc.perform(get("/css/transitpass.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"))
                .andExpect(content().bytes(new org.springframework.core.io.ClassPathResource(
                        "static/css/transitpass.css").getContentAsByteArray()));
    }

    @BeforeEach
    void setup() {
        Invoice record = new Invoice();
        record.setInvoiceId(7L);
        record.setBillingMonth(9);
        record.setBillingYear(2026);
        record.setIssueDate(LocalDate.of(2026, 9, 1));
        record.setDueDate(LocalDate.of(2026, 9, 30));
        record.setTotalAmount(new BigDecimal("12.34"));
        when(service.getInvoiceById(7L)).thenReturn(record);
        when(service.getAllInvoices()).thenReturn(List.of(record));
    }

    @Test
    void formsUseSeparateActionsAndNoHiddenIds() throws Exception {
        mvc.perform(get("/invoices/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/invoices/save\"")))
                .andExpect(content().string(not(containsString("name=\"invoiceId\""))))
                .andExpect(content().string(containsString("max=\"99999999.99\"")));
        mvc.perform(get("/invoices/edit/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/invoices/edit/7\"")))
                .andExpect(content().string(not(containsString("name=\"invoiceId\""))))
                .andExpect(content().string(containsString("value=\"2026-09-01\"")));
    }

    @Test
    void createAndUpdateUseScalarFormsAndRouteTarget() throws Exception {
        mvc.perform(validPost("/invoices/save")).andExpect(redirectedUrl("/invoices"));
        verify(service).createInvoice(argThat(form -> form.getBillingMonth() == 9
                && form.getBillingYear() == 2026
                && form.getIssueDate().equals(LocalDate.of(2026, 9, 1))
                && form.getDueDate().equals(LocalDate.of(2026, 9, 30))
                && form.getTotalAmount().equals(new BigDecimal("12.34"))));
        mvc.perform(validPost("/invoices/edit/7")).andExpect(redirectedUrl("/invoices"));
        verify(service).updateInvoice(eq(7L), any(InvoiceForm.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invoiceId", "id", "invoiceStatus", "payments[0].amount"})
    void rejectsUnexpectedFieldsOnCreateAndUpdate(String field) throws Exception {
        for (String path : List.of("/invoices/save", "/invoices/edit/7")) {
            mvc.perform(validPost(path).param(field, "99"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasErrors("invoice"))
                    .andExpect(content().string(containsString("Unexpected form fields")));
        }
        verify(service, never()).createInvoice(any());
        verify(service, never()).updateInvoice(anyLong(), any());
    }

    @Test
    void validationErrorsPreserveValuesAndEditAction() throws Exception {
        when(service.createInvoice(any())).thenThrow(new IllegalArgumentException("Validation message"));
        when(service.updateInvoice(eq(7L), any())).thenThrow(new IllegalArgumentException("Validation message"));
        for (String path : List.of("/invoices/save", "/invoices/edit/7")) {
            mvc.perform(validPost(path))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasErrors("invoice"))
                    .andExpect(content().string(containsString("Validation message")))
                    .andExpect(content().string(containsString("action=\"" + path + "\"")))
                    .andExpect(content().string(containsString("value=\"12.34\"")))
                    .andExpect(content().string(containsString("value=\"2026-09-01\"")));
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, 99})
    void missingTargetsReturn404ForEditUpdateAndDelete(long id) throws Exception {
        when(service.getInvoiceById(id)).thenThrow(new NoSuchElementException("Invoice not found"));
        doThrow(new NoSuchElementException("Invoice not found")).when(service).deleteInvoice(id);
        mvc.perform(get("/invoices/edit/" + id)).andExpect(status().isNotFound());
        mvc.perform(validPost("/invoices/edit/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/invoices/delete/" + id)).andExpect(status().isNotFound());
        verify(service, never()).updateInvoice(anyLong(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "999999999999999999999999"})
    void malformedRouteIdsReturn400(String id) throws Exception {
        mvc.perform(get("/invoices/edit/" + id)).andExpect(status().isBadRequest());
        mvc.perform(validPost("/invoices/edit/" + id)).andExpect(status().isBadRequest());
        mvc.perform(get("/invoices/delete/" + id)).andExpect(status().isBadRequest());
        verify(service, never()).updateInvoice(anyLong(), any());
        verify(service, never()).deleteInvoice(anyLong());
    }

    @Test
    void deletesExistingRecordAndRedirects() throws Exception {
        mvc.perform(get("/invoices/delete/7")).andExpect(redirectedUrl("/invoices"));
        verify(service).deleteInvoice(7L);
    }

    @ParameterizedTest
    @CsvSource({"billingMonth,abc", "billingYear,abc", "issueDate,2026-02-30", "dueDate,not-a-date", "totalAmount,abc"})
    void malformedFieldsReturnFormWithoutSaving(String field, String value) throws Exception {
        mvc.perform(validPost("/invoices/save").with(request -> {
                    request.setParameter(field, value);
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("invoice", field))
                .andExpect(content().string(containsString("Please correct the invoice details")));
        verify(service, never()).createInvoice(any());
    }

    @Test
    void paymentConflictRedirectsWithExactMessageAndListDisplaysIt() throws Exception {
        String message = "This invoice cannot be deleted because payment records reference it.";
        doThrow(new InvoiceDeletionBlockedException()).when(service).deleteInvoice(7L);
        mvc.perform(get("/invoices/delete/7"))
                .andExpect(redirectedUrl("/invoices"))
                .andExpect(flash().attributeCount(1))
                .andExpect(flash().attribute("deleteError", message));
        mvc.perform(get("/invoices").flashAttr("deleteError", message))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(message)))
                .andExpect(content().string(containsString("role=\"alert\"")));
    }

    @Test
    void lateIntegrityConflictUsesNeutralMessage() throws Exception {
        doThrow(new DataIntegrityViolationException("not necessarily payment")).when(service).deleteInvoice(7L);
        mvc.perform(get("/invoices/delete/7"))
                .andExpect(redirectedUrl("/invoices"))
                .andExpect(flash().attributeCount(1))
                .andExpect(flash().attribute("deleteError",
                        "This invoice could not be deleted because of a database integrity conflict. Refresh the list and try again."));
    }

    @Test
    void unrelatedDatabaseFailureIsNotMisreportedAsPaymentConflict() {
        DataAccessResourceFailureException failure = new DataAccessResourceFailureException("database offline");
        doThrow(failure).when(service).deleteInvoice(7L);
        ServletException error = assertThrows(ServletException.class,
                () -> mvc.perform(get("/invoices/delete/7")));
        assertSame(failure, error.getCause());
    }


    @Test
    void pagesHaveConsistentNavigationAccessibilityAndMessages() throws Exception {
        for (String path : List.of("/invoices", "/invoices/new")) {
            String html = mvc.perform(get(path).flashAttr("successMessage", "Invoice record saved."))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("lang=\"en\"")))
                    .andExpect(content().string(containsString("name=\"viewport\"")))
                    .andExpect(content().string(not(containsString("href=\"#\""))))
                    .andExpect(content().string(containsString("aria-current=\"page\"")))
                    .andExpect(content().string(containsString("Unavailable")))
                    .andReturn().getResponse().getContentAsString();
            if (path.endsWith("/new")) {
                java.util.regex.Matcher labels = java.util.regex.Pattern.compile("<label for=\"([^\"]+)\"").matcher(html);
                int count = 0;
                while (labels.find()) {
                    org.junit.jupiter.api.Assertions.assertTrue(html.contains("id=\"" + labels.group(1) + "\""));
                    count++;
                }
                org.junit.jupiter.api.Assertions.assertTrue(count > 0);
            } else {
                org.junit.jupiter.api.Assertions.assertTrue(html.contains("role=\"status\""));
                org.junit.jupiter.api.Assertions.assertTrue(html.contains("Invoice record saved."));
                org.junit.jupiter.api.Assertions.assertTrue(html.contains("class=\"table-scroll\""));
                org.junit.jupiter.api.Assertions.assertTrue(html.contains("scope=\"col\""));
            }
            if (Boolean.getBoolean("stage4.preview")) {
                java.nio.file.Path output = java.nio.file.Path.of("target/stage4-preview" + path + "/index.html");
                java.nio.file.Files.createDirectories(output.getParent());
                java.nio.file.Files.writeString(output, html);
            }
        }
    }

    @Test
    void emptyListStillRenders() throws Exception {
        when(service.getAllInvoices()).thenReturn(List.of());
        mvc.perform(get("/invoices")).andExpect(status().isOk())
                .andExpect(content().string(containsString("No invoices available yet.")));
    }

    @Test
    void successfulActionsHaveFactualFlashMessages() throws Exception {
        mvc.perform(validPost("/invoices/save"))
                .andExpect(flash().attribute("successMessage", "Invoice record saved."));
        mvc.perform(validPost("/invoices/edit/7"))
                .andExpect(flash().attribute("successMessage", "Invoice record saved."));
        mvc.perform(get("/invoices/delete/7"))
                .andExpect(flash().attribute("successMessage", "Invoice record deleted."));
    }

    @Test
    void invalidSaveDoesNotProduceSuccessMessage() throws Exception {
        when(service.createInvoice(any())).thenThrow(new IllegalArgumentException("Invalid input"));
        mvc.perform(validPost("/invoices/save"))
                .andExpect(model().attributeHasErrors("invoice"))
                .andExpect(flash().attributeCount(0))
                .andExpect(content().string(containsString("message-error")));
    }

    @Test
    void renderedFormSubmitsSuccessfullyWithBrowserHeaders() throws Exception {
        String html = mvc.perform(get("/invoices/new")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        java.util.Map<String, String> values = java.util.Map.of(
                "billingMonth", "9",
                "billingYear", "2026",
                "issueDate", "2026-09-20",
                "dueDate", "2026-10-05",
                "totalAmount", "12500.0");
        java.util.regex.Matcher controls = java.util.regex.Pattern
                .compile("<(?:input|select|textarea)\\b[^>]*\\bname=\"([^\"]+)\"", java.util.regex.Pattern.DOTALL)
                .matcher(html);
        MockHttpServletRequestBuilder request = browserPost("/invoices/save");
        java.util.Set<String> names = new java.util.HashSet<>();
        while (controls.find()) {
            String name = controls.group(1);
            org.junit.jupiter.api.Assertions.assertTrue(values.containsKey(name), "Unexpected rendered control: " + name);
            names.add(name);
            request.param(name, values.get(name));
        }
        org.junit.jupiter.api.Assertions.assertEquals(values.keySet(), names);
        var result = mvc.perform(request).andReturn();
        if (result.getModelAndView() != null) {
            Object binding = result.getModelAndView().getModel().get(
                    org.springframework.validation.BindingResult.MODEL_KEY_PREFIX + "invoice");
            if (binding instanceof org.springframework.validation.BindingResult errors) {
                org.junit.jupiter.api.Assertions.assertFalse(errors.hasErrors(),
                        "Rejected browser submission; suppressed fields: " + java.util.Arrays.toString(errors.getSuppressedFields()));
            }
        }
        org.junit.jupiter.api.Assertions.assertEquals(302, result.getResponse().getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("/invoices", result.getResponse().getRedirectedUrl());
        verify(service).createInvoice(any(InvoiceForm.class));
    }


    @ParameterizedTest
    @ValueSource(strings = {"invoiceId", "id", "contentType", "userAgent", "unexpectedBusinessField"})
    void browserHeadersDoNotAllowUnexpectedBodyFields(String field) throws Exception {
        for (String path : List.of("/invoices/save", "/invoices/edit/7")) {
            mvc.perform(validPost(path).contentType("application/x-www-form-urlencoded")
                            .header("User-Agent", "Mozilla/5.0").param(field, "99"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasErrors("invoice"))
                    .andExpect(content().string(containsString("Unexpected form fields")));
        }
        verify(service, never()).createInvoice(any());
        verify(service, never()).updateInvoice(anyLong(), any());
    }

    @Test
    void browserHeadersAlsoWorkOnUpdate() throws Exception {
        mvc.perform(validPost("/invoices/edit/7")
                        .contentType("application/x-www-form-urlencoded")
                        .header("User-Agent", "Mozilla/5.0").header("Sec-Fetch-Mode", "navigate"))
                .andExpect(redirectedUrl("/invoices"));
        verify(service).updateInvoice(eq(7L), any(InvoiceForm.class));
    }

    @Test
    void headersCannotSupplyMissingBusinessValues() throws Exception {
        when(service.createInvoice(any())).thenThrow(new IllegalArgumentException("Required field missing"));
        mvc.perform(validPost("/invoices/save").header("totalAmount", "999")
                        .with(request -> { request.removeParameter("totalAmount"); return request; }))
                .andExpect(model().attributeHasErrors("invoice"));
        verify(service).createInvoice(argThat(form -> form.getTotalAmount() == null));
    }

    private MockHttpServletRequestBuilder browserPost(String path) {
        return post(path).contentType("application/x-www-form-urlencoded")
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Accept-Encoding", "gzip, deflate")
                .header("Sec-Fetch-Site", "same-origin")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Dest", "document")
                .header("Upgrade-Insecure-Requests", "1");
    }

    private MockHttpServletRequestBuilder validPost(String path) {
        return post(path).param("billingMonth", "9").param("billingYear", "2026")
                .param("issueDate", "2026-09-01").param("dueDate", "2026-09-30")
                .param("totalAmount", "12.34");
    }
}




