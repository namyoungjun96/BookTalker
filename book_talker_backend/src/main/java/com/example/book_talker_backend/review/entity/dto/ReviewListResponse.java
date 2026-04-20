package com.example.book_talker_backend.review.entity.dto;

import com.example.book_talker_backend.review.entity.Review;

import java.time.LocalDateTime;

public record ReviewListResponse(
        Long id,
        String headline,
        Integer rating,
        LocalDateTime regDate,
        String bookTitle,
        String bookCover,
        String bookIsbn13
) {
    public static ReviewListResponse from(Review review) {
        return new ReviewListResponse(
                review.getId(),
                review.getHeadline(),
                review.getRating(),
                review.getRegDate(),
                review.getBook() != null ? review.getBook().getTitle() : null,
                review.getBook() != null ? review.getBook().getCover() : null,
                review.getBook() != null ? review.getBook().getIsbn13() : null
        );
    }
}
