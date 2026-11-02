package com.example.book_talker_backend.book.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book {
    @Id
    private String isbn13;
    private String title;
    private String author;
    @Column(nullable = false)
    private String genre;
    private String publisher;
    private String cover;

    private static final String UNCLASSIFIED = "미분류";

    public Book(String isbn13) {
        this.isbn13 = isbn13;
    }

    public Book(String isbn13, String title, String author, String genre, String publisher, String cover) {
        if (genre == null || genre.isBlank())
            throw new IllegalArgumentException("장르는 비어 있을 수 없습니다.");

        this.isbn13 = isbn13;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.publisher = publisher;
        this.cover = cover;
    }

    public static Book withoutGenre(String isbn13, String title, String author, String publisher, String cover) {
        return new Book(isbn13, title, author, UNCLASSIFIED, publisher, cover);
    }
}
