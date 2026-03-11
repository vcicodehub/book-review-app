package com.example.pdfreview.dto;

public record ImportKindleRequest(
        String filePath,
        String bookTitle,
        String author,
        String bookSize,
        String amazonUrl
) {}
