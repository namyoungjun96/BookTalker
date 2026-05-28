package com.example.book_talker_backend.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ApiUsageReportSchedulerTest {

    @Mock DataSource dataSource;

    private ApiUsageCounter counter;
    private ApiUsageReportScheduler scheduler;

    @BeforeEach
    void setUp() {
        counter = new ApiUsageCounter();
        scheduler = new ApiUsageReportScheduler(counter, dataSource);
        ReflectionTestUtils.setField(scheduler, "webhookUrl", "");
    }

    @Test
    void buildReportMessage_상위3개_내림차순_포함() {
        counter.increment("GET /review/list");
        counter.increment("GET /review/list");
        counter.increment("GET /review/list");
        counter.increment("GET /rank");
        counter.increment("GET /rank");
        counter.increment("GET /book/search");

        String msg = scheduler.buildReportMessage();

        assertThat(msg).contains("1위", "GET /review/list", "3회");
        assertThat(msg).contains("2위", "GET /rank", "2회");
        assertThat(msg).contains("3위", "GET /book/search", "1회");
    }

    @Test
    void buildReportMessage_호출_없을때_안내_문구() {
        String msg = scheduler.buildReportMessage();

        assertThat(msg).contains("(호출 없음)");
    }

    @Test
    void buildReportMessage_JVM_힙_정보_포함() {
        String msg = scheduler.buildReportMessage();

        assertThat(msg).contains("JVM Heap", "MB");
    }

    @Test
    void sendDailyReport_실행후_카운터_초기화() {
        counter.increment("GET /rank");

        scheduler.sendDailyReport();

        assertThat(counter.getTopN(1)).isEmpty();
    }
}
