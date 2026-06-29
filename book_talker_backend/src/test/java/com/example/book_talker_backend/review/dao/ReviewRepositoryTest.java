package com.example.book_talker_backend.review.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.example.book_talker_backend.book.dao.BookRepository;
import com.example.book_talker_backend.book.entity.Book;
import com.example.book_talker_backend.review.entity.Review;
import com.example.book_talker_backend.review.entity.dto.BookRatingStats;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ReviewRepositoryTest {
    @Autowired ReviewRepository reviewRepository;
    @Autowired BookRepository bookRepository;
    @Autowired TestEntityManager em;

    @Test
    void 리뷰_3개는_집계되고_2개는_제외된다 () {
        Book dummyBook = bookRepository.save(book("1234567891011"));
        Book boundaryDummyBook = bookRepository.save(book("1234567891112"));

        List<Review> dummyReview = List.of(
            review(dummyBook, "one", 3),
            review(dummyBook, "two", 4),
            review(dummyBook, "three", 5)
        );
        List<Review> boundaryDummyReview = List.of(
            review(boundaryDummyBook, "one", 3),
            review(boundaryDummyBook, "two", 4)
        );
        
        reviewRepository.saveAll(dummyReview);
        reviewRepository.saveAll(boundaryDummyReview);

        em.flush();
        em.clear();

        List<BookRatingStats> result = reviewRepository.aggregateRankData();
        assertThat(result).hasSize(1);
        BookRatingStats stats = result.get(0);
        assertThat(stats.isbn13()).isEqualTo("1234567891011");
        assertThat(stats.reviewCount()).isEqualTo(3);
        assertThat(stats.avgRating()).isCloseTo(4.0, within(0.0001));
    }

    @Test
    void 회독수가_2이상인_리뷰는_집계에서_제외된다 () {
        Book dummyBook = bookRepository.save(book("1234567891011"));

        List<Review> dummyReview = List.of(
            review(dummyBook, "one", 3),
            review(dummyBook, "two", 4),
            review(dummyBook, "three", 5),
            makeReview(dummyBook, "one", 3, 2),
            makeReview(dummyBook, "two", 4, 2)
        );

        reviewRepository.saveAll(dummyReview);

        em.flush();
        em.clear();

        List<BookRatingStats> result = reviewRepository.aggregateRankData();
        assertThat(result).hasSize(1);
        BookRatingStats stats = result.get(0);
        assertThat(stats.isbn13()).isEqualTo("1234567891011");
        assertThat(stats.reviewCount()).isEqualTo(3);
        assertThat(stats.avgRating()).isCloseTo(4.0, within(0.0001));
    }

    private Book book(String isbn13) {
        return new Book(isbn13, "title", "author", 
            "genre", "publisher", "cover");
    }

    private Review review(Book book, String writer, int rating) {
        return makeReview(book, writer, rating, 1);
    }

    private Review makeReview(Book book, String writer, int rating, int readingCount) {
        Review review = new Review();
        review.setBook(book);
        review.setWriter(writer);
        review.setContent("good");
        review.setHeadline("headline");
        review.setRating(rating);
        review.setRegDate(LocalDateTime.now());
        review.setModDate(LocalDateTime.now());
        review.setIsPublic(true);
        review.setReadingCount(readingCount);
        
        return review;
    }
}
