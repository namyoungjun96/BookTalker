package com.example.book_talker_backend.review.entity.dto;

import com.example.book_talker_backend.review.entity.Review;

import java.time.LocalDateTime;

public record PublicReviewResponse(
        Long id,
        String headline,
        String content,
        Integer rating,
        LocalDateTime regDate
) {
    public static PublicReviewResponse from(Review review) {
        return new PublicReviewResponse(
                review.getId(),
                review.getHeadline(),
                review.getContent(),
                review.getRating(),
                review.getRegDate()
        );
    }
}
