# ATS Scorer Enhancer

A web-based **Applicant Tracking System (ATS) Scorer** built with Spring Boot that evaluates how well a resume matches a job description using keyword-based analysis.

This project checks how well a resume matches a job description and can tailor it for the role.

## Features

- **File Upload**: Supports PDF, DOCX, and TXT resume files
- **Keyword Matching**: Extracts and matches keywords from job descriptions against resumes
- **Section Analysis**: Checks for standard resume sections (Experience, Education, Skills, etc.)
- **Experience Scoring**: Evaluates years of experience and role relevance
- **Format Scoring**: Assesses resume formatting and structure
- **Detailed Reports**: Provides matched/missing keywords and improvement suggestions
- **REST API**: Clean API endpoints for integration
- **Web UI**: Modern drag-and-drop interface for easy use
- **Docker Support**: Ready for deployment anywhere

## Tech Stack

- **Backend**: Spring Boot 3.4.2, Java 17
- **PDF Parsing**: Apache PDFBox 2.0.33
- **DOCX Parsing**: Apache POI 5.3.0
- **Frontend**: Vanilla HTML/CSS/JavaScript
- **Deployment**: Docker & Docker Compose

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker (optional, for containerized deployment)

### Local Development

```bash
# Build and run
mvn spring-boot:run

# Open in browser
open http://localhost:8080
```

### Docker Deployment

```bash
# Build and run with Docker Compose
docker-compose up --build

# Open in browser
open http://localhost:8080
```

### Docker Only

```bash
# Build image
docker build -t ats-scorer .

# Run container
docker run -p 8080:8080 ats-scorer
```

## Logging

- Requests are logged with method, path, status, and duration. Each response includes an `X-Request-Id` header that can be used to correlate it with application logs.
- Logs never include uploaded resume contents, job descriptions, or uploaded filenames. Unexpected failures are recorded in server logs while clients receive a generic error message.
- Local runs use readable console logs. Docker and the `prod` Spring profile emit structured JSON logs to standard output; Compose rotates container logs at 10 MB per file and keeps five files. Use `docker compose logs -f ats-scorer` to view them. Set `SPRING_PROFILES_ACTIVE` to override the Compose default.

## API Endpoints

### Score Resume

**POST** `/api/score`

Upload a resume file and job description to get a scoring report.

**Request:**
- `resume` (MultipartFile): Resume file (PDF, DOCX, TXT)
- `jobDescription` (String): Job description text

**Response:**
```json
{
  "overallScore": 75.5,
  "keywordScore": 80.0,
  "sectionScore": 100.0,
  "experienceScore": 60.0,
  "formatScore": 70.0,
  "matchedKeywords": ["java", "spring", "docker"],
  "missingKeywords": ["kubernetes", "aws"],
  "suggestions": ["Consider adding these missing keywords..."],
  "grade": "B"
}
```

### Health Check

**GET** `/api/health`

Returns service health status.

## Scoring Criteria

| Category | Weight | Description |
|----------|--------|-------------|
| Keywords | 40% | Match between resume and job description keywords |
| Sections | 20% | Presence of standard resume sections |
| Experience | 20% | Years of experience and role relevance |
| Format | 20% | Resume structure and formatting quality |

### Grades

| Score Range | Grade |
|-------------|-------|
| 90-100% | A+ |
| 80-89% | A |
| 70-79% | B |
| 60-69% | C |
| 50-59% | D |
| Below 50% | F |

## Project Structure

```
ATS_Scorer/
├── src/main/java/com/ats/scorer/
│   ├── AtsScorerApplication.java      # Main application entry point
│   ├── config/
│   │   └── GlobalExceptionHandler.java # Global error handling
│   ├── controller/
│   │   └── ScoringController.java      # REST API endpoints
│   ├── dto/
│   │   ├── ScoreRequest.java           # Request DTO
│   │   └── ScoreResponse.java          # Response DTO
│   └── service/
│       ├── AtsScoringService.java      # Core scoring logic
│       └── ResumeParserService.java    # File parsing (PDF/DOCX/TXT)
├── src/main/resources/
│   ├── application.properties          # Application configuration
│   └── static/
│       └── index.html                  # Web UI
├── Dockerfile                          # Multi-stage Docker build
├── docker-compose.yml                  # Docker Compose configuration
├── .dockerignore                       # Docker ignore rules
└── pom.xml                             # Maven dependencies
```

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# Server port
server.port=8080

# File upload limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Scoring weights (must sum to 1.0)
ats.scoring.keyword-weight=0.4
ats.scoring.section-weight=0.2
ats.scoring.experience-weight=0.2
ats.scoring.format-weight=0.2
```

## License

MIT
