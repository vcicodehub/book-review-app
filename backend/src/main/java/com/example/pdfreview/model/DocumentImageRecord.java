package com.example.pdfreview.model;

public record DocumentImageRecord(
        Long id,
        Long documentId,
        Long largeObjectOid,
        String contentType,
        String originalFileName,
        String createdAt
) {
}
