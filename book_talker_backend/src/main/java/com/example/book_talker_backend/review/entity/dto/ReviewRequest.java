package com.example.book_talker_backend.review.entity.dto;

import com.example.book_talker_backend.review.entity.Review;
import com.example.book_talker_backend.review.entity.ReviewVisibilityEnum;

import java.time.LocalDateTime;

public record ReviewRequest (
        Long reviewId,   // update 시 필수, insert 시 null
        String isbn13,
        String writer,
        String headline,
        String content,
        Integer rating,
        ReviewVisibilityEnum visibility
) {
    public Review to() {
        if (visibility == null) {
            throw new IllegalArgumentException("공개 범위는 필수입니다.");
        }

        Review review = new Review();
        review.setWriter(this.writer);
        review.setHeadline(this.headline);
        review.setContent(this.content);
        review.setRating(this.rating);
        review.setVisibility(this.visibility);
        review.setRegDate(LocalDateTime.now());
        return review;
    }
}
