package com.example.pdfreview.service;

import com.example.pdfreview.dto.GenerateReviewResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final int summaryMaxChars;

    public AiService(
            @Value("${app.ai.base-url}") String baseUrl,
            @Value("${app.ai.api-key}") String apiKey,
            @Value("${app.ai.model}") String model,
            @Value("${app.ai.summary-max-chars}") int summaryMaxChars
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.summaryMaxChars = summaryMaxChars;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public String summarize(String extractedText) {
        String prompt = """
                Summarize the following book content for a reader who wants to understand the book quickly.
                The content may be plain text, markdown, or extracted from PDF—summarize it regardless of format.
                Write a concise but useful summary in plain English.
                Include the main ideas, audience, and overall impression.
                Keep the response under 350 words.

                Book content:
                %s
                """.formatted(truncate(extractedText));

        return callTextModel(prompt);
    }

    public GenerateReviewResponse generateReview(String summary, int starRating, String tone, String category, String notes) {
        return generateReview(summary, starRating, tone, category, notes, List.of());
    }

    public GenerateReviewResponse generateReview(String summary, int starRating, String tone, String category, String notes, List<ImageData> imageData) {
        String normalizedTone = StringUtils.hasText(tone) ? tone.trim() : "balanced";
        String categoryGuidance = getCategoryGuidance(category);
        String notesSection = StringUtils.hasText(notes)
                ? "\n\nThe reader provided these notes about the book. Incorporate these points into the review:\n\n" + notes.trim() + "\n\n"
                : "";

        String imageGuidance = !imageData.isEmpty()
                ? "\n\nThe user has also shared screenshots or images related to the book (e.g. Kindle highlights, passages, or illustrations). Use the content visible in these images to enrich the review with specific details, quotes, or observations.\n\n"
                : "";

        String prompt = """
                Using the summary below, write a realistic Amazon customer review.
                Match a %d-star sentiment.
                Tone: %s.
                %s
                Make it natural, specific, believable, and not overly promotional.
                Return valid JSON only with this exact shape:
                {"reviewTitle":"...","reviewBody":"..."}
                %s

                Summary:
                %s%s
                """.formatted(starRating, normalizedTone, categoryGuidance, imageGuidance, summary, notesSection);

        String raw;
        if (imageData.isEmpty()) {
            raw = callTextModel(prompt).trim();
        } else {
            raw = callVisionModel(prompt, imageData).trim();
        }
        return parseJsonFallback(raw, starRating);
    }

    private String getCategoryGuidance(String category) {
        if (category == null || category.isBlank()) {
            return "Review it as a book.";
        }
        return switch (category.trim().toLowerCase()) {
            case "book" -> "Review it as a book.";
            case "puzzles" -> "Review it as a product that contains puzzles.";
            case "activity" -> "Review it as an activity book with various activities for children.";
            case "coloring" -> "Review it as a coloring book with pictures that kids can color in.";
            default -> "Review it as a book.";
        };
    }

    public GenerateReviewResponse shortenReview(String reviewTitle, String reviewBody) {
        String prompt = """
                Shorten this Amazon customer review. Keep the same sentiment and key points, but make it more concise.
                Preserve the tone and star-rating feel. Return valid JSON only with this exact shape:
                {"reviewTitle":"...","reviewBody":"..."}

                Current title: %s

                Current review body:
                %s
                """.formatted(
                StringUtils.hasText(reviewTitle) ? reviewTitle : "(no title)",
                reviewBody
        );

        String raw = callTextModel(prompt).trim();
        return parseJsonFallback(raw, 4);
    }

    public GenerateReviewResponse humanizeReview(String reviewTitle, String reviewBody) {
        String prompt = """
                Rewrite this Amazon customer review to sound more like a normal human wrote it—less polished, more imperfect.
                Add casual phrasing, minor imperfections, or conversational touches. Keep the same sentiment and key points.
                It should feel like a real person typing quickly, not a professional writer. Return valid JSON only with this exact shape:
                {"reviewTitle":"...","reviewBody":"..."}

                Current title: %s

                Current review body:
                %s
                """.formatted(
                StringUtils.hasText(reviewTitle) ? reviewTitle : "(no title)",
                reviewBody
        );

        String raw = callTextModel(prompt).trim();
        return parseJsonFallback(raw, 4);
    }

    private String callVisionModel(String prompt, List<ImageData> imageData) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured.");
        }

        List<Object> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text", prompt));

        for (ImageData img : imageData) {
            String base64 = Base64.getEncoder().encodeToString(img.content());
            String mimeType = img.contentType();
            if (mimeType == null || !mimeType.startsWith("image/")) {
                mimeType = "image/jpeg";
            }
            String dataUrl = "data:" + mimeType + ";base64," + base64;
            content.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", dataUrl)
            ));
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("messages", List.of(
                Map.of("role", "system", "content", "You are a helpful assistant for summarizing books (from PDF, Kindle, or text) and drafting customer reviews. You can analyze images such as book screenshots or Kindle highlights to create more detailed, personalized reviews."),
                Map.of("role", "user", "content", content)
        ));
        payload.put("temperature", 0.7);

        Map<?, ?> response = restClient.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(payload)
                .retrieve()
                .body(Map.class);

        return extractContentFromResponse(response);
    }

    private String callTextModel(String prompt) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured.");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("messages", List.of(
                Map.of("role", "system", "content", "You are a helpful assistant for summarizing books (from PDF, Kindle, or text) and drafting customer reviews."),
                Map.of("role", "user", "content", prompt)
        ));
        payload.put("temperature", 0.7);

        Map<?, ?> response = restClient.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(payload)
                .retrieve()
                .body(Map.class);

        return extractContentFromResponse(response);
    }

    private String extractContentFromResponse(Map<?, ?> response) {
        Object choicesObject = response == null ? null : response.get("choices");
        if (!(choicesObject instanceof List<?> choices) || choices.isEmpty()) {
            throw new IllegalStateException("AI response did not contain choices.");
        }

        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> choiceMap)) {
            throw new IllegalStateException("AI response choice had an unexpected shape.");
        }

        Object messageObject = choiceMap.get("message");
        if (!(messageObject instanceof Map<?, ?> messageMap)) {
            throw new IllegalStateException("AI response message had an unexpected shape.");
        }

        Object content = messageMap.get("content");
        if (!(content instanceof String text) || text.isBlank()) {
            throw new IllegalStateException("AI response did not contain text content.");
        }

        return text.trim();
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private GenerateReviewResponse parseJsonFallback(String raw, int starRating) {
        String cleaned = raw.replace("```json", "").replace("```", "").trim();
        String title = null;
        String body = null;

        try {
            JsonNode root = OBJECT_MAPPER.readTree(cleaned);
            if (root != null) {
                JsonNode titleNode = root.get("reviewTitle");
                JsonNode bodyNode = root.get("reviewBody");
                if (titleNode != null && titleNode.isTextual()) {
                    title = titleNode.asText();
                }
                if (bodyNode != null && bodyNode.isTextual()) {
                    body = bodyNode.asText();
                }
            }
        } catch (Exception ignored) {
            // Fall through to fallback
        }

        if (StringUtils.hasText(title) && StringUtils.hasText(body)) {
            return new GenerateReviewResponse(title, body);
        }

        return new GenerateReviewResponse(
                starRating >= 4 ? "Worth checking out" : "A mixed reading experience",
                cleaned
        );
    }

    private String truncate(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.length() <= summaryMaxChars ? text : text.substring(0, summaryMaxChars);
    }
}
