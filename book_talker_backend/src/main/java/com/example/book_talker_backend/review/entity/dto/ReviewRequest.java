package com.example.book_talker_backend.review.entity.dto;

import com.example.book_talker_backend.review.entity.Review;

import java.time.LocalDateTime;

public record ReviewRequest (
        Long reviewId,   // update 시 필수, insert 시 null
        String isbn13,
        String writer,
        String headline,
        String content,
        Integer rating,
        Boolean isPublic
) {
    public Review to() {
        Review review = new Review();
        review.setWriter(this.writer);
        review.setHeadline(this.headline);
        review.setContent(this.content);
        review.setRating(this.rating);
        review.setIsPublic(this.isPublic != null ? this.isPublic : false);
        review.setRegDate(LocalDateTime.now());
        return review;
    }
}
