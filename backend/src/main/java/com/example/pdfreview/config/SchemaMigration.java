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
        addColumnIfMissing("reviews", "posted", "boolean not null default false");
        addColumnIfMissing("reviews", "posted_at", "timestamptz");
        addColumnIfMissing("reviews", "reminder_sent_at", "timestamptz");
        addColumnIfMissing("reviews", "notes_for_ai", "text");
        addColumnIfMissing("documents", "category", "text");
        addColumnIfMissing("documents", "book_size", "text");
        addColumnIfMissing("documents", "amazon_url", "text");
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        try {
            jdbcTemplate.execute("ALTER TABLE %s ADD COLUMN %s %s".formatted(table, column, definition));
            log.info("Added column {}.{}", table, column);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (msg.contains("already exists") || msg.contains("duplicate column")) {
                log.debug("Column {}.{} already exists", table, column);
            } else {
                throw e;
            }
        }
    }
}
