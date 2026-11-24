package com.ats.scorer.controller;

import com.ats.scorer.dto.ScoreRequest;
import com.ats.scorer.dto.ScoreResponse;
import com.ats.scorer.dto.TailoredResumeResponse;
import com.ats.scorer.service.AtsScoringService;
import com.ats.scorer.service.ResumeParserService;
import com.ats.scorer.service.ResumeTailoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ScoringController {

    private final AtsScoringService atsScoringService;
    private final ResumeParserService resumeParserService;
    private final ResumeTailoringService resumeTailoringService;

    public ScoringController(AtsScoringService atsScoringService, ResumeParserService resumeParserService, ResumeTailoringService resumeTailoringService) {
        this.atsScoringService = atsScoringService;
        this.resumeParserService = resumeParserService;
        this.resumeTailoringService = resumeTailoringService;
    }

    @PostMapping("/score")
    public ResponseEntity<?> scoreResume(
            @RequestParam("resume") MultipartFile resume,
            @RequestParam("jobDescription") String jobDescription) {

        // Validate inputs
        if (resume == null || resume.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Please upload a resume file",
                    "supportedFormats", "PDF, DOCX, TXT"
            ));
        }

        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Please provide a job description"
            ));
        }

        try {
            // Extract text from resume
            String resumeText = resumeParserService.extractText(resume);

            if (resumeText == null || resumeText.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Could not extract text from the uploaded resume. Please try a different file."
                ));
            }

            // Score the resume against the job description
            ScoreResponse scoreResponse = atsScoringService.score(resumeText, jobDescription);

            return ResponseEntity.ok(scoreResponse);

        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to process the uploaded file: " + e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "An unexpected error occurred: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/tailor")
    public ResponseEntity<?> tailorResume(
            @RequestParam("resume") MultipartFile resume,
            @RequestParam("jobDescription") String jobDescription) {

        // Validate inputs
        if (resume == null || resume.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Please upload a resume file",
                    "supportedFormats", "PDF, DOCX, TXT"
            ));
        }

        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Please provide a job description"
            ));
        }

        try {
            // Extract text from resume
            String resumeText = resumeParserService.extractText(resume);

            if (resumeText == null || resumeText.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Could not extract text from the uploaded resume. Please try a different file."
                ));
            }

            // Tailor the resume based on the job description
            TailoredResumeResponse tailoredResponse = resumeTailoringService.tailorResume(resumeText, jobDescription);

            return ResponseEntity.ok(tailoredResponse);

        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to process the uploaded file: " + e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "An unexpected error occurred: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "ATS Scorer API"
        ));
    }
}
