package com.example.pdfreview.migration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Migrates data from SQLite to PostgreSQL.
 * Images are read from disk and stored as PostgreSQL large objects.
 *
 * Usage:
 *   Run from backend directory. Set env vars:
 *   - SQLITE_DB_PATH: path to SQLite DB (default: ./data/pdf-review-app.db)
 *   - POSTGRES_URL: jdbc:postgresql://host:port/db
 *   - POSTGRES_USER: database user
 *   - POSTGRES_PASSWORD: database password
 *
 *   Or run: gradle runMigration
 */
public class SqliteToPostgresMigration {

    public static void main(String[] args) throws Exception {
        String sqlitePath = System.getenv().getOrDefault("SQLITE_DB_PATH", "./data/pdf-review-app.db");
        String pgUrl = System.getenv().getOrDefault("POSTGRES_URL", "jdbc:postgresql://localhost:5432/pdf_review");
        String pgUser = System.getenv().getOrDefault("POSTGRES_USER", "postgres");
        String pgPassword = System.getenv().getOrDefault("POSTGRES_PASSWORD", "postgres");

        Path sqliteFile = Path.of(sqlitePath);
        if (!Files.exists(sqliteFile)) {
            System.err.println("SQLite database not found: " + sqliteFile.toAbsolutePath());
            System.exit(1);
        }

        System.out.println("Migrating from " + sqlitePath + " to PostgreSQL...");

        try (Connection sqlite = DriverManager.getConnection("jdbc:sqlite:" + sqlitePath);
             Connection pg = DriverManager.getConnection(pgUrl, pgUser, pgPassword)) {

            pg.setAutoCommit(false);

            try {
                truncatePostgres(pg);
                migrateDocuments(sqlite, pg);
                migrateReviews(sqlite, pg);
                migrateDocumentImages(sqlite, pg);
                pg.commit();
                System.out.println("Migration completed successfully.");
            } catch (Exception e) {
                pg.rollback();
                throw e;
            }
        }
    }

    private static void truncatePostgres(Connection pg) throws SQLException {
        try (Statement st = pg.createStatement()) {
            st.execute("TRUNCATE document_images, reviews, documents RESTART IDENTITY CASCADE");
        }
    }

    private static void migrateDocuments(Connection sqlite, Connection pg) throws SQLException {
        String select = "SELECT id, file_name, original_file_name, book_title, author, book_size, category, pdf_path, summary, created_at, amazon_url FROM documents ORDER BY id";
        String insert = """
            INSERT INTO documents (id, file_name, original_file_name, book_title, author, book_size, category, pdf_path, summary, created_at, amazon_url)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::timestamptz, ?)
            """;

        try (PreparedStatement sel = sqlite.prepareStatement(select);
             ResultSet rs = sel.executeQuery();
             PreparedStatement ins = pg.prepareStatement(insert)) {

            int count = 0;
            while (rs.next()) {
                ins.setLong(1, rs.getLong("id"));
                ins.setString(2, rs.getString("file_name"));
                ins.setString(3, rs.getString("original_file_name"));
                ins.setString(4, rs.getString("book_title"));
                ins.setString(5, rs.getString("author"));
                ins.setString(6, rs.getString("book_size"));
                ins.setString(7, rs.getString("category"));
                ins.setString(8, rs.getString("pdf_path"));
                ins.setString(9, rs.getString("summary"));
                ins.setString(10, rs.getString("created_at"));
                ins.setString(11, rs.getString("amazon_url"));
                ins.executeUpdate();
                count++;
            }
            System.out.println("  Migrated " + count + " documents.");
            updateSequence(pg, "documents");
        }
    }

    private static void migrateReviews(Connection sqlite, Connection pg) throws SQLException {
        String select = "SELECT id, document_id, star_rating, tone, review_title, review_body, notes_for_ai, posted, posted_at, reminder_sent_at, created_at, updated_at FROM reviews ORDER BY id";
        String insert = """
            INSERT INTO reviews (id, document_id, star_rating, tone, review_title, review_body, notes_for_ai, posted, posted_at, reminder_sent_at, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::timestamptz, ?::timestamptz, ?::timestamptz, ?::timestamptz)
            """;

        try (PreparedStatement sel = sqlite.prepareStatement(select);
             ResultSet rs = sel.executeQuery();
             PreparedStatement ins = pg.prepareStatement(insert)) {

            int count = 0;
            while (rs.next()) {
                ins.setLong(1, rs.getLong("id"));
                ins.setLong(2, rs.getLong("document_id"));
                ins.setInt(3, rs.getInt("star_rating"));
                ins.setString(4, rs.getString("tone"));
                ins.setString(5, rs.getString("review_title"));
                ins.setString(6, rs.getString("review_body"));
                ins.setString(7, rs.getString("notes_for_ai"));
                ins.setBoolean(8, rs.getInt("posted") != 0);
                ins.setString(9, rs.getString("posted_at"));
                ins.setString(10, rs.getString("reminder_sent_at"));
                ins.setString(11, rs.getString("created_at"));
                ins.setString(12, rs.getString("updated_at"));
                ins.executeUpdate();
                count++;
            }
            System.out.println("  Migrated " + count + " reviews.");
            updateSequence(pg, "reviews");
        }
    }

    private static void migrateDocumentImages(Connection sqlite, Connection pg) throws SQLException, IOException {
        String select = "SELECT id, document_id, file_path, original_file_name, created_at FROM document_images ORDER BY id";
        String insert = """
            INSERT INTO document_images (id, document_id, large_object_oid, content_type, original_file_name, created_at)
            VALUES (?, ?, ?, ?, ?, ?::timestamptz)
            """;

        var lobjApi = pg.unwrap(org.postgresql.PGConnection.class).getLargeObjectAPI();

        try (PreparedStatement sel = sqlite.prepareStatement(select);
             ResultSet rs = sel.executeQuery();
             PreparedStatement ins = pg.prepareStatement(insert)) {

            int count = 0;
            int skipped = 0;
            while (rs.next()) {
                long id = rs.getLong("id");
                long documentId = rs.getLong("document_id");
                String filePath = rs.getString("file_path");
                String originalFileName = rs.getString("original_file_name");
                String createdAt = rs.getString("created_at");

                byte[] bytes;
                String contentType;
                try {
                    Path path = Path.of(filePath);
                    if (!Files.exists(path)) {
                        System.err.println("  Image file not found, skipping: " + filePath);
                        skipped++;
                        continue;
                    }
                    bytes = Files.readAllBytes(path);
                    contentType = Files.probeContentType(path);
                    if (contentType == null || !contentType.startsWith("image/")) {
                        contentType = guessContentType(originalFileName, filePath);
                    }
                } catch (IOException e) {
                    System.err.println("  Failed to read image " + filePath + ": " + e.getMessage());
                    skipped++;
                    continue;
                }

                long oid = lobjApi.createLO(org.postgresql.largeobject.LargeObjectManager.READWRITE);
                var largeObj = lobjApi.open(oid, org.postgresql.largeobject.LargeObjectManager.WRITE);
                try {
                    largeObj.write(bytes);
                } finally {
                    largeObj.close();
                }

                ins.setLong(1, id);
                ins.setLong(2, documentId);
                ins.setLong(3, oid);
                ins.setString(4, contentType);
                ins.setString(5, originalFileName);
                ins.setString(6, createdAt);
                ins.executeUpdate();
                count++;
            }
            System.out.println("  Migrated " + count + " document images (skipped " + skipped + ").");
            updateSequence(pg, "document_images");
        }
    }

    private static String guessContentType(String originalFileName, String filePath) {
        String name = (originalFileName != null ? originalFileName : filePath).toLowerCase();
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".gif")) return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    private static void updateSequence(Connection pg, String tableName) throws SQLException {
        try (Statement st = pg.createStatement()) {
            st.execute("SELECT setval(pg_get_serial_sequence('" + tableName + "', 'id'), (SELECT COALESCE(MAX(id), 1) FROM " + tableName + "))");
        }
    }
}
