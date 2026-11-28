package com.ats.scorer.config;

import com.ats.scorer.service.GroqResumeAnalysisProvider;
import com.ats.scorer.service.OllamaResumeAnalysisProvider;
import com.ats.scorer.service.OpenAiResumeAnalysisProvider;
import com.ats.scorer.service.ResumeAnalysisProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Locale;

@Configuration(proxyBeanMethods = false)
public class ResumeAnalysisProviderConfiguration {

    @Bean
    public RestClient.Builder resumeAnalysisRestClientBuilder(
            @Value("${ats.ai.http.connect-timeout:5s}") String connectTimeoutValue,
            @Value("${ats.ai.http.read-timeout:60s}") String readTimeoutValue) {
        Duration connectTimeout = DurationStyle.detectAndParse(connectTimeoutValue);
        Duration readTimeout = DurationStyle.detectAndParse(readTimeoutValue);
        if (connectTimeout.isZero() || connectTimeout.isNegative()
                || readTimeout.isZero() || readTimeout.isNegative()) {
            throw new IllegalArgumentException("AI HTTP connect and read timeouts must be positive durations");
        }

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder().requestFactory(requestFactory);
    }

    @Bean
    @ConditionalOnProperty(name = "ats.ai.enabled", havingValue = "true")
    public ResumeAnalysisProvider resumeAnalysisProvider(
            RestClient.Builder restClientBuilder,
            JsonMapper jsonMapper,
            @Value("${ats.ai.provider:groq}") String provider,
            @Value("${ats.ai.groq.api-key:}") String groqApiKey,
            @Value("${ats.ai.groq.base-url:https://api.groq.com/openai/v1}") String groqBaseUrl,
            @Value("${ats.ai.groq.model:openai/gpt-oss-20b}") String groqModel,
            @Value("${ats.ai.openai.api-key:}") String openAiApiKey,
            @Value("${ats.ai.openai.base-url:https://api.openai.com/v1}") String openAiBaseUrl,
            @Value("${ats.ai.openai.model:gpt-4o-mini}") String openAiModel,
            @Value("${ats.ai.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl,
            @Value("${ats.ai.ollama.model:llama3.2}") String ollamaModel) {
        return switch (provider.trim().toLowerCase(Locale.ROOT)) {
            case "groq" -> new GroqResumeAnalysisProvider(restClientBuilder, jsonMapper,
                    groqApiKey, groqBaseUrl, groqModel);
            case "openai" -> new OpenAiResumeAnalysisProvider(restClientBuilder, jsonMapper,
                    openAiApiKey, openAiBaseUrl, openAiModel);
            case "ollama" -> new OllamaResumeAnalysisProvider(restClientBuilder, jsonMapper,
                    ollamaBaseUrl, ollamaModel);
            default -> throw new IllegalArgumentException(
                    "Unsupported ATS_AI_PROVIDER '%s'. Choose groq, openai, or ollama.".formatted(provider));
        };
    }
}