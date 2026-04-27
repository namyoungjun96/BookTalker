package com.example.book_talker_backend.review.entity.dto;

import com.example.book_talker_backend.review.entity.Review;

import java.time.LocalDateTime;

public record ReviewDetailResponse(
        Long id,
        String headline,
        String content,
        Integer rating,
        Integer readingCount,
        Boolean isPublic,
        LocalDateTime regDate,
        LocalDateTime modDate,
        String bookTitle,
        String bookCover,
        String bookIsbn13
) {
    public static ReviewDetailResponse from(Review review) {
        return new ReviewDetailResponse(
                review.getId(),
                review.getHeadline(),
                review.getContent(),
                review.getRating(),
                review.getReadingCount(),
                review.getIsPublic(),
                review.getRegDate(),
                review.getModDate(),
                review.getBook() != null ? review.getBook().getTitle() : null,
                review.getBook() != null ? review.getBook().getCover() : null,
                review.getBook() != null ? review.getBook().getIsbn13() : null
        );
    }
}
