package com.transport.uni_transport_system.controller;

import com.transport.uni_transport_system.entity.Invoice;
import com.transport.uni_transport_system.entity.Payment;
import com.transport.uni_transport_system.form.PaymentForm;
import com.transport.uni_transport_system.service.InvoiceService;
import com.transport.uni_transport_system.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean PaymentService payments;
    @MockitoBean InvoiceService invoices;

    @BeforeEach
    void setup() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(1L);
        invoice.setTotalAmount(new BigDecimal("20.00"));
        when(invoices.getAllInvoices()).thenReturn(List.of(invoice));
        Payment payment = new Payment();
        payment.setPaymentId(7L);
        payment.setInvoice(invoice);
        payment.setAmount(new BigDecimal("12.34"));
        payment.setPaymentStatus("FAILED");
        payment.setPaymentDate(LocalDate.of(2020, 1, 1));
        when(payments.getPaymentById(7L)).thenReturn(payment);
    }

    @Test
    void newFormRequiresExplicitStatusAndHasNoPaymentIdInput() throws Exception {
        mvc.perform(get("/payments/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/payments/save\"")))
                .andExpect(content().string(matchesPattern("(?s).*<select(?=[^>]*name=\"paymentStatus\")(?=[^>]*required)[^>]*>.*")))
                .andExpect(content().string(containsString("Select status")))
                .andExpect(content().string(not(matchesPattern("(?s).*<option(?=[^>]*value=\"PAID\")(?=[^>]*selected)[^>]*>.*"))))
                .andExpect(content().string(not(containsString("name=\"paymentId\""))))
                .andExpect(content().string(containsString("name=\"invoiceId\"")));
    }

    @Test
    void editFormPreservesSelectionAndPostsToRouteId() throws Exception {
        mvc.perform(get("/payments/edit/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/payments/edit/7\"")))
                .andExpect(content().string(matchesPattern("(?s).*<option(?=[^>]*value=\"FAILED\")(?=[^>]*selected)[^>]*>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<option(?=[^>]*value=\"1\")(?=[^>]*selected)[^>]*>.*")))
                .andExpect(content().string(not(containsString("name=\"paymentId\""))));
    }

    @Test
    void createPassesOnlyScalarValues() throws Exception {
        mvc.perform(validPost("/payments/save"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payments"));
        verify(payments).createPayment(argThat(form ->
                form.getInvoiceId().equals(1L)
                && form.getAmount().equals(new BigDecimal("12.34"))
                && form.getPaymentStatus().equals("PENDING")
                && form.getPaymentDate().equals(LocalDate.of(2020, 1, 1))));
        verify(payments, never()).updatePayment(anyLong(), any());
    }

    @Test
    void updateUsesRouteTarget() throws Exception {
        mvc.perform(validPost("/payments/edit/7"))
                .andExpect(redirectedUrl("/payments"));
        verify(payments).updatePayment(eq(7L), any(PaymentForm.class));
        verify(payments, never()).createPayment(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"paymentId", "id", "invoice", "invoice.invoiceId", "invoice.totalAmount"})
    void createRejectsUnexpectedFields(String field) throws Exception {
        mvc.perform(validPost("/payments/save").param(field, "999"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("payment"))
                .andExpect(content().string(containsString("Unexpected form fields")))
                .andExpect(model().attributeExists("invoices"));
        verify(payments, never()).createPayment(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "paymentId", "invoice.invoiceId", "invoice.totalAmount"})
    void updateRejectsBodyIdWithoutChangingAnyRecord(String field) throws Exception {
        mvc.perform(validPost("/payments/edit/7").param(field, "99"))
                .andExpect(model().attributeHasErrors("payment"))
                .andExpect(content().string(containsString("action=\"/payments/edit/7\"")));
        verify(payments, never()).updatePayment(anyLong(), any());
    }

    @ParameterizedTest
    @CsvSource({"invoiceId,abc", "amount,abc", "paymentDate,not-a-date"})
    void malformedFieldsReturnUsableForm(String field, String value) throws Exception {
        MockHttpServletRequestBuilder request = validPost("/payments/save");
        // param() appends, so replace the selected parameter explicitly.
        request.with(servletRequest -> { servletRequest.setParameter(field, value); return servletRequest; });
        mvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("payment", field))
                .andExpect(model().attributeExists("invoices"))
                .andExpect(content().string(containsString("Please correct the payment details")));
        verify(payments, never()).createPayment(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Select a payment status", "Selected invoice no longer exists. Select another invoice."})
    void validationFailurePreservesFormValues(String message) throws Exception {
        when(payments.createPayment(any())).thenThrow(new IllegalArgumentException(message));
        mvc.perform(validPost("/payments/save"))
                .andExpect(model().attributeHasErrors("payment"))
                .andExpect(content().string(containsString(message)))
                .andExpect(content().string(matchesPattern("(?s).*<input(?=[^>]*name=\"amount\")(?=[^>]*value=\"12.34\")[^>]*>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<option(?=[^>]*value=\"PENDING\")(?=[^>]*selected)[^>]*>.*")))
                .andExpect(model().attributeExists("invoices"));
    }

    @Test
    void invalidUpdatePreservesEditRouteAndValues() throws Exception {
        when(payments.updatePayment(eq(7L), any())).thenThrow(new IllegalArgumentException("Invalid payment status"));
        mvc.perform(validPost("/payments/edit/7"))
                .andExpect(model().attributeHasErrors("payment"))
                .andExpect(content().string(containsString("action=\"/payments/edit/7\"")))
                .andExpect(content().string(matchesPattern("(?s).*<input(?=[^>]*name=\"amount\")(?=[^>]*value=\"12.34\")[^>]*>.*")));
    }

    @Test
    void staleInvoiceAndUnsupportedStatusStayVisibleAfterFailure() throws Exception {
        when(payments.createPayment(any())).thenThrow(new IllegalArgumentException("Invalid payment status"));
        mvc.perform(validPost("/payments/save").with(request -> {
                    request.setParameter("invoiceId", "999");
                    request.setParameter("paymentStatus", "UNKNOWN");
                    return request;
                }))
                .andExpect(content().string(containsString("Unavailable invoice: 999")))
                .andExpect(content().string(containsString("Submitted status: UNKNOWN")))
                .andExpect(content().string(matchesPattern("(?s).*<option(?=[^>]*value=\"999\")(?=[^>]*selected)[^>]*>.*")))
                .andExpect(content().string(matchesPattern("(?s).*<option(?=[^>]*value=\"UNKNOWN\")(?=[^>]*selected)[^>]*>.*")));
    }

    @Test
    void missingTargetsReturn404ForGetAndPost() throws Exception {
        when(payments.getPaymentById(99L)).thenThrow(new NoSuchElementException("Payment not found"));
        mvc.perform(get("/payments/edit/99")).andExpect(status().isNotFound());
        mvc.perform(validPost("/payments/edit/99")).andExpect(status().isNotFound());
        verify(payments, never()).updatePayment(anyLong(), any());
    }

    @Test
    void malformedRouteIdsReturn400() throws Exception {
        mvc.perform(get("/payments/edit/abc")).andExpect(status().isBadRequest());
        mvc.perform(validPost("/payments/edit/abc")).andExpect(status().isBadRequest());
        verify(payments, never()).updatePayment(anyLong(), any());
    }


    @Test
    void pagesHaveConsistentNavigationAccessibilityAndMessages() throws Exception {
        for (String path : List.of("/payments", "/payments/new")) {
            String html = mvc.perform(get(path).flashAttr("successMessage", "Payment record saved."))
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
                org.junit.jupiter.api.Assertions.assertTrue(html.contains("Payment record saved."));
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
        when(payments.getAllPayments()).thenReturn(List.of());
        mvc.perform(get("/payments")).andExpect(status().isOk())
                .andExpect(content().string(containsString("No payments recorded yet.")));
    }

    @Test
    void successfulActionsHaveFactualFlashMessages() throws Exception {
        mvc.perform(validPost("/payments/save"))
                .andExpect(flash().attribute("successMessage", "Payment record saved."));
        mvc.perform(validPost("/payments/edit/7"))
                .andExpect(flash().attribute("successMessage", "Payment record saved."));
        mvc.perform(get("/payments/delete/7"))
                .andExpect(flash().attribute("successMessage", "Payment record deleted."));
    }

    @Test
    void invalidSaveDoesNotProduceSuccessMessage() throws Exception {
        when(payments.createPayment(any())).thenThrow(new IllegalArgumentException("Invalid input"));
        mvc.perform(validPost("/payments/save"))
                .andExpect(model().attributeHasErrors("payment"))
                .andExpect(flash().attributeCount(0))
                .andExpect(content().string(containsString("message-error")));
    }
    @ParameterizedTest
    @CsvSource({"PAID,status-paid", "PENDING,status-pending", "FAILED,status-failed", "UNKNOWN,status-neutral"})
    void paymentBadgesKeepTextAndDistinctStyle(String status, String style) throws Exception {
        Payment payment = payments.getPaymentById(7L);
        payment.setPaymentStatus(status);
        when(payments.getAllPayments()).thenReturn(List.of(payment));
        mvc.perform(get("/payments")).andExpect(status().isOk())
                .andExpect(content().string(containsString("status " + style)))
                .andExpect(content().string(containsString(">" + status + "</span>")))
                .andExpect(content().string(containsString("do not verify real transactions")));
    }

    @Test
    void paymentFormConstraintsMatchBackendAndCssIsServed() throws Exception {
        String today = LocalDate.now().toString();
        mvc.perform(get("/payments/new"))
                .andExpect(content().string(containsString("max=\"99999999.99\"")))
                .andExpect(content().string(containsString("max=\"" + today + "\"")))
                .andExpect(content().string(containsString("does not verify a real transaction")));
        mvc.perform(get("/css/transitpass.css")).andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"))
                .andExpect(content().string(containsString(".status-paid")))
                .andExpect(content().string(containsString("@media (max-width: 600px)")));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, 99})
    void missingPaymentDeletionReturns404(long id) throws Exception {
        doThrow(new NoSuchElementException("Payment not found")).when(payments).deletePayment(id);
        mvc.perform(get("/payments/delete/" + id)).andExpect(status().isNotFound())
                .andExpect(flash().attributeCount(0));
    }

    @Test
    void malformedPaymentDeletionReturns400() throws Exception {
        mvc.perform(get("/payments/delete/abc")).andExpect(status().isBadRequest());
        verify(payments, never()).deletePayment(anyLong());
    }


    @Test
    void renderedFormSubmitsSuccessfullyWithBrowserHeaders() throws Exception {
        String html = mvc.perform(get("/payments/new")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        java.util.Map<String, String> values = java.util.Map.of(
                "invoiceId", "1",
                "amount", "12500.0",
                "paymentDate", "2026-09-20",
                "paymentStatus", "PENDING");
        java.util.regex.Matcher controls = java.util.regex.Pattern
                .compile("<(?:input|select|textarea)\\b[^>]*\\bname=\"([^\"]+)\"", java.util.regex.Pattern.DOTALL)
                .matcher(html);
        MockHttpServletRequestBuilder request = browserPost("/payments/save");
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
                    org.springframework.validation.BindingResult.MODEL_KEY_PREFIX + "payment");
            if (binding instanceof org.springframework.validation.BindingResult errors) {
                org.junit.jupiter.api.Assertions.assertFalse(errors.hasErrors(),
                        "Rejected browser submission; suppressed fields: " + java.util.Arrays.toString(errors.getSuppressedFields()));
            }
        }
        org.junit.jupiter.api.Assertions.assertEquals(302, result.getResponse().getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("/payments", result.getResponse().getRedirectedUrl());
        verify(payments).createPayment(any(PaymentForm.class));
    }


    @ParameterizedTest
    @ValueSource(strings = {"paymentId", "id", "contentType", "userAgent", "unexpectedBusinessField"})
    void browserHeadersDoNotAllowUnexpectedBodyFields(String field) throws Exception {
        for (String path : List.of("/payments/save", "/payments/edit/7")) {
            mvc.perform(validPost(path).contentType("application/x-www-form-urlencoded")
                            .header("User-Agent", "Mozilla/5.0").param(field, "99"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasErrors("payment"))
                    .andExpect(content().string(containsString("Unexpected form fields")));
        }
        verify(payments, never()).createPayment(any());
        verify(payments, never()).updatePayment(anyLong(), any());
    }

    @Test
    void browserHeadersAlsoWorkOnUpdate() throws Exception {
        mvc.perform(validPost("/payments/edit/7")
                        .contentType("application/x-www-form-urlencoded")
                        .header("User-Agent", "Mozilla/5.0").header("Sec-Fetch-Mode", "navigate"))
                .andExpect(redirectedUrl("/payments"));
        verify(payments).updatePayment(eq(7L), any(PaymentForm.class));
    }

    @Test
    void headersCannotSupplyMissingBusinessValues() throws Exception {
        when(payments.createPayment(any())).thenThrow(new IllegalArgumentException("Required field missing"));
        mvc.perform(validPost("/payments/save").header("amount", "999")
                        .with(request -> { request.removeParameter("amount"); return request; }))
                .andExpect(model().attributeHasErrors("payment"));
        verify(payments).createPayment(argThat(form -> form.getAmount() == null));
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
        return post(path).param("invoiceId", "1").param("amount", "12.34")
                .param("paymentDate", "2020-01-01").param("paymentStatus", "PENDING");
    }
}




