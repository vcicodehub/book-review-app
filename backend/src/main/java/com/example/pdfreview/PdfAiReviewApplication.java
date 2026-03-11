package com.example.pdfreview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
@EnableScheduling
public class PdfAiReviewApplication {

    public static void main(String[] args) {
        ensureDataDirectoriesExist();
        SpringApplication.run(PdfAiReviewApplication.class, args);
    }

    private static void ensureDataDirectoriesExist() {
        try {
            Files.createDirectories(Path.of("data"));
            Files.createDirectories(Path.of("data", "uploads"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create data directories", e);
        }
    }
}
