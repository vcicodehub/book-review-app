# PDF AI Review Monorepo

A local-first monorepo containing:

- `frontend/` — Angular standalone app with Tailwind CSS
- `backend/` — Java Spring Boot app using Gradle and PostgreSQL

## What this app does

1. Upload a PDF, or import a Kindle/ebook from your Kindle app folder
2. Extract text on the backend (PDF or EPUB; DRM-protected Kindle files may not extract)
3. Ask an AI engine to summarize it
4. View the summary in the browser
5. Generate an Amazon review draft from the summary
6. Edit the review and choose the star rating
7. Save the result to a PostgreSQL database

## Monorepo layout

```text
pdf-ai-review-monorepo/
  frontend/
  backend/
  .gitignore
  README.md
```

## Frontend stack

- Angular standalone components
- Tailwind CSS
- Reactive forms
- HttpClient

## Backend stack

- Java 21+
- Spring Boot
- Gradle
- Spring Web
- Spring JDBC
- Apache PDFBox
- PostgreSQL (images stored as large objects)

## Prerequisites

- Node.js 20.19+ recommended
- npm 10+
- Java 21+
- Gradle 8.14+ or 9.x
- PostgreSQL 14+

## Database setup

Create a PostgreSQL database:

```sql
CREATE DATABASE pdf_review;
```

The backend connects using these environment variables (defaults shown):

```bash
export POSTGRES_HOST=localhost
export POSTGRES_PORT=5432
export POSTGRES_DB=pdf_review
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=postgres
```

On Windows (PowerShell):

```powershell
$env:POSTGRES_HOST = "localhost"
$env:POSTGRES_DB = "pdf_review"
$env:POSTGRES_USER = "postgres"
$env:POSTGRES_PASSWORD = "postgres"
```

The schema is created automatically on first run. Images are stored as PostgreSQL large objects (LOBs).

## Migrating from SQLite

If you have existing data in a SQLite database, run the migration tool.

**Windows (Command Prompt):**

```cmd
cd backend
set POSTGRES_URL=jdbc:postgresql://localhost:5432/pdf_review
set POSTGRES_USER=noorluca
set POSTGRES_PASSWORD=superSecurePasswordHere
set SQLITE_DB_PATH=.\data\pdf-review-app.db
gradle runMigration
```

**Windows (PowerShell):**

```powershell
cd backend
$env:POSTGRES_URL = "jdbc:postgresql://localhost:5432/pdf_review"
$env:POSTGRES_USER = "postgres"
$env:POSTGRES_PASSWORD = "postgres"
$env:SQLITE_DB_PATH = ".\data\pdf-review-app.db"
gradle runMigration
```

**macOS/Linux:**

```bash
cd backend
export POSTGRES_URL=jdbc:postgresql://localhost:5432/pdf_review
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=postgres
export SQLITE_DB_PATH=./data/pdf-review-app.db
gradle runMigration
```

The migration will:

1. Copy all documents and reviews to PostgreSQL
2. Read image files from disk and store them as PostgreSQL large objects
3. Preserve IDs and relationships

**Note:** Run the backend once with PostgreSQL first so the schema exists. The migration truncates existing PostgreSQL tables before importing.

## Environment variables

The backend uses OpenAI for AI summaries and review generation.

Set this environment variable before starting the backend:

```bash
export OPENAI_API_KEY=your-key-here
```

Optional overrides:

```bash
export AI_BASE_URL=https://api.openai.com   # default
export AI_MODEL=gpt-4o-mini                 # default
export KINDLE_CONTENT_PATH=/path/to/kindle  # default: ~/Documents/My Kindle Content (Windows: C:\Users\<you>\Documents\My Kindle Content)
```

On Windows (PowerShell), to override the Kindle folder:

```powershell
$env:KINDLE_CONTENT_PATH = "C:\Users\YourUsername\Documents\My Kindle Content"
```

### Email reminders for unposted reviews

If a review is saved but not posted to Amazon within 4 days, the backend can send an email reminder. To enable:

```bash
export REMINDER_ENABLED=true
export REMINDER_RECIPIENT=your-email@example.com
export SMTP_HOST=smtp.example.com
export SMTP_PORT=587
export SMTP_USERNAME=your-smtp-username
export SMTP_PASSWORD=your-smtp-password
```

The reminder runs daily at 9:00 AM (configurable via `REMINDER_CRON`, default `0 0 9 * * ?`).

## Run the backend

```bash
cd backend
gradle bootRun
```

The backend starts on `http://localhost:8010`.

## Run the frontend

```bash
cd frontend
npm install
npm start
```

The frontend starts on `http://localhost:4200`.

## Storage

- **Database:** PostgreSQL (documents, reviews, document metadata)
- **Images:** Stored in PostgreSQL as large objects (no separate image files)
- **PDFs:** `backend/data/uploads/` (PDF files remain on disk)

## API overview

- `POST /api/documents/upload`
- `GET /api/kindle/books` — list ebooks in Kindle content folder
- `POST /api/kindle/import` — import and summarize an ebook by path
- `GET /api/documents`
- `GET /api/documents/{id}`
- `DELETE /api/documents/{id}`
- `POST /api/documents/{id}/images` — upload image for Kindle document
- `GET /api/documents/{documentId}/images/{imageId}` — serve image
- `POST /api/reviews/generate`
- `POST /api/reviews/shorten`
- `POST /api/reviews/humanize`
- `POST /api/reviews`
- `PUT /api/reviews/{id}`
- `DELETE /api/reviews/{id}`

## Notes

- API keys stay on the backend only.
- The backend truncates extracted PDF text before sending it to the AI provider.
- PostgreSQL stores all structured data and images (as large objects).

