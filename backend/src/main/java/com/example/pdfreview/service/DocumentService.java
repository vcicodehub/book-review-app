package com.example.pdfreview.service;

import com.example.pdfreview.dto.DocumentDetailResponse;
import com.example.pdfreview.dto.DocumentImageResponse;
import com.example.pdfreview.model.DocumentRecord;
import com.example.pdfreview.repository.DocumentImageRepository;
import com.example.pdfreview.repository.DocumentRepository;
import com.example.pdfreview.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ReviewRepository reviewRepository;
    private final DocumentImageRepository documentImageRepository;
    private final PdfService pdfService;
    private final AiService aiService;
    private final EbookService ebookService;
    private final KindleService kindleService;
    private final Path uploadDir;

    public DocumentService(
            DocumentRepository documentRepository,
            ReviewRepository reviewRepository,
            DocumentImageRepository documentImageRepository,
            PdfService pdfService,
            AiService aiService,
            EbookService ebookService,
            KindleService kindleService,
            @Value("${app.storage.upload-dir}") String uploadDir
    ) {
        this.documentRepository = documentRepository;
        this.reviewRepository = reviewRepository;
        this.documentImageRepository = documentImageRepository;
        this.pdfService = pdfService;
        this.aiService = aiService;
        this.ebookService = ebookService;
        this.kindleService = kindleService;
        this.uploadDir = Path.of(uploadDir);
    }

    public DocumentDetailResponse upload(MultipartFile file, String bookTitle, String author, String bookSize, String category, String amazonUrl) {
        validatePdf(file);

        try {
            Files.createDirectories(uploadDir);
            String storedFileName = UUID.randomUUID() + ".pdf";
            Path destination = uploadDir.resolve(storedFileName);
            file.transferTo(destination);

            String extractedText = pdfService.extractText(destination);
            String summary = aiService.summarize(extractedText);
            String createdAt = OffsetDateTime.now().toString();
            String normalizedCategory = normalizeCategory(blankToNull(category));

            Long id = documentRepository.insert(new DocumentRecord(
                    null,
                    storedFileName,
                    file.getOriginalFilename(),
                    blankToNull(bookTitle),
                    blankToNull(author),
                    blankToNull(bookSize),
                    normalizedCategory,
                    destination.toString(),
                    summary,
                    createdAt,
                    blankToNull(amazonUrl)
            ));

            return getDocument(id);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to save the uploaded PDF", ex);
        }
    }

    public DocumentDetailResponse saveKindle(String bookTitle, String author, String bookSize, String amazonUrl) {
        String createdAt = OffsetDateTime.now().toString();
        String summary = "Kindle book – no PDF available. Please write your review based on your reading experience.";
        String pdfPathSentinel = "kindle:no-file";

        Long id = documentRepository.insert(new DocumentRecord(
                null,
                "",
                null,
                blankToNull(bookTitle),
                blankToNull(author),
                blankToNull(bookSize),
                "kindle",
                pdfPathSentinel,
                summary,
                createdAt,
                blankToNull(amazonUrl)
        ));

        return getDocument(id);
    }

    /**
     * Imports a Kindle/ebook from the given file path: extracts text, summarizes with AI, and saves.
     * Path must be under the configured Kindle content folder for security.
     */
    public DocumentDetailResponse importKindleFromPath(String filePath, String bookTitle, String author, String bookSize, String amazonUrl) {
        Path path = Path.of(filePath);
        if (!kindleService.isPathUnderKindleFolder(path)) {
            throw new IllegalArgumentException("File must be in the Kindle content folder: " + kindleService.getKindleContentPath());
        }

        EbookService.ExtractedEbook extracted = ebookService.extractText(path);

        String summary = aiService.summarize(extracted.text());
        String createdAt = OffsetDateTime.now().toString();
        String resolvedTitle = blankToNull(bookTitle) != null ? blankToNull(bookTitle) : extracted.title();
        String resolvedAuthor = blankToNull(author) != null ? blankToNull(author) : extracted.author();

        Long id = documentRepository.insert(new DocumentRecord(
                null,
                "",
                path.getFileName().toString(),
                resolvedTitle,
                resolvedAuthor,
                blankToNull(bookSize),
                "kindle",
                "kindle:imported:" + path.toAbsolutePath(),
                summary,
                createdAt,
                blankToNull(amazonUrl)
        ));

        return getDocument(id);
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "book";
        }
        String lower = category.trim().toLowerCase();
        return switch (lower) {
            case "book", "puzzles", "activity", "coloring", "kindle" -> lower;
            default -> "book";
        };
    }

    public List<DocumentDetailResponse> getDocuments() {
        return documentRepository.findAll().stream()
                .map(document -> new DocumentDetailResponse(
                        document.id(),
                        document.fileName(),
                        document.originalFileName(),
                        document.bookTitle(),
                        document.author(),
                        document.bookSize(),
                        document.category(),
                        document.summary(),
                        document.createdAt(),
                        reviewRepository.findByDocumentId(document.id()).orElse(null),
                        mapImages(document.id()),
                        document.amazonUrl()
                ))
                .toList();
    }

    public DocumentDetailResponse getDocument(Long id) {
        DocumentRecord document = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found for id " + id));

        return new DocumentDetailResponse(
                document.id(),
                document.fileName(),
                document.originalFileName(),
                document.bookTitle(),
                document.author(),
                document.bookSize(),
                document.category(),
                document.summary(),
                document.createdAt(),
                reviewRepository.findByDocumentId(document.id()).orElse(null),
                mapImages(document.id()),
                document.amazonUrl()
        );
    }

    private List<DocumentImageResponse> mapImages(Long documentId) {
        return documentImageRepository.findByDocumentId(documentId).stream()
                .map(r -> new DocumentImageResponse(
                        r.id(),
                        r.documentId(),
                        r.originalFileName(),
                        r.createdAt()
                ))
                .toList();
    }

    public void delete(Long id) {
        DocumentRecord document = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found for id " + id));
        reviewRepository.deleteByDocumentId(id);
        deleteDocumentImages(id);
        documentRepository.deleteById(id);
        try {
            String pdfPath = document.pdfPath();
            if (pdfPath != null && !pdfPath.startsWith("kindle:")) {
                Path path = Path.of(pdfPath);
                if (Files.exists(path)) {
                    Files.delete(path);
                }
            }
        } catch (IOException ignored) {
            // Best effort to remove file
        }
    }

    private void deleteDocumentImages(Long documentId) {
        documentImageRepository.deleteByDocumentId(documentId);
    }

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A PDF file is required.");
        }

        String originalFileName = file.getOriginalFilename();
        boolean hasPdfExtension = originalFileName != null && originalFileName.toLowerCase().endsWith(".pdf");
        if (!hasPdfExtension) {
            throw new IllegalArgumentException("Only PDF files are accepted.");
        }
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
