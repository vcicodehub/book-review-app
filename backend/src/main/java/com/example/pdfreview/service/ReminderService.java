package com.example.pdfreview.service;

import com.example.pdfreview.model.DocumentRecord;
import com.example.pdfreview.model.ReviewRecord;
import com.example.pdfreview.repository.DocumentRepository;
import com.example.pdfreview.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@ConditionalOnBean(org.springframework.mail.javamail.JavaMailSender.class)
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);
    private static final int REMINDER_DAYS = 4;

    private final ReviewRepository reviewRepository;
    private final DocumentRepository documentRepository;
    private final JavaMailSender mailSender;

    @Value("${app.reminder.recipient:}")
    private String recipientEmail;

    @Value("${app.reminder.enabled:false}")
    private boolean reminderEnabled;

    public ReminderService(
            ReviewRepository reviewRepository,
            DocumentRepository documentRepository,
            JavaMailSender mailSender
    ) {
        this.reviewRepository = reviewRepository;
        this.documentRepository = documentRepository;
        this.mailSender = mailSender;
    }

    @Scheduled(cron = "${app.reminder.cron:0 0 9 * * ?}")
    public void sendUnpostedReminders() {
        if (!reminderEnabled || !StringUtils.hasText(recipientEmail)) {
            log.debug("Reminder skipped: enabled={}, recipient configured={}", reminderEnabled, StringUtils.hasText(recipientEmail));
            return;
        }

        List<ReviewRecord> reviews = reviewRepository.findUnpostedOlderThanDays(REMINDER_DAYS);
        if (reviews.isEmpty()) {
            return;
        }

        for (ReviewRecord review : reviews) {
            try {
                documentRepository.findById(review.documentId()).ifPresent(doc -> {
                    sendReminderEmail(review, doc);
                    reviewRepository.markReminderSent(review.id());
                });
            } catch (Exception e) {
                log.warn("Failed to send reminder for review {}: {}", review.id(), e.getMessage());
            }
        }
    }

    private void sendReminderEmail(ReviewRecord review, DocumentRecord document) {
        String bookTitle = StringUtils.hasText(document.bookTitle()) ? document.bookTitle() : document.originalFileName();
        if (bookTitle == null) {
            bookTitle = "Untitled";
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setSubject("Reminder: Post your review for \"" + bookTitle + "\"");
        message.setText("""
                You saved a review for "%s" more than %d days ago but haven't posted it to Amazon yet.

                Review title: %s

                Don't forget to post your review when you get a chance!
                """.formatted(bookTitle, REMINDER_DAYS, review.reviewTitle() != null ? review.reviewTitle() : "(no title)"));

        mailSender.send(message);
        log.info("Sent reminder for review {} (document: {})", review.id(), document.id());
    }
}
