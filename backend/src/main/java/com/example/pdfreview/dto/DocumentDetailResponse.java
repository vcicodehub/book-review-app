package com.example.pdfreview.dto;

import com.example.pdfreview.model.ReviewRecord;

import java.util.List;

public record DocumentDetailResponse(
        Long id,
        String fileName,
        String originalFileName,
        String bookTitle,
        String author,
        String bookSize,
        String category,
        String summary,
        String createdAt,
        ReviewRecord review,
        List<DocumentImageResponse> images,
        String amazonUrl
) {
}
