package com.ats.scorer.service;

import com.ats.scorer.dto.TailoredResumeResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ResumeTailoringService {

    // Section headers and their aliases for parsing
    private static final Map<String, String[]> SECTION_ALIASES = new LinkedHashMap<>();

    static {
        SECTION_ALIASES.put("contact", new String[]{"contact", "contact information", "personal details"});
        SECTION_ALIASES.put("summary", new String[]{"summary", "objective", "profile", "about me", "professional summary", "career objective"});
        SECTION_ALIASES.put("experience", new String[]{"experience", "work experience", "employment", "professional experience", "work history", "employment history"});
        SECTION_ALIASES.put("education", new String[]{"education", "academic", "academic background", "qualifications"});
        SECTION_ALIASES.put("skills", new String[]{"skills", "technical skills", "core skills", "skill set", "competencies", "technical competencies"});
        SECTION_ALIASES.put("projects", new String[]{"project", "projects", "portfolio", "key projects"});
        SECTION_ALIASES.put("certifications", new String[]{"certification", "certifications", "licenses", "certificates", "professional certifications"});
        SECTION_ALIASES.put("achievements", new String[]{"achievement", "achievements", "awards", "honors", "recognitions"});
        SECTION_ALIASES.put("languages", new String[]{"language", "languages", "language proficiency"});
        SECTION_ALIASES.put("volunteer", new String[]{"volunteer", "volunteering", "community service", "voluntary work"});
        SECTION_ALIASES.put("interests", new String[]{"interest", "interests", "hobbies", "personal interests"});
        SECTION_ALIASES.put("references", new String[]{"reference", "references", "referees"});
    }

    // Keywords that indicate section importance for different job types
    private static final Map<String, Set<String>> SECTION_RELEVANCE_KEYWORDS = new HashMap<>();

    static {
        // Technical roles value skills and projects highly
        SECTION_RELEVANCE_KEYWORDS.put("skills", Set.of(
                "skill", "technology", "tool", "framework", "library", "platform",
                "proficient", "expertise", "experience with", "hands-on", "technical"
        ));
        SECTION_RELEVANCE_KEYWORDS.put("experience", Set.of(
                "experience", "year", "worked", "role", "position", "responsibility",
                "managed", "led", "developed", "designed", "implemented"
        ));
        SECTION_RELEVANCE_KEYWORDS.put("projects", Set.of(
                "project", "built", "developed", "created", "delivered", "shipped",
                "architecture", "system", "application", "product"
        ));
        SECTION_RELEVANCE_KEYWORDS.put("education", Set.of(
                "degree", "education", "university", "college", "bachelor", "master",
                "phd", "diploma", "gpa", "academic"
        ));
        SECTION_RELEVANCE_KEYWORDS.put("certifications", Set.of(
                "certif", "license", "aws", "azure", "gcp", "pmp", "cisa", "cism",
                "itil", "scrum", "kubernetes", "docker"
        ));
        SECTION_RELEVANCE_KEYWORDS.put("achievements", Set.of(
                "award", "achievement", "recognized", "patent", "publication",
                "speaker", "contributor", "open source"
        ));
        SECTION_RELEVANCE_KEYWORDS.put("summary", Set.of(
                "summary", "objective", "profile", "about", "professional",
                "career", "goal"
        ));
        SECTION_RELEVANCE_KEYWORDS.put("contact", Set.of(
                "contact", "email", "phone", "address", "linkedin", "github",
                "portfolio", "website"
        ));
    }

    /**
     * Tailors a resume based on a job description.
     * Parses the resume into sections, analyzes which sections are most relevant
     * to the job description, and generates a tailored version.
     */
    public TailoredResumeResponse tailorResume(String resumeText, String jobDescription) {
        // Parse resume into sections
        Map<String, String> sections = parseResumeIntoSections(resumeText);

        // Extract keywords from job description
        List<String> jdKeywords = extractJdKeywords(jobDescription.toLowerCase());

        // Score each section's relevance to the job description
        Map<String, Double> sectionRelevanceScores = new LinkedHashMap<>();
        for (String sectionName : sections.keySet()) {
            double score = calculateSectionRelevance(sectionName, sections.get(sectionName), jobDescription.toLowerCase(), jdKeywords);
            sectionRelevanceScores.put(sectionName, score);
        }

        // Sort sections by relevance score (descending)
        List<Map.Entry<String, Double>> sortedSections = new ArrayList<>(sectionRelevanceScores.entrySet());
        sortedSections.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Determine which sections to include (threshold: at least some relevance)
        double relevanceThreshold = 10.0; // Minimum relevance score to include a section

        List<String> includedSections = new ArrayList<>();
        List<String> excludedSections = new ArrayList<>();

        for (Map.Entry<String, Double> entry : sortedSections) {
            if (entry.getValue() >= relevanceThreshold) {
                includedSections.add(entry.getKey());
            } else {
                excludedSections.add(entry.getKey());
            }
        }

        // Always include contact section if present
        if (sections.containsKey("contact") && !includedSections.contains("contact")) {
            includedSections.add(0, "contact");
            excludedSections.remove("contact");
        }

        // Find highlighted keywords (JD keywords that appear in the resume)
        List<String> highlightedKeywords = findHighlightedKeywords(resumeText.toLowerCase(), jdKeywords);

        // Build the tailored resume text
        String tailoredText = buildTailoredResume(includedSections, sections, highlightedKeywords, jobDescription);

        // Calculate overall relevance score
        double relevanceScore = calculateOverallRelevance(includedSections, sections, jdKeywords);

        // Build response
        TailoredResumeResponse response = new TailoredResumeResponse();
        response.setTailoredResumeText(tailoredText);
        response.setIncludedSections(includedSections);
        response.setExcludedSections(excludedSections);
        response.setHighlightedKeywords(highlightedKeywords);
        response.setRelevanceScore(Math.round(relevanceScore * 100.0) / 100.0);

        return response;
    }

    /**
     * Parses resume text into structured sections based on section headers.
     */
    private Map<String, String> parseResumeIntoSections(String resumeText) {
        Map<String, String> sections = new LinkedHashMap<>();
        String[] lines = resumeText.split("\n");

        List<String> currentSectionLines = new ArrayList<>();
        String currentSection = "contact"; // Default first section is contact info

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // Check if this line is a section header
            String detectedSection = detectSectionHeader(line);

            if (detectedSection != null) {
                // Save previous section
                if (!currentSectionLines.isEmpty()) {
                    String content = String.join("\n", currentSectionLines).trim();
                    if (!content.isEmpty()) {
                        sections.put(currentSection, content);
                    }
                }
                currentSection = detectedSection;
                currentSectionLines = new ArrayList<>();
                currentSectionLines.add(lines[i].trim()); // Include the header line
            } else {
                if (!line.isEmpty()) {
                    currentSectionLines.add(lines[i]);
                }
            }
        }

        // Save the last section
        if (!currentSectionLines.isEmpty()) {
            String content = String.join("\n", currentSectionLines).trim();
            if (!content.isEmpty()) {
                sections.put(currentSection, content);
            }
        }

        return sections;
    }

    /**
     * Detects if a line is a section header and returns the section name.
     */
    private String detectSectionHeader(String line) {
        String lowerLine = line.toLowerCase().trim();

        // Check for common section header patterns
        for (Map.Entry<String, String[]> entry : SECTION_ALIASES.entrySet()) {
            for (String alias : entry.getValue()) {
                if (lowerLine.equals(alias) ||
                    lowerLine.equals(alias + ":") ||
                    lowerLine.equals(alias + " :") ||
                    lowerLine.startsWith("=== " + alias) ||
                    lowerLine.startsWith("--- " + alias) ||
                    lowerLine.startsWith("__" + alias)) {
                    return entry.getKey();
                }
            }
        }

        return null;
    }

    /**
     * Calculates how relevant a section is to the job description.
     */
    private double calculateSectionRelevance(String sectionName, String sectionContent,
                                             String jdLower, List<String> jdKeywords) {
        double score = 0.0;

        // Check if JD mentions keywords relevant to this section type
        Set<String> relevanceKeywords = SECTION_RELEVANCE_KEYWORDS.get(sectionName);
        if (relevanceKeywords != null) {
            for (String keyword : relevanceKeywords) {
                if (jdLower.contains(keyword)) {
                    score += 15.0;
                }
            }
        }

        // Check how many JD keywords appear in this section
        String sectionLower = sectionContent.toLowerCase();
        int matchedKeywords = 0;
        for (String jdKeyword : jdKeywords) {
            if (sectionLower.contains(jdKeyword.toLowerCase())) {
                matchedKeywords++;
                score += 10.0;
            }
        }

        // Bonus for contact section - always relevant
        if ("contact".equals(sectionName)) {
            score = Math.max(score, 50.0);
        }

        // Bonus for summary section - generally useful
        if ("summary".equals(sectionName)) {
            score = Math.max(score, 20.0);
        }

        return Math.min(score, 100.0);
    }

    /**
     * Extracts significant keywords from job description.
     */
    private List<String> extractJdKeywords(String jdLower) {
        Set<String> keywords = new LinkedHashSet<>();

        // Common stop words to filter
        Set<String> stopWords = Set.of(
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
                "take", "go", "come", "see", "know", "think", "say", "said",
                "our", "us", "looking", "seeking", "require", "required",
                "position", "role", "join", "team", "company"
        );

        // Extract words
        String[] words = jdLower.split("[\\s,;:()\\-]+");
        for (String word : words) {
            String cleaned = word.replaceAll("[^a-z0-9]", "").trim();
            if (cleaned.length() > 3 && !stopWords.contains(cleaned)) {
                keywords.add(cleaned);
            }
        }

        return new ArrayList<>(keywords);
    }

    /**
     * Finds JD keywords that appear in the resume for highlighting.
     */
    private List<String> findHighlightedKeywords(String resumeLower, List<String> jdKeywords) {
        return jdKeywords.stream()
                .filter(keyword -> resumeLower.contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Builds the tailored resume text from included sections.
     */
    private String buildTailoredResume(List<String> includedSections, Map<String, String> sections,
                                       List<String> highlightedKeywords, String jobDescription) {
        StringBuilder tailoredResume = new StringBuilder();

        // Add a tailored summary at the top
        tailoredResume.append(generateTailoredSummary(highlightedKeywords, jobDescription));
        tailoredResume.append("\n\n");

        // Add sections in order of relevance
        for (String sectionName : includedSections) {
            if ("contact".equals(sectionName)) continue; // Contact already in header

            String content = sections.get(sectionName);
            if (content != null && !content.isEmpty()) {
                String sectionTitle = formatSectionTitle(sectionName);
                tailoredResume.append(sectionTitle).append("\n");
                tailoredResume.append(content).append("\n\n");
            }
        }

        // Add contact info at the end if present
        if (sections.containsKey("contact")) {
            String contactContent = sections.get("contact");
            if (contactContent != null && !contactContent.isEmpty()) {
                tailoredResume.append("CONTACT\n");
                tailoredResume.append(contactContent).append("\n");
            }
        }

        return tailoredResume.toString().trim();
    }

    /**
     * Generates a tailored professional summary based on matched keywords.
     */
    private String generateTailoredSummary(List<String> highlightedKeywords, String jobDescription) {
        StringBuilder summary = new StringBuilder();
        summary.append("PROFESSIONAL SUMMARY\n");

        // Extract key requirements from JD
        List<String> keyRequirements = new ArrayList<>();
        String jdLower = jobDescription.toLowerCase();

        // Look for required skills/technologies
        if (!highlightedKeywords.isEmpty()) {
            // Take top matching keywords for the summary
            List<String> topKeywords = highlightedKeywords.stream()
                    .limit(10)
                    .collect(Collectors.toList());

            summary.append("Professional with expertise in ");
            summary.append(String.join(", ", topKeywords));
            summary.append(".");
        }

        summary.append("\n\nKey matched skills: ");
        summary.append(String.join(", ", highlightedKeywords));

        return summary.toString();
    }

    /**
     * Formats section name as a title.
     */
    private String formatSectionTitle(String sectionName) {
        return sectionName.toUpperCase();
    }

    /**
     * Calculates overall relevance score of the tailored resume.
     */
    private double calculateOverallRelevance(List<String> includedSections, Map<String, String> sections,
                                             List<String> jdKeywords) {
        if (includedSections.isEmpty() || jdKeywords.isEmpty()) {
            return 0.0;
        }

        // Calculate keyword match ratio
        StringBuilder includedContent = new StringBuilder();
        for (String section : includedSections) {
            String content = sections.get(section);
            if (content != null) {
                includedContent.append(content).append("\n");
            }
        }

        String contentLower = includedContent.toString().toLowerCase();
        long matchedKeywords = jdKeywords.stream()
                .filter(keyword -> contentLower.contains(keyword.toLowerCase()))
                .count();

        double keywordMatchRatio = (double) matchedKeywords / jdKeywords.size() * 100.0;

        // Section coverage score
        double sectionCoverage = (double) includedSections.size() / sections.size() * 100.0;

        // Weighted combination
        return (keywordMatchRatio * 0.7) + (sectionCoverage * 0.3);
    }
}
