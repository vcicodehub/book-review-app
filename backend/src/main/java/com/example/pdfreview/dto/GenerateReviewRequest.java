package com.example.pdfreview.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GenerateReviewRequest(
        @NotNull Long documentId,
        @NotNull @Min(1) @Max(5) Integer starRating,
        String tone,
        String summary,
        String notes
) {
}
