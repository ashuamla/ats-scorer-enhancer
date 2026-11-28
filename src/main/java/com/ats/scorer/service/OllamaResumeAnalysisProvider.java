package com.ats.scorer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;

public class OllamaResumeAnalysisProvider extends AbstractResumeAnalysisProvider {

    private final String chatEndpoint;

    public OllamaResumeAnalysisProvider(RestClient.Builder restClientBuilder,
                                        JsonMapper jsonMapper,
                                        @Value("${ats.ai.ollama.base-url}") String baseUrl,
                                        @Value("${ats.ai.ollama.model}") String model) {
        super(restClientBuilder, jsonMapper, model);
        this.chatEndpoint = endpoint(baseUrl, "/api/chat");
    }

    @Override
    protected String sendRequest(String systemPrompt, String userPrompt) {
        Map<String, Object> request = new HashMap<>(messages(systemPrompt, userPrompt));
        request.put("model", model);
        request.put("stream", false);
        request.put("format", "json");
        request.put("options", Map.of("temperature", 0.1));
        JsonNode response = postJson(chatEndpoint, request);
        return requiredText(response.path("message").path("content"), "a chat completion");
    }
}