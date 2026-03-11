package com.example.pdfreview.controller;

import com.example.pdfreview.dto.GenerateReviewRequest;
import com.example.pdfreview.dto.GenerateReviewResponse;
import com.example.pdfreview.dto.SaveReviewRequest;
import com.example.pdfreview.dto.ShortenReviewRequest;
import com.example.pdfreview.model.ReviewRecord;
import com.example.pdfreview.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/generate")
    public GenerateReviewResponse generate(@Valid @RequestBody GenerateReviewRequest request) {
        return reviewService.generate(request);
    }

    @PostMapping("/shorten")
    public GenerateReviewResponse shorten(@Valid @RequestBody ShortenReviewRequest request) {
        return reviewService.shorten(request);
    }

    @PostMapping("/humanize")
    public GenerateReviewResponse humanize(@Valid @RequestBody ShortenReviewRequest request) {
        return reviewService.humanize(request);
    }

    @PostMapping
    public ReviewRecord save(@Valid @RequestBody SaveReviewRequest request) {
        return reviewService.save(request);
    }

    @PutMapping("/{id}")
    public ReviewRecord update(@PathVariable Long id, @Valid @RequestBody SaveReviewRequest request) {
        return reviewService.update(id, request);
    }

    @PutMapping("/{id}/posted")
    public ReviewRecord markAsPosted(@PathVariable Long id) {
        return reviewService.markAsPosted(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        reviewService.delete(id);
    }
}
