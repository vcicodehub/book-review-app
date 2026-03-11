package com.example.pdfreview.repository;

import com.example.pdfreview.model.DocumentImageRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class DocumentImageRepository {

    private final JdbcTemplate jdbcTemplate;

    public DocumentImageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Inserts a document image with binary data stored as a PostgreSQL large object.
     * Must be called within a transaction (autoCommit=false) for large object support.
     */
    public Long insert(Long documentId, long largeObjectOid, String contentType, String originalFileName, String createdAt) {
        return jdbcTemplate.queryForObject(
                """
                insert into document_images (document_id, large_object_oid, content_type, original_file_name, created_at)
                values (?, ?, ?, ?, ?::timestamptz)
                returning id
                """,
                Long.class,
                documentId,
                largeObjectOid,
                contentType != null ? contentType : "image/jpeg",
                originalFileName,
                createdAt
        );
    }

    public List<DocumentImageRecord> findByDocumentId(Long documentId) {
        return jdbcTemplate.query(
                """
                select id, document_id, large_object_oid, content_type, original_file_name, created_at::text
                from document_images where document_id = ? order by id
                """,
                this::mapRow,
                documentId
        );
    }

    public Optional<DocumentImageRecord> findById(Long id) {
        List<DocumentImageRecord> results = jdbcTemplate.query(
                """
                select id, document_id, large_object_oid, content_type, original_file_name, created_at::text
                from document_images where id = ?
                """,
                this::mapRow,
                id
        );
        return results.stream().findFirst();
    }

    public void deleteById(Long id) {
        Optional<DocumentImageRecord> record = findById(id);
        if (record.isPresent()) {
            unlinkLargeObject(record.get().largeObjectOid());
        }
        jdbcTemplate.update("delete from document_images where id = ?", id);
    }

    public void deleteByDocumentId(Long documentId) {
        List<DocumentImageRecord> images = findByDocumentId(documentId);
        for (DocumentImageRecord image : images) {
            unlinkLargeObject(image.largeObjectOid());
        }
        jdbcTemplate.update("delete from document_images where document_id = ?", documentId);
    }

    private void unlinkLargeObject(long oid) {
        try {
            jdbcTemplate.update("SELECT lo_unlink(?)", oid);
        } catch (Exception e) {
            // Best effort - large object may already be deleted
        }
    }

    private DocumentImageRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new DocumentImageRecord(
                rs.getLong("id"),
                rs.getLong("document_id"),
                rs.getLong("large_object_oid"),
                rs.getString("content_type"),
                rs.getString("original_file_name"),
                rs.getString("created_at")
        );
    }
}
