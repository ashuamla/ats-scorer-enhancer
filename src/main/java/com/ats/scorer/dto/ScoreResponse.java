package com.ats.scorer.dto;

import java.util.List;

public class ScoreResponse {

    private double overallScore;
    private double keywordScore;
    private double sectionScore;
    private double experienceScore;
    private double formatScore;
    private List<String> matchedKeywords;
    private List<String> missingKeywords;
    private List<String> suggestions;
    private String grade;
    private String analysisMethod;
    private String analysisSummary;

    public double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(double overallScore) {
        this.overallScore = overallScore;
    }

    public double getKeywordScore() {
        return keywordScore;
    }

    public void setKeywordScore(double keywordScore) {
        this.keywordScore = keywordScore;
    }

    public double getSectionScore() {
        return sectionScore;
    }

    public void setSectionScore(double sectionScore) {
        this.sectionScore = sectionScore;
    }

    public double getExperienceScore() {
        return experienceScore;
    }

    public void setExperienceScore(double experienceScore) {
        this.experienceScore = experienceScore;
    }

    public double getFormatScore() {
        return formatScore;
    }

    public void setFormatScore(double formatScore) {
        this.formatScore = formatScore;
    }

    public List<String> getMatchedKeywords() {
        return matchedKeywords;
    }

    public void setMatchedKeywords(List<String> matchedKeywords) {
        this.matchedKeywords = matchedKeywords;
    }

    public List<String> getMissingKeywords() {
        return missingKeywords;
    }

    public void setMissingKeywords(List<String> missingKeywords) {
        this.missingKeywords = missingKeywords;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getAnalysisMethod() {
        return analysisMethod;
    }

    public void setAnalysisMethod(String analysisMethod) {
        this.analysisMethod = analysisMethod;
    }

    public String getAnalysisSummary() {
        return analysisSummary;
    }

    public void setAnalysisSummary(String analysisSummary) {
        this.analysisSummary = analysisSummary;
    }
}
