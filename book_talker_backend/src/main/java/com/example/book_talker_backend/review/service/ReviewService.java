package com.example.book_talker_backend.review.service;

import com.example.book_talker_backend.book.entity.Book;
import com.example.book_talker_backend.book.entity.dto.AladinBook;
import com.example.book_talker_backend.book.service.BookService;
import com.example.book_talker_backend.review.dao.ReviewRepository;
import com.example.book_talker_backend.review.entity.Review;
import com.example.book_talker_backend.review.entity.dto.ReviewRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final BookService bookService;

    public int insertReview(ReviewRequest request) {
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
}
