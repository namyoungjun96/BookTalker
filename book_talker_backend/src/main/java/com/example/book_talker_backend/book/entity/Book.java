package com.example.book_talker_backend.book.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table
@IdClass(BookId.class)
public class Book {
    @Id
    private String title;
    @Id
    private String author;
    private Integer isbn;
    private String genre;
    private String publisher;
    private String cover;
}
