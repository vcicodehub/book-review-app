package com.example.pdfreview.dto;

import jakarta.validation.constraints.NotBlank;

public record ShortenReviewRequest(
        String reviewTitle,
        @NotBlank(message = "Review body is required") String reviewBody
) {}
