package com.bustrans.fleettrack.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FeedbackController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {JwtAuthenticationFilter.class, ApiExceptionHandler.class, GlobalExceptionHandler.class}))
@AutoConfigureMockMvc(addFilters = false)
class FeedbackControllerTest {
    @Autowired MockMvc mvc;
    @MockBean FeedbackService service;

    @BeforeEach
    void setup() {
        Feedback record = new Feedback();
        record.setFeedbackId(7L);
        record.setComments("  Original text  ");
        record.setFeedbackDate(LocalDate.of(2020, 1, 1));
        when(service.getFeedbackById(7L)).thenReturn(record);
        when(service.getAllFeedback()).thenReturn(List.of(record));
    }

    @Test
    void formsUseSeparateActionsAndNoHiddenIds() throws Exception {
        mvc.perform(get("/feedback/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/feedback/save\"")))
                .andExpect(content().string(not(containsString("name=\"feedbackId\""))))
                .andExpect(content().string(not(containsString("name=\"feedbackDate\""))));
        mvc.perform(get("/feedback/edit/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/feedback/edit/7\"")))
                .andExpect(content().string(not(containsString("name=\"feedbackId\""))))
                .andExpect(content().string(not(containsString("name=\"feedbackDate\""))))
                .andExpect(content().string(containsString("  Original text  ")));
    }

    @Test
    void createAndUpdateUseScalarFormsAndRouteTarget() throws Exception {
        mvc.perform(validPost("/feedback/save")).andExpect(redirectedUrl("/feedback"));
        verify(service).createFeedback(argThat(form -> form.getComments().equals("  Submitted text  ")));
        mvc.perform(validPost("/feedback/edit/7")).andExpect(redirectedUrl("/feedback"));
        verify(service).updateFeedback(eq(7L), any(FeedbackForm.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"feedbackId", "id", "feedbackDate", "student.id"})
    void rejectsUnexpectedFieldsOnCreateAndUpdate(String field) throws Exception {
        for (String path : List.of("/feedback/save", "/feedback/edit/7")) {
            mvc.perform(validPost(path).param(field, "99"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasErrors("feedback"))
                    .andExpect(content().string(containsString("Unexpected form fields")));
        }
        verify(service, never()).createFeedback(any());
        verify(service, never()).updateFeedback(anyLong(), any());
    }

    @Test
    void validationErrorsPreserveValuesAndEditAction() throws Exception {
        when(service.createFeedback(any())).thenThrow(new IllegalArgumentException("Validation message"));
        when(service.updateFeedback(eq(7L), any())).thenThrow(new IllegalArgumentException("Validation message"));
        for (String path : List.of("/feedback/save", "/feedback/edit/7")) {
            mvc.perform(validPost(path))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasErrors("feedback"))
                    .andExpect(content().string(containsString("Validation message")))
                    .andExpect(content().string(containsString("action=\"" + path + "\"")))
                    .andExpect(content().string(containsString("  Submitted text  ")));
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, 99})
    void missingTargetsReturn404ForEditUpdateAndDelete(long id) throws Exception {
        when(service.getFeedbackById(id)).thenThrow(new NoSuchElementException("Feedback not found"));
        doThrow(new NoSuchElementException("Feedback not found")).when(service).deleteFeedback(id);
        mvc.perform(get("/feedback/edit/" + id)).andExpect(status().isNotFound());
        mvc.perform(validPost("/feedback/edit/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/feedback/delete/" + id)).andExpect(status().isNotFound());
        verify(service, never()).updateFeedback(anyLong(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "999999999999999999999999"})
    void malformedRouteIdsReturn400(String id) throws Exception {
        mvc.perform(get("/feedback/edit/" + id)).andExpect(status().isBadRequest());
        mvc.perform(validPost("/feedback/edit/" + id)).andExpect(status().isBadRequest());
        mvc.perform(get("/feedback/delete/" + id)).andExpect(status().isBadRequest());
        verify(service, never()).updateFeedback(anyLong(), any());
        verify(service, never()).deleteFeedback(anyLong());
    }

    @Test
    void deletesExistingRecordAndRedirects() throws Exception {
        mvc.perform(get("/feedback/delete/7")).andExpect(redirectedUrl("/feedback"));
        verify(service).deleteFeedback(7L);
    }
 
    @Test
    void commentsAreEscapedWhenRedisplayingErrors() throws Exception {
        when(service.createFeedback(any())).thenThrow(new IllegalArgumentException("Invalid feedback"));
        mvc.perform(post("/feedback/save").param("comments", "<script>alert(1)</script>"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
    }


    @Test
    void pagesHaveConsistentNavigationAccessibilityAndMessages() throws Exception {
        for (String path : List.of("/feedback", "/feedback/new")) {
            String html = mvc.perform(get(path).flashAttr("successMessage", "Feedback record saved."))
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
                org.junit.jupiter.api.Assertions.assertTrue(html.contains("Feedback record saved."));
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
        when(service.getAllFeedback()).thenReturn(List.of());
        mvc.perform(get("/feedback")).andExpect(status().isOk())
                .andExpect(content().string(containsString("No feedback submitted yet.")));
    }

    @Test
    void successfulActionsHaveFactualFlashMessages() throws Exception {
        mvc.perform(validPost("/feedback/save"))
                .andExpect(flash().attribute("successMessage", "Feedback record saved."));
        mvc.perform(validPost("/feedback/edit/7"))
                .andExpect(flash().attribute("successMessage", "Feedback record saved."));
        mvc.perform(get("/feedback/delete/7"))
                .andExpect(flash().attribute("successMessage", "Feedback record deleted."));
    }

    @Test
    void invalidSaveDoesNotProduceSuccessMessage() throws Exception {
        when(service.createFeedback(any())).thenThrow(new IllegalArgumentException("Invalid input"));
        mvc.perform(validPost("/feedback/save"))
                .andExpect(model().attributeHasErrors("feedback"))
                .andExpect(flash().attributeCount(0))
                .andExpect(content().string(containsString("message-error")));
    }

    @Test
    void renderedFormSubmitsSuccessfullyWithBrowserHeaders() throws Exception {
        String html = mvc.perform(get("/feedback/new")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        java.util.Map<String, String> values = java.util.Map.of(
                "comments", "Valid feedback");
        java.util.regex.Matcher controls = java.util.regex.Pattern
                .compile("<(?:input|select|textarea)\\b[^>]*\\bname=\"([^\"]+)\"", java.util.regex.Pattern.DOTALL)
                .matcher(html);
        MockHttpServletRequestBuilder request = browserPost("/feedback/save");
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
                    org.springframework.validation.BindingResult.MODEL_KEY_PREFIX + "feedback");
            if (binding instanceof org.springframework.validation.BindingResult errors) {
                org.junit.jupiter.api.Assertions.assertFalse(errors.hasErrors(),
                        "Rejected browser submission; suppressed fields: " + java.util.Arrays.toString(errors.getSuppressedFields()));
            }
        }
        org.junit.jupiter.api.Assertions.assertEquals(302, result.getResponse().getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("/feedback", result.getResponse().getRedirectedUrl());
        verify(service).createFeedback(any(FeedbackForm.class));
    }


    @ParameterizedTest
    @ValueSource(strings = {"feedbackId", "id", "contentType", "userAgent", "unexpectedBusinessField"})
    void browserHeadersDoNotAllowUnexpectedBodyFields(String field) throws Exception {
        for (String path : List.of("/feedback/save", "/feedback/edit/7")) {
            mvc.perform(validPost(path).contentType("application/x-www-form-urlencoded")
                            .header("User-Agent", "Mozilla/5.0").param(field, "99"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasErrors("feedback"))
                    .andExpect(content().string(containsString("Unexpected form fields")));
        }
        verify(service, never()).createFeedback(any());
        verify(service, never()).updateFeedback(anyLong(), any());
    }

    @Test
    void browserHeadersAlsoWorkOnUpdate() throws Exception {
        mvc.perform(validPost("/feedback/edit/7")
                        .contentType("application/x-www-form-urlencoded")
                        .header("User-Agent", "Mozilla/5.0").header("Sec-Fetch-Mode", "navigate"))
                .andExpect(redirectedUrl("/feedback"));
        verify(service).updateFeedback(eq(7L), any(FeedbackForm.class));
    }

    @Test
    void headersCannotSupplyMissingBusinessValues() throws Exception {
        when(service.createFeedback(any())).thenThrow(new IllegalArgumentException("Required field missing"));
        mvc.perform(validPost("/feedback/save").header("comments", "999")
                        .with(request -> { request.removeParameter("comments"); return request; }))
                .andExpect(model().attributeHasErrors("feedback"));
        verify(service).createFeedback(argThat(form -> form.getComments() == null));
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
        return post(path).param("comments", "  Submitted text  ");
    }
}




