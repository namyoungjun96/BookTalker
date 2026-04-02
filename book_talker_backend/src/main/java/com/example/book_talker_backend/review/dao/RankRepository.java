package com.example.book_talker_backend.review.dao;

import com.example.book_talker_backend.review.entity.Rank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RankRepository extends JpaRepository<Rank, String> {
    List<Rank> findTop10ByGenreContainingOrderByAvgRatingDescReviewCountDesc(String genre);
    List<Rank> findAllByOrderByAvgRatingDescReviewCountDesc();

    @Query("SELECT r FROM Rank r ORDER BY r.genre ASC, r.avgRating DESC, r.reviewCount DESC")
    List<Rank> findAllGroupable();
}
