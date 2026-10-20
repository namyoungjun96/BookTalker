package com.example.book_talker_backend.review.entity.dto;

import com.example.book_talker_backend.review.entity.ReviewVisibilityEnum;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record TeamReviewDetailResponse(
        Long reviewId,
        String headline,
        String content,
        String author,
        Integer rating,
        Integer readingCount,
        ReviewVisibilityEnum visibility,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDateTime regDate,
        String bookTitle,
        String cover,
        String teamName,
        String displayName,
        Boolean mine
) {
}
