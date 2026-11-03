package com.example.book_talker_backend.review.entity.dto;

import com.example.book_talker_backend.review.entity.Review;
import com.example.book_talker_backend.review.entity.ReviewVisibilityEnum;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record BookReviewListResponse(
        Long id,
        String headline,
        Integer rating,
        Integer readingCount,
        ReviewVisibilityEnum visibility,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDateTime regDate
) {
    public static BookReviewListResponse from(Review review) {
        return new BookReviewListResponse(
                review.getId(),
                review.getHeadline(),
                review.getRating(),
                review.getReadingCount(),
                review.getVisibility(),
                review.getRegDate()
        );
    }
}
