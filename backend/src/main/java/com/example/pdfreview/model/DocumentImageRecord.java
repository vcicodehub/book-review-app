package com.example.pdfreview.model;

public record DocumentImageRecord(
        Long id,
        Long documentId,
        String filePath,
        String originalFileName,
        String createdAt
) {
}
