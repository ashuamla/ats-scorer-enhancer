package com.ats.scorer.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
public class ResumeParserService {

    public String extractText(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("No file uploaded");
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IOException("Unknown file name");
        }

        String extension = getFileExtension(filename).toLowerCase();

        try (InputStream inputStream = file.getInputStream()) {
            return switch (extension) {
                case "pdf" -> extractFromPdf(inputStream);
                case "docx" -> extractFromDocx(inputStream);
                case "txt" -> new String(inputStream.readAllBytes());
                default -> throw new IOException("Unsupported file type: " + extension + ". Please upload PDF, DOCX, or TXT files.");
            };
        }
    }

    private String extractFromPdf(InputStream inputStream) throws IOException {
        try {
            org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.pdmodel.PDDocument.load(inputStream);
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            String text = stripper.getText(document);
            document.close();
            return text;
        } catch (Exception e) {
            throw new IOException("Failed to parse PDF file: " + e.getMessage(), e);
        }
    }

    private String extractFromDocx(InputStream inputStream) throws IOException {
        try {
            org.apache.poi.xwpf.usermodel.XWPFDocument document = new org.apache.poi.xwpf.usermodel.XWPFDocument(inputStream);
            StringBuilder text = new StringBuilder();
            for (org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText());
                text.append("\n");
            }
            document.close();
            return text.toString();
        } catch (Exception e) {
            throw new IOException("Failed to parse DOCX file: " + e.getMessage(), e);
        }
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot >= 0) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }
}
