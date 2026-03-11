package com.example.pdfreview.model;

public record ReviewRecord(
        Long id,
        Long documentId,
        Integer starRating,
        String tone,
        String reviewTitle,
        String reviewBody,
        String notesForAi,
        Boolean posted,
        String postedAt,
        String reminderSentAt,
        String createdAt,
        String updatedAt
) {
}
