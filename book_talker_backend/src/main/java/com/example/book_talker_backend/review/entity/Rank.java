package com.example.book_talker_backend.review.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

@Entity
@Table(name = "book_rank")
@Setter
@Getter
public class Rank implements Persistable<String> {
    @Id
    private String isbn13;
    private String genre;
    private double avgRating;
    private double weightedScore;
    private int reviewCount;
    private String title;
    private String cover;
    private LocalDateTime updatedAt;

    @Transient
    private boolean isNew = true;

    @Override
    public String getId() {
        return this.isbn13;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PrePersist
    @PostLoad
    void markNotNew() {
        isNew = false;
    }
}
