package com.example.book_talker_backend.review.dao;


import com.example.book_talker_backend.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByWriter(String writer);
    Review findByBookIsbn13(String isbn13);

    @Query("SELECT r FROM Review r WHERE r.book.isbn13 = :isbn13 AND r.isPublic = true ORDER BY r.regDate DESC")
    Page<Review> findPublicReviews(@Param("isbn13") String isbn13, Pageable pageable);

    @Query("SELECT r.book.isbn13, r.book.genre, r.book.title, r.book.cover, " +
           "AVG(r.rating), COUNT(r) " +
           "FROM Review r " +
           "GROUP BY r.book.isbn13, r.book.genre, r.book.title, r.book.cover " +
           "HAVING COUNT(r) >= 3 " +
           "ORDER BY AVG(r.rating) DESC, COUNT(r) DESC")
    List<Object[]> aggregateRankData();
}
