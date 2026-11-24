package com.ats.scorer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ResumeParserServiceTest {

    private ResumeParserService service;

    @BeforeEach
    void setUp() {
        service = new ResumeParserService();
    }

    @Nested
    @DisplayName("extractText() - TXT file tests")
    class TxtFileTests {

        @Test
        @DisplayName("Should extract text from valid TXT file")
        void extractText_validTxt_returnsText() throws IOException {
            // Arrange
            String content = "John Doe\nSoftware Engineer\n5 years of experience in Java.";
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "resume.txt", "text/plain", content.getBytes());

            // Act
            String result = service.extractText(file);

            // Assert
            assertNotNull(result);
            assertEquals(content, result);
        }

        @Test
        @DisplayName("Should extract multiline text from TXT file")
        void extractText_multilineTxt() throws IOException {
            // Arrange
            String content = """
                    John Doe
                    Email: john@example.com
                    Phone: 123-456-7890

                    SUMMARY
                    Experienced developer.

                    EXPERIENCE
                    Senior Developer at Tech Corp
                    """;
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "resume.txt", "text/plain", content.getBytes());

            // Act
            String result = service.extractText(file);

            // Assert
            assertNotNull(result);
            assertTrue(result.contains("John Doe"));
            assertTrue(result.contains("EXPERIENCE"));
            assertTrue(result.contains("SUMMARY"));
        }

        @Test
        @DisplayName("Should handle TXT file with special characters")
        void extractText_txtWithSpecialChars() throws IOException {
            // Arrange
            String content = "Skills: Java, C++, Python, #JavaScript, @Docker, $AWS!";
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "resume.txt", "text/plain", content.getBytes());

            // Act
            String result = service.extractText(file);

            // Assert
            assertEquals(content, result);
        }

        @Test
        @DisplayName("Should handle TXT file with Unicode characters")
        void extractText_txtWithUnicode() throws IOException {
            // Arrange
            String content = "João Doe\nDesenvolvedor com experiência em Java.";
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "resume.txt", "text/plain", content.getBytes());

            // Act
            String result = service.extractText(file);

            // Assert
            assertNotNull(result);
            assertTrue(result.contains("João"));
        }

        @Test
        @DisplayName("Should throw IOException for empty TXT file")
        void extractText_emptyTxt_throwsIOException() throws IOException {
            // Arrange
            String content = "";
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "resume.txt", "text/plain", content.getBytes());

            // Act & Assert
            // MockMultipartFile with empty bytes is considered isEmpty()=true
            IOException exception = assertThrows(IOException.class,
                    () -> service.extractText(file));
            assertEquals("No file uploaded", exception.getMessage(),
                    "Should indicate no file uploaded for empty content");
        }

        @Test
        @DisplayName("Should handle TXT file with only whitespace")
        void extractText_whitespaceOnlyTxt() throws IOException {
            // Arrange
            String content = "   \n\n   ";
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "resume.txt", "text/plain", content.getBytes());

            // Act
            String result = service.extractText(file);

            // Assert
            assertNotNull(result);
            assertEquals(content, result);
        }

        @Test
        @DisplayName("Should handle large TXT file")
        void extractText_largeTxt() throws IOException {
            // Arrange
            StringBuilder content = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                content.append("Line ").append(i).append(": Java Spring Boot Docker Kubernetes AWS\n");
            }
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "resume.txt", "text/plain", content.toString().getBytes());

            // Act
            String result = service.extractText(file);

            // Assert
            assertNotNull(result);
            assertTrue(result.contains("Line 0"));
            assertTrue(result.contains("Line 999"));
        }
    }

    @Nested
    @DisplayName("extractText() - PDF file tests")
    class PdfFileTests {

        @Test
        @DisplayName("Should throw IOException for invalid PDF content")
        void extractText_invalidPdf_throwsIOException() throws IOException {
            // Arrange
            String invalidPdf = "This is not a valid PDF file";
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "resume.pdf", "application/pdf", invalidPdf.getBytes());

            // Act & Assert
            IOException exception = assertThrows(IOException.class,
                    () -> service.extractText(file));
            assertTrue(exception.getMessage().contains("Failed to parse PDF"),
                    "Error message should indicate PDF parsing failure");
        }

        @Test
        @DisplayName("Should throw IOException for empty PDF file")
        void extractText_emptyPdf_throwsIOException() throws IOException {
            // Arrange
            byte[] emptyContent = new byte[0];
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "resume.pdf", "application/pdf", emptyContent);

            // Act & Assert
            assertThrows(IOException.class, () -> service.extractText(file),
                    "Should throw IOException for empty PDF content");
        }

        @Test
        @DisplayName("Should handle PDF file with .PDF extension (uppercase)")
        void extractText_uppercasePdfExtension() throws IOException {
            // Arrange
            String invalidPdf = "This is not a valid PDF file";
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "resume.PDF", "application/pdf", invalidPdf.getBytes());

            // Act & Assert
            assertThrows(IOException.class, () -> service.extractText(file),
                    "Should handle uppercase PDF extension");
        }
    }

    @Nested
    @DisplayName("extractText() - DOCX file tests")
    class DocxFileTests {

        @Test
        @DisplayName("Should throw IOException for invalid DOCX content")
        void extractText_invalidDocx_throwsIOException() throws IOException {
            // Arrange
            String invalidDocx = "This is not a valid DOCX file";
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "resume.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    invalidDocx.getBytes());

            // Act & Assert
            IOException exception = assertThrows(IOException.class,
                    () -> service.extractText(file));
            assertTrue(exception.getMessage().contains("Failed to parse DOCX"),
                    "Error message should indicate DOCX parsing failure");
        }

        @Test
        @DisplayName("Should throw IOException for empty DOCX file")
        void extractText_emptyDocx_throwsIOException() throws IOException {
            // Arrange
            byte[] emptyContent = new byte[0];
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "resume.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    emptyContent);

            // Act & Assert
            assertThrows(IOException.class, () -> service.extractText(file),
                    "Should throw IOException for empty DOCX content");
        }

        @Test
        @DisplayName("Should handle DOCX file with .DOCX extension (uppercase)")
        void extractText_uppercaseDocxExtension() throws IOException {
            // Arrange
            String invalidDocx = "This is not a valid DOCX file";
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "resume.DOCX", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    invalidDocx.getBytes());

            // Act & Assert
            assertThrows(IOException.class, () -> service.extractText(file),
                    "Should handle uppercase DOCX extension");
        }
    }

    @Nested
    @DisplayName("extractText() - Unsupported file type tests")
    class UnsupportedFileTypeTests {

        @Test
        @DisplayName("Should throw IOException for JPG file")
        void extractText_jpgFile_throwsIOException() throws IOException {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "resume.jpg", "image/jpeg", "image data".getBytes());

            // Act & Assert
            IOException exception = assertThrows(IOException.class,
                    () -> service.extractText(file));
            assertTrue(exception.getMessage().contains("Unsupported file type"),
                    "Error message should indicate unsupported file type");
            assertTrue(exception.getMessage().contains("jpg"),
                    "Error message should mention the file extension");
        }

        @Test
        @DisplayName("Should throw IOException for PNG file")
        void extractText_pngFile_throwsIOException() throws IOException {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "resume.png", "image/png", "image data".getBytes());

            // Act & Assert
            IOException exception = assertThrows(IOException.class,
                    () -> service.extractText(file));
            assertTrue(exception.getMessage().contains("Unsupported file type"),
                    "Error message should indicate unsupported file type");
        }

        @Test
        @DisplayName("Should throw IOException for HTML file")
        void extractText_htmlFile_throwsIOException() throws IOException {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "resume.html", "text/html", "<html></html>".getBytes());

            // Act & Assert
            IOException exception = assertThrows(IOException.class,
                    () -> service.extractText(file));
            assertTrue(exception.getMessage().contains("Unsupported file type"),
                    "Error message should indicate unsupported file type");
        }

        @Test
        @DisplayName("Should throw IOException for XML file")
        void extractText_xmlFile_throwsIOException() throws IOException {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "resume.xml", "application/xml", "<xml></xml>".getBytes());

            // Act & Assert
            assertThrows(IOException.class, () -> service.extractText(file),
                    "Should throw IOException for XML file");
        }

        @Test
        @DisplayName("Should throw IOException for file with no extension")
        void extractText_noExtension_throwsIOException() throws IOException {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "resume", "application/octet-stream", "data".getBytes());

            // Act & Assert
            IOException exception = assertThrows(IOException.class,
                    () -> service.extractText(file));
            assertTrue(exception.getMessage().contains("Unsupported file type"),
                    "Error message should indicate unsupported file type");
        }

        @Test
        @DisplayName("Error message should suggest supported file types")
        void extractText_unsupported_suggestsSupportedTypes() throws IOException {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "resume.xyz", "application/octet-stream", "data".getBytes());

            // Act & Assert
            IOException exception = assertThrows(IOException.class,
                    () -> service.extractText(file));
            String message = exception.getMessage();
            assertTrue(message.contains("PDF"), "Should mention PDF as supported");
            assertTrue(message.contains("DOCX"), "Should mention DOCX as supported");
            assertTrue(message.contains("TXT"), "Should mention TXT as supported");
        }
    }

    @Nested
    @DisplayName("extractText() - Null and empty file tests")
    class NullAndEmptyFileTests {

        @Test
        @DisplayName("Should throw IOException for null file")
        void extractText_nullFile_throwsIOException() throws IOException {
            // Act & Assert
            IOException exception = assertThrows(IOException.class,
                    () -> service.extractText(null));
            assertEquals("No file uploaded", exception.getMessage(),
                    "Error message should indicate no file uploaded");
        }

        @Test
        @DisplayName("Should throw IOException for empty file content")
        void extractText_emptyFile_throwsIOException() throws IOException {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "resume.txt", "text/plain", new byte[0]);

            // Act & Assert
            // MockMultipartFile with zero-length bytes is considered isEmpty()=true
            IOException exception = assertThrows(IOException.class,
                    () -> service.extractText(file));
            assertEquals("No file uploaded", exception.getMessage(),
                    "Should indicate no file uploaded for empty content");
        }

        @Test
        @DisplayName("Should throw IOException for file with empty name")
        void extractText_noFileName_throwsIOException() throws IOException {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "", "text/plain", "content".getBytes());

            // Act & Assert
            // Empty filename "" passes the null check but gets extension ""
            // which is treated as unsupported file type
            IOException exception = assertThrows(IOException.class,
                    () -> service.extractText(file));
            assertTrue(exception.getMessage().contains("Unsupported file type"),
                    "Error message should indicate unsupported file type for empty filename");
        }

        @Test
        @DisplayName("Should handle file with null original filename")
        void extractText_nullFileName_throwsIOException() throws IOException {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "resume", (String) null, "text/plain", "content".getBytes());

            // Act & Assert
            // MockMultipartFile with null filename returns null for getOriginalFilename()
            // which triggers the "Unknown file name" check
            IOException exception = assertThrows(IOException.class,
                    () -> service.extractText(file));
            // The actual behavior depends on MockMultipartFile implementation:
            // null filename may return "" for getOriginalFilename(), leading to unsupported type
            assertTrue(exception.getMessage().contains("Unknown file name") ||
                    exception.getMessage().contains("Unsupported file type"),
                    "Should throw IOException for null filename");
        }
    }

    @Nested
    @DisplayName("extractText() - File extension detection tests")
    class FileExtensionTests {

        @Test
        @DisplayName("Should detect .txt extension")
        void extractText_detectsTxtExtension() throws IOException {
            // Arrange
            String content = "Test content";
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "document.txt", "text/plain", content.getBytes());

            // Act
            String result = service.extractText(file);

            // Assert
            assertEquals(content, result);
        }

        @Test
        @DisplayName("Should detect .TXT extension (uppercase)")
        void extractText_detectsUppercaseTxtExtension() throws IOException {
            // Arrange
            String content = "Test content";
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "document.TXT", "text/plain", content.getBytes());

            // Act
            String result = service.extractText(file);

            // Assert
            assertEquals(content, result);
        }

        @Test
        @DisplayName("Should detect .pdf extension")
        void extractText_detectsPdfExtension() throws IOException {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "document.pdf", "application/pdf", "data".getBytes());

            // Act & Assert
            assertThrows(IOException.class, () -> service.extractText(file),
                    "Should attempt to parse as PDF (and fail on invalid content)");
        }

        @Test
        @DisplayName("Should detect .docx extension")
        void extractText_detectsDocxExtension() throws IOException {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "document.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "data".getBytes());

            // Act & Assert
            assertThrows(IOException.class, () -> service.extractText(file),
                    "Should attempt to parse as DOCX (and fail on invalid content)");
        }

        @Test
        @DisplayName("Should handle file with multiple dots in name")
        void extractText_multipleDotsInName() throws IOException {
            // Arrange
            String content = "Test content";
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "my.resume.txt", "text/plain", content.getBytes());

            // Act
            String result = service.extractText(file);

            // Assert
            assertEquals(content, result);
        }

        @Test
        @DisplayName("Should handle file with spaces in name")
        void extractText_spacesInFileName() throws IOException {
            // Arrange
            String content = "Test content";
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "my resume.txt", "text/plain", content.getBytes());

            // Act
            String result = service.extractText(file);

            // Assert
            assertEquals(content, result);
        }
    }

    @Nested
    @DisplayName("extractText() - Content validation tests")
    class ContentValidationTests {

        @Test
        @DisplayName("Should preserve line breaks in TXT content")
        void extractText_preservesLineBreaks() throws IOException {
            // Arrange
            String content = "Line 1\nLine 2\nLine 3";
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "resume.txt", "text/plain", content.getBytes());

            // Act
            String result = service.extractText(file);

            // Assert
            assertEquals("Line 1\nLine 2\nLine 3", result);
        }

        @Test
        @DisplayName("Should preserve tabs in TXT content")
        void extractText_preservesTabs() throws IOException {
            // Arrange
            String content = "Name\tEmail\tPhone\nJohn\tjohn@example.com\t123-456-7890";
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "resume.txt", "text/plain", content.getBytes());

            // Act
            String result = service.extractText(file);

            // Assert
            assertEquals(content, result);
        }

        @Test
        @DisplayName("Should handle TXT content with mixed line endings")
        void extractText_mixedLineEndings() throws IOException {
            // Arrange
            String content = "Line 1\r\nLine 2\nLine 3\rLine 4";
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "resume.txt", "text/plain", content.getBytes());

            // Act
            String result = service.extractText(file);

            // Assert
            assertNotNull(result);
            assertTrue(result.contains("Line 1"));
            assertTrue(result.contains("Line 4"));
        }

        @Test
        @DisplayName("Should handle TXT content with bullet points")
        void extractText_bulletPoints() throws IOException {
            // Arrange
            String content = """
                    EXPERIENCE
                    • Developed software
                    - Implemented features
                    * Fixed bugs
                    """;
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "resume.txt", "text/plain", content.getBytes());

            // Act
            String result = service.extractText(file);

            // Assert
            assertTrue(result.contains("•"));
            assertTrue(result.contains("-"));
            assertTrue(result.contains("*"));
        }

        @Test
        @DisplayName("Should handle TXT content with numbers and symbols")
        void extractText_numbersAndSymbols() throws IOException {
            // Arrange
            String content = "Phone: (123) 456-7890\nEmail: test@example.com\nSalary: $100,000";
            MockMultipartFile file = new MockMultipartFile(
                    "resume", "resume.txt", "text/plain", content.getBytes());

            // Act
            String result = service.extractText(file);

            // Assert
            assertEquals(content, result);
        }
    }
}
