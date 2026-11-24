package com.ats.scorer.service;

import com.ats.scorer.dto.ScoreResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AtsScoringService {

    @Value("${ats.scoring.keyword-weight:0.4}")
    private double keywordWeight;

    @Value("${ats.scoring.section-weight:0.2}")
    private double sectionWeight;

    @Value("${ats.scoring.experience-weight:0.2}")
    private double experienceWeight;

    @Value("${ats.scoring.format-weight:0.2}")
    private double formatWeight;

    // Common stop words to filter out during keyword extraction
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "is", "are", "was", "were", "be", "been",
            "being", "have", "has", "had", "do", "does", "did", "will", "would",
            "could", "should", "may", "might", "shall", "can", "need", "dare",
            "ought", "used", "it", "its", "this", "that", "these", "those",
            "i", "you", "he", "she", "we", "they", "what", "which", "who",
            "whom", "whose", "where", "when", "why", "how", "all", "each",
            "every", "both", "few", "more", "most", "other", "some", "such",
            "no", "nor", "not", "only", "own", "same", "so", "than", "too",
            "very", "just", "because", "as", "until", "while", "about", "between",
            "through", "during", "before", "after", "above", "below", "up", "down",
            "out", "off", "over", "under", "again", "further", "then", "once",
            "here", "there", "any", "also", "new", "if", "into", "much", "well",
            "back", "even", "still", "get", "got", "like", "make", "made",
            "take", "go", "come", "see", "know", "think", "say", "said"
    );

    // Common tech keywords and skills to look for
    private static final Set<String> TECH_KEYWORDS = Set.of(
            "java", "python", "javascript", "typescript", "c++", "c#", "go", "golang",
            "rust", "kotlin", "swift", "ruby", "php", "scala", "spring", "spring boot",
            "react", "angular", "vue", "node.js", "nodejs", "django", "flask",
            "express", "html", "css", "sql", "nosql", "mongodb", "postgresql",
            "mysql", "oracle", "redis", "elasticsearch", "docker", "kubernetes",
            "aws", "azure", "gcp", "terraform", "ansible", "jenkins", "ci/cd",
            "git", "agile", "scrum", "rest", "graphql", "microservices", "api",
            "machine learning", "deep learning", "ai", "nlp", "data science",
            "kafka", "rabbitmq", "elk", "kibana", "prometheus", "grafana",
            "linux", "unix", "bash", "shell", "maven", "gradle", "webpack",
            "jira", "confluence", "aws lambda", "serverless", "saas", "paas",
            "iaas", "devops", "sre", "cloud", "microservice",
            "design patterns", "clean code", "tdd", "bdd", "unit testing",
            "integration testing", "junit", "selenium", "cucumber",
            "intellij", "eclipse", "vscode", "vim", "emacs"
    );

    public ScoreResponse score(String resumeText, String jobDescription) {
        ScoreResponse response = new ScoreResponse();

        // Normalize texts
        String resumeLower = resumeText.toLowerCase().trim();
        String jdLower = jobDescription.toLowerCase().trim();

        // Extract keywords from job description
        List<String> jdKeywords = extractKeywords(jdLower);

        // Score each dimension
        double keywordScore = calculateKeywordScore(resumeLower, jdKeywords);
        double sectionScore = calculateSectionScore(resumeLower);
        double experienceScore = calculateExperienceScore(resumeLower, jdLower);
        double formatScore = calculateFormatScore(resumeText);

        // Calculate weighted overall score
        double overallScore = (keywordScore * keywordWeight) +
                (sectionScore * sectionWeight) +
                (experienceScore * experienceWeight) +
                (formatScore * formatWeight);

        // Round to 2 decimal places
        overallScore = Math.round(overallScore * 100.0) / 100.0;

        // Determine matched and missing keywords
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String keyword : jdKeywords) {
            if (resumeLower.contains(keyword.toLowerCase())) {
                matched.add(keyword);
            } else {
                missing.add(keyword);
            }
        }

        // Generate suggestions
        List<String> suggestions = generateSuggestions(missing, resumeLower, sectionScore, formatScore);

        // Determine grade
        String grade = calculateGrade(overallScore);

        // Populate response
        response.setOverallScore(overallScore);
        response.setKeywordScore(Math.round(keywordScore * 100.0) / 100.0);
        response.setSectionScore(Math.round(sectionScore * 100.0) / 100.0);
        response.setExperienceScore(Math.round(experienceScore * 100.0) / 100.0);
        response.setFormatScore(Math.round(formatScore * 100.0) / 100.0);
        response.setMatchedKeywords(matched);
        response.setMissingKeywords(missing);
        response.setSuggestions(suggestions);
        response.setGrade(grade);

        return response;
    }

    private List<String> extractKeywords(String text) {
        Set<String> keywords = new LinkedHashSet<>();

        // Extract multi-word tech keywords first
        for (String tech : TECH_KEYWORDS) {
            if (text.contains(tech)) {
                keywords.add(tech);
            }
        }

        // Extract significant single words (length > 3, not stop words)
        String[] words = text.split("[\\s,;:()\\-]+");
        for (String word : words) {
            String cleaned = word.replaceAll("[^a-z0-9]", "").trim();
            if (cleaned.length() > 3 && !STOP_WORDS.contains(cleaned)) {
                // Only add if it appears at least twice or is a known tech keyword
                long count = text.split("\\b" + cleaned + "\\b", -1).length - 1;
                if (count >= 2 || TECH_KEYWORDS.contains(cleaned)) {
                    keywords.add(cleaned);
                }
            }
        }

        return new ArrayList<>(keywords);
    }

    private double calculateKeywordScore(String resume, List<String> jdKeywords) {
        if (jdKeywords.isEmpty()) {
            return 100.0;
        }

        long matched = jdKeywords.stream()
                .filter(keyword -> resume.contains(keyword.toLowerCase()))
                .count();

        return (double) matched / jdKeywords.size() * 100.0;
    }

    private double calculateSectionScore(String resume) {
        double score = 0;
        double sectionPoints = 20.0; // Each section is worth 20 points

        String[] sections = {
                "summary", "objective", "profile", "about me",
                "experience", "work experience", "employment", "professional experience",
                "education", "academic",
                "skills", "technical skills", "core skills",
                "certification", "certifications", "licenses",
                "project", "projects", "portfolio",
                "achievement", "achievements", "awards",
                "reference", "references", "contact"
        };

        int foundSections = 0;
        for (String section : sections) {
            if (resume.contains(section)) {
                foundSections++;
                break;
            }
        }

        // Check for key sections individually
        boolean hasSummary = resume.contains("summary") || resume.contains("objective") || resume.contains("profile");
        boolean hasExperience = resume.contains("experience") || resume.contains("employment");
        boolean hasEducation = resume.contains("education") || resume.contains("academic") ||
                resume.contains("degree") || resume.contains("university") || resume.contains("college");
        boolean hasSkills = resume.contains("skill") || resume.contains("technical");
        boolean hasContact = resume.contains("@") || resume.contains("phone") ||
                resume.contains("email") || resume.contains("contact");

        int sectionCount = 0;
        if (hasSummary) sectionCount++;
        if (hasExperience) sectionCount++;
        if (hasEducation) sectionCount++;
        if (hasSkills) sectionCount++;
        if (hasContact) sectionCount++;

        score = (double) sectionCount / 5.0 * 100.0;
        return score;
    }

    private double calculateExperienceScore(String resume, String jd) {
        double score = 0;

        // Check for years of experience patterns
        Pattern yearsPattern = Pattern.compile("(\\d+)\\s*(?:years?|yrs?)\\s*(?:of\\s*)?(?:experience|exp)");
        Matcher matcher = yearsPattern.matcher(resume);

        if (matcher.find()) {
            int years = Integer.parseInt(matcher.group(1));
            score = Math.min(years * 10.0, 100.0);
        }

        // Check for job titles and roles
        String[] roleIndicators = {"manager", "lead", "senior", "architect", "engineer",
                "developer", "analyst", "consultant", "director", "vp", "cto", "ceo"};

        int roleMatches = 0;
        for (String role : roleIndicators) {
            if (resume.contains(role)) {
                roleMatches++;
            }
        }

        // Boost score if relevant roles are mentioned
        if (roleMatches > 0) {
            score = Math.min(score + (roleMatches * 5.0), 100.0);
        }

        // Check if JD mentions specific experience requirements
        Pattern jdYearsPattern = Pattern.compile("(\\d+)\\s*(?:years?|yrs?)\\s*(?:of\\s*)?(?:experience|exp)");
        Matcher jdMatcher = jdYearsPattern.matcher(jd);
        if (jdMatcher.find()) {
            int requiredYears = Integer.parseInt(jdMatcher.group(1));
            if (matcher.find()) {
                int candidateYears = Integer.parseInt(matcher.group(1));
                if (candidateYears >= requiredYears) {
                    score = Math.min(score + 10.0, 100.0);
                }
            }
        }

        return Math.min(score, 100.0);
    }

    private double calculateFormatScore(String resume) {
        double score = 50.0; // Start with base score

        // Check for proper structure
        if (resume.contains("\n")) {
            score += 10.0; // Has line breaks
        }

        // Check for bullet points
        if (resume.contains("•") || resume.contains("-") || resume.contains("*")) {
            score += 10.0;
        }

        // Check for email format
        Pattern emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        if (emailPattern.matcher(resume).find()) {
            score += 10.0;
        }

        // Check for phone number format
        Pattern phonePattern = Pattern.compile("\\d{3}[-.]?\\d{3}[-.]?\\d{4}|\\+?\\d{1,3}[-.]?\\d{6,14}");
        if (phonePattern.matcher(resume).find()) {
            score += 10.0;
        }

        // Penalize for very short resumes
        if (resume.trim().length() < 200) {
            score -= 20.0;
        }

        // Penalize for very long resumes (likely poorly formatted)
        if (resume.trim().length() > 5000) {
            score -= 10.0;
        }

        return Math.max(0, Math.min(score, 100.0));
    }

    private List<String> generateSuggestions(List<String> missingKeywords, String resume,
                                              double sectionScore, double formatScore) {
        List<String> suggestions = new ArrayList<>();

        if (!missingKeywords.isEmpty()) {
            suggestions.add("Consider adding these missing keywords to your resume: " +
                    String.join(", ", missingKeywords.subList(0, Math.min(5, missingKeywords.size()))));
        }

        if (sectionScore < 60) {
            suggestions.add("Add clear section headings like 'Experience', 'Education', and 'Skills' to improve parseability.");
        }

        if (formatScore < 60) {
            suggestions.add("Improve formatting: include contact information, use bullet points, and ensure proper structure.");
        }

        if (!resume.contains("@")) {
            suggestions.add("Include your email address for better ATS parsing.");
        }

        if (resume.trim().length() < 500) {
            suggestions.add("Your resume seems short. Consider adding more details about your experience and achievements.");
        }

        return suggestions;
    }

    private String calculateGrade(double score) {
        if (score >= 90) return "A+";
        if (score >= 80) return "A";
        if (score >= 70) return "B";
        if (score >= 60) return "C";
        if (score >= 50) return "D";
        return "F";
    }
}
