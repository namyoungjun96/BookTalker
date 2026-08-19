package com.example.book_talker_backend.review.service;

import java.util.List;

import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.StopWatch;

import com.example.book_talker_backend.review.dao.RankRepository;
import com.example.book_talker_backend.review.dao.ReviewRepository;
import com.example.book_talker_backend.review.entity.Rank;
import com.example.book_talker_backend.review.entity.dto.BookRatingStats;

import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Slf4j
public class RankPerformanceTest {
    @Autowired RankService rankService;
    @Autowired ReviewRepository reviewRepository;
    @Autowired RankRepository rankRepository;
    @Autowired EntityManagerFactory emf;
    @Autowired EntityManager em;

    private static final int CONFIDENCE_THRESHOLD = 3;
    StopWatch stopWatch;

    @BeforeEach
    void setUp() {
        stopWatch = new StopWatch();
    }

    @Test
    void aggregateRank_실행_시간_확인() {
        stopWatch.start("aggregateTask1");
        rankService.aggregateRank();
        stopWatch.stop();
        log.info("aggregateTask1: {}", stopWatch.prettyPrint());
    }

    @Test
    void aggregateRank_세부_로직_실행_시간_확인() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        stopWatch.start("aggregateRankData");
        List<BookRatingStats> rawData = reviewRepository.aggregateRankData();
        stopWatch.stop();

        stopWatch.start("aggregate");
        List<Rank> ranks = rankService.calculateTopRanksByGenre(rawData);
        stopWatch.stop();

        stopWatch.start("deleteAll SaveAll");
        rankRepository.deleteAllInBatch();
        stopWatch.stop();

        stopWatch.start("saveAll");
        rankRepository.saveAll(ranks);
        stopWatch.stop();

        log.info("Rank aggregation completed. Total: {}", ranks.size());
        log.info("aggregate logic test: {}", stopWatch.prettyPrint());
        log.info("실제 DB로 나간 SQL 문 개수: {}", stats.getPrepareStatementCount());
        log.info("삭제된 엔티티 수(건별 delete 여부 확인용): {}", stats.getEntityDeleteCount());
        log.info("삽입된 엔티티 수: {}", stats.getEntityInsertCount());
    }
}
