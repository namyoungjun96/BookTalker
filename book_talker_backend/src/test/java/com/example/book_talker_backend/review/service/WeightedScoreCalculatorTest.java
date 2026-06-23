package com.example.book_talker_backend.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.book_talker_backend.review.WeightedScoreCalculator;
import com.example.book_talker_backend.review.entity.dto.BookRatingStats;


public class WeightedScoreCalculatorTest {
    @Test
    void 가중_평점_정상_계산() {
        int reviewCount = 3;
        double bookAvg = 5.0;
        int threshold = 3; 
        double globalAvg = 4.0;

        double weightedScore = WeightedScoreCalculator.calculateWeightedScore(reviewCount, bookAvg, threshold, globalAvg);
        assertThat(weightedScore).isCloseTo(4.5, within(0.0001));
    }

    @Test
    void 리뷰가_적으면_전체평균쪽으로_내려간다() {
        int reviewCount = 1;
        double bookAvg = 5.0;
        int threshold = 3; 
        double globalAvg = 4.0;

        double weightedScore = WeightedScoreCalculator.calculateWeightedScore(reviewCount, bookAvg, threshold, globalAvg);
        assertThat(weightedScore).isCloseTo(4.25, within(0.0001));
    }

    @Test
    void 리뷰_단위_가중평균_정상_계산() {
        List<BookRatingStats> bookRatingStats = List.of(
            new BookRatingStats(3, 100),
            new BookRatingStats(5, 5),
            new BookRatingStats(5, 5)
        );

        double cScore = WeightedScoreCalculator.calculateGlobalAverage(bookRatingStats);
        assertThat(cScore).isCloseTo(350.0/110.0, within(0.0001));
    }

    @Test
    void 가중평균_빈_리스트면_예외를_던진다() {
        List<BookRatingStats> bookRatingStats = List.of();

        assertThatThrownBy(() -> 
            WeightedScoreCalculator.calculateGlobalAverage(bookRatingStats)
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
