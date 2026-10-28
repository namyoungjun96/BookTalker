package com.example.book_talker_backend.book.infrastructure;

public record BookSearchItem (
        String isbn13,
        String title,
        String author,
        String publisher,
        String cover
) {
}
