package com.example.book_talker_backend.review.entity.dto;


public record BookRatingStats (
    double avgRating,
    int reviewCount
) {
}
