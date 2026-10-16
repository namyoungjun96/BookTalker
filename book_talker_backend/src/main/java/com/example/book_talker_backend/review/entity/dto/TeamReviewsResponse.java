package com.example.book_talker_backend.review.entity.dto;

import java.util.List;

public record TeamReviewsResponse(
        long totalCount,
        int page,
        int size,
        int totalPages,
        List<TeamReviewItem> reviews
) {
}
