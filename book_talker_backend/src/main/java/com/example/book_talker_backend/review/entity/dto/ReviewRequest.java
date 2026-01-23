package com.example.book_talker_backend.review.entity.dto;

import com.example.book_talker_backend.review.entity.Review;
import com.example.book_talker_backend.book.entity.Book;

import java.time.LocalDateTime;

public record ReviewRequest (
        String isbn13,
        String writer,
        String content,
        String rating
) {
    public Review to() {
        Review review = new Review();
        review.setBook(new Book(this.isbn13));
        review.setWriter(this.writer);
        review.setContent(this.content);
        review.setRating(this.rating);
        review.setRegDate(LocalDateTime.now());
        return review;
    }
}
