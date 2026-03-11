package com.example.pdfreview.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaMigration {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        addColumnIfMissing("reviews", "posted", "integer not null default 0");
        addColumnIfMissing("reviews", "posted_at", "text");
        addColumnIfMissing("reviews", "reminder_sent_at", "text");
        addColumnIfMissing("reviews", "notes_for_ai", "text");
        addColumnIfMissing("documents", "category", "text");
        addColumnIfMissing("documents", "book_size", "text");
        addColumnIfMissing("documents", "amazon_url", "text");
        createDocumentImagesTableIfMissing();
    }

    private void createDocumentImagesTableIfMissing() {
        try {
            jdbcTemplate.execute("""
                create table if not exists document_images (
                  id integer primary key autoincrement,
                  document_id integer not null,
                  file_path text not null,
                  original_file_name text,
                  created_at text not null,
                  foreign key(document_id) references documents(id)
                )
                """);
            log.info("Created document_images table if it did not exist");
        } catch (Exception e) {
            log.warn("Could not create document_images table: {}", e.getMessage());
        }
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        try {
            jdbcTemplate.execute("alter table %s add column %s %s".formatted(table, column, definition));
            log.info("Added column {}.{}", table, column);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("duplicate column name")) {
                log.debug("Column {}.{} already exists", table, column);
            } else {
                throw e;
            }
        }
    }
}
