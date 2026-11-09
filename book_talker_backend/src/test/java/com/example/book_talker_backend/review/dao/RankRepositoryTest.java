package com.example.book_talker_backend.review.dao;

import com.example.book_talker_backend.review.entity.Rank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RankRepositoryTest {
    @Autowired
    RankRepository rankRepository;
    @Autowired
    TestEntityManager em;

    private static final String ISBN13_1 = "1234567891011";
    private static final String ISBN13_2 = "1234567891112";
    private static final String ISBN13_3 = "1234567891131";

    @ServiceConnection
    static PostgreSQLContainer postgreSQLContainer =
            new PostgreSQLContainer(DockerImageName.parse("postgres:13.10-alpine"));

    @Nested
    @DisplayName("장르별 그룹 랭킹")
    class FindAllGroupable {
        @Test
        void 랭킹은_베이즈_평점_순으로_정렬() {
            List<Rank> ranks = List.of(
                    rank(ISBN13_1, "philosophy", 4.3, 3.5, 3, "이방인"),
                    rank(ISBN13_2, "philosophy", 4.0, 3.9, 50, "마흔에 읽는 괴테")
            );

            rankRepository.saveAll(ranks);

            em.flush();
            em.clear();

            List<Rank> rankList = rankRepository.findAllGroupable();

            assertThat(rankList).hasSize(ranks.size());
            assertThat(rankList).extracting(Rank::getIsbn13).containsExactly(ISBN13_2, ISBN13_1);
        }

        @Test
        void 베이즈_평점이_같으면_리뷰_개수_순서대로_정렬() {
            List<Rank> ranks = List.of(
                    rank(ISBN13_1, "philosophy", 4.3, 3.5, 3, "이방인"),
                    rank(ISBN13_2, "philosophy", 3.7, 3.5, 50, "마흔에 읽는 괴테"),
                    rank(ISBN13_3, "philosophy", 3.0, 3.0, 20, "시지푸 신화")
            );

            rankRepository.saveAll(ranks);

            em.flush();
            em.clear();

            List<Rank> rankList = rankRepository.findAllGroupable();

            assertThat(rankList).hasSize(ranks.size());
            assertThat(rankList).extracting(Rank::getIsbn13).containsExactly(ISBN13_2, ISBN13_1, ISBN13_3);
        }
    }


    Rank rank(String isbn13, String genre, double avgRating, double weightedScore, int reviewCount, String title) {
        Rank rank = new Rank();
        rank.setIsbn13(isbn13);
        rank.setGenre(genre);
        rank.setAvgRating(avgRating);
        rank.setWeightedScore(weightedScore);
        rank.setReviewCount(reviewCount);
        rank.setTitle(title);
        return rank;
    }
}