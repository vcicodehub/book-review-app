package com.example.pdfreview.model;

public record DocumentRecord(
        Long id,
        String fileName,
        String originalFileName,
        String bookTitle,
        String author,
        String bookSize,
        String category,
        String pdfPath,
        String summary,
        String createdAt,
        String amazonUrl
) {
}
