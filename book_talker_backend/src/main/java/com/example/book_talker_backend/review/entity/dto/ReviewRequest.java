package com.example.book_talker_backend.review.entity.dto;

import com.example.book_talker_backend.review.entity.Review;

import java.time.LocalDate;

public record ReviewRequest (
        String isbn,
        String title,
        String writer,
        String content,
        String rating
) {
    public Review to() {
        Review review = new Review();
        review.setWriter(this.writer);
        review.setContent(this.content);
        review.setRating(this.rating);
        review.setRegDate(LocalDate.now());
        return review;
    }
}
