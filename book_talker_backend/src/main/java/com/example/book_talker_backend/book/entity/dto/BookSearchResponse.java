package com.example.book_talker_backend.book.entity.dto;

import java.util.List;

public record BookSearchResponse(
        List<AladinBook> items,
        String totalResults,
        long startIndex
) {
}
