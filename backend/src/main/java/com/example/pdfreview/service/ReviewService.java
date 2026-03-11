package com.example.pdfreview.service;

import com.example.pdfreview.dto.GenerateReviewRequest;
import com.example.pdfreview.dto.GenerateReviewResponse;
import com.example.pdfreview.dto.SaveReviewRequest;
import com.example.pdfreview.dto.ShortenReviewRequest;
import com.example.pdfreview.model.ReviewRecord;
import com.example.pdfreview.repository.DocumentRepository;
import com.example.pdfreview.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final DocumentRepository documentRepository;
    private final DocumentImageService documentImageService;
    private final AiService aiService;

    public ReviewService(ReviewRepository reviewRepository, DocumentRepository documentRepository, DocumentImageService documentImageService, AiService aiService) {
        this.reviewRepository = reviewRepository;
        this.documentRepository = documentRepository;
        this.documentImageService = documentImageService;
        this.aiService = aiService;
    }

    public GenerateReviewResponse generate(GenerateReviewRequest request) {
        var document = documentRepository.findById(request.documentId())
                .orElseThrow(() -> new IllegalArgumentException("Document not found for id " + request.documentId()));

        String summary = request.summary() != null && !request.summary().isBlank()
                ? request.summary()
                : document.summary();

        List<ImageData> imageData;
        try {
            imageData = documentImageService.getImageDataForDocument(request.documentId());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read document images", e);
        }
        return aiService.generateReview(summary, request.starRating(), request.tone(), document.category(), request.notes(), imageData);
    }

    public GenerateReviewResponse shorten(ShortenReviewRequest request) {
        return aiService.shortenReview(request.reviewTitle(), request.reviewBody());
    }

    public GenerateReviewResponse humanize(ShortenReviewRequest request) {
        return aiService.humanizeReview(request.reviewTitle(), request.reviewBody());
    }

    public ReviewRecord save(SaveReviewRequest request) {
        documentRepository.findById(request.documentId())
                .orElseThrow(() -> new IllegalArgumentException("Document not found for id " + request.documentId()));

        ReviewRecord existing = reviewRepository.findByDocumentId(request.documentId()).orElse(null);
        boolean posted = Boolean.TRUE.equals(request.posted());
        String postedAt = posted ? OffsetDateTime.now().toString() : null;
        return reviewRepository.save(new ReviewRecord(
                existing == null ? null : existing.id(),
                request.documentId(),
                request.starRating(),
                request.tone(),
                request.reviewTitle(),
                request.reviewBody(),
                request.notesForAi(),
                posted,
                postedAt,
                existing != null ? existing.reminderSentAt() : null,
                existing == null ? OffsetDateTime.now().toString() : existing.createdAt(),
                OffsetDateTime.now().toString()
        ));
    }

    public ReviewRecord update(Long reviewId, SaveReviewRequest request) {
        ReviewRecord existing = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found for id " + reviewId));

        boolean posted = Boolean.TRUE.equals(request.posted());
        String postedAt = posted ? OffsetDateTime.now().toString() : null;
        return reviewRepository.save(new ReviewRecord(
                existing.id(),
                existing.documentId(),
                request.starRating(),
                request.tone(),
                request.reviewTitle(),
                request.reviewBody(),
                request.notesForAi(),
                posted,
                posted ? postedAt : null,
                existing.reminderSentAt(),
                existing.createdAt(),
                OffsetDateTime.now().toString()
        ));
    }

    public ReviewRecord markAsPosted(Long reviewId) {
        ReviewRecord existing = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found for id " + reviewId));
        String postedAt = OffsetDateTime.now().toString();
        return reviewRepository.save(new ReviewRecord(
                existing.id(),
                existing.documentId(),
                existing.starRating(),
                existing.tone(),
                existing.reviewTitle(),
                existing.reviewBody(),
                existing.notesForAi(),
                true,
                postedAt,
                existing.reminderSentAt(),
                existing.createdAt(),
                OffsetDateTime.now().toString()
        ));
    }

    public void delete(Long reviewId) {
        reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found for id " + reviewId));
        reviewRepository.deleteById(reviewId);
    }
}
