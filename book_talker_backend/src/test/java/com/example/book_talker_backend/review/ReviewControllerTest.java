package com.example.book_talker_backend.review;

import com.example.book_talker_backend.config.SecurityConfig;
import com.example.book_talker_backend.exception.GlobalExceptionHandler;
import com.example.book_talker_backend.review.service.ReviewService;
import com.example.book_talker_backend.user.dao.OAuth2UserRepository;
import com.example.book_talker_backend.user.dao.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 보안 설정(SecurityConfig)까지 포함해 비로그인 요청을 검증한다. 인증 사용자(@WithMockUser 등) 없이 요청한다.
@WebMvcTest(ReviewController.class)
class ReviewControllerTest {
    @Configuration
    @Import({ReviewController.class, SecurityConfig.class, GlobalExceptionHandler.class})
    static class TestConfig {
    }

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ReviewService reviewService;

    // SecurityConfig 의존성
    @MockitoBean
    OAuth2UserRepository oAuth2UserRepository;
    @MockitoBean
    UserRepository userRepository;

    private static final String ISBN13 = "1234567891011";

    @Test
    void 리뷰_작성시_비로그인_사용자는_401() throws Exception {
        mockMvc.perform(post("/review")
                        .content("""
                                {
                                    "isbn13": "1234567891011",
                                    "headline": "headline",
                                    "content": "content",
                                    "rating": 3,
                                    "visibility": "PUBLIC"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."))
                ;

        verifyNoInteractions(reviewService);
    }
}