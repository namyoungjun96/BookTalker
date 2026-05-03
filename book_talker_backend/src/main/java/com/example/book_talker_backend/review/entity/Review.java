package com.example.book_talker_backend.review.entity;

import com.example.book_talker_backend.book.entity.Book;
import jakarta.persistence.*;
import lombok.Setter;
import lombok.Getter;

import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table(
    uniqueConstraints = @UniqueConstraint(
        name = "uk_review_writer_isbn13_reading_count",
        columnNames = {"writer", "isbn13", "reading_count"}
    )
)
@Setter
@Getter
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "isbn13")
    private Book book;
    private String writer;
    private String content;
    private String headline;  // 필수, 항상 공개
    private Integer rating;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime regDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modDate;
    private Boolean isPublic; // content 공개 여부, 기본 false
    @Column(name = "reading_count", nullable = false, columnDefinition = "integer default 1")
    private Integer readingCount; // 회독 수, 1부터 시작
}
