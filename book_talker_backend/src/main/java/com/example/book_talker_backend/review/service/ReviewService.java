package com.example.book_talker_backend.review.service;

import com.example.book_talker_backend.book.entity.Book;
import com.example.book_talker_backend.book.entity.dto.AladinBook;
import com.example.book_talker_backend.book.service.BookService;
import com.example.book_talker_backend.review.dao.ReviewRepository;
import com.example.book_talker_backend.review.entity.Review;
import com.example.book_talker_backend.review.entity.dto.ReviewRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final BookService bookService;

    @Transactional
    public void insertReview(ReviewRequest request) {
        log.debug("Inserting review for book with ISBN13: {}", request.isbn13());

        getOrFetchBook(request.isbn13());
        Review review = request.to();

        reviewRepository.save(review);
    }

    public void getOrFetchBook(String isbn13) {
        Book existingBook = bookService.getBookByIsbn13(isbn13);

        if (existingBook != null)
            return;

        List<AladinBook> aladinBooks = bookService.getBookByIsbn13WithApi(isbn13).item();

        if (aladinBooks.isEmpty()) {
            throw new IllegalArgumentException("Book with ISBN13 " + isbn13 + " does not exist");
        }

        Book newBbook = aladinBooks.get(0).to();
        bookService.insertBook(newBbook);
    }

    @Transactional
    public int updateReview(ReviewRequest request) {
        Review review = reviewRepository.findByBookIsbn13(request.isbn13());
        review.setContent(request.content());
        review.setRating(request.rating());
        review.setModDate(java.time.LocalDateTime.now());
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
