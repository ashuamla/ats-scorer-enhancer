package com.ats.scorer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

public class GroqResumeAnalysisProvider extends OpenAiCompatibleResumeAnalysisProvider {

    public GroqResumeAnalysisProvider(RestClient.Builder restClientBuilder,
                                      JsonMapper jsonMapper,
                                      @Value("${ats.ai.groq.api-key:}") String apiKey,
                                      @Value("${ats.ai.groq.base-url}") String baseUrl,
                                      @Value("${ats.ai.groq.model}") String model) {
        super(restClientBuilder, jsonMapper, apiKey, baseUrl, model, "GROQ_API_KEY");
    }
}