package com.example.book_talker_backend.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiUsageCounterTest {

    private ApiUsageCounter counter;

    @BeforeEach
    void setUp() {
        counter = new ApiUsageCounter();
    }

    @Test
    void increment_새로운_키_생성() {
        counter.increment("GET /rank");

        List<Map.Entry<String, Long>> result = counter.getTopN(1);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getKey()).isEqualTo("GET /rank");
        assertThat(result.get(0).getValue()).isEqualTo(1L);
    }

    @Test
    void increment_기존_키_누적() {
        counter.increment("GET /rank");
        counter.increment("GET /rank");
        counter.increment("GET /rank");

        assertThat(counter.getTopN(1).get(0).getValue()).isEqualTo(3L);
    }

    @Test
    void getTopN_내림차순_정렬() {
        counter.increment("GET /rank");
        counter.increment("GET /rank");
        counter.increment("GET /book/search");
        counter.increment("GET /review/list");
        counter.increment("GET /review/list");
        counter.increment("GET /review/list");

        List<Map.Entry<String, Long>> top = counter.getTopN(3);

        assertThat(top.get(0).getKey()).isEqualTo("GET /review/list");
        assertThat(top.get(0).getValue()).isEqualTo(3L);
        assertThat(top.get(1).getKey()).isEqualTo("GET /rank");
        assertThat(top.get(2).getKey()).isEqualTo("GET /book/search");
    }

    @Test
    void getTopN_N이_실제_항목_수_초과() {
        counter.increment("GET /rank");
        counter.increment("GET /book/search");

        List<Map.Entry<String, Long>> result = counter.getTopN(5);

        assertThat(result).hasSize(2);
    }

    @Test
    void getTopN_빈_카운터() {
        assertThat(counter.getTopN(3)).isEmpty();
    }

    @Test
    void clear_후_카운터_초기화() {
        counter.increment("GET /rank");
        counter.increment("GET /book/search");

        counter.clear();

        assertThat(counter.getTopN(3)).isEmpty();
    }
}
