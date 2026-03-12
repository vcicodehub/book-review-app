package com.example.pdfreview.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SchemaMigration {

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        // Schema is managed externally; no runtime migrations.
    }
}
