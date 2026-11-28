package com.ats.scorer.service;

import com.ats.scorer.dto.ScoreResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ResumeAnalysisProviderHttpTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
    }

    @Test
    void groqProviderUsesOpenAiCompatibleChatCompletionsApi() throws Exception {
        GroqResumeAnalysisProvider provider = new GroqResumeAnalysisProvider(
                restClientBuilder, jsonMapper, "test-groq-key", "https://groq.test/openai/v1/", "groq-model");
        mockOpenAiCompatibleResponse("https://groq.test/openai/v1/chat/completions", "test-groq-key", "groq-model");

        ScoreResponse response = provider.analyze("resume text", "job description");

        assertEquals(85.0, response.getKeywordScore());
        mockServer.verify();
    }

    @Test
    void openAiProviderUsesItsOwnEndpointAndCredential() throws Exception {
        OpenAiResumeAnalysisProvider provider = new OpenAiResumeAnalysisProvider(
                restClientBuilder, jsonMapper, "test-openai-key", "https://openai.test/v1", "openai-model");
        mockOpenAiCompatibleResponse("https://openai.test/v1/chat/completions", "test-openai-key", "openai-model");

        ScoreResponse response = provider.analyze("resume text", "job description");

        assertEquals(85.0, response.getKeywordScore());
        mockServer.verify();
    }

    @Test
    void ollamaProviderUsesNativeChatApiWithoutAnApiKey() throws Exception {
        OllamaResumeAnalysisProvider provider = new OllamaResumeAnalysisProvider(
                restClientBuilder, jsonMapper, "http://ollama.test:11434/", "local-model");
        mockServer.expect(requestTo("http://ollama.test:11434/api/chat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"format\":\"json\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"model\":\"local-model\"")))
                .andRespond(withSuccess(ollamaResponse(), MediaType.APPLICATION_JSON));

        ScoreResponse response = provider.analyze("resume text", "job description");

        assertEquals(85.0, response.getKeywordScore());
        mockServer.verify();
    }

    @Test
    void rejectsIncompleteAiScoreInsteadOfAcceptingMissingScoresAsZero() throws Exception {
        GroqResumeAnalysisProvider provider = new GroqResumeAnalysisProvider(
                restClientBuilder, jsonMapper, "test-groq-key", "https://groq.test/openai/v1", "groq-model");
        mockOpenAiCompatibleResponse("https://groq.test/openai/v1/chat/completions", "test-groq-key",
                Map.of("sectionScore", 75, "experienceScore", 65, "formatScore", 95,
                        "matchedKeywords", List.of(), "missingKeywords", List.of(), "suggestions", List.of(),
                        "grade", "B", "analysisSummary", "A complete-looking response."));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> provider.analyze("resume text", "job description"));

        assertTrue(exception.getMessage().contains("keywordScore"));
        mockServer.verify();
    }

    @Test
    void rejectsAiScoresOutsideTheExpectedRange() throws Exception {
        GroqResumeAnalysisProvider provider = new GroqResumeAnalysisProvider(
                restClientBuilder, jsonMapper, "test-groq-key", "https://groq.test/openai/v1", "groq-model");
        mockOpenAiCompatibleResponse("https://groq.test/openai/v1/chat/completions", "test-groq-key",
                Map.of("keywordScore", 101, "sectionScore", 75, "experienceScore", 65, "formatScore", 95,
                        "matchedKeywords", List.of(), "missingKeywords", List.of(), "suggestions", List.of(),
                        "grade", "B", "analysisSummary", "An invalid score."));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> provider.analyze("resume text", "job description"));

        assertTrue(exception.getMessage().contains("keywordScore"));
        mockServer.verify();
    }

    @Test
    void rejectsMalformedKeywordLists() throws Exception {
        GroqResumeAnalysisProvider provider = new GroqResumeAnalysisProvider(
                restClientBuilder, jsonMapper, "test-groq-key", "https://groq.test/openai/v1", "groq-model");
        mockOpenAiCompatibleResponse("https://groq.test/openai/v1/chat/completions", "test-groq-key",
                Map.of("keywordScore", 85, "sectionScore", 75, "experienceScore", 65, "formatScore", 95,
                        "matchedKeywords", "java", "missingKeywords", List.of(), "suggestions", List.of(),
                        "grade", "B", "analysisSummary", "A malformed response."));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> provider.analyze("resume text", "job description"));

        assertTrue(exception.getMessage().contains("matchedKeywords"));
        mockServer.verify();
    }

    @Test
    void rejectsMissingAnalysisSummary() throws Exception {
        GroqResumeAnalysisProvider provider = new GroqResumeAnalysisProvider(
                restClientBuilder, jsonMapper, "test-groq-key", "https://groq.test/openai/v1", "groq-model");
        mockOpenAiCompatibleResponse("https://groq.test/openai/v1/chat/completions", "test-groq-key",
                Map.of("keywordScore", 85, "sectionScore", 75, "experienceScore", 65, "formatScore", 95,
                        "matchedKeywords", List.of(), "missingKeywords", List.of(), "suggestions", List.of(),
                        "grade", "B"));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> provider.analyze("resume text", "job description"));

        assertTrue(exception.getMessage().contains("analysisSummary"));
        mockServer.verify();
    }

    private void mockOpenAiCompatibleResponse(String endpoint, String apiKey, String model) {
        mockServer.expect(requestTo(endpoint))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"model\":\"" + model + "\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"response_format\":{")))
                .andRespond(withSuccess(openAiCompatibleResponse(), MediaType.APPLICATION_JSON));
    }

    private void mockOpenAiCompatibleResponse(String endpoint, String apiKey, Map<String, Object> scoreFields) {
        mockServer.expect(requestTo(endpoint))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"response_format\":{")))
                .andRespond(withSuccess(openAiCompatibleResponse(scoreFields), MediaType.APPLICATION_JSON));
    }

    private String openAiCompatibleResponse() {
        return openAiCompatibleResponse(scoreFields());
    }

    private String openAiCompatibleResponse(Map<String, Object> scoreFields) {
        String content = jsonMapper.writeValueAsString(scoreFields);
        return jsonMapper.writeValueAsString(Map.of("choices", List.of(Map.of(
                "message", Map.of("content", content)))));
    }

    private String ollamaResponse() {
        return jsonMapper.writeValueAsString(Map.of("message", Map.of("content",
                jsonMapper.writeValueAsString(scoreFields()))));
    }

    private Map<String, Object> scoreFields() {
        return Map.of(
                "keywordScore", 85,
                "sectionScore", 75,
                "experienceScore", 65,
                "formatScore", 95,
                "matchedKeywords", List.of("java"),
                "missingKeywords", List.of("kubernetes"),
                "suggestions", List.of("Add relevant project details."),
                "grade", "B",
                "analysisSummary", "The resume contains relevant experience.");
    }
}