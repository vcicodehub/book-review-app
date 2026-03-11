# PDF AI Review Monorepo

A local-first monorepo containing:

- `frontend/` — Angular standalone app with Tailwind CSS
- `backend/` — Java Spring Boot app using Gradle and SQLite

## What this app does

1. Upload a PDF, or import a Kindle/ebook from your Kindle app folder
2. Extract text on the backend (PDF or EPUB; DRM-protected Kindle files may not extract)
3. Ask an AI engine to summarize it
4. View the summary in the browser
5. Generate an Amazon review draft from the summary
6. Edit the review and choose the star rating
7. Save the result to a local SQLite database file on your laptop

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
- SQLite

## Prerequisites

- Node.js 20.19+ recommended
- npm 10+
- Java 21+
- Gradle 8.14+ or 9.x

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

## Database file

The backend stores data in:

```text
backend/data/pdf-review-app.db
```

Uploaded files are stored in:

```text
backend/data/uploads/
```

## API overview

- `POST /api/documents/upload`
- `GET /api/kindle/books` — list ebooks in Kindle content folder
- `POST /api/kindle/import` — import and summarize an ebook by path
- `GET /api/documents`
- `GET /api/documents/{id}`
- `DELETE /api/documents/{id}`
- `POST /api/reviews/generate`
- `POST /api/reviews/shorten`
- `POST /api/reviews/humanize`
- `POST /api/reviews`
- `PUT /api/reviews/{id}`
- `DELETE /api/reviews/{id}`

## Notes

- API keys stay on the backend only.
- The backend truncates extracted PDF text before sending it to the AI provider.
- SQLite keeps everything local in a file-based database.
