package com.example.book_talker_backend.review.service;

import com.example.book_talker_backend.book.entity.Book;
import com.example.book_talker_backend.book.service.BookService;
import com.example.book_talker_backend.review.dao.ReviewRepository;
import com.example.book_talker_backend.review.entity.Review;
import com.example.book_talker_backend.review.entity.dto.ReviewDetailResponse;
import com.example.book_talker_backend.review.entity.dto.ReviewListResponse;
import com.example.book_talker_backend.review.entity.dto.ReviewRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final BookService bookService;

    @Transactional
    public void insertReview(ReviewRequest request, String writer) {
        log.debug("Inserting review for book with ISBN13: {}", request.isbn13());

        Book book = getOrFetchBook(request.isbn13());
        Review review = request.to();
        review.setWriter(writer);
        review.setBook(book);

        reviewRepository.save(review);
    }

    private Book getOrFetchBook(String isbn13) {
        Book existingBook = bookService.getBookByIsbn13(isbn13);
        if (existingBook != null)
            return existingBook;

        Book newBook = bookService.getBookByIsbn13WithApi(isbn13);
        bookService.insertBook(newBook);
        return newBook;
    }

    @Transactional
    public void updateReview(ReviewRequest request, String email) {
        if (request.reviewId() == null) {
            throw new IllegalArgumentException("reviewId는 필수입니다.");
        }
        if (!StringUtils.hasText(request.headline())) {
            throw new IllegalArgumentException("한 줄 요약은 필수입니다.");
        }
        if (!StringUtils.hasText(request.content())) {
            throw new IllegalArgumentException("독후감 내용은 필수입니다.");
        }
        if (request.rating() == null || request.rating() < 1 || request.rating() > 5) {
            throw new IllegalArgumentException("평점은 1~5 사이여야 합니다.");
        }

        Review review = reviewRepository.findById(request.reviewId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 독후감입니다: " + request.reviewId()));

        if (!review.getWriter().equals(email)) {
            throw new AccessDeniedException("해당 독후감에 접근 권한이 없습니다.");
        }

        review.setHeadline(request.headline());
        review.setContent(request.content());
        review.setRating(request.rating());
        review.setIsPublic(request.isPublic() != null ? request.isPublic() : review.getIsPublic());
        review.setModDate(LocalDateTime.now());
    }

    public List<ReviewListResponse> getReviews(String email) {
        List<Review> reviews = reviewRepository.findByWriter(email);
        log.debug("Getting reviews for user with email: {}, count: {}", email, reviews.size());
        return reviews.stream()
                .map(ReviewListResponse::from)
                .collect(Collectors.toList());
    }

    public ReviewDetailResponse getReviewDetail(Long reviewId, String email) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
        if (!review.getWriter().equals(email)) {
            throw new AccessDeniedException("해당 독후감에 접근 권한이 없습니다.");
        }
        return ReviewDetailResponse.from(review);
    }
}
