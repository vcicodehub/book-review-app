package com.example.pdfreview.service;

import com.example.pdfreview.dto.DocumentImageResponse;
import com.example.pdfreview.model.DocumentImageRecord;
import com.example.pdfreview.model.DocumentRecord;
import com.example.pdfreview.repository.DocumentImageRepository;
import com.example.pdfreview.repository.DocumentRepository;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

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
    private final DataSource dataSource;

    public DocumentImageService(
            DocumentRepository documentRepository,
            DocumentImageRepository documentImageRepository,
            DataSource dataSource
    ) {
        this.documentRepository = documentRepository;
        this.documentImageRepository = documentImageRepository;
        this.dataSource = dataSource;
    }

    @Transactional
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

        try {
            byte[] bytes = file.getBytes();
            long oid = createLargeObject(bytes);

            String createdAt = OffsetDateTime.now().toString();
            Long id = documentImageRepository.insert(
                    documentId,
                    oid,
                    contentType,
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
    }

    public Optional<DocumentImageRecord> getImageRecord(Long imageId) {
        return documentImageRepository.findById(imageId);
    }

    public byte[] getImageBytes(Long imageId) throws IOException {
        DocumentImageRecord record = documentImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found for id " + imageId));

        return readLargeObject(record.largeObjectOid());
    }

    public List<DocumentImageRecord> getImageRecordsForDocument(Long documentId) {
        return documentImageRepository.findByDocumentId(documentId);
    }

    /**
     * Returns image content for all images of a document, for use by AI review generation.
     */
    public List<ImageData> getImageDataForDocument(Long documentId) throws IOException {
        List<DocumentImageRecord> records = documentImageRepository.findByDocumentId(documentId);
        List<ImageData> result = new java.util.ArrayList<>();
        for (DocumentImageRecord r : records) {
            byte[] bytes = readLargeObject(r.largeObjectOid());
            String contentType = r.contentType() != null ? r.contentType() : "image/jpeg";
            result.add(new ImageData(bytes, contentType));
        }
        return result;
    }

    public void deleteImagesForDocument(Long documentId) {
        documentImageRepository.deleteByDocumentId(documentId);
    }

    /**
     * Creates a PostgreSQL large object from bytes. Must be called within a transaction.
     */
    private long createLargeObject(byte[] bytes) throws IOException {
        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            conn.setAutoCommit(false);

            var pgConn = conn.unwrap(org.postgresql.PGConnection.class);
            var lobj = pgConn.getLargeObjectAPI();
            long oid = lobj.createLO(org.postgresql.largeobject.LargeObjectManager.READWRITE);
            var largeObj = lobj.open(oid, org.postgresql.largeobject.LargeObjectManager.WRITE);
            try {
                largeObj.write(bytes);
            } finally {
                largeObj.close();
            }
            return oid;
        } catch (Exception e) {
            throw new IOException("Failed to create large object", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (Exception ignored) {
                }
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }
    }

    private byte[] readLargeObject(long oid) throws IOException {
        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            conn.setAutoCommit(false);

            var pgConn = conn.unwrap(org.postgresql.PGConnection.class);
            var lobj = pgConn.getLargeObjectAPI();
            var largeObj = lobj.open(oid, org.postgresql.largeobject.LargeObjectManager.READ);
            try {
                byte[] buf = new byte[largeObj.size()];
                largeObj.read(buf, 0, buf.length);
                return buf;
            } finally {
                largeObj.close();
            }
        } catch (Exception e) {
            throw new IOException("Failed to read large object " + oid, e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (Exception ignored) {
                }
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }
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
