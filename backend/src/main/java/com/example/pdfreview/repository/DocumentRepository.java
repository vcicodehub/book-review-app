package com.example.pdfreview.repository;

import com.example.pdfreview.model.DocumentRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class DocumentRepository {

    private final JdbcTemplate jdbcTemplate;

    public DocumentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(DocumentRecord document) {
        return jdbcTemplate.queryForObject(
                """
                insert into documents (file_name, original_file_name, book_title, author, book_size, category, pdf_path, summary, created_at, amazon_url)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?::timestamptz, ?)
                returning id
                """,
                Long.class,
                document.fileName(),
                document.originalFileName(),
                document.bookTitle(),
                document.author(),
                document.bookSize(),
                document.category(),
                document.pdfPath(),
                document.summary(),
                document.createdAt(),
                document.amazonUrl()
        );
    }

    public List<DocumentRecord> findAll() {
        return jdbcTemplate.query(
                "select id, file_name, original_file_name, book_title, author, book_size, category, pdf_path, summary, created_at::text, amazon_url from documents order by id desc",
                this::mapRow
        );
    }

    public Optional<DocumentRecord> findById(Long id) {
        List<DocumentRecord> results = jdbcTemplate.query(
                "select id, file_name, original_file_name, book_title, author, book_size, category, pdf_path, summary, created_at::text, amazon_url from documents where id = ?",
                this::mapRow,
                id
        );
        return results.stream().findFirst();
    }

    public void deleteById(Long id) {
        jdbcTemplate.update("delete from documents where id = ?", id);
    }

    private DocumentRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new DocumentRecord(
                rs.getLong("id"),
                rs.getString("file_name"),
                rs.getString("original_file_name"),
                rs.getString("book_title"),
                rs.getString("author"),
                rs.getString("book_size"),
                rs.getString("category"),
                rs.getString("pdf_path"),
                rs.getString("summary"),
                rs.getString("created_at"),
                rs.getString("amazon_url")
        );
    }
}
