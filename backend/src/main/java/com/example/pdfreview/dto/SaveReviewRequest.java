package com.example.pdfreview.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SaveReviewRequest(
        @NotNull Long documentId,
        @NotNull @Min(1) @Max(5) Integer starRating,
        String tone,
        String reviewTitle,
        @NotBlank String reviewBody,
        String notesForAi,
        Boolean posted
) {
}
