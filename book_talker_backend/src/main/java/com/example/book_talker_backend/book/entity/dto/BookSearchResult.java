package com.example.book_talker_backend.book.entity.dto;

import java.util.List;

public record BookSearchResult (
        List<BookSearchItem> items,
        long totalResults,
        long startIndex,
        boolean hasNext
) {
}
