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

    public Long insert(Long documentId, String filePath, String originalFileName, String createdAt) {
        jdbcTemplate.update(
                """
                insert into document_images (document_id, file_path, original_file_name, created_at)
                values (?, ?, ?, ?)
                """,
                documentId,
                filePath,
                originalFileName,
                createdAt
        );
        return jdbcTemplate.queryForObject("select last_insert_rowid()", Long.class);
    }

    public List<DocumentImageRecord> findByDocumentId(Long documentId) {
        return jdbcTemplate.query(
                "select id, document_id, file_path, original_file_name, created_at from document_images where document_id = ? order by id",
                this::mapRow,
                documentId
        );
    }

    public Optional<DocumentImageRecord> findById(Long id) {
        List<DocumentImageRecord> results = jdbcTemplate.query(
                "select id, document_id, file_path, original_file_name, created_at from document_images where id = ?",
                this::mapRow,
                id
        );
        return results.stream().findFirst();
    }

    public void deleteById(Long id) {
        jdbcTemplate.update("delete from document_images where id = ?", id);
    }

    public void deleteByDocumentId(Long documentId) {
        jdbcTemplate.update("delete from document_images where document_id = ?", documentId);
    }

    private DocumentImageRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new DocumentImageRecord(
                rs.getLong("id"),
                rs.getLong("document_id"),
                rs.getString("file_path"),
                rs.getString("original_file_name"),
                rs.getString("created_at")
        );
    }
}
