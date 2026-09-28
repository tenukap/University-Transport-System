package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.config.SecurityConfig;
import com.bustrans.fleettrack.security.JwtService;
import com.bustrans.fleettrack.service.FeedbackService;
import com.bustrans.fleettrack.service.InvoiceService;
import com.bustrans.fleettrack.service.PaymentService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Uses the team's security/filter/advice with mocked business services: no database or Flyway.
@WebMvcTest({InvoiceController.class, PaymentController.class, FeedbackController.class})
@Import(SecurityConfig.class)
class ModuleSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @MockBean InvoiceService invoices;
    @MockBean PaymentService payments;
    @MockBean FeedbackService feedback;
    @MockBean JwtService jwt;

    @ParameterizedTest
    @ValueSource(strings = {"/invoices", "/payments", "/feedback"})
    void anonymousModuleRequestsRemainUnauthorized(String route) throws Exception {
        mvc.perform(get(route)).andExpect(status().isUnauthorized());
        verifyNoInteractions(invoices, payments, feedback);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/invoices", "/payments", "/feedback"})
    void bearerAuthenticatedRequestsReachModuleViews(String route) throws Exception {
        authenticate();
        mvc.perform(get(route).header("Authorization", "Bearer integration-test-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    void teamAdviceControlsUnhandledDatabaseErrors() throws Exception {
        authenticate();
        doThrow(new DataAccessResourceFailureException("Database unavailable"))
                .when(invoices).deleteInvoice(7L);
        mvc.perform(get("/invoices/delete/7").header("Authorization", "Bearer integration-test-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Database unavailable"));
    }

    private void authenticate() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("7");
        when(claims.get("role", String.class)).thenReturn("STUDENT");
        when(jwt.parse("integration-test-token")).thenReturn(claims);
    }
}
