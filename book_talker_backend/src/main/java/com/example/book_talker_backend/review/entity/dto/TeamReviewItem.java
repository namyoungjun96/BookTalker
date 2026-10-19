package com.example.book_talker_backend.review.entity.dto;

import com.example.book_talker_backend.review.entity.ReviewVisibilityEnum;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record TeamReviewItem(
        Long reviewId,
        String headline,
        String summary,
        String bookTitle,
        String cover,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDateTime regDate,
        String displayName,
        ReviewVisibilityEnum visibility
) {
    private static final int SUMMARY_LENGTH = 40;

    public TeamReviewItem {
        summary = summarize(summary);
    }

    // review의 content를 받아, summary로 바꾼다.
    public static String summarize(String content) {
        if(content.codePointCount(0, content.length()) > SUMMARY_LENGTH)
            return content.substring(0, content.offsetByCodePoints(0, SUMMARY_LENGTH)) + "...";

        return content;
    }
}
