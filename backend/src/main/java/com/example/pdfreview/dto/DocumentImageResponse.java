package com.example.pdfreview.dto;

public record DocumentImageResponse(
        Long id,
        Long documentId,
        String originalFileName,
        String createdAt
) {
}
