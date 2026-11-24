package com.ats.scorer.controller;

import com.ats.scorer.service.AtsScoringService;
import com.ats.scorer.service.ResumeParserService;
import com.ats.scorer.service.ResumeTailoringService;
import com.ats.scorer.dto.TailoredResumeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@WebMvcTest(ScoringController.class)
class ScoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResumeParserService resumeParserService;

    @MockitoBean
    private ResumeTailoringService resumeTailoringService;

    @MockitoBean
    private AtsScoringService atsScoringService;

    private String sampleResumeText;
    private String sampleJd;

    {
        sampleResumeText = """
                John Doe
                Email: john@example.com
                Phone: 123-456-7890

                SUMMARY
                Experienced software developer with 5 years of experience in Java and Spring Boot.

                SKILLS
                Java, Spring Boot, Docker, AWS
                """;

        sampleJd = "We are looking for a Java developer with Spring Boot and Docker experience.";
    }

    @Nested
    @DisplayName("POST /api/tailor - Happy path tests")
    class TailorHappyPathTests {

        @Test
        @DisplayName("Should return tailored resume for valid TXT file upload")
        void tailorResume_validTxtFile_returnsOk() throws Exception {
            // Arrange
            MockMultipartFile resumeFile = new MockMultipartFile(
                    "resume", "resume.txt", "text/plain", sampleResumeText.getBytes());

            TailoredResumeResponse mockResponse = new TailoredResumeResponse();
            mockResponse.setTailoredResumeText("Tailored resume text");
            mockResponse.setIncludedSections(List.of("contact", "skills"));
            mockResponse.setExcludedSections(List.of("interests"));
            mockResponse.setHighlightedKeywords(List.of("java", "spring", "docker"));
            mockResponse.setRelevanceScore(75.5);

            when(resumeParserService.extractText(any())).thenReturn(sampleResumeText);
            when(resumeTailoringService.tailorResume(anyString(), anyString())).thenReturn(mockResponse);

            // Act & Assert
            mockMvc.perform(multipart("/api/tailor")
                            .file(resumeFile)
                            .param("jobDescription", sampleJd))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tailoredResumeText").value("Tailored resume text"))
                    .andExpect(jsonPath("$.includedSections").isArray())
                    .andExpect(jsonPath("$.excludedSections").isArray())
                    .andExpect(jsonPath("$.highlightedKeywords").isArray())
                    .andExpect(jsonPath("$.relevanceScore").exists());
        }

        @Test
        @DisplayName("Should return tailored resume for valid DOCX file upload")
        void tailorResume_validDocxFile_returnsOk() throws Exception {
            // Arrange
            MockMultipartFile resumeFile = new MockMultipartFile(
                    "resume", "resume.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    sampleResumeText.getBytes());

            TailoredResumeResponse mockResponse = new TailoredResumeResponse();
            mockResponse.setTailoredResumeText("Tailored resume from DOCX");
            mockResponse.setIncludedSections(List.of("contact", "experience"));
            mockResponse.setExcludedSections(List.of());
            mockResponse.setHighlightedKeywords(List.of("java"));
            mockResponse.setRelevanceScore(60.0);

            when(resumeParserService.extractText(any())).thenReturn(sampleResumeText);
            when(resumeTailoringService.tailorResume(anyString(), anyString())).thenReturn(mockResponse);

            // Act & Assert
            mockMvc.perform(multipart("/api/tailor")
                            .file(resumeFile)
                            .param("jobDescription", sampleJd))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tailoredResumeText").value("Tailored resume from DOCX"));
        }

        @Test
        @DisplayName("Should return tailored resume for valid PDF file upload")
        void tailorResume_validPdfFile_returnsOk() throws Exception {
            // Arrange
            MockMultipartFile resumeFile = new MockMultipartFile(
                    "resume", "resume.pdf", "application/pdf", sampleResumeText.getBytes());

            TailoredResumeResponse mockResponse = new TailoredResumeResponse();
            mockResponse.setTailoredResumeText("Tailored resume from PDF");
            mockResponse.setIncludedSections(List.of("contact", "skills", "experience"));
            mockResponse.setExcludedSections(List.of());
            mockResponse.setHighlightedKeywords(List.of("java", "docker"));
            mockResponse.setRelevanceScore(80.0);

            when(resumeParserService.extractText(any())).thenReturn(sampleResumeText);
            when(resumeTailoringService.tailorResume(anyString(), anyString())).thenReturn(mockResponse);

            // Act & Assert
            mockMvc.perform(multipart("/api/tailor")
                            .file(resumeFile)
                            .param("jobDescription", sampleJd))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.relevanceScore").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/tailor - Validation error tests")
    class TailorValidationTests {

        @Test
        @DisplayName("Should return error when resume file is missing")
        void tailorResume_missingResumeFile_returnsError() throws Exception {
            // Act & Assert
            mockMvc.perform(multipart("/api/tailor")
                            .param("jobDescription", sampleJd))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status == 400 || status == 500,
                                "Expected 400 or 500, but got " + status);
                    })
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("Should return 400 when resume file is empty")
        void tailorResume_emptyResumeFile_returnsBadRequest() throws Exception {
            // Arrange
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "resume", "empty.txt", "text/plain", new byte[0]);

            // Act & Assert
            mockMvc.perform(multipart("/api/tailor")
                            .file(emptyFile)
                            .param("jobDescription", sampleJd))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("Should return error when job description is missing")
        void tailorResume_missingJobDescription_returnsError() throws Exception {
            // Arrange
            MockMultipartFile resumeFile = new MockMultipartFile(
                    "resume", "resume.txt", "text/plain", sampleResumeText.getBytes());

            // Act & Assert
            mockMvc.perform(multipart("/api/tailor")
                            .file(resumeFile))
                    .andExpect(result -> {
                        int status = result.getResponse().getStatus();
                        assertTrue(status == 400 || status == 500,
                                "Expected 400 or 500, but got " + status);
                    })
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("Should return 400 when job description is empty")
        void tailorResume_emptyJobDescription_returnsBadRequest() throws Exception {
            // Arrange
            MockMultipartFile resumeFile = new MockMultipartFile(
                    "resume", "resume.txt", "text/plain", sampleResumeText.getBytes());

            // Act & Assert
            mockMvc.perform(multipart("/api/tailor")
                            .file(resumeFile)
                            .param("jobDescription", ""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("Should return 400 when job description is whitespace only")
        void tailorResume_whitespaceJobDescription_returnsBadRequest() throws Exception {
            // Arrange
            MockMultipartFile resumeFile = new MockMultipartFile(
                    "resume", "resume.txt", "text/plain", sampleResumeText.getBytes());

            // Act & Assert
            mockMvc.perform(multipart("/api/tailor")
                            .file(resumeFile)
                            .param("jobDescription", "   "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/tailor - Error handling tests")
    class TailorErrorHandlingTests {

        @Test
        @DisplayName("Should return 400 when text extraction fails")
        void tailorResume_extractionFails_returnsBadRequest() throws Exception {
            // Arrange
            MockMultipartFile resumeFile = new MockMultipartFile(
                    "resume", "resume.txt", "text/plain", sampleResumeText.getBytes());

            when(resumeParserService.extractText(any())).thenThrow(new IOException("File corrupted"));

            // Act & Assert
            mockMvc.perform(multipart("/api/tailor")
                            .file(resumeFile)
                            .param("jobDescription", sampleJd))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("Should return 400 when extracted text is empty")
        void tailorResume_emptyExtractedText_returnsBadRequest() throws Exception {
            // Arrange
            MockMultipartFile resumeFile = new MockMultipartFile(
                    "resume", "resume.txt", "text/plain", sampleResumeText.getBytes());

            when(resumeParserService.extractText(any())).thenReturn("");

            // Act & Assert
            mockMvc.perform(multipart("/api/tailor")
                            .file(resumeFile)
                            .param("jobDescription", sampleJd))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("Should return 500 when tailoring service throws unexpected exception")
        void tailorResume_serviceException_returnsInternalServerError() throws Exception {
            // Arrange
            MockMultipartFile resumeFile = new MockMultipartFile(
                    "resume", "resume.txt", "text/plain", sampleResumeText.getBytes());

            when(resumeParserService.extractText(any())).thenReturn(sampleResumeText);
            when(resumeTailoringService.tailorResume(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Unexpected error"));

            // Act & Assert
            mockMvc.perform(multipart("/api/tailor")
                            .file(resumeFile)
                            .param("jobDescription", sampleJd))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("An unexpected error occurred. Please try again later."));
        }
    }

    @Nested
    @DisplayName("GET /api/health - Health check tests")
    class HealthCheckTests {

        @Test
        @DisplayName("Should return health status UP")
        void health_returnsOk() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.service").value("ATS Scorer API"))
                    .andExpect(result -> assertTrue(result.getResponse().getHeader("X-Request-Id")
                            .matches("[0-9a-fA-F-]{36}")));
        }

        @Test
        @DisplayName("Should return 404 for a missing favicon")
        void missingFavicon_returnsNotFound() throws Exception {
            mockMvc.perform(get("/favicon.ico"))
                    .andExpect(status().isNotFound());
        }
    }
}
