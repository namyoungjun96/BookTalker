package com.example.book_talker_backend.review.service;

import com.example.book_talker_backend.book.entity.Book;
import com.example.book_talker_backend.book.service.BookService;
import com.example.book_talker_backend.review.dao.ReviewRepository;
import com.example.book_talker_backend.review.entity.Review;
import com.example.book_talker_backend.review.entity.dto.ReviewRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService 단위 테스트")
class ReviewServiceTest {

    @InjectMocks ReviewService reviewService;
    @Mock ReviewRepository reviewRepository;
    @Mock BookService bookService;

    private static final String WRITER = "youngjun@test.com";
    private static final String OTHER_WRITER = "other@test.com";
    private static final String ISBN13 = "9791234567890";

    // ─────────────────────────────────────────────
    // insertReview
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("insertReview (1회독)")
    class InsertReview {

        @Test
        @DisplayName("성공 - DB에 책이 이미 존재하는 경우")
        void 성공_DB에_책_존재() {
            given(reviewRepository.existsByWriterAndBookIsbn13AndReadingCount(WRITER, ISBN13, 1))
                    .willReturn(false);
            given(bookService.getBookByIsbn13(ISBN13)).willReturn(createBook());

            ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
            reviewService.insertReview(createInsertRequest(), WRITER);

            then(reviewRepository).should().save(captor.capture());
            Review saved = captor.getValue();

            assertThat(saved.getWriter()).isEqualTo(WRITER);
            assertThat(saved.getHeadline()).isEqualTo("꿀잼!");
            assertThat(saved.getContent()).isEqualTo("재밌었음");
            assertThat(saved.getRating()).isEqualTo(3);
            assertThat(saved.getReadingCount()).isEqualTo(1);
            assertThat(saved.getIsPublic()).isTrue();
            assertThat(saved.getRegDate()).isNotNull();

            then(bookService).should(never()).getBookByIsbn13WithApi(anyString());
        }

        @Test
        @DisplayName("성공 - DB에 책이 없어 알라딘 API로 조회하는 경우")
        void 성공_DB에_책_없음_API_조회() {
            Book fetchedBook = createBook();
            given(reviewRepository.existsByWriterAndBookIsbn13AndReadingCount(WRITER, ISBN13, 1))
                    .willReturn(false);
            given(bookService.getBookByIsbn13(ISBN13)).willReturn(null);
            given(bookService.getBookByIsbn13WithApi(ISBN13)).willReturn(fetchedBook);

            ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
            reviewService.insertReview(createInsertRequest(), WRITER);

            then(reviewRepository).should().save(captor.capture());
            Review saved = captor.getValue();
            assertThat(saved.getBook()).isEqualTo(fetchedBook);
            assertThat(saved.getReadingCount()).isEqualTo(1);

            then(bookService).should().getBookByIsbn13(ISBN13);
            then(bookService).should().getBookByIsbn13WithApi(ISBN13);
        }

        @Test
        @DisplayName("실패 - 알라딘에도 존재하지 않는 책")
        void 실패_알라딘에도_책_없음() {
            given(reviewRepository.existsByWriterAndBookIsbn13AndReadingCount(WRITER, ISBN13, 1))
                    .willReturn(false);
            given(bookService.getBookByIsbn13(ISBN13)).willReturn(null);
            given(bookService.getBookByIsbn13WithApi(ISBN13))
                    .willThrow(new IllegalArgumentException("[Aladin] Book with ISBN13 " + ISBN13 + " does not exist"));

            assertThatThrownBy(() -> reviewService.insertReview(createInsertRequest(), WRITER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(ISBN13);

            then(reviewRepository).should(never()).save(any(Review.class));
        }

        @Test
        @DisplayName("실패 - 해당 책의 1회독 독후감이 이미 존재 (409)")
        void 실패_1회독_이미_존재() {
            given(reviewRepository.existsByWriterAndBookIsbn13AndReadingCount(WRITER, ISBN13, 1))
                    .willReturn(true);

            assertThatThrownBy(() -> reviewService.insertReview(createInsertRequest(), WRITER))
                    .isInstanceOf(DataIntegrityViolationException.class);

            then(bookService).should(never()).getBookByIsbn13(anyString());
            then(reviewRepository).should(never()).save(any(Review.class));
        }
    }

    // ─────────────────────────────────────────────
    // insertNextReading
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("insertNextReading (2회독+)")
    class InsertNextReading {

        @Test
        @DisplayName("성공 - 1회독 존재, readingCount = max + 1")
        void 성공_readingCount_자동증가() {
            given(reviewRepository.findMaxReadingCount(WRITER, ISBN13)).willReturn(1);
            given(bookService.getBookByIsbn13(ISBN13)).willReturn(createBook());

            ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
            reviewService.insertNextReading(createInsertRequest(), WRITER);

            then(reviewRepository).should().save(captor.capture());
            Review saved = captor.getValue();
            assertThat(saved.getReadingCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("성공 - 3회독 이후에도 max + 1 적용")
        void 성공_3회독_이후() {
            given(reviewRepository.findMaxReadingCount(WRITER, ISBN13)).willReturn(3);
            given(bookService.getBookByIsbn13(ISBN13)).willReturn(createBook());

            ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
            reviewService.insertNextReading(createInsertRequest(), WRITER);

            then(reviewRepository).should().save(captor.capture());
            assertThat(captor.getValue().getReadingCount()).isEqualTo(4);
        }

        @Test
        @DisplayName("성공 - isPublic은 요청값에 관계없이 항상 false")
        void 성공_isPublic_강제_false() {
            given(reviewRepository.findMaxReadingCount(WRITER, ISBN13)).willReturn(1);
            given(bookService.getBookByIsbn13(ISBN13)).willReturn(createBook());

            // isPublic=true로 요청해도
            ReviewRequest requestWithPublic = new ReviewRequest(null, ISBN13, WRITER, "꿀잼!", "재밌었음", 3, true);

            ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
            reviewService.insertNextReading(requestWithPublic, WRITER);

            then(reviewRepository).should().save(captor.capture());
            assertThat(captor.getValue().getIsPublic()).isFalse();
        }

        @Test
        @DisplayName("실패 - 선행 회독이 없으면 400")
        void 실패_선행_회독_없음() {
            given(reviewRepository.findMaxReadingCount(WRITER, ISBN13)).willReturn(null);

            assertThatThrownBy(() -> reviewService.insertNextReading(createInsertRequest(), WRITER))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("1회독");

            then(reviewRepository).should(never()).save(any(Review.class));
        }
    }

    // ─────────────────────────────────────────────
    // updateReview
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("updateReview")
    class UpdateReview {

        @Test
        @DisplayName("성공 - 1회독은 isPublic 변경 가능")
        void 성공_1회독_isPublic_변경() {
            Review existing = createReviewInDb(1L, WRITER, 1);

            given(reviewRepository.findById(1L)).willReturn(Optional.of(existing));

            ReviewRequest request = new ReviewRequest(1L, ISBN13, WRITER, "수정 요약", "수정 본문", 4, true);
            reviewService.updateReview(request, WRITER);

            assertThat(existing.getHeadline()).isEqualTo("수정 요약");
            assertThat(existing.getContent()).isEqualTo("수정 본문");
            assertThat(existing.getRating()).isEqualTo(4);
            assertThat(existing.getIsPublic()).isTrue();
            assertThat(existing.getModDate()).isNotNull();
        }

        @Test
        @DisplayName("성공 - 2회독+ 수정 시 isPublic은 강제 false 유지")
        void 성공_2회독_isPublic_강제_false() {
            Review existing = createReviewInDb(2L, WRITER, 2);

            given(reviewRepository.findById(2L)).willReturn(Optional.of(existing));

            // isPublic=true로 요청해도
            ReviewRequest request = new ReviewRequest(2L, ISBN13, WRITER, "수정 요약", "수정 본문", 4, true);
            reviewService.updateReview(request, WRITER);

            assertThat(existing.getIsPublic()).isFalse();
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 독후감")
        void 실패_독후감_없음() {
            given(reviewRepository.findById(99L)).willReturn(Optional.empty());

            ReviewRequest request = new ReviewRequest(99L, ISBN13, WRITER, "요약", "본문", 3, false);
            assertThatThrownBy(() -> reviewService.updateReview(request, WRITER))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("실패 - 다른 사용자의 독후감 수정 시도 (403)")
        void 실패_권한_없음() {
            Review existing = createReviewInDb(1L, WRITER, 1);
            given(reviewRepository.findById(1L)).willReturn(Optional.of(existing));

            ReviewRequest request = new ReviewRequest(1L, ISBN13, OTHER_WRITER, "요약", "본문", 3, false);
            assertThatThrownBy(() -> reviewService.updateReview(request, OTHER_WRITER))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("실패 - reviewId 없이 요청")
        void 실패_reviewId_없음() {
            ReviewRequest request = new ReviewRequest(null, ISBN13, WRITER, "요약", "본문", 3, false);
            assertThatThrownBy(() -> reviewService.updateReview(request, WRITER))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─────────────────────────────────────────────
    // deleteReview
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("deleteReview")
    class DeleteReview {

        @Test
        @DisplayName("성공 - 본인 독후감 삭제")
        void 성공() {
            Review existing = createReviewInDb(1L, WRITER, 1);
            given(reviewRepository.findById(1L)).willReturn(Optional.of(existing));

            reviewService.deleteReview(1L, WRITER);

            then(reviewRepository).should().delete(existing);
        }

        @Test
        @DisplayName("실패 - 다른 사용자의 독후감 삭제 시도 (403)")
        void 실패_권한_없음() {
            Review existing = createReviewInDb(1L, WRITER, 1);
            given(reviewRepository.findById(1L)).willReturn(Optional.of(existing));

            assertThatThrownBy(() -> reviewService.deleteReview(1L, OTHER_WRITER))
                    .isInstanceOf(AccessDeniedException.class);

            then(reviewRepository).should(never()).delete(any(Review.class));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 독후감")
        void 실패_독후감_없음() {
            given(reviewRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.deleteReview(99L, WRITER))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─────────────────────────────────────────────
    // 헬퍼
    // ─────────────────────────────────────────────
    private ReviewRequest createInsertRequest() {
        return new ReviewRequest(null, ISBN13, WRITER, "꿀잼!", "재밌었음", 3, true);
    }

    private Book createBook() {
        return new Book(ISBN13);
    }

    private Review createReviewInDb(Long id, String writer, int readingCount) {
        Review review = new Review();
        review.setId(id);
        review.setWriter(writer);
        review.setHeadline("원래 요약");
        review.setContent("원래 내용");
        review.setRating(3);
        review.setIsPublic(false);
        review.setReadingCount(readingCount);
        return review;
    }
}
