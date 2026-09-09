package com.example.book_talker_backend.book.cache;

public record SearchCacheKey (
        String normalizedQuery,
        int page
) {
    public SearchCacheKey {
        normalizedQuery = normalizedQuery.trim().replaceAll("\\s+", " ").toLowerCase();
        if (page < 1) {
            throw new IllegalArgumentException("page must be >= 1, but was: " + page);
        }
    }
}
