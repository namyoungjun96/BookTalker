package com.example.book_talker_backend.review.service;

import com.example.book_talker_backend.book.entity.Book;
import com.example.book_talker_backend.book.entity.dto.AladinBook;
import com.example.book_talker_backend.book.service.BookService;
import com.example.book_talker_backend.review.dao.ReviewRepository;
import com.example.book_talker_backend.review.entity.Review;
import com.example.book_talker_backend.review.entity.dto.ReviewRequest;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final BookService bookService;

    public int insertReview(ReviewRequest request) {
        log.debug("Inserting review for book with ISBN13: {}", request.isbn13());

        if (bookService.checkExistBook(request.isbn13()) == 0) {
            AladinBook aladinBook = bookService.getBookByIsbn13WithApi(request.isbn13()).item().get(0);

            if (aladinBook == null) {
                return 0;
            }

            Book book = aladinBook.to();

            if (bookService.insertBook(book) == 0) {
                return 0;
            }
        }

        Review review = request.to();

        try {
            reviewRepository.save(review);
        } catch (IllegalArgumentException | OptimisticLockingFailureException e) {
            return 0;
        }

        return 1;
    }

    public List<Review> getReviews(String email) {
        List<Review> reviews = reviewRepository.findByWriter(email);
        log.debug("Gettting reviews for user with email: {}, {}", email, reviews.size());
        return reviews;
    }

    public Review getReviewById(Long id) {
        log.debug("Fetching review with ID: {}", id);
        return reviewRepository.findById(id).orElse(null);
    }
}
