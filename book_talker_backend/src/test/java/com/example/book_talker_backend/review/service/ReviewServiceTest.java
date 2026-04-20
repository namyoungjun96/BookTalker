package com.example.book_talker_backend.review.service;

import com.example.book_talker_backend.book.entity.Book;
import com.example.book_talker_backend.book.service.BookService;
import com.example.book_talker_backend.review.dao.ReviewRepository;
import com.example.book_talker_backend.review.entity.Review;
import com.example.book_talker_backend.review.entity.dto.ReviewRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService 단위 테스트")
class ReviewServiceTest {
    @InjectMocks ReviewService reviewService;

    @Mock ReviewRepository reviewRepository;

    @Mock BookService bookService;

    private final String successIsbn13 = "123456789";
    private final String failedIsbn13 = "000000000";

    @Test
    @DisplayName("독후감_등록_실제_책이_존재하지_않을때")
    void when_insert_report_not_exist_book() {
        given(bookService.getBookByIsbn13(anyString()))
                .willReturn(null);

        given(bookService.getBookByIsbn13WithApi(anyString()))
                .willThrow(new IllegalArgumentException("[Aladin] Book with ISBN13 " + failedIsbn13 + " does not exist"));

        ReviewRequest request = createSuccessReviewRequest(failedIsbn13);
        IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, () -> reviewService.insertReview(request));
        assertThat(e.getMessage()).isEqualTo("[Aladin] Book with ISBN13 " + failedIsbn13 + " does not exist");
        then(reviewRepository).should(never()).save(any(Review.class));
    }

    @Test
    @DisplayName("독후감_등록_책이_DB에_존재할때_성공")
    void when_success_insert_report_exist_book() {
        Book existingBook = createExistingBook();

        given(bookService.getBookByIsbn13(successIsbn13))
                .willReturn(existingBook);

        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);

        ReviewRequest request = createSuccessReviewRequest(successIsbn13);
        reviewService.insertReview(request);

        then(reviewRepository).should().save(reviewCaptor.capture());

        Review savedReview = reviewCaptor.getValue();
        assertThat(savedReview.getBook()).isEqualTo(existingBook);

        assertThat(savedReview.getWriter()).isEqualTo("youngjun@test.com");
        assertThat(savedReview.getContent()).isEqualTo("재밌었음");
        assertThat(savedReview.getRating()).isEqualTo("3");
        assertThat(savedReview.getRegDate()).isNotNull();

        then(bookService).should().getBookByIsbn13(successIsbn13);
        then(bookService).should(never()).getBookByIsbn13WithApi(successIsbn13);
        then(reviewRepository).should().save(any(Review.class));
    }

    @Test
    @DisplayName("독후감_등록_책이_DB에_존재하지_않을때_성공")
    void when_success_insert_report_not_exsist_book() {
        Book exstingBook = createExistingBook();

        given(bookService.getBookByIsbn13(successIsbn13))
                .willReturn(null);

        given(bookService.getBookByIsbn13WithApi(anyString()))
                .willReturn(exstingBook);

        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);

        ReviewRequest request = createSuccessReviewRequest(successIsbn13);
        reviewService.insertReview(request);

        then(reviewRepository).should().save(reviewCaptor.capture());

        Review savedReview = reviewCaptor.getValue();
        assertThat(savedReview.getBook()).isEqualTo(exstingBook);

        assertThat(savedReview.getWriter()).isEqualTo("youngjun@test.com");
        assertThat(savedReview.getContent()).isEqualTo("재밌었음");
        assertThat(savedReview.getRating()).isEqualTo("3");
        assertThat(savedReview.getRegDate()).isNotNull();

        then(bookService).should().getBookByIsbn13(successIsbn13);
        then(bookService).should().getBookByIsbn13WithApi(successIsbn13);
        then(reviewRepository).should().save(savedReview);
    }

    private ReviewRequest createSuccessReviewRequest(String isbn13) {
        return new ReviewRequest(isbn13,"youngjun@test.com", "재밌었음", "3");
    }

    private Book createExistingBook() {
        return new Book(successIsbn13);
    }

    private Book createNotExistingBook() {
        return new Book(successIsbn13);
    }
}