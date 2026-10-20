package com.example.book_talker_backend.review.dao;

import com.example.book_talker_backend.book.dao.BookRepository;
import com.example.book_talker_backend.book.entity.Book;
import com.example.book_talker_backend.review.entity.Review;
import com.example.book_talker_backend.review.entity.ReviewVisibilityEnum;
import com.example.book_talker_backend.review.entity.dto.BookRatingStats;
import com.example.book_talker_backend.review.entity.dto.TeamReviewDetailResponse;
import com.example.book_talker_backend.review.entity.dto.TeamReviewItem;
import com.example.book_talker_backend.team.dao.TeamMemberRepository;
import com.example.book_talker_backend.team.dao.TeamRepository;
import com.example.book_talker_backend.team.entity.MemberRoleEnum;
import com.example.book_talker_backend.team.entity.MemberStatusEnum;
import com.example.book_talker_backend.team.entity.Team;
import com.example.book_talker_backend.team.entity.TeamMember;
import com.example.book_talker_backend.user.dao.OAuth2UserRepository;
import com.example.book_talker_backend.user.entity.OAuth2UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ReviewRepositoryTest {
    @Autowired ReviewRepository reviewRepository;
    @Autowired BookRepository bookRepository;
    @Autowired TeamRepository teamRepository;
    @Autowired TeamMemberRepository teamMemberRepository;
    @Autowired OAuth2UserRepository oAuth2UserRepository;
    @Autowired TestEntityManager em;

    @ServiceConnection
    static PostgreSQLContainer postgreSQLContainer =
            new PostgreSQLContainer(DockerImageName.parse("postgres:13.10-alpine"));

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
        assertThat(stats.getIsbn13()).isEqualTo("1234567891011");
        assertThat(stats.getReviewCount()).isEqualTo(3);
        assertThat(stats.getAvgRating()).isCloseTo(4.0, within(0.0001));
    }

    @Test
    void 회독수가_2이상인_리뷰는_집계에서_제외된다 () {
        Book dummyBook = bookRepository.save(book("1234567891011"));

        List<Review> dummyReview = List.of(
            review(dummyBook, "one", 3),
            review(dummyBook, "two", 4),
            review(dummyBook, "three", 5),
            review(dummyBook, "one", 3, 2),
            review(dummyBook, "two", 4, 2)
        );

        reviewRepository.saveAll(dummyReview);

        em.flush();
        em.clear();

        List<BookRatingStats> result = reviewRepository.aggregateRankData();
        assertThat(result).hasSize(1);
        BookRatingStats stats = result.get(0);
        assertThat(stats.getIsbn13()).isEqualTo("1234567891011");
        assertThat(stats.getReviewCount()).isEqualTo(3);
        assertThat(stats.getAvgRating()).isCloseTo(4.0, within(0.0001));
    }

    @Nested
    @DisplayName("모임 독후감 조회")
    class TeamReview {
        @Test
        void 모임_독후감_조회_성공() {
            Team naver = saveTeam("naver");
            saveMember(naver, "userA", "치이카와", MemberStatusEnum.ACTIVE);
            saveMember(naver, "userB", "하치와레", MemberStatusEnum.ACTIVE);

            Book book = bookRepository.save(book("1234567891011"));
            reviewRepository.saveAll(List.of(
                    review(book, "userA", day(13), ReviewVisibilityEnum.PUBLIC),
                    review(book, "userB", day(15), ReviewVisibilityEnum.GROUP)
            ));

            em.flush();
            em.clear();

            Page<TeamReviewItem> result = reviewRepository.findAllByTeamReview(naver.getTeamId(), Pageable.ofSize(10));
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result).extracting(TeamReviewItem::displayName).containsExactly("하치와레", "치이카와");
        }

        @Test
        void 모임_독후감_조회_4개씩_페이지_분할() {
            Team naver = saveTeam("naver");
            saveMember(naver, "userA", "치이카와", MemberStatusEnum.ACTIVE);
            saveMember(naver, "userB", "하치와레", MemberStatusEnum.ACTIVE);
            saveMember(naver, "userC", "우사기", MemberStatusEnum.ACTIVE);
            saveMember(naver, "userD", "모몽가", MemberStatusEnum.ACTIVE);
            saveMember(naver, "userE", "랏코", MemberStatusEnum.ACTIVE);

            Book book = bookRepository.save(book("1234567891011"));
            reviewRepository.saveAll(List.of(
                    review(book, "userA", day(11), ReviewVisibilityEnum.PUBLIC),
                    review(book, "userB", day(12), ReviewVisibilityEnum.PUBLIC),
                    review(book, "userC", day(13), ReviewVisibilityEnum.PUBLIC),
                    review(book, "userD", day(14), ReviewVisibilityEnum.PUBLIC),
                    review(book, "userE", day(15), ReviewVisibilityEnum.PUBLIC)
            ));

            em.flush();
            em.clear();

            Page<TeamReviewItem> firstPage = reviewRepository.findAllByTeamReview(naver.getTeamId(), PageRequest.of(0, 4));
            assertThat(firstPage.getTotalElements()).isEqualTo(5);
            assertThat(firstPage.getTotalPages()).isEqualTo(2);
            assertThat(firstPage).extracting(TeamReviewItem::displayName).containsExactly("랏코", "모몽가", "우사기", "하치와레");

            Page<TeamReviewItem> secondPage = reviewRepository.findAllByTeamReview(naver.getTeamId(), PageRequest.of(1, 4));
            assertThat(secondPage).extracting(TeamReviewItem::displayName).containsExactly("치이카와");
        }

        @Test
        void 모임_독후감_조회_다른_모임_모임원이면_제외() {
            Team naver = saveTeam("naver");
            Team line = saveTeam("line");
            saveMember(naver, "userA", "치이카와", MemberStatusEnum.ACTIVE);
            saveMember(naver, "userB", "하치와레", MemberStatusEnum.ACTIVE);
            saveMember(line, "userC", "우사기", MemberStatusEnum.ACTIVE);

            Book book = bookRepository.save(book("1234567891011"));
            reviewRepository.saveAll(List.of(
                    review(book, "userA", day(13), ReviewVisibilityEnum.PUBLIC),
                    review(book, "userB", day(15), ReviewVisibilityEnum.GROUP),
                    review(book, "userC", day(13), ReviewVisibilityEnum.PUBLIC)
            ));

            em.flush();
            em.clear();

            Page<TeamReviewItem> result = reviewRepository.findAllByTeamReview(naver.getTeamId(), Pageable.ofSize(10));
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result).extracting(TeamReviewItem::displayName).containsExactly("하치와레", "치이카와");
        }

        @Test
        void 모임_독후감_조회_두_모임에_속했으면_조회한_모임_닉네임으로_한번만() {
            Team naver = saveTeam("naver");
            Team line = saveTeam("line");
            saveMember(naver, "userA", "치이카와", MemberStatusEnum.ACTIVE);
            saveMember(naver, "userB", "하치와레", MemberStatusEnum.ACTIVE);
            saveMember(line, "userB", "모몽가", MemberStatusEnum.ACTIVE);
            saveMember(line, "userC", "우사기", MemberStatusEnum.ACTIVE);

            Book book = bookRepository.save(book("1234567891011"));
            reviewRepository.saveAll(List.of(
                    review(book, "userA", day(13), ReviewVisibilityEnum.PUBLIC),
                    review(book, "userB", day(15), ReviewVisibilityEnum.GROUP),
                    review(book, "userC", day(13), ReviewVisibilityEnum.PUBLIC)
            ));

            em.flush();
            em.clear();

            Page<TeamReviewItem> result = reviewRepository.findAllByTeamReview(naver.getTeamId(), Pageable.ofSize(10));
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result).extracting(TeamReviewItem::displayName).containsExactly("하치와레", "치이카와");
        }

        @Test
        void 모임_독후감_조회_PRIVATE면_제외() {
            Team naver = saveTeam("naver");
            saveMember(naver, "userA", "치이카와", MemberStatusEnum.ACTIVE);
            saveMember(naver, "userB", "하치와레", MemberStatusEnum.ACTIVE);

            Book book = bookRepository.save(book("1234567891011"));
            Book otherBook = bookRepository.save(book("1234567891028"));
            reviewRepository.saveAll(List.of(
                    review(book, "userA", day(13), ReviewVisibilityEnum.PUBLIC),
                    review(book, "userB", day(15), ReviewVisibilityEnum.GROUP),
                    review(otherBook, "userA", day(18), ReviewVisibilityEnum.PRIVATE)   // 가장 최신 - 필터가 빠지면 맨 앞에 나옴
            ));

            em.flush();
            em.clear();

            Page<TeamReviewItem> result = reviewRepository.findAllByTeamReview(naver.getTeamId(), Pageable.ofSize(10));
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result).extracting(TeamReviewItem::displayName).containsExactly("하치와레", "치이카와");
        }

        @ParameterizedTest
        @EnumSource(value = MemberStatusEnum.class, names = {"INACTIVE", "BANNED"})
        void 모임_독후감_조회_INACTIVE_BANNED면_제외(MemberStatusEnum status) {
            Team naver = saveTeam("naver");
            saveMember(naver, "userA", "치이카와", MemberStatusEnum.ACTIVE);
            saveMember(naver, "userB", "하치와레", MemberStatusEnum.ACTIVE);
            saveMember(naver, "userC", "우사기", status);

            Book book = bookRepository.save(book("1234567891011"));
            reviewRepository.saveAll(List.of(
                    review(book, "userA", day(13), ReviewVisibilityEnum.PUBLIC),
                    review(book, "userB", day(15), ReviewVisibilityEnum.PUBLIC),
                    review(book, "userC", day(13), ReviewVisibilityEnum.PUBLIC)
            ));

            em.flush();
            em.clear();

            Page<TeamReviewItem> result = reviewRepository.findAllByTeamReview(naver.getTeamId(), Pageable.ofSize(10));
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result).extracting(TeamReviewItem::displayName).containsExactly("하치와레", "치이카와");
        }
    }


    @Nested
    @DisplayName("모임 독후감 상세 조회")
    class FindByTeamReviewDetail {
        @ParameterizedTest
        @EnumSource(value = ReviewVisibilityEnum.class, names = {"PUBLIC", "GROUP"})
        void 모임_독후감_상세_조회_성공(ReviewVisibilityEnum visibility) {
            Team naver = saveTeam("naver");
            saveMember(naver, "userA", "치이카와", MemberStatusEnum.ACTIVE);

            Book book = bookRepository.save(book("1234567891011"));
            Review review = reviewRepository.save(
                    review(book, "userA", day(13), visibility)
            );

            em.flush();
            em.clear();

            TeamReviewDetailResponse teamReviewDetail = reviewRepository.findByTeamReviewDetail(
                    naver.getTeamId(), review.getId(), "userA"
            ).orElseThrow();

            assertThat(teamReviewDetail.headline()).isEqualTo("headline");
            assertThat(teamReviewDetail.content()).isEqualTo("good");
            assertThat(teamReviewDetail.author()).isEqualTo("author");
            assertThat(teamReviewDetail.bookTitle()).isEqualTo("title");
            assertThat(teamReviewDetail.cover()).isEqualTo("cover");
            assertThat(teamReviewDetail.displayName()).isEqualTo("치이카와");
            assertThat(teamReviewDetail.teamName()).isEqualTo("naver");
            assertThat(teamReviewDetail.mine()).isTrue();
        }

        @Test
        void 모임_독후감_상세_조회_작성자가_아니면_mine은_false() {
            Team naver = saveTeam("naver");
            saveMember(naver, "userA", "치이카와", MemberStatusEnum.ACTIVE);
            saveMember(naver, "userB", "우사기", MemberStatusEnum.ACTIVE);

            Book book = bookRepository.save(book("1234567891011"));
            Review review = reviewRepository.save(
                    review(book, "userA", day(13), ReviewVisibilityEnum.PUBLIC)
            );

            em.flush();
            em.clear();

            TeamReviewDetailResponse teamReviewDetail = reviewRepository.findByTeamReviewDetail(
                    naver.getTeamId(), review.getId(), "userB"
            ).orElseThrow();

            assertThat(teamReviewDetail.bookTitle()).isEqualTo("title");
            assertThat(teamReviewDetail.cover()).isEqualTo("cover");
            assertThat(teamReviewDetail.displayName()).isEqualTo("치이카와");
            assertThat(teamReviewDetail.mine()).isFalse();
        }

        @Test
        void 모임_독후감_상세_조회_PRIVATE는_빈_결과() {
            Team naver = saveTeam("naver");
            saveMember(naver, "userA", "치이카와", MemberStatusEnum.ACTIVE);

            Book book = bookRepository.save(book("1234567891011"));
            Review review = reviewRepository.save(
                    review(book, "userA", day(13), ReviewVisibilityEnum.PRIVATE)
            );

            em.flush();
            em.clear();

            assertThat(reviewRepository.findByTeamReviewDetail(
                    naver.getTeamId(), review.getId(), "userA"
            )).isEmpty();
        }

        @ParameterizedTest
        @EnumSource(value = MemberStatusEnum.class, names = {"INACTIVE", "BANNED"})
        void 모임_독후감_상세_조회_작성자가_INACTIVE_BANNED면_빈_결과(MemberStatusEnum status) {
            Team naver = saveTeam("naver");
            saveMember(naver, "userA", "치이카와", status);
            saveMember(naver, "userB", "하치와레", MemberStatusEnum.ACTIVE);

            Book book = bookRepository.save(book("1234567891011"));
            Review review = reviewRepository.save(
                    review(book, "userA", day(13), ReviewVisibilityEnum.PUBLIC)
            );

            em.flush();
            em.clear();

            assertThat(reviewRepository.findByTeamReviewDetail(
                    naver.getTeamId(), review.getId(), "userB"
            )).isEmpty();
        }

        @Test
        void 모임_독후감_상세_조회_두_모임에_속했으면_조회한_모임_닉네임() {
            Team naver = saveTeam("naver");
            Team line = saveTeam("line");
            saveMember(naver, "userA", "치이카와", MemberStatusEnum.ACTIVE);
            saveMember(line, "userA", "모몽가", MemberStatusEnum.ACTIVE);

            Book book = bookRepository.save(book("1234567891011"));
            Review review = reviewRepository.save(
                    review(book, "userA", day(13), ReviewVisibilityEnum.PUBLIC)
            );

            em.flush();
            em.clear();

            TeamReviewDetailResponse teamReviewDetail = reviewRepository.findByTeamReviewDetail(
                    naver.getTeamId(), review.getId(), "userA"
            ).orElseThrow();

            assertThat(teamReviewDetail.displayName()).isEqualTo("치이카와");
        }

        @Test
        void 모임_독후감_상세_조회_다른_모임_소속_작성자의_글이면_빈_결과() {
            Team naver = saveTeam("naver");
            Team line = saveTeam("line");
            saveMember(naver, "userA", "치이카와", MemberStatusEnum.ACTIVE);
            saveMember(line, "userB", "모몽가", MemberStatusEnum.ACTIVE);

            Book book = bookRepository.save(book("1234567891011"));
            Review review = reviewRepository.save(
                    review(book, "userB", day(13), ReviewVisibilityEnum.PUBLIC)
            );

            em.flush();
            em.clear();

            assertThat(reviewRepository.findByTeamReviewDetail(
                    naver.getTeamId(), review.getId(), "userA"
            )).isEmpty();
        }
    }

    private Team saveTeam(String teamName) {
        Team team = new Team();
        team.setTeamName(teamName);
        team.setTeamDescription(teamName + " in book");
        team.setCode(teamName + "Code");

        return teamRepository.save(team);
    }

    // 같은 providerId로 여러 모임에 가입시킬 수 있도록 사용자는 없을 때만 생성
    private void saveMember(Team team, String providerId, String displayName, MemberStatusEnum status) {
        OAuth2UserEntity user = oAuth2UserRepository.findByProviderId(providerId);
        if (user == null) {
            user = oAuth2UserRepository.save(new OAuth2UserEntity("naver", providerId));
        }

        teamMemberRepository.save(new TeamMember(team, user, MemberRoleEnum.MEMBER, status, displayName));
    }

    private LocalDateTime day(int dayOfMonth) {
        return LocalDateTime.of(2026, Month.OCTOBER, dayOfMonth, 4, 31);
    }

    private Book book(String isbn13) {
        return new Book(isbn13, "title", "author", 
            "genre", "publisher", "cover");
    }

    private Review review(Book book, String writer, int rating) {
        return makeReview(book, writer, rating, 1, LocalDateTime.now(), ReviewVisibilityEnum.PUBLIC);
    }

    private Review review(Book book, String writer, int rating, int readingCount) {
        return makeReview(book, writer, rating, readingCount, LocalDateTime.now(), ReviewVisibilityEnum.PUBLIC);
    }

    private Review review(Book book, String writer, LocalDateTime regDate, ReviewVisibilityEnum visibility) {
        return makeReview(book, writer, 3, 1, regDate, visibility);
    }

    private Review makeReview(Book book, String writer, int rating, int readingCount, LocalDateTime regDate, ReviewVisibilityEnum visibility) {
        Review review = new Review();
        review.setBook(book);
        review.setWriter(writer);
        review.setContent("good");
        review.setHeadline("headline");
        review.setRating(rating);
        review.setRegDate(regDate);
        review.setModDate(LocalDateTime.now());
        review.setVisibility(visibility);
        review.setReadingCount(readingCount);
        
        return review;
    }
}
