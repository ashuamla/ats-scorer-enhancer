# ATS Scorer Enhancer

A web-based **Applicant Tracking System (ATS) Scorer** built with Spring Boot that evaluates how well a resume matches a job description using semantic LLM analysis when configured, with a transparent rule-based fallback.

This project compares a resume with a job description and provides a score breakdown, keyword comparison, and actionable improvement suggestions.

## Features

- **File Upload**: Supports PDF, DOCX, and TXT resume files
- **AI Resume Analysis**: Selects Groq, OpenAI, or local Ollama at startup and returns structured semantic scoring results through a shared provider interface
- **Rule-based Fallback**: Continues to score locally when AI is disabled or unavailable and identifies the analysis method
- **Section Analysis**: Checks for standard resume sections (Experience, Education, Skills, etc.)
- **Experience Scoring**: Evaluates years of experience and role relevance
- **Format Scoring**: Assesses resume formatting and structure
- **Detailed Reports**: Provides matched/missing keywords and improvement suggestions
- **REST API**: Clean API endpoints for integration
- **Web UI**: Modern drag-and-drop interface for easy use
- **Docker Support**: Ready for deployment anywhere

## Tech Stack

- **Backend**: Spring Boot 4.1.2, Java 21
- **AI**: Provider-specific services using Spring `RestClient`; Groq/OpenAI-compatible Chat Completions and Ollama's native chat API
- **PDF Parsing**: Apache PDFBox 2.0.37
- **DOCX Parsing**: Apache POI 5.5.1
- **Frontend**: Vanilla HTML/CSS/JavaScript
- **Deployment**: Docker & Docker Compose

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- Docker (optional, for containerized deployment)

### Local Development

```bash
# Build and run
mvn spring-boot:run

# Open in browser
open http://localhost:8080
```

AI scoring is opt-in. Set `ATS_AI_ENABLED=true` and choose `ATS_AI_PROVIDER=groq`, `openai`, or `ollama`. For example, Groq can be configured from PowerShell as follows:

```powershell
$env:GROQ_API_KEY = "your-groq-api-key"
$env:ATS_AI_ENABLED = "true"
$env:ATS_AI_PROVIDER = "groq"
$env:GROQ_MODEL = "openai/gpt-oss-20b"
mvn spring-boot:run
```

Provider-specific settings:

| `ATS_AI_PROVIDER` | Required settings | Defaults |
| --- | --- | --- |
| `groq` | `GROQ_API_KEY` | `GROQ_BASE_URL=https://api.groq.com/openai/v1`, `GROQ_MODEL=openai/gpt-oss-20b` |
| `openai` | `OPENAI_API_KEY` | `OPENAI_BASE_URL=https://api.openai.com/v1`, `OPENAI_MODEL=gpt-4o-mini` |
| `ollama` | Ollama server reachable at the configured base URL; no API key | `OLLAMA_BASE_URL=http://localhost:11434`, `OLLAMA_MODEL=llama3.2` |

For Ollama, install/start Ollama and pull the chosen model first (for example, `ollama pull llama3.2`). When the application runs in Docker and Ollama runs on the host, Compose defaults to `http://host.docker.internal:11434`; set `OLLAMA_BASE_URL` if Ollama is elsewhere. Provider requests use a 5-second connection timeout and a 60-second response timeout by default; override them with `ATS_AI_HTTP_CONNECT_TIMEOUT` and `ATS_AI_HTTP_READ_TIMEOUT` if needed. Resume text and job descriptions are sent to the selected provider. Groq and OpenAI have their own account limits and pricing; local Ollama avoids sending the text to a hosted AI provider. If an AI request fails, the app logs the failure type and falls back to the local rule-based scorer.

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

To enable AI in Docker Compose, set `ATS_AI_ENABLED=true` and `ATS_AI_PROVIDER` to the desired provider in the environment where Compose runs, then provide that provider's settings. Do not commit API keys to the repository.

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
  "grade": "B",
  "analysisMethod": "AI",
  "analysisSummary": "The resume shows relevant experience in ..."
}
```

`analysisMethod` is `AI` when the selected AI provider produced the assessment and `RULE_BASED` when local heuristics were used.

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

In AI mode, the model assesses these dimensions semantically; the configured weights determine the overall score. Rule-based mode uses local text and structure heuristics instead.

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
│   │   ├── GlobalExceptionHandler.java # Global error handling
│   │   └── ResumeAnalysisProviderConfiguration.java # Provider selection
│   ├── controller/
│   │   └── ScoringController.java      # REST API endpoints
│   ├── dto/
│   │   ├── ScoreRequest.java           # Request DTO
│   │   └── ScoreResponse.java          # Response DTO
│   └── service/
│       ├── AtsScoringService.java      # Core scoring logic
│       ├── ResumeAnalysisProvider.java # Scoring provider abstraction
│       ├── GroqResumeAnalysisProvider.java # Groq OpenAI-compatible API
│       ├── OpenAiResumeAnalysisProvider.java # OpenAI API
│       ├── OllamaResumeAnalysisProvider.java # Ollama native API
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

Set `ATS_AI_ENABLED=true` and `ATS_AI_PROVIDER` to `groq`, `openai`, or `ollama` to enable an AI provider. Credentials and provider URLs are read from environment variables; never store API keys in source control. The application defaults are:

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

# Optional AI configuration
ats.ai.enabled=${ATS_AI_ENABLED:false}
ats.ai.provider=${ATS_AI_PROVIDER:groq}
ats.ai.http.connect-timeout=${ATS_AI_HTTP_CONNECT_TIMEOUT:5s}
ats.ai.http.read-timeout=${ATS_AI_HTTP_READ_TIMEOUT:60s}
ats.ai.groq.api-key=${GROQ_API_KEY:}
ats.ai.groq.base-url=${GROQ_BASE_URL:https://api.groq.com/openai/v1}
ats.ai.groq.model=${GROQ_MODEL:openai/gpt-oss-20b}
ats.ai.openai.api-key=${OPENAI_API_KEY:}
ats.ai.openai.base-url=${OPENAI_BASE_URL:https://api.openai.com/v1}
ats.ai.openai.model=${OPENAI_MODEL:gpt-4o-mini}
ats.ai.ollama.base-url=${OLLAMA_BASE_URL:http://localhost:11434}
ats.ai.ollama.model=${OLLAMA_MODEL:llama3.2}
```

## License

MIT
