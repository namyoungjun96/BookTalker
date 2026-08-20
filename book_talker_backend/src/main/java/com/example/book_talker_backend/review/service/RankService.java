package com.example.book_talker_backend.review.service;

import com.example.book_talker_backend.book.dao.BookRepository;
import com.example.book_talker_backend.book.entity.Book;
import com.example.book_talker_backend.review.WeightedScoreCalculator;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RankService {
    private final RankRepository rankRepository;
    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private static final int CONFIDENCE_THRESHOLD = 3;
    private static final int TOP_N_PER_GENRE = 10;

    @Transactional
    @Scheduled(cron = "0 0 4 * * ?")
    public void aggregateRank() {
        List<BookRatingStats> rawData = reviewRepository.aggregateRankData();

        if (rawData.isEmpty()) {
            log.info("aggregateRank -> 집계 데이터가 없습니다.");
            return;
        }

        Map<String, Book> isbn13Maps = mapBooksByIsbn13(rawData);

        List<Rank> ranks = calculateTopRanksByGenre(rawData, isbn13Maps);

        rankRepository.deleteAllInBatch();
        rankRepository.saveAll(ranks);

        log.info("Rank aggregation completed. Total: {}", ranks.size());
    }

    public Map<String, Book> mapBooksByIsbn13(List<BookRatingStats> rawData) {
        List<String> isbn13s = rawData.stream().map(BookRatingStats::getIsbn13).toList();

        List<Book> books = bookRepository.findAllById(isbn13s);

        return books.stream().collect(Collectors.toMap(Book::getIsbn13, book -> book));
    }

    public List<Rank> calculateTopRanksByGenre(List<BookRatingStats> rawData, Map<String, Book> isbn13Maps) {
        double globalAverage = WeightedScoreCalculator.calculateGlobalAverage(rawData);

        Map<String, List<Rank>> grouped = buildRankCandidate(rawData, isbn13Maps, globalAverage);

        return grouped.values().stream()
                .flatMap(item -> item.stream()
                        .sorted(Comparator.comparing(Rank::getWeightedScore).reversed())
                        .limit(TOP_N_PER_GENRE))
                .toList();
    }

    public Map<String, List<Rank>> buildRankCandidate(List<BookRatingStats> rawData, Map<String, Book> isbn13Maps, double globalAverage) {
        return rawData.stream().map(row -> {
                    Book book = isbn13Maps.get(row.getIsbn13());
                    if (book == null) {
                        log.error("{} -> aggregateRank ::: 랭킹 계산 배치 도중 잘못된 데이터 입니다.", row.getIsbn13());
                        return null;
                    }

                    return mergeToRank(row, book, globalAverage);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Rank::getGenre));
    }

    private Rank mergeToRank(BookRatingStats row, Book book, double globalAverage) {
        Rank rank = new Rank();
        rank.setIsbn13(row.getIsbn13());
        rank.setGenre(book.getGenre());
        rank.setTitle(book.getTitle());
        rank.setCover(book.getCover());
        rank.setWeightedScore(WeightedScoreCalculator.calculateWeightedScore(
                row.getReviewCount(), row.getAvgRating(), CONFIDENCE_THRESHOLD, globalAverage));
        rank.setAvgRating(row.getAvgRating());
        rank.setReviewCount(row.getReviewCount());
        rank.setUpdatedAt(LocalDateTime.now());
        return rank;
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
