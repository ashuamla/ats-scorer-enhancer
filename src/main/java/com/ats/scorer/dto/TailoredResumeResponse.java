package com.ats.scorer.dto;

import java.util.List;

public class TailoredResumeResponse {

    private String tailoredResumeText;
    private List<String> includedSections;
    private List<String> excludedSections;
    private List<String> highlightedKeywords;
    private double relevanceScore;

    public String getTailoredResumeText() {
        return tailoredResumeText;
    }

    public void setTailoredResumeText(String tailoredResumeText) {
        this.tailoredResumeText = tailoredResumeText;
    }

    public List<String> getIncludedSections() {
        return includedSections;
    }

    public void setIncludedSections(List<String> includedSections) {
        this.includedSections = includedSections;
    }

    public List<String> getExcludedSections() {
        return excludedSections;
    }

    public void setExcludedSections(List<String> excludedSections) {
        this.excludedSections = excludedSections;
    }

    public List<String> getHighlightedKeywords() {
        return highlightedKeywords;
    }

    public void setHighlightedKeywords(List<String> highlightedKeywords) {
        this.highlightedKeywords = highlightedKeywords;
    }

    public double getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }
}
