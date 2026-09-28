package com.bustrans.fleettrack.controller;

import com.bustrans.fleettrack.config.SecurityConfig;
import com.bustrans.fleettrack.entity.Feedback;
import com.bustrans.fleettrack.entity.User;
import com.bustrans.fleettrack.repository.FeedbackRepository;
import com.bustrans.fleettrack.repository.UserRepository;
import com.bustrans.fleettrack.security.JwtService;
import com.bustrans.fleettrack.service.FeedbackService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Real controller/service/team security; mock only JWT verification and database boundaries.
@WebMvcTest(FeedbackController.class)
@Import({SecurityConfig.class, FeedbackService.class})
class FeedbackOwnershipIntegrationTest {
    @Autowired MockMvc mvc;
    @MockBean FeedbackRepository feedback;
    @MockBean UserRepository users;
    @MockBean JwtService jwt;

    @BeforeEach
    void authenticate() {
        token("11", "STUDENT");
        User owner = new User();
        owner.setUserId(11L);
        when(users.findById(11L)).thenReturn(Optional.of(owner));
        when(feedback.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    private void token(String subject, String role) {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(subject);
        when(claims.get("role", String.class)).thenReturn(role);
        when(jwt.parse("test-token")).thenReturn(claims);
    }

    @Test
    void createUsesJwtOwnerAndServerMetadata() throws Exception {
        mvc.perform(post("/feedback/save").header("Authorization", "Bearer test-token")
                        .param("subject", " Transport ").param("comments", " Exact text "))
                .andExpect(redirectedUrl("/feedback"));
        verify(feedback).save(argThat(record -> record.getUser().getUserId().equals(11L)
                && record.getSubject().equals(" Transport ")
                && record.getComments().equals(" Exact text ")
                && record.getStatus().equals("Pending") && record.getSubmittedAt() != null
                && record.getFeedbackId() == null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"STUDENT", "ADMIN"})
    void noRoleGetsAnUnapprovedOwnershipBypass(String role) throws Exception {
        token("11", role);
        mvc.perform(get("/feedback/edit/7").header("Authorization", "Bearer test-token"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/feedback/edit/7").header("Authorization", "Bearer test-token")
                        .param("subject", "Attempt").param("comments", "Attempt"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/feedback/delete/7").header("Authorization", "Bearer test-token"))
                .andExpect(status().isNotFound());
        verify(feedback, times(3)).findByFeedbackIdAndUser_UserId(7, 11L);
        verify(feedback, never()).save(any());
        verify(feedback, never()).delete(any());
    }

    @Test
    void ownerCanEditAndDeleteWithoutChangingServerFields() throws Exception {
        Feedback record = new Feedback();
        User owner = users.findById(11L).orElseThrow();
        record.setFeedbackId(7);
        record.setUser(owner);
        record.setSubject("Original");
        record.setComments("Original");
        record.setStatus("Reviewed");
        var submitted = java.time.LocalDateTime.of(2020, 1, 1, 10, 20);
        record.setSubmittedAt(submitted);
        when(feedback.findByFeedbackIdAndUser_UserId(7, 11L)).thenReturn(Optional.of(record));
        mvc.perform(post("/feedback/edit/7").header("Authorization", "Bearer test-token")
                        .param("subject", "Edited").param("comments", "Edited text"))
                .andExpect(redirectedUrl("/feedback"));
        verify(feedback).save(argThat(value -> value.getFeedbackId() == 7
                && value.getUser() == owner && value.getStatus().equals("Reviewed")
                && value.getSubmittedAt().equals(submitted) && value.getSubject().equals("Edited")));
        mvc.perform(get("/feedback/delete/7").header("Authorization", "Bearer test-token"))
                .andExpect(redirectedUrl("/feedback"));
        verify(feedback).delete(record);
    }

    @Test
    void listUsesOnlyPrincipalOwner() throws Exception {
        when(feedback.findByUser_UserId(11L)).thenReturn(List.of());
        mvc.perform(get("/feedback").header("Authorization", "Bearer test-token").param("userId", "22"))
                .andExpect(status().isOk());
        verify(feedback).findByUser_UserId(11L);
        verify(feedback, never()).findAll();
    }

    @ParameterizedTest
    @ValueSource(strings = {"userId", "UserId", "user.userId", "feedbackId", "status", "submittedAt"})
    void rejectsPostedServerFields(String field) throws Exception {
        mvc.perform(post("/feedback/save").header("Authorization", "Bearer test-token")
                        .param("subject", "Transport").param("comments", "Valid").param(field, "22"))
                .andExpect(model().attributeHasErrors("feedback"));
        verify(feedback, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-user-id", "0", "-1", "2147483648"})
    void invalidJwtIdentityFailsClosed(String subject) throws Exception {
        token(subject, "STUDENT");
        mvc.perform(post("/feedback/save").header("Authorization", "Bearer test-token")
                        .param("subject", "Transport").param("comments", "Valid"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(feedback);
    }

    @Test
    void nonexistentJwtUserCannotCreate() throws Exception {
        when(users.findById(11L)).thenReturn(Optional.empty());
        mvc.perform(post("/feedback/save").header("Authorization", "Bearer test-token")
                        .param("subject", "Transport").param("comments", "Valid"))
                .andExpect(status().isForbidden());
        verify(feedback, never()).save(any());
    }

    @Test
    void missingJwtCannotCreate() throws Exception {
        mvc.perform(post("/feedback/save").param("subject", "Transport").param("comments", "Valid"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(feedback);
    }
}
