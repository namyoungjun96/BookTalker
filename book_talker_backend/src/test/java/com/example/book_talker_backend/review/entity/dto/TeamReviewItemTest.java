package com.example.book_talker_backend.review.entity.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TeamReviewItemTest {
    @Test
    void 독후감_요약_40자_이하면_그대로() {
        assertThat(TeamReviewItem.summarize("안녕하세요")).isEqualTo("안녕하세요");
    }

    @Test
    void 독후감_요약_정확히_40자면_그대로() {
        assertThat(TeamReviewItem.summarize("단순히 고통이라는 이유 때문에 고통 그 자체를 사랑하거나 추구하거나 소유")).isEqualTo("단순히 고통이라는 이유 때문에 고통 그 자체를 사랑하거나 추구하거나 소유");
    }

    @Test
    void 독후감_요약_40자_초과면_40자에서_생략표시() {
        assertThat(TeamReviewItem.summarize("단순히 고통이라는 이유 때문에 고통 그 자체를 사랑하거나 추구하거나 소유하려는 자는 없다.")).isEqualTo("단순히 고통이라는 이유 때문에 고통 그 자체를 사랑하거나 추구하거나 소유...");
    }

    @Test
    void 독후감_요약_이모지가_경계에_걸리면_깨지지_않음() {
        assertThat(TeamReviewItem.summarize("단순히 고통이라는 이유 때문에 고통 그 자체를 사랑하거나 추구하거나 소\uD83D\uDE00유하려는 자는 없다.")).isEqualTo("단순히 고통이라는 이유 때문에 고통 그 자체를 사랑하거나 추구하거나 소\uD83D\uDE00...");
    }
}