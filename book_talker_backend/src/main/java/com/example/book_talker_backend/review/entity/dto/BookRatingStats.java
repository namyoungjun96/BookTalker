package com.example.book_talker_backend.review.entity.dto;


public record BookRatingStats (
    String isbn13,
    String genre,
    String title,
    String cover,
    double avgRating,
    int reviewCount
) {
}
