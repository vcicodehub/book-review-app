package com.example.pdfreview.service;

import com.example.pdfreview.dto.DocumentImageResponse;
import com.example.pdfreview.model.DocumentImageRecord;
import com.example.pdfreview.model.DocumentRecord;
import com.example.pdfreview.repository.DocumentImageRepository;
import com.example.pdfreview.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentImageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    private final DocumentRepository documentRepository;
    private final DocumentImageRepository documentImageRepository;
    private final Path imagesBaseDir;

    public DocumentImageService(
            DocumentRepository documentRepository,
            DocumentImageRepository documentImageRepository,
            @Value("${app.storage.upload-dir}") String uploadDir
    ) {
        this.documentRepository = documentRepository;
        this.documentImageRepository = documentImageRepository;
        this.imagesBaseDir = Path.of(uploadDir).resolve("images");
    }

    public DocumentImageResponse uploadImage(Long documentId, MultipartFile file) {
        DocumentRecord document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found for id " + documentId));

        if (!"kindle".equalsIgnoreCase(document.category())) {
            throw new IllegalArgumentException("Image upload is only available for Kindle documents.");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid image type. Allowed: JPEG, PNG, GIF, WebP.");
        }

        String extension = getExtension(contentType, file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + extension;
        Path documentDir = imagesBaseDir.resolve(documentId.toString());

        try {
            Files.createDirectories(documentDir);
            Path destination = documentDir.resolve(storedFileName);
            file.transferTo(destination);

            String createdAt = OffsetDateTime.now().toString();
            Long id = documentImageRepository.insert(
                    documentId,
                    destination.toString(),
                    file.getOriginalFilename(),
                    createdAt
            );

            DocumentImageRecord record = documentImageRepository.findById(id).orElseThrow();
            return toResponse(record);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to save image", ex);
        }
    }

    public List<DocumentImageResponse> listImages(Long documentId) {
        documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found for id " + documentId));

        return documentImageRepository.findByDocumentId(documentId).stream()
                .map(this::toResponse)
                .toList();
    }

    public void deleteImage(Long documentId, Long imageId) {
        DocumentImageRecord image = documentImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found for id " + imageId));

        if (!image.documentId().equals(documentId)) {
            throw new IllegalArgumentException("Image does not belong to this document.");
        }

        documentImageRepository.deleteById(imageId);

        try {
            Path path = Path.of(image.filePath());
            if (Files.exists(path)) {
                Files.delete(path);
            }
        } catch (IOException ignored) {
            // Best effort cleanup
        }
    }

    public Optional<DocumentImageRecord> getImageRecord(Long imageId) {
        return documentImageRepository.findById(imageId);
    }

    public List<Path> getImagePathsForDocument(Long documentId) {
        return documentImageRepository.findByDocumentId(documentId).stream()
                .map(r -> Path.of(r.filePath()))
                .filter(Files::exists)
                .toList();
    }

    public void deleteImagesForDocument(Long documentId) {
        List<DocumentImageRecord> images = documentImageRepository.findByDocumentId(documentId);
        for (DocumentImageRecord image : images) {
            try {
                Path path = Path.of(image.filePath());
                if (Files.exists(path)) {
                    Files.delete(path);
                }
            } catch (IOException ignored) {
                // Best effort cleanup
            }
        }
        documentImageRepository.deleteByDocumentId(documentId);
    }

    private String getExtension(String contentType, String originalFilename) {
        if (contentType != null) {
            return switch (contentType.toLowerCase()) {
                case "image/jpeg", "image/jpg" -> ".jpg";
                case "image/png" -> ".png";
                case "image/gif" -> ".gif";
                case "image/webp" -> ".webp";
                default -> ".jpg";
            };
        }
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
            if (ext.matches("^\\.(jpg|jpeg|png|gif|webp)$")) {
                return ext;
            }
        }
        return ".jpg";
    }

    private DocumentImageResponse toResponse(DocumentImageRecord r) {
        return new DocumentImageResponse(
                r.id(),
                r.documentId(),
                r.originalFileName(),
                r.createdAt()
        );
    }
}
