package com.example.book_talker_backend.book.entity.dto;

import java.util.List;

public record AladinResponse(
        String version,
        String logo,
        String title,
        String link,
        String pubDate,
        String totalResults,
        long startIndex,
        long itemsPerPage,
        String query,
        String QueryType,
        String SearchTarget,
        long searchCategoryId,
        String searchCategoryName,
        List<AladinBook> item
) {
}
