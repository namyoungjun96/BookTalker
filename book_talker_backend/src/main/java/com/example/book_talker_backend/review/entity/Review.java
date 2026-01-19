package com.example.book_talker_backend.review.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import com.example.book_talker_backend.book.entity.Book;

@Entity
@Table
public class Review {
    @Id
    private Long id;
    @ManyToOne
    @JoinColumn(name = "book_isbn")
    private Book book;
    private String writer;
    private String content;
    private String rating;
    private String regDate;
}
