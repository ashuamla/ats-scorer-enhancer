package com.ats.scorer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

public class OpenAiResumeAnalysisProvider extends OpenAiCompatibleResumeAnalysisProvider {

    public OpenAiResumeAnalysisProvider(RestClient.Builder restClientBuilder,
                                        JsonMapper jsonMapper,
                                        @Value("${ats.ai.openai.api-key:}") String apiKey,
                                        @Value("${ats.ai.openai.base-url}") String baseUrl,
                                        @Value("${ats.ai.openai.model}") String model) {
        super(restClientBuilder, jsonMapper, apiKey, baseUrl, model, "OPENAI_API_KEY");
    }
}