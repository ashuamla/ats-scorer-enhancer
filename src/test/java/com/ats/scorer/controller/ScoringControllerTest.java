package com.ats.scorer.controller;

import com.ats.scorer.dto.ScoreResponse;
import com.ats.scorer.service.AtsScoringService;
import com.ats.scorer.service.ResumeParserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScoringController.class)
class ScoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResumeParserService resumeParserService;

    @MockitoBean
    private AtsScoringService atsScoringService;

    private static final String SAMPLE_RESUME = "John Doe\nJava, Spring Boot, Docker";
    private static final String SAMPLE_JOB_DESCRIPTION = "Java developer with Spring Boot experience";

    @Nested
    @DisplayName("POST /api/score")
    class ScoreResumeTests {

        @Test
        @DisplayName("Returns a complete score report for a valid resume")
        void scoreResume_validRequest_returnsReport() throws Exception {
            MockMultipartFile resume = new MockMultipartFile(
                    "resume", "resume.txt", "text/plain", SAMPLE_RESUME.getBytes());
            ScoreResponse scoreResponse = new ScoreResponse();
            scoreResponse.setOverallScore(82.5);
            scoreResponse.setKeywordScore(80.0);
            scoreResponse.setSectionScore(100.0);
            scoreResponse.setExperienceScore(60.0);
            scoreResponse.setFormatScore(70.0);
            scoreResponse.setMatchedKeywords(List.of("java", "spring"));
            scoreResponse.setMissingKeywords(List.of("docker"));
            scoreResponse.setSuggestions(List.of("Add more role-specific details."));
            scoreResponse.setGrade("A");

            when(resumeParserService.extractText(any())).thenReturn(SAMPLE_RESUME);
            when(atsScoringService.score(SAMPLE_RESUME, SAMPLE_JOB_DESCRIPTION)).thenReturn(scoreResponse);

            mockMvc.perform(multipart("/api/score")
                            .file(resume)
                            .param("jobDescription", SAMPLE_JOB_DESCRIPTION))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.overallScore").value(82.5))
                    .andExpect(jsonPath("$.keywordScore").value(80.0))
                    .andExpect(jsonPath("$.matchedKeywords[0]").value("java"))
                    .andExpect(jsonPath("$.missingKeywords[0]").value("docker"))
                    .andExpect(jsonPath("$.suggestions[0]").value("Add more role-specific details."))
                    .andExpect(jsonPath("$.grade").value("A"));
        }

        @Test
        @DisplayName("Returns a bad request for an empty resume")
        void scoreResume_emptyResume_returnsBadRequest() throws Exception {
            MockMultipartFile resume = new MockMultipartFile("resume", "resume.txt", "text/plain", new byte[0]);

            mockMvc.perform(multipart("/api/score")
                            .file(resume)
                            .param("jobDescription", SAMPLE_JOB_DESCRIPTION))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Please upload a resume file"));
        }

        @Test
        @DisplayName("Returns a bad request for a blank job description")
        void scoreResume_blankJobDescription_returnsBadRequest() throws Exception {
            MockMultipartFile resume = new MockMultipartFile(
                    "resume", "resume.txt", "text/plain", SAMPLE_RESUME.getBytes());

            mockMvc.perform(multipart("/api/score")
                            .file(resume)
                            .param("jobDescription", "   "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Please provide a job description"));
        }

        @Test
        @DisplayName("Returns a bad request when resume parsing fails")
        void scoreResume_parserThrows_returnsBadRequest() throws Exception {
            MockMultipartFile resume = new MockMultipartFile(
                    "resume", "resume.txt", "text/plain", SAMPLE_RESUME.getBytes());
            when(resumeParserService.extractText(any())).thenThrow(new IOException("Corrupted file"));

            mockMvc.perform(multipart("/api/score")
                            .file(resume)
                            .param("jobDescription", SAMPLE_JOB_DESCRIPTION))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("Returns a bad request when resume text cannot be extracted")
        void scoreResume_emptyExtractedText_returnsBadRequest() throws Exception {
            MockMultipartFile resume = new MockMultipartFile(
                    "resume", "resume.txt", "text/plain", SAMPLE_RESUME.getBytes());
            when(resumeParserService.extractText(any())).thenReturn("  ");

            mockMvc.perform(multipart("/api/score")
                            .file(resume)
                            .param("jobDescription", SAMPLE_JOB_DESCRIPTION))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("Returns a generic server error when scoring fails unexpectedly")
        void scoreResume_serviceThrows_returnsInternalServerError() throws Exception {
            MockMultipartFile resume = new MockMultipartFile(
                    "resume", "resume.txt", "text/plain", SAMPLE_RESUME.getBytes());
            when(resumeParserService.extractText(any())).thenReturn(SAMPLE_RESUME);
            when(atsScoringService.score(SAMPLE_RESUME, SAMPLE_JOB_DESCRIPTION))
                    .thenThrow(new RuntimeException("Scoring failure"));

            mockMvc.perform(multipart("/api/score")
                            .file(resume)
                            .param("jobDescription", SAMPLE_JOB_DESCRIPTION))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("An unexpected error occurred. Please try again later."));
        }
    }

    @Nested
    @DisplayName("Health endpoints")
    class HealthTests {

        @Test
        @DisplayName("Serves the scoring-only resume analysis page")
        void homePage_showsScoringExperience() throws Exception {
            String page;
            try (var input = new ClassPathResource("static/index.html").getInputStream()) {
                page = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }

            assertTrue(page.contains("Understand your resume match"));
            assertFalse(page.contains("Tailor Resume"));
        }

        @Test
        @DisplayName("Returns the health status and request ID")
        void health_returnsOk() throws Exception {
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.service").value("ATS Scorer API"))
                    .andExpect(result -> {
                        String requestId = result.getResponse().getHeader("X-Request-Id");
                        assertNotNull(requestId);
                        assertTrue(requestId.matches("[0-9a-fA-F-]{36}"));
                    });
        }

        @Test
        @DisplayName("Returns not found for a missing favicon")
        void missingFavicon_returnsNotFound() throws Exception {
            mockMvc.perform(get("/favicon.ico"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Does not expose the removed resume tailoring endpoint")
        void tailorEndpoint_returnsNotFound() throws Exception {
            mockMvc.perform(multipart("/api/tailor")
                            .param("jobDescription", SAMPLE_JOB_DESCRIPTION))
                    .andExpect(status().isNotFound());
        }
    }
}