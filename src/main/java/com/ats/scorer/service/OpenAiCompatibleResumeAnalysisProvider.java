package com.ats.scorer.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

abstract class OpenAiCompatibleResumeAnalysisProvider extends AbstractResumeAnalysisProvider {

    private final String completionsEndpoint;

    OpenAiCompatibleResumeAnalysisProvider(RestClient.Builder restClientBuilder,
                                           JsonMapper jsonMapper,
                                           String apiKey,
                                           String baseUrl,
                                           String model,
                                           String apiKeyEnvironmentVariable) {
        super(restClientBuilder, jsonMapper, model);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Set " + apiKeyEnvironmentVariable + " before enabling ATS_AI_ENABLED");
        }
        this.completionsEndpoint = endpoint(baseUrl, "/chat/completions");
        this.apiKey = apiKey;
    }

    private final String apiKey;

    @Override
    protected String sendRequest(String systemPrompt, String userPrompt) {
        Map<String, Object> request = new HashMap<>(messages(systemPrompt, userPrompt));
        request.put("model", model);
        request.put("temperature", 0.1);
        request.put("response_format", Map.of("type", "json_object"));

        JsonNode response = postJson(completionsEndpoint, request,
                Map.of(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey));
        JsonNode content = response.path("choices").path(0).path("message").path("content");
        return requiredText(content, "a chat completion");
    }
}