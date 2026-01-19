package com.example.book_talker_backend.book.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table
public class Book {
    @Id
    private String isbn;
    private String title;
    private String author;
    private String genre;
    private String publisher;
    private String cover;
}
