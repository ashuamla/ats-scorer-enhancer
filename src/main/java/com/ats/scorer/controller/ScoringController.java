package com.ats.scorer.controller;

import com.ats.scorer.dto.ScoreResponse;
import com.ats.scorer.service.AtsScoringService;
import com.ats.scorer.service.ResumeParserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ScoringController {

    private static final Logger logger = LoggerFactory.getLogger(ScoringController.class);

    private final AtsScoringService atsScoringService;
    private final ResumeParserService resumeParserService;

    public ScoringController(AtsScoringService atsScoringService, ResumeParserService resumeParserService) {
        this.atsScoringService = atsScoringService;
        this.resumeParserService = resumeParserService;
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
            logger.warn("Resume parsing failed during scoring (fileSizeBytes={})", resume.getSize(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to process the uploaded file. Please verify the file and try again."
            ));
        } catch (Exception e) {
            logger.error("Resume scoring failed (fileSizeBytes={})", resume.getSize(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "An unexpected error occurred. Please try again later."
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
