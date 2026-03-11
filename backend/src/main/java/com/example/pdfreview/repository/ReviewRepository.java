package com.example.pdfreview.repository;

import com.example.pdfreview.model.ReviewRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ReviewRepository {

    private final JdbcTemplate jdbcTemplate;

    public ReviewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<ReviewRecord> findByDocumentId(Long documentId) {
        List<ReviewRecord> results = jdbcTemplate.query(
                "select id, document_id, star_rating, tone, review_title, review_body, notes_for_ai, posted, posted_at, reminder_sent_at, created_at, updated_at from reviews where document_id = ?",
                this::mapRow,
                documentId
        );
        return results.stream().findFirst();
    }

    public Optional<ReviewRecord> findById(Long id) {
        List<ReviewRecord> results = jdbcTemplate.query(
                "select id, document_id, star_rating, tone, review_title, review_body, notes_for_ai, posted, posted_at, reminder_sent_at, created_at, updated_at from reviews where id = ?",
                this::mapRow,
                id
        );
        return results.stream().findFirst();
    }

    public ReviewRecord save(ReviewRecord review) {
        String now = OffsetDateTime.now().toString();
        if (review.id() == null) {
            Long id = jdbcTemplate.queryForObject(
                    """
                    insert into reviews (document_id, star_rating, tone, review_title, review_body, notes_for_ai, posted, posted_at, reminder_sent_at, created_at, updated_at)
                    values (?, ?, ?, ?, ?, ?, ?, ?::timestamptz, ?::timestamptz, ?::timestamptz, ?::timestamptz)
                    returning id
                    """,
                    Long.class,
                    review.documentId(),
                    review.starRating(),
                    review.tone(),
                    review.reviewTitle(),
                    review.reviewBody(),
                    review.notesForAi(),
                    Boolean.TRUE.equals(review.posted()),
                    review.postedAt(),
                    review.reminderSentAt(),
                    now,
                    now
            );
            return findById(id).orElseThrow();
        }

        jdbcTemplate.update(
                """
                update reviews
                set star_rating = ?, tone = ?, review_title = ?, review_body = ?, notes_for_ai = ?, posted = ?, posted_at = ?::timestamptz, reminder_sent_at = ?::timestamptz, updated_at = ?::timestamptz
                where id = ?
                """,
                review.starRating(),
                review.tone(),
                review.reviewTitle(),
                review.reviewBody(),
                review.notesForAi(),
                Boolean.TRUE.equals(review.posted()),
                review.postedAt(),
                review.reminderSentAt(),
                now,
                review.id()
        );
        return findById(review.id()).orElseThrow();
    }

    private ReviewRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ReviewRecord(
                rs.getLong("id"),
                rs.getLong("document_id"),
                rs.getInt("star_rating"),
                rs.getString("tone"),
                rs.getString("review_title"),
                rs.getString("review_body"),
                rs.getString("notes_for_ai"),
                rs.getBoolean("posted"),
                rs.getObject("posted_at") != null ? rs.getTimestamp("posted_at").toInstant().toString() : null,
                rs.getObject("reminder_sent_at") != null ? rs.getTimestamp("reminder_sent_at").toInstant().toString() : null,
                rs.getObject("created_at") != null ? rs.getTimestamp("created_at").toInstant().toString() : null,
                rs.getObject("updated_at") != null ? rs.getTimestamp("updated_at").toInstant().toString() : null
        );
    }

    public List<ReviewRecord> findUnpostedOlderThanDays(int days) {
        return jdbcTemplate.query(
                """
                select id, document_id, star_rating, tone, review_title, review_body, notes_for_ai, posted, posted_at, reminder_sent_at, created_at, updated_at
                from reviews
                where posted = false and reminder_sent_at is null
                and created_at <= now() - make_interval(days => ?)
                """,
                this::mapRow,
                days
        );
    }

    public void markReminderSent(Long reviewId) {
        jdbcTemplate.update(
                "update reviews set reminder_sent_at = ?::timestamptz where id = ?",
                OffsetDateTime.now().toString(),
                reviewId
        );
    }

    public void deleteById(Long id) {
        jdbcTemplate.update("delete from reviews where id = ?", id);
    }

    public void deleteByDocumentId(Long documentId) {
        jdbcTemplate.update("delete from reviews where document_id = ?", documentId);
    }
}
