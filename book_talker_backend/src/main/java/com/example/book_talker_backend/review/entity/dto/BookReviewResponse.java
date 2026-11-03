package com.example.book_talker_backend.review.entity.dto;

import com.example.book_talker_backend.review.entity.Review;
import com.example.book_talker_backend.review.entity.ReviewVisibilityEnum;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record BookReviewResponse(
        Long reviewId,
        String headline,
        String content,
        Integer rating,
        Integer readingCount,
        ReviewVisibilityEnum visibility,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDateTime regDate,
        String bookTitle,
        String author,
        String cover
) {
    public static BookReviewResponse from(Review review) {
        return new BookReviewResponse(
                review.getId(),
                review.getHeadline(),
                review.getContent(),
                review.getRating(),
                review.getReadingCount(),
                review.getVisibility(),
                review.getRegDate(),
                review.getBook().getTitle(),
                review.getBook().getAuthor(),
                review.getBook().getCover()
        );
    }
}
