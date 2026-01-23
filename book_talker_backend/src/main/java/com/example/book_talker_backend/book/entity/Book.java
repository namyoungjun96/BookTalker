package com.example.book_talker_backend.book.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Setter;
import lombok.Getter;

@Entity
@Table
@Setter
@Getter
public class Book {
    @Id
    private String isbn13;
    private String title;
    private String author;
    private String genre;
    private String publisher;
    private String cover;

    public Book() {}

    public Book(String isbn13) {
        this.isbn13 = isbn13;
    }
}
