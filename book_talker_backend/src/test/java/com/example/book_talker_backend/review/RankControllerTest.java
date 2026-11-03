package com.example.book_talker_backend.review;

import com.example.book_talker_backend.book.entity.Book;
import com.example.book_talker_backend.config.SecurityConfig;
import com.example.book_talker_backend.exception.GlobalExceptionHandler;
import com.example.book_talker_backend.review.entity.Review;
import com.example.book_talker_backend.review.entity.ReviewVisibilityEnum;
import com.example.book_talker_backend.review.entity.dto.BookReviewListResponse;
import com.example.book_talker_backend.review.entity.dto.BookReviewResponse;
import com.example.book_talker_backend.review.exception.NotFoundReviewException;
import com.example.book_talker_backend.review.service.RankService;
import com.example.book_talker_backend.review.service.ReviewService;
import com.example.book_talker_backend.user.dao.OAuth2UserRepository;
import com.example.book_talker_backend.user.dao.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 보안 설정(SecurityConfig)까지 포함해 비로그인 요청을 검증한다. 인증 사용자(@WithMockUser 등) 없이 요청한다.
@WebMvcTest(RankController.class)
@ContextConfiguration(classes = RankControllerTest.TestConfig.class)
class RankControllerTest {
    // 메인 클래스(BookTalkerBackendApplication) 대신 쓰는 테스트 전용 설정.
    // 메인 클래스의 @EnableJpaRepositories가 슬라이스 테스트에서도 레포지토리를 등록해 entityManagerFactory를 요구하므로,
    // 필요한 빈만 직접 등록한다. 컴포넌트 스캔을 하지 않아 WebConfig 등 무관한 빈도 올라오지 않는다.
    @Configuration
    @Import({RankController.class, SecurityConfig.class, GlobalExceptionHandler.class})
    static class TestConfig {
    }

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    ReviewService reviewService;
    @MockitoBean
    RankService rankService;

    // SecurityConfig 의존성
    @MockitoBean
    OAuth2UserRepository oAuth2UserRepository;
    @MockitoBean
    UserRepository userRepository;

    private static final Long REVIEW_ID = 12345678L;
    private static final String ISBN13 = "1234567891011";

    @Nested
    @DisplayName("책별 독후감 목록")
    class ReadBookReviews {
        @Test
        void 독후감_목록은_본문없음() throws Exception {
            given(reviewService.getBookReviews(ISBN13, 0))
                    .willReturn(bookPage());

            mockMvc.perform(get("/rank/reviews").param("isbn13", ISBN13))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].headline").value("원래 요약"))
                    .andExpect(jsonPath("$.content[0].visibility").value("PUBLIC"))
                    .andExpect(jsonPath("$.content[0].regDate").value("2026-11-03"))
                    .andExpect(jsonPath("$.content[0].content").doesNotExist())
            ;
        }
    }

    @Nested
    @DisplayName("공개 독후감 상세")
    class getReview {
        @Test
        void 독후감_상세_비로그인도_조회_가능() throws Exception {
            given(reviewService.getBookReview(REVIEW_ID))
                    .willReturn(BookReviewResponse.from(review(REVIEW_ID, "userA", 1)));

            mockMvc.perform(get("/rank/reviews/{reviewId}", REVIEW_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reviewId").value(REVIEW_ID))
                    .andExpect(jsonPath("$.bookTitle").value("title"))
                    .andExpect(jsonPath("$.regDate").value("2026-11-03"))
            ;
        }

        @Test
        void 독후감_상세_조회_없으면_404() throws Exception {
            given(reviewService.getBookReview(anyLong()))
                    .willThrow(new NotFoundReviewException("독후감이 존재하지 않습니다."));

            mockMvc.perform(get("/rank/reviews/{reviewId}", REVIEW_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        void 독후감_상세_조회_id가_숫자가_아니면_400() throws Exception {
            mockMvc.perform(get("/rank/reviews/{reviewId}", "notexist"))
                    .andExpect(status().isBadRequest());
        }
    }

    private Page<BookReviewListResponse> bookPage() {
        return new PageImpl<>(List.of(BookReviewListResponse.from(review(REVIEW_ID, "userA", 1))));
    }

    private Review review(Long id, String writer, int readingCount) {
        Review review = new Review();
        review.setId(id);
        review.setBook(book(ISBN13));
        review.setWriter(writer);
        review.setHeadline("원래 요약");
        review.setContent("원래 내용");
        review.setRating(3);
        review.setRegDate(LocalDateTime.of(2026, 11, 3, 0, 0, 0));
        review.setVisibility(ReviewVisibilityEnum.PUBLIC);
        review.setReadingCount(readingCount);
        return review;
    }

    private Book book(String isbn13) {
        return new Book(isbn13, "title", "author",
                "genre", "publisher", "cover");
    }
}