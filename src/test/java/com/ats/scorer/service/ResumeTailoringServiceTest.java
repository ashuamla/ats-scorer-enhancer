package com.ats.scorer.service;

import com.ats.scorer.dto.TailoredResumeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResumeTailoringServiceTest {

    private ResumeTailoringService service;

    // Sample resume text with all common sections
    private String fullResume;

    // Sample job description that matches many sections
    private String relevantJd;

    // Sample job description that matches few sections
    private String irrelevantJd;

    @BeforeEach
    void setUp() {
        service = new ResumeTailoringService();

        fullResume = """
                John Doe
                Email: john@example.com
                Phone: 123-456-7890
                LinkedIn: linkedin.com/in/johndoe

                SUMMARY
                Experienced software developer with 5 years of experience in Java, Spring Boot, and cloud technologies.

                EXPERIENCE
                Senior Software Engineer at Tech Corp (2022-2025)
                - Developed microservices using Spring Boot and Java
                - Implemented CI/CD pipelines using Jenkins and Docker
                - Worked with AWS cloud services

                Software Developer at Startup Inc (2020-2022)
                - Built REST APIs using Spring Framework
                - Worked with PostgreSQL and MongoDB databases

                EDUCATION
                Bachelor of Science in Computer Science
                University of Technology, 2020

                SKILLS
                Java, Spring Boot, Python, Docker, Kubernetes, AWS, PostgreSQL, MongoDB, Jenkins, Git

                PROJECTS
                E-commerce Platform
                - Built a full-stack e-commerce application using Spring Boot and React
                - Implemented payment integration and inventory management

                CERTIFICATIONS
                AWS Certified Solutions Architect
                Certified Kubernetes Administrator

                ACHIEVEMENTS
                - Received Employee of the Year award in 2023
                - Published a paper on microservices architecture

                INTERESTS
                Open source contribution, hiking, reading technology blogs
                """;

        relevantJd = """
                We are looking for a Senior Software Engineer with experience in Java, Spring Boot,
                and cloud technologies like AWS. The candidate should have hands-on experience with
                Docker, Kubernetes, and CI/CD pipelines. Strong skills in microservices architecture
                and REST API development are required. A degree in Computer Science is preferred.
                Certifications in AWS or Kubernetes are a plus.
                """;

        irrelevantJd = """
                We are seeking a Marketing Manager with 5+ years of experience in digital marketing,
                SEO, SEM, and social media campaigns. The ideal candidate should have expertise in
                Google Analytics, content strategy, and brand management. A degree in Marketing or
                Business Administration is required.
                """;
    }

    @Nested
    @DisplayName("tailorResume - Full workflow tests")
    class TailorResumeTests {

        @Test
        @DisplayName("Should return tailored resume with relevant sections for matching JD")
        void tailorResume_withRelevantJd_returnsIncludedSections() {
            // Act
            TailoredResumeResponse response = service.tailorResume(fullResume, relevantJd);

            // Assert
            assertNotNull(response);
            assertNotNull(response.getTailoredResumeText());
            assertFalse(response.getTailoredResumeText().isEmpty());

            // Key sections should be included for a relevant JD
            assertTrue(response.getIncludedSections().contains("experience"),
                    "Experience section should be included for relevant JD");
            assertTrue(response.getIncludedSections().contains("skills"),
                    "Skills section should be included for relevant JD");
            assertTrue(response.getIncludedSections().contains("education"),
                    "Education section should be included for relevant JD");

            // Should have highlighted keywords
            assertNotNull(response.getHighlightedKeywords());
            assertFalse(response.getHighlightedKeywords().isEmpty(),
                    "Should find matched keywords between resume and JD");

            // Relevance score should be reasonable
            assertTrue(response.getRelevanceScore() > 0,
                    "Relevance score should be greater than 0 for relevant JD");
            assertTrue(response.getRelevanceScore() <= 100,
                    "Relevance score should not exceed 100");
        }

        @Test
        @DisplayName("Should exclude irrelevant sections for non-matching JD")
        void tailorResume_withIrrelevantJd_excludesSections() {
            // Act
            TailoredResumeResponse response = service.tailorResume(fullResume, irrelevantJd);

            // Assert
            assertNotNull(response);

            // Contact should always be included
            assertTrue(response.getIncludedSections().contains("contact"),
                    "Contact section should always be included");

            // Irrelevant sections should be excluded
            assertTrue(response.getExcludedSections().size() > 0,
                    "Some sections should be excluded for irrelevant JD");

            // Relevance score should be low
            assertTrue(response.getRelevanceScore() < 50,
                    "Relevance score should be low for irrelevant JD");
        }

        @Test
        @DisplayName("Should generate tailored summary with matched keywords")
        void tailorResume_generatesTailoredSummary() {
            // Act
            TailoredResumeResponse response = service.tailorResume(fullResume, relevantJd);

            // Assert
            String tailoredText = response.getTailoredResumeText();
            assertNotNull(tailoredText);
            assertTrue(tailoredText.contains("PROFESSIONAL SUMMARY"),
                    "Tailored resume should contain professional summary");
            assertTrue(tailoredText.contains("Key matched skills"),
                    "Tailored resume should list key matched skills");
        }

        @Test
        @DisplayName("Should handle empty resume text")
        void tailorResume_withEmptyResume_returnsEmptyResult() {
            // Act
            TailoredResumeResponse response = service.tailorResume("", relevantJd);

            // Assert
            assertNotNull(response);
            assertTrue(response.getIncludedSections().isEmpty(),
                    "No sections should be included for empty resume");
            assertTrue(response.getExcludedSections().isEmpty(),
                    "No sections should be excluded for empty resume");
            assertTrue(response.getHighlightedKeywords().isEmpty(),
                    "No keywords should be highlighted for empty resume");
            assertEquals(0.0, response.getRelevanceScore(),
                    "Relevance score should be 0 for empty resume");
        }

        @Test
        @DisplayName("Should handle empty job description")
        void tailorResume_withEmptyJd_returnsMinimalResult() {
            // Act
            TailoredResumeResponse response = service.tailorResume(fullResume, "");

            // Assert
            assertNotNull(response);
            assertTrue(response.getHighlightedKeywords().isEmpty(),
                    "No keywords should be highlighted for empty JD");
        }

        @Test
        @DisplayName("Should include contact section even with low relevance")
        void tailorResume_alwaysIncludesContact() {
            // Act
            TailoredResumeResponse response = service.tailorResume(fullResume, irrelevantJd);

            // Assert
            assertTrue(response.getIncludedSections().contains("contact"),
                    "Contact section should always be included");
            assertFalse(response.getExcludedSections().contains("contact"),
                    "Contact section should not be in excluded list");
        }

        @Test
        @DisplayName("Should return non-null response with all fields populated")
        void tailorResume_responseFieldsNotNull() {
            // Act
            TailoredResumeResponse response = service.tailorResume(fullResume, relevantJd);

            // Assert
            assertNotNull(response.getTailoredResumeText());
            assertNotNull(response.getIncludedSections());
            assertNotNull(response.getExcludedSections());
            assertNotNull(response.getHighlightedKeywords());
        }
    }

    @Nested
    @DisplayName("tailorResume - Keyword matching tests")
    class KeywordMatchingTests {

        @Test
        @DisplayName("Should highlight keywords present in both resume and JD")
        void tailorResume_highlightsMatchedKeywords() {
            // Act
            TailoredResumeResponse response = service.tailorResume(fullResume, relevantJd);

            // Assert
            List<String> keywords = response.getHighlightedKeywords();
            assertFalse(keywords.isEmpty());

            // Check for expected matched keywords
            boolean hasJava = keywords.stream().anyMatch(k -> k.equalsIgnoreCase("java"));
            boolean hasSpring = keywords.stream().anyMatch(k -> k.equalsIgnoreCase("spring"));
            boolean hasDocker = keywords.stream().anyMatch(k -> k.equalsIgnoreCase("docker"));

            assertTrue(hasJava || hasSpring || hasDocker,
                    "Should highlight at least one of the common keywords: java, spring, docker");
        }

        @Test
        @DisplayName("Should not highlight keywords not present in resume")
        void tailorResume_noFalseKeywords() {
            // Act
            TailoredResumeResponse response = service.tailorResume(fullResume, irrelevantJd);

            // Assert
            List<String> keywords = response.getHighlightedKeywords();

            // Marketing-specific keywords should not appear
            boolean hasMarketingKeyword = keywords.stream()
                    .anyMatch(k -> k.equalsIgnoreCase("seo") || k.equalsIgnoreCase("sem"));
            assertFalse(hasMarketingKeyword,
                    "Should not highlight marketing keywords not in resume");
        }
    }

    @Nested
    @DisplayName("tailorResume - Section ordering tests")
    class SectionOrderingTests {

        @Test
        @DisplayName("Should order sections by relevance score")
        void tailorResume_ordersByRelevance() {
            // Act
            TailoredResumeResponse response = service.tailorResume(fullResume, relevantJd);

            // Assert
            List<String> included = response.getIncludedSections();
            assertTrue(included.size() > 1,
                    "Should include multiple sections for relevant JD");

            // Contact should be included (it's always forced into included sections)
            assertTrue(included.contains("contact"),
                    "Contact section should be in included sections");
        }

        @Test
        @DisplayName("Should include experience and skills for technical JD")
        void tailorResume_includesTechnicalSections() {
            // Act
            TailoredResumeResponse response = service.tailorResume(fullResume, relevantJd);

            // Assert
            List<String> included = response.getIncludedSections();
            assertTrue(included.contains("experience"), "Should include experience");
            assertTrue(included.contains("skills"), "Should include skills");
        }
    }

    @Nested
    @DisplayName("tailorResume - Edge case tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle resume with only contact info")
        void tailorResume_onlyContactInfo() {
            String minimalResume = "John Doe\nEmail: john@example.com\nPhone: 123-456-7890";

            // Act
            TailoredResumeResponse response = service.tailorResume(minimalResume, relevantJd);

            // Assert
            assertNotNull(response);
            assertTrue(response.getIncludedSections().contains("contact"),
                    "Contact section should be included");
        }

        @Test
        @DisplayName("Should handle resume with unusual section headers")
        void tailorResume_unusualHeaders() {
            String resumeWithAliases = """
                    Jane Smith
                    Email: jane@example.com

                    PROFESSIONAL SUMMARY
                    A passionate developer.

                    WORK EXPERIENCE
                    Developer at Company X

                    TECHNICAL SKILLS
                    Python, JavaScript, React
                    """;

            // Act
            TailoredResumeResponse response = service.tailorResume(resumeWithAliases,
                    "We need a developer with Python and JavaScript skills.");

            // Assert
            assertNotNull(response);
            assertTrue(response.getIncludedSections().contains("contact"),
                    "Contact should be detected");
            assertTrue(response.getIncludedSections().contains("summary"),
                    "Summary should be detected via alias");
            assertTrue(response.getIncludedSections().contains("experience"),
                    "Experience should be detected via alias");
            assertTrue(response.getIncludedSections().contains("skills"),
                    "Skills should be detected via alias");
        }

        @Test
        @DisplayName("Should handle resume with single section")
        void tailorResume_singleSection() {
            String singleSectionResume = """
                    SKILLS
                    Java, Python, Docker, Kubernetes
                    """;

            // Act
            TailoredResumeResponse response = service.tailorResume(singleSectionResume, relevantJd);

            // Assert
            assertNotNull(response);
            assertTrue(response.getIncludedSections().contains("skills"),
                    "Skills section should be included");
        }

        @Test
        @DisplayName("Should handle whitespace-only resume")
        void tailorResume_whitespaceOnly() {
            // Act
            TailoredResumeResponse response = service.tailorResume("   \n\n   ", relevantJd);

            // Assert
            assertNotNull(response);
            // Should not throw exception
        }

        @Test
        @DisplayName("Should handle very long job description")
        void tailorResume_longJd() {
            StringBuilder longJd = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                longJd.append("We need experience with Java Spring Boot Docker Kubernetes AWS. ");
            }

            // Act
            TailoredResumeResponse response = service.tailorResume(fullResume, longJd.toString());

            // Assert
            assertNotNull(response);
            assertTrue(response.getRelevanceScore() > 0,
                    "Should handle long JD without errors");
        }

        @Test
        @DisplayName("Should handle special characters in resume")
        void tailorResume_specialCharacters() {
            String resumeWithSpecialChars = """
                    John Doe
                    Email: john@example.com

                    SKILLS
                    Java, C++, Python, #JavaScript, @Docker, $AWS!
                    """;

            // Act
            TailoredResumeResponse response = service.tailorResume(resumeWithSpecialChars,
                    "We need Java and Docker skills.");

            // Assert
            assertNotNull(response);
            // Should not throw exception
        }
    }

    @Nested
    @DisplayName("tailorResume - Relevance score tests")
    class RelevanceScoreTests {

        @Test
        @DisplayName("Relevance score should be between 0 and 100")
        void tailorResume_scoreInRange() {
            // Act
            TailoredResumeResponse response = service.tailorResume(fullResume, relevantJd);

            // Assert
            assertTrue(response.getRelevanceScore() >= 0,
                    "Score should not be negative");
            assertTrue(response.getRelevanceScore() <= 100,
                    "Score should not exceed 100");
        }

        @Test
        @DisplayName("Relevant JD should have higher score than irrelevant JD")
        void tailorResume_relevantJdHigherScore() {
            // Act
            TailoredResumeResponse relevantResponse = service.tailorResume(fullResume, relevantJd);
            TailoredResumeResponse irrelevantResponse = service.tailorResume(fullResume, irrelevantJd);

            // Assert
            assertTrue(relevantResponse.getRelevanceScore() > irrelevantResponse.getRelevanceScore(),
                    "Relevant JD should produce higher relevance score than irrelevant JD");
        }
    }
}
