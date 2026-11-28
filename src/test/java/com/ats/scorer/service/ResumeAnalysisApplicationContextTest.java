package com.ats.scorer.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "ats.ai.enabled=true",
        "ats.ai.provider=groq",
        "ats.ai.groq.api-key=test-groq-api-key"
})
class ResumeAnalysisApplicationContextTest {

    @Autowired
    private ResumeAnalysisProvider resumeAnalysisProvider;

    @Test
    void startsApplicationWithConfiguredProviderWithoutCallingExternalApi() {
        assertInstanceOf(GroqResumeAnalysisProvider.class, resumeAnalysisProvider);
        assertEquals("openai/gpt-oss-20b", ((AbstractResumeAnalysisProvider) resumeAnalysisProvider).model);
    }
}