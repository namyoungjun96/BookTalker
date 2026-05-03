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

    // 특정 사용자의 특정 책 1회독 존재 여부
    boolean existsByWriterAndBookIsbn13AndReadingCount(String writer, String isbn13, int readingCount);

    // 특정 사용자의 특정 책 최대 회독 수 조회 (없으면 null)
    @Query("SELECT MAX(r.readingCount) FROM Review r WHERE r.writer = :writer AND r.book.isbn13 = :isbn13")
    Integer findMaxReadingCount(@Param("writer") String writer, @Param("isbn13") String isbn13);

    // 공개 리뷰: isPublic=true AND 1회독만
    @Query("SELECT r FROM Review r WHERE r.book.isbn13 = :isbn13 AND r.isPublic = true AND r.readingCount = 1 ORDER BY r.regDate DESC")
    Page<Review> findPublicReviews(@Param("isbn13") String isbn13, Pageable pageable);

    // 집계: 1회독 리뷰만 대상, 3개 이상인 책
    @Query("SELECT r.book.isbn13, r.book.genre, r.book.title, r.book.cover, " +
           "AVG(r.rating), COUNT(r) " +
           "FROM Review r " +
           "WHERE r.readingCount = 1 " +
           "GROUP BY r.book.isbn13, r.book.genre, r.book.title, r.book.cover " +
           "HAVING COUNT(r) >= 3 " +
           "ORDER BY AVG(r.rating) DESC, COUNT(r) DESC")
    List<Object[]> aggregateRankData();
}
