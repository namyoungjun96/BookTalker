package com.example.book_talker_backend.book.infrastructure;

import java.util.Optional;

public final class IsbnParser {
    private static final int ISBN13_LENGTH = 13;
    private static final int ISBN10_LENGTH = 10;

    public static Optional<String> toIsbn13(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        String[] isbnArr = raw.trim().split("\\s+");

        for (String isbn : isbnArr) {
            if (isbn.length() == ISBN13_LENGTH) return Optional.of(isbn);
        }

        for (String isbn : isbnArr) {
            if (isbn.length() == ISBN10_LENGTH) return Optional.of(convertIsbn10To13(isbn));
        }

        return Optional.empty();
    }

    public static String convertIsbn10To13(String isbn10) {
        String body = "978" + isbn10.substring(0, 9);

        int sum = 0;
        for (int i = 0; i < body.length(); i++) {
            int digit = body.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }

        return body + (10 - sum % 10) % 10;
    }
}
