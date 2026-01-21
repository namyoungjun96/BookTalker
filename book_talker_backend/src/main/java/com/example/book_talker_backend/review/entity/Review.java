package com.example.book_talker_backend.review.entity;

import com.example.book_talker_backend.book.entity.Book;
import jakarta.persistence.*;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table
@Setter
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "book_isbn")
    private Book book;
    private String writer;
    private String content;
    private String rating;
    private LocalDate regDate;
}
