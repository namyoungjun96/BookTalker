package com.example.book_talker_backend.review.service;

import com.example.book_talker_backend.review.dao.RankRepository;
import com.example.book_talker_backend.review.dao.ReviewRepository;
import com.example.book_talker_backend.review.entity.Rank;
import com.example.book_talker_backend.review.entity.dto.BookRatingStats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankService {
    private final RankRepository rankRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    @Scheduled(cron = "0 0 4 * * ?")
    public void aggregateRank() {
        List<BookRatingStats> rawData = reviewRepository.aggregateRankData();

        rankRepository.deleteAll();

        List<Rank> ranks = rawData.stream().map(row -> {
            Rank rank = new Rank();
            rank.setIsbn13(row.isbn13());
            rank.setGenre(row.genre());
            rank.setTitle(row.title());
            rank.setCover(row.cover());
            rank.setAvgRating(row.avgRating());
            rank.setReviewCount(row.reviewCount());
            rank.setUpdatedAt(LocalDateTime.now());
            return rank;
        }).collect(Collectors.toList());

        rankRepository.saveAll(ranks);
        log.info("Rank aggregation completed. Total: {}", ranks.size());
    }

    public List<Rank> getRank(String genre) {
        if (genre != null && !genre.isBlank()) {
            return rankRepository.findTop10ByGenreContainingOrderByAvgRatingDescReviewCountDesc(genre);
        }
        return rankRepository.findAllByOrderByAvgRatingDescReviewCountDesc();
    }

    public Map<String, List<Rank>> getRankByGenre() {
        return rankRepository.findAllGroupable().stream()
                .collect(Collectors.groupingBy(
                        Rank::getGenre,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream().limit(10).collect(Collectors.toList())
                        )
                ));
    }
}
