package com.example.book_talker_backend.book.entity.dto;

public record BookRequest (
    String isbn13,
    String title,
    String author,
    String genre,
    String publisher,
    String cover
) {
}
