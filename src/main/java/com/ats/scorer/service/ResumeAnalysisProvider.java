package com.ats.scorer.service;

import com.ats.scorer.dto.ScoreResponse;

public interface ResumeAnalysisProvider {

    ScoreResponse analyze(String resumeText, String jobDescription);
}