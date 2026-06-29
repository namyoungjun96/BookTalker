package com.example.book_talker_backend.review.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.book_talker_backend.book.dao.BookRepository;
import com.example.book_talker_backend.book.entity.Book;
import com.example.book_talker_backend.review.dao.RankRepository;
import com.example.book_talker_backend.review.dao.ReviewRepository;
import com.example.book_talker_backend.review.entity.Rank;
import com.example.book_talker_backend.review.entity.Review;

@SpringBootTest
public class RankServiceIntegrationTest {
    @Autowired
    RankRepository rankRepository;
    @Autowired
    BookRepository bookRepository;
    @Autowired
    ReviewRepository reviewRepository;
    @Autowired
    RankService rankService;

    @Test
    void 배치_집계_데이터가_없으면_종료() {
        Rank rank = rankHelper();

        rankRepository.save(rank);

        rankService.aggregateRank();

        List<Rank> ranks = rankRepository.findAll();

        assertThat(ranks).hasSize(1);
        Rank result = ranks.get(0);
        assertThat(result.getIsbn13()).isEqualTo("1234567891011");
    }

    @Test
    void 배치_집계_데이터_생성 () {
        Rank rank = rankHelper();
        rankRepository.save(rank);

        Book book = book("1234567891112", "title", "genre");
        bookRepository.save(book);

        List<Review> reviews = reviewHelper(book, 3);
        reviewRepository.saveAll(reviews);

        rankService.aggregateRank();

        List<Rank> ranks = rankRepository.findAll();
        assertThat(ranks).hasSize(1);
        Rank result = ranks.get(0);
        assertThat(result.getIsbn13()).isEqualTo("1234567891112");
    }

    @AfterEach
    void clearRankData() {
        rankRepository.deleteAll();
        reviewRepository.deleteAll();
        bookRepository.deleteAll();
    }

    private Book book(String isbn13, String title, String genre) {
        return new Book(isbn13, title, "author", 
            genre, "publisher", "cover");
    }

    private Rank rankHelper () {
        Rank rank = new Rank();
        rank.setIsbn13("1234567891011");
        rank.setGenre("genre");
        rank.setAvgRating(4.5);
        rank.setWeightedScore(3.3);
        rank.setReviewCount(10);
        rank.setTitle("title");
        rank.setCover("cover");
        rank.setUpdatedAt(LocalDateTime.now());

        return rank;
    }

    private List<Review> reviewHelper(Book book, int index) {
        List<Review> result = new ArrayList<>();

        for (int i=0; i<index; i++) {
            result.add(review(book, "writer"+i, 3));
        }

        return result;
    }

    private Review review(Book book, String writer, int rating) {
        Review review = new Review();
        review.setBook(book);
        review.setWriter(writer);
        review.setContent("good");
        review.setHeadline("headline");
        review.setRating(rating);
        review.setRegDate(LocalDateTime.now());
        review.setModDate(LocalDateTime.now());
        review.setIsPublic(true);
        review.setReadingCount(1);
        
        return review;
    }
}
