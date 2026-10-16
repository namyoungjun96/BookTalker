package com.example.book_talker_backend.review.entity.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TeamReviewItemTest {
    @Test
    void 독후감_요약_20자_이하면_그대로() {
        assertThat(TeamReviewItem.summarize("안녕하세요")).isEqualTo("안녕하세요");
    }

    @Test
    void 독후감_요약_정확히_20자면_그대로() {
        assertThat(TeamReviewItem.summarize("12345678912345678912")).isEqualTo("12345678912345678912");
    }

    @Test
    void 독후감_요약_20자_초과면_20자에서_생략표시() {
        assertThat(TeamReviewItem.summarize("123456789123456789123")).isEqualTo("12345678912345678912...");
    }

    @Test
    void 독후감_요약_이모지가_경계에_걸리면_깨지지_않음() {
        assertThat(TeamReviewItem.summarize("1234567891234567891\uD83D\uDE0023456")).isEqualTo("1234567891234567891\uD83D\uDE00...");
    }
}