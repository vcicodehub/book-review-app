package com.example.pdfreview.controller;

import com.example.pdfreview.dto.DocumentDetailResponse;
import com.example.pdfreview.dto.DocumentImageResponse;
import com.example.pdfreview.dto.SaveKindleRequest;
import com.example.pdfreview.service.DocumentImageService;
import com.example.pdfreview.service.DocumentService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentImageService documentImageService;

    public DocumentController(DocumentService documentService, DocumentImageService documentImageService) {
        this.documentService = documentService;
        this.documentImageService = documentImageService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentDetailResponse upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "bookTitle", required = false) String bookTitle,
            @RequestPart(value = "author", required = false) String author,
            @RequestPart(value = "bookSize", required = false) String bookSize,
            @RequestPart(value = "category", required = false) String category,
            @RequestPart(value = "amazonUrl", required = false) String amazonUrl
    ) {
        return documentService.upload(file, bookTitle, author, bookSize, category, amazonUrl);
    }

    @PostMapping("/kindle")
    public DocumentDetailResponse saveKindle(@RequestBody SaveKindleRequest request) {
        return documentService.saveKindle(
                request.bookTitle(),
                request.author(),
                request.bookSize(),
                request.amazonUrl()
        );
    }

    @GetMapping
    public List<DocumentDetailResponse> getDocuments() {
        return documentService.getDocuments();
    }

    @GetMapping("/{id}")
    public DocumentDetailResponse getDocument(@PathVariable Long id) {
        return documentService.getDocument(id);
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentImageResponse uploadImage(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file
    ) {
        return documentImageService.uploadImage(id, file);
    }

    @GetMapping("/{id}/images")
    public List<DocumentImageResponse> listImages(@PathVariable Long id) {
        return documentImageService.listImages(id);
    }

    @GetMapping("/{documentId}/images/{imageId}")
    public ResponseEntity<Resource> getImageContent(
            @PathVariable Long documentId,
            @PathVariable Long imageId
    ) throws IOException {
        var record = documentImageService.getImageRecord(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));
        if (!record.documentId().equals(documentId)) {
            throw new IllegalArgumentException("Image does not belong to this document.");
        }
        byte[] bytes = documentImageService.getImageBytes(imageId);

        String contentType = record.contentType() != null ? record.contentType() : getContentType(record.originalFileName());
        Resource resource = new ByteArrayResource(bytes);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + (record.originalFileName() != null ? record.originalFileName() : "image") + "\"")
                .body(resource);
    }

    @DeleteMapping("/{documentId}/images/{imageId}")
    public void deleteImage(
            @PathVariable Long documentId,
            @PathVariable Long imageId
    ) {
        documentImageService.deleteImage(documentId, imageId);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        documentService.delete(id);
    }

    private String getContentType(String originalFileName) {
        if (originalFileName == null) return "image/jpeg";
        String lower = originalFileName.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }
}
