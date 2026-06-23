package com.example.book_talker_backend.review;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.book_talker_backend.review.entity.dto.BookRatingStats;

@Component
public class WeightedScoreCalculator {
    public static double calculateWeightedScore(int reviewCount, double bookAvgRating, int threshold, double globalAvgRating) {
        return ((double) reviewCount / (reviewCount + threshold)) * bookAvgRating 
        + ((double) threshold / (reviewCount + threshold)) * globalAvgRating;
    }

    public static double calculateGlobalAverage(List<BookRatingStats> bookRatingStats) {
        if (bookRatingStats.isEmpty()) {
            throw new IllegalArgumentException("집계 대상 책이 비어있습니다. 배치에서 빈 리스트를 걸렀어야 합니다.");
        }

        double weightedSum = 0.0;
        double totalReviews = 0.0;

        for (BookRatingStats bookRating: bookRatingStats) {
            weightedSum += bookRating.avgRating() * bookRating.reviewCount();
            totalReviews += bookRating.reviewCount();
        }

        return weightedSum / totalReviews;
    }
}
