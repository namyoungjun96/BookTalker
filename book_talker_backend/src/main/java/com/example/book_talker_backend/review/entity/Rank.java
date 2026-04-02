package com.example.book_talker_backend.review.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "book_rank")
@Setter
@Getter
public class Rank {
    @Id
    private String isbn13;
    private String genre;
    private Double avgRating;
    private Integer reviewCount;
    private String title;
    private String cover;
    private LocalDateTime updatedAt;
}
