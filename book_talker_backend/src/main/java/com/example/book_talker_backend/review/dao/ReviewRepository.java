package com.example.book_talker_backend.review.dao;


import com.example.book_talker_backend.review.entity.Review;
import com.example.book_talker_backend.review.entity.dto.BookRatingStats;
import com.example.book_talker_backend.review.entity.dto.TeamReviewDetailResponse;
import com.example.book_talker_backend.review.entity.dto.TeamReviewItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByWriter(String writer);

    // 특정 사용자의 특정 책 1회독 존재 여부
    boolean existsByWriterAndBookIsbn13AndReadingCount(String writer, String isbn13, int readingCount);

    // 특정 사용자의 특정 책 최대 회독 수 조회 (없으면 null)
    @Query("SELECT MAX(r.readingCount) FROM Review r WHERE r.writer = :writer AND r.book.isbn13 = :isbn13")
    Integer findMaxReadingCount(@Param("writer") String writer, @Param("isbn13") String isbn13);

    // 책별 독후감 목록: 공개 범위 무관 (한 줄 요약은 항상 공개, 본문은 상세에서 PUBLIC만)
    @Query("SELECT r FROM Review r WHERE r.book.isbn13 = :isbn13 " +
            "ORDER BY r.regDate DESC")
    Page<Review> findBookReviews(@Param("isbn13") String isbn13, Pageable pageable);

    // 공개 독후감 상세: PUBLIC만 (없음·비공개 모두 empty → 404)
    @Query("SELECT r FROM Review r " +
            "JOIN FETCH r.book b " +
            "WHERE r.id = :reviewId and r.visibility = com.example.book_talker_backend.review.entity.ReviewVisibilityEnum.PUBLIC")
    Optional<Review> findBookReview(@Param("reviewId") Long reviewId);

    // 집계: 1회독 리뷰만 대상, 3개 이상인 책
    @Query("SELECT new com.example.book_talker_backend.review.entity.dto.BookRatingStats(" +
                  "r.book.isbn13, AVG(r.rating), CAST(COUNT(r) AS Integer)) " +
                  "FROM Review r " +
                  "WHERE r.readingCount = 1 " +
                  "GROUP BY r.book.isbn13 " +
                  "HAVING COUNT(r) >= 3")
    List<BookRatingStats> aggregateRankData();

    // 모임 공개 이상 리뷰만
    @Query("""
        SELECT new com.example.book_talker_backend.review.entity.dto.TeamReviewItem(
                r.id, r.headline, r.content, b.title, b.cover, r.regDate, tm.displayName, r.visibility
        )
                FROM Review r
                JOIN TeamMember tm on r.writer = tm.oAuth2User.providerId
                JOIN r.book b
                WHERE tm.team.teamId = :teamId and tm.status = 'ACTIVE' and r.visibility != 'PRIVATE'
                ORDER BY r.regDate DESC, r.id DESC
        """)
    Page<TeamReviewItem> findAllByTeamReview(Long teamId, Pageable pageable);

    @Query("""
    SELECT new com.example.book_talker_backend.review.entity.dto.TeamReviewDetailResponse(
            r.id, r.headline, r.content, b.author, r.rating, r.readingCount, r.visibility, r.regDate, b.title, b.cover, tm.team.teamName, tm.displayName,
            CASE WHEN r.writer = :providerId THEN true ELSE false END
    )
            FROM Review r
            JOIN TeamMember tm on r.writer = tm.oAuth2User.providerId
            JOIN r.book b
            WHERE tm.team.teamId = :teamId
                AND tm.status = 'ACTIVE'
                AND r.visibility != 'PRIVATE'
                AND r.id = :reviewId
    """)
    Optional<TeamReviewDetailResponse> findByTeamReviewDetail(Long teamId, Long reviewId, String providerId);

    List<Review> findAllByWriterInAndRegDateBetween(List<String> writers, LocalDateTime start, LocalDateTime end);
}
