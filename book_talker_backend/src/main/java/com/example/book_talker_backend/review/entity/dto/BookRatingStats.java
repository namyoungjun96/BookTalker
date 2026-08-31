package com.example.book_talker_backend.review.entity.dto;

import lombok.Getter;

@Getter
public class BookRatingStats {
    String isbn13;
    double avgRating;
    int reviewCount;

    public BookRatingStats(String isbn13, double avgRating, int reviewCount) {
        this.isbn13 = isbn13;
        this.avgRating = avgRating;
        this.reviewCount = reviewCount;
    }
}
