package com.example.pdfreview.dto;

public record SaveKindleRequest(
        String bookTitle,
        String author,
        String bookSize,
        String amazonUrl
) {
}
