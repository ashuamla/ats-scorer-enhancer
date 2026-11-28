package com.ats.scorer.service;

import com.ats.scorer.config.ResumeAnalysisProviderConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroqProviderConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ResumeAnalysisProviderConfiguration.class)
            .withBean(JsonMapper.class, () -> JsonMapper.builder().build());

    @Test
    void selectsGroqProvider() {
        runWithProvider("groq", "ats.ai.groq.api-key=test-groq-key", GroqResumeAnalysisProvider.class);
    }

    @Test
    void selectsOpenAiProvider() {
        runWithProvider("openai", "ats.ai.openai.api-key=test-openai-key", OpenAiResumeAnalysisProvider.class);
    }

    @Test
    void selectsOllamaProviderWithoutApiKey() {
        runWithProvider("ollama", null, OllamaResumeAnalysisProvider.class);
    }

    @Test
    void omitsAiProviderWhenDisabled() {
        contextRunner.withPropertyValues("ats.ai.enabled=false").run(context -> {
            assertNull(context.getBeanProvider(ResumeAnalysisProvider.class).getIfAvailable());
        });
    }

    @Test
    void rejectsUnsupportedProvider() {
        contextRunner.withPropertyValues("ats.ai.enabled=true", "ats.ai.provider=unknown")
                .run(context -> {
                    assertNotNull(context.getStartupFailure());
                    assertTrue(context.getStartupFailure().getMessage().contains("Choose groq, openai, or ollama"));
                });
    }

    private void runWithProvider(String provider, String credentialProperty, Class<?> expectedType) {
        var runner = contextRunner.withPropertyValues("ats.ai.enabled=true", "ats.ai.provider=" + provider);
        if (credentialProperty != null) {
            runner = runner.withPropertyValues(credentialProperty);
        }
        runner.run(context -> {
            assertNull(context.getStartupFailure());
            assertInstanceOf(expectedType, context.getBean(ResumeAnalysisProvider.class));
        });
    }
}