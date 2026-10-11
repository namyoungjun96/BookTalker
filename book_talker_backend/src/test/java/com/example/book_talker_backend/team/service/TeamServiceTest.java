package com.example.book_talker_backend.team.service;

import com.example.book_talker_backend.team.dao.TeamMemberRepository;
import com.example.book_talker_backend.team.dao.TeamRepository;
import com.example.book_talker_backend.team.entity.MemberRoleEnum;
import com.example.book_talker_backend.team.entity.MemberStatusEnum;
import com.example.book_talker_backend.team.entity.Team;
import com.example.book_talker_backend.team.entity.TeamMember;
import com.example.book_talker_backend.team.entity.dto.*;
import com.example.book_talker_backend.team.exception.*;
import com.example.book_talker_backend.user.dao.OAuth2UserRepository;
import com.example.book_talker_backend.user.entity.OAuth2UserEntity;
import com.example.book_talker_backend.user.exception.NotFoundUserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.UnexpectedRollbackException;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(TeamService.class)
@Testcontainers
public class TeamServiceTest {
    @Autowired TeamService teamService;
    @Autowired TeamRepository teamRepository;
    @Autowired TeamMemberRepository teamMemberRepository;
    @Autowired OAuth2UserRepository oAuth2UserRepository;
    @Autowired TestEntityManager em;

    @ServiceConnection
    static PostgreSQLContainer postgreSQLContainer =
            new PostgreSQLContainer(DockerImageName.parse("postgres:13.10-alpine"));

    @BeforeEach
    void setUp() {
        oAuth2UserRepository.save(new OAuth2UserEntity("naver", "userA"));
        oAuth2UserRepository.save(new OAuth2UserEntity("naver", "userB"));
        oAuth2UserRepository.save(new OAuth2UserEntity("naver", "userC"));

        em.flush();
        em.clear();
    }

    private CreateTeamRequest createTeamRequest(String teamName) {
        return new CreateTeamRequest(teamName, teamName + " in book", "owner");
    }

    /** userA가 모임장인 모임을 만들고 teamId 반환 (flush·clear 완료) */
    private Long createTeamOwnedByA() {
        Long teamId = teamService.createTeam(createTeamRequest("naver"), "userA").teamId();

        em.flush();
        em.clear();

        return teamId;
    }

    /** userA가 모임장인 모임에 userB를 MEMBER·지정 상태로 추가하고 teamId 반환 (flush·clear 완료) */
    private Long createTeamWithMemberB(MemberStatusEnum status) {
        Long teamId = teamService.createTeam(createTeamRequest("naver"), "userA").teamId();
        insertMember(teamId, "userB", status);

        em.flush();
        em.clear();

        return teamId;
    }

    /** 서비스(addMember·가입)를 거치지 않고 모임원 행을 직접 저장 — 테스트 대상 외 로직에 의존하지 않기 위함 */
    private void insertMember(Long teamId, String providerId, MemberStatusEnum status) {
        Team team = teamRepository.findById(teamId).orElseThrow();
        OAuth2UserEntity user = oAuth2UserRepository.findByProviderId(providerId);

        teamMemberRepository.save(new TeamMember(team, user, MemberRoleEnum.MEMBER, status, providerId));
    }

    /** 모임의 초대 코드 조회 */
    private String inviteCodeOf(Long teamId) {
        return teamRepository.findById(teamId).orElseThrow().getCode();
    }

    @Nested
    @DisplayName("팀 crud")
    class TeamCRUD {
        @Test
        void 모임_생성_성공() {
            CreateTeamResponse response = teamService.createTeam(new CreateTeamRequest("mobigen", "mobigen in book", "owner"), "userA");

            em.flush();
            em.clear();

            Team team = teamRepository.findById(response.teamId()).orElse(null);

            assertThat(team).isNotNull();
            assertThat(team.getTeamName()).isEqualTo("mobigen");

            TeamMember owner = teamMemberRepository.findByoAuth2User_ProviderIdAndTeam_TeamId("userA", team.getTeamId()).get();

            assertThat(owner.getRole()).isEqualTo(MemberRoleEnum.OWNER);
            assertThat(owner.getStatus()).isEqualTo(MemberStatusEnum.ACTIVE);
            assertThat(owner.getDisplayName()).isEqualTo("owner");
        }

        @Test
        void 모임_생성_초대코드_함께_발급() {
            Long teamId = createTeamOwnedByA();
            String code = inviteCodeOf(teamId);

            assertThat(code).matches("[a-zA-Z0-9]{13}");
        }

        @Test
        void 모임_생성_DB에_없는_사용자면_404() {
            assertThatThrownBy(() -> teamService
                    .createTeam(new CreateTeamRequest("mobigen", "mobigen in book", "owner"), "userX")
            ).isInstanceOf(NotFoundUserException.class);
        }

        @Test
        void 모임_목록_ACTIVE인_모임만_조회() {
            CreateTeamResponse mobigen = teamService.createTeam(createTeamRequest("mobigen"), "userA");
            CreateTeamResponse naver = teamService.createTeam(createTeamRequest("naver"), "userA");
            CreateTeamResponse line = teamService.createTeam(createTeamRequest("line"), "userA");

            insertMember(naver.teamId(), "userB", MemberStatusEnum.ACTIVE);
            insertMember(naver.teamId(), "userC", MemberStatusEnum.BANNED);
            insertMember(line.teamId(), "userB", MemberStatusEnum.PENDING);
            insertMember(mobigen.teamId(), "userB", MemberStatusEnum.BANNED);

            em.flush();
            em.clear();

            List<TeamListResponse> teams = teamService.getMyTeams("userB");

            assertThat(teams).isNotNull();
            assertThat(teams).hasSize(1);
            assertThat(teams.get(0).teamName()).isEqualTo("naver");
            assertThat(teams.get(0).memberCount()).isEqualTo(2);
        }

        @Test
        void 모임_목록_가입한_모임이_없으면_빈_리스트() {
            List<TeamListResponse> teams = teamService.getMyTeams("userB");

            assertThat(teams).isEmpty();
        }

        @Test
        void 모임_수정_성공() {
            Long teamId = createTeamOwnedByA();

            teamService.updateTeam(teamId, new UpdateTeamRequest("ver", "book in naver"), "userA");

            em.flush();
            em.clear();

            Team team = teamRepository.findById(teamId).orElseThrow();

            assertThat(team.getTeamName()).isEqualTo("ver");
            assertThat(team.getTeamDescription()).isEqualTo("book in naver");
        }

        @Test
        void 모임_수정_비모임원이면_404() {
            Long teamId = createTeamOwnedByA();

            assertThatThrownBy(() -> teamService.updateTeam(
                    teamId,
                    new UpdateTeamRequest("ver", "book in ver"), "userB"))
                    .isInstanceOf(NotFoundTeamException.class);
        }

        @Test
        void 모임_수정_모임장이_아니면_403() {
            Long teamId = createTeamWithMemberB(MemberStatusEnum.ACTIVE);

            assertThatThrownBy(() -> teamService.updateTeam(teamId, new UpdateTeamRequest("ver", "book in ver"), "userB"))
                    .isInstanceOf(TeamAccessDeniedException.class);
        }

        @Test
        void 모임_수정_없는_teamId면_404() {
            assertThatThrownBy(() -> teamService.updateTeam(999L, new UpdateTeamRequest("a", "b"), "userA"))
                    .isInstanceOf(NotFoundTeamException.class);
        }

        @ParameterizedTest
        @EnumSource(value = MemberStatusEnum.class, names = {"INACTIVE", "BANNED"})
        void 모임_수정_ACTIVE가_아니면_404(MemberStatusEnum status) {
            Long teamId = createTeamWithMemberB(status);

            assertThatThrownBy(() -> teamService.updateTeam(teamId, new UpdateTeamRequest("ver", "book in ver"), "userB"))
                    .isInstanceOf(NotFoundTeamException.class);
        }

        @Test
        void 모임_삭제_모임원도_함께_삭제() {
            Long teamId = createTeamWithMemberB(MemberStatusEnum.ACTIVE);

            teamService.deleteTeam(teamId, "userA");

            em.flush();
            em.clear();

            assertThat(teamRepository.findById(teamId)).isEmpty();
            assertThat(teamMemberRepository.findByoAuth2User_ProviderIdAndTeam_TeamId("userA", teamId)).isEmpty();
            assertThat(teamMemberRepository.findByoAuth2User_ProviderIdAndTeam_TeamId("userB", teamId)).isEmpty();
        }

        @Test
        void 모임_삭제_비모임원이면_404() {
            Long teamId = createTeamOwnedByA();

            assertThatThrownBy(() -> teamService.deleteTeam(teamId, "userB"))
                    .isInstanceOf(NotFoundTeamException.class);
        }

        @Test
        void 모임_삭제_모임장이_아니면_403() {
            Long teamId = createTeamWithMemberB(MemberStatusEnum.ACTIVE);

            assertThatThrownBy(() -> teamService.deleteTeam(teamId, "userB"))
                    .isInstanceOf(TeamAccessDeniedException.class);
        }

        @Test
        void 모임_삭제_없는_teamId면_404() {
            assertThatThrownBy(() -> teamService.deleteTeam(999L, "userA"))
                    .isInstanceOf(NotFoundTeamException.class);
        }

        @ParameterizedTest
        @EnumSource(value = MemberStatusEnum.class, names = {"INACTIVE", "BANNED"})
        void 모임_삭제_ACTIVE가_아니면_404(MemberStatusEnum status) {
            Long teamId = createTeamWithMemberB(status);

            assertThatThrownBy(() -> teamService.deleteTeam(teamId, "userB"))
                    .isInstanceOf(NotFoundTeamException.class);
        }
    }

    @Test
    void 롤백온리_실험() {
        Long teamId = createTeamOwnedByA();

        assertThatThrownBy(() -> teamService.updateTeam(teamId, new UpdateTeamRequest("ver", "book in ver"), "userB"))
                .isInstanceOf(NotFoundTeamException.class);

        TestTransaction.flagForCommit();
        assertThatThrownBy(() -> TestTransaction.end())
                .isInstanceOf(UnexpectedRollbackException.class);
    }

    @Test
    void 모임원_등록_성공() {
        Long teamId = createTeamOwnedByA();

        teamService.addMember(teamId, "userB", "userA", "displayName");
        em.flush();
        em.clear();

        TeamMember member = teamMemberRepository.findByoAuth2User_ProviderIdAndTeam_TeamId("userB", teamId).get();

        assertThat(member.getDisplayName()).isEqualTo("displayName");
        assertThat(member.getRole()).isEqualTo(MemberRoleEnum.MEMBER);
        assertThat(member.getStatus()).isEqualTo(MemberStatusEnum.ACTIVE);
    }

    @Test
    void 모임원_등록_DB에_없는_사용자면_404() {
        Long teamId = createTeamOwnedByA();

        assertThatThrownBy(() -> teamService.addMember(teamId, "X", "userA", "displayName"))
                .isInstanceOf(NotFoundUserException.class);
    }

    @Test
    void 모임원_등록_이미_ACTIVE면_409() {
        Long teamId = createTeamWithMemberB(MemberStatusEnum.ACTIVE);

        assertThatThrownBy(() -> teamService.addMember(teamId, "userB", "userA", "displayName"))
                .isInstanceOf(DuplicateTeamMemberException.class);
    }

    @Test
    void 모임원_등록_INACTIVE면_재가입() {
        Long teamId = createTeamWithMemberB(MemberStatusEnum.INACTIVE);
        TeamMember originalMemberB = teamMemberRepository.findByoAuth2User_ProviderIdAndTeam_TeamId("userB", teamId).orElseThrow();

        teamService.addMember(teamId, "userB","userA", "activeUser");

        em.flush();
        em.clear();

        TeamMember memberB = teamMemberRepository.findByoAuth2User_ProviderIdAndTeam_TeamId("userB", teamId).orElseThrow();

        assertThat(memberB.getTeamMemberId()).isEqualTo(originalMemberB.getTeamMemberId());
        assertThat(memberB.getDisplayName()).isEqualTo("activeUser");
        assertThat(memberB.getStatus()).isEqualTo(MemberStatusEnum.ACTIVE);
        assertThat(memberB.getRole()).isEqualTo(MemberRoleEnum.MEMBER);
    }

    @Test
    void 모임원_등록_BANNED면_409() {
        Long teamId = createTeamWithMemberB(MemberStatusEnum.BANNED);

        assertThatThrownBy(() -> teamService.addMember(teamId, "userB", "userA", "displayName"))
                .isInstanceOf(BannedTeamMemberException.class);
    }

    @Nested
    @DisplayName("초대코드")
    class InviteCode {
        @Test
        void 초대코드_조회_성공() {
            Long teamId = createTeamOwnedByA();

            Team team = teamRepository.findById(teamId).orElseThrow();
            InviteCodeResponse inviteCode = teamService.getTeamInviteCode(teamId, "userA");

            assertThat(inviteCode.code()).isEqualTo(team.getCode());
        }

        @Test
        void 초대코드_조회_모임장이_아니면_403() {
            Long teamId = createTeamWithMemberB(MemberStatusEnum.ACTIVE);

            assertThatThrownBy(() -> teamService.getTeamInviteCode(teamId, "userB"))
                    .isInstanceOf(TeamAccessDeniedException.class);
        }

        @Test
        void 초대코드_조회_없는_teamId거나_비모임원이면_404() {
            assertThatThrownBy(() -> teamService.getTeamInviteCode(9999L, "userA"))
                    .isInstanceOf(NotFoundTeamException.class);

            Long teamId = createTeamOwnedByA();

            assertThatThrownBy(() -> teamService.getTeamInviteCode(teamId, "userC"))
                    .isInstanceOf(NotFoundTeamException.class);
        }

        @ParameterizedTest
        @EnumSource(value = MemberStatusEnum.class, names = {"INACTIVE", "BANNED"})
        void 초대코드_조회_ACTIVE가_아니면_404(MemberStatusEnum status) {
            Long teamId = createTeamWithMemberB(status);

            assertThatThrownBy(() -> teamService.getTeamInviteCode(teamId, "userB"))
                    .isInstanceOf(NotFoundTeamException.class);
        }

        @Test
        void 초대코드_가입_성공() {
            Long teamId = createTeamOwnedByA();
            Team teamNaver = teamRepository.findById(teamId).orElseThrow();
            JoinTeamResponse response = teamService.joinTeamByInviteCode(new JoinByInviteCodeRequest(teamNaver.getCode(), "userB"), "userB");

            em.flush();
            em.clear();

            TeamMember member = teamMemberRepository.findByoAuth2User_ProviderIdAndTeam_TeamId("userB", teamNaver.getTeamId()).orElseThrow();

            assertThat(response.teamId()).isEqualTo(teamNaver.getTeamId());
            assertThat(member.getDisplayName()).isEqualTo("userB");
            assertThat(member.getRole()).isEqualTo(MemberRoleEnum.MEMBER);
            assertThat(member.getStatus()).isEqualTo(MemberStatusEnum.ACTIVE);
        }

        @Test
        void 초대코드_가입_없는_코드면_404() {
            assertThatThrownBy(() -> teamService.joinTeamByInviteCode(
                    new JoinByInviteCodeRequest("dsafmlkasmfvlasdmlkfc",
                            "hello"), "userA"))
                    .isInstanceOf(NotFoundInviteCodeException.class);
        }

        @Test
        void 초대코드_가입_DB에_없는_사용자면_404() {
            Long teamId = createTeamOwnedByA();
            assertThatThrownBy(() -> teamService.joinTeamByInviteCode(new JoinByInviteCodeRequest(inviteCodeOf(teamId), "userX"), "userX"))
                    .isInstanceOf(NotFoundUserException.class);
        }

        @Test
        void 초대코드_가입_이미_ACTIVE면_409() {
            Long teamId = createTeamWithMemberB(MemberStatusEnum.ACTIVE);

            assertThatThrownBy(() -> teamService.joinTeamByInviteCode(new JoinByInviteCodeRequest(inviteCodeOf(teamId), "userB"), "userB"))
                    .isInstanceOf(DuplicateTeamMemberException.class);
        }

        @Test
        void 초대코드_가입_INACTIVE면_재가입() {
            Long teamId = createTeamWithMemberB(MemberStatusEnum.INACTIVE);
            TeamMember originalMemberB = teamMemberRepository.findByoAuth2User_ProviderIdAndTeam_TeamId("userB", teamId).orElseThrow();
            String code = inviteCodeOf(teamId);

            teamService.joinTeamByInviteCode(new JoinByInviteCodeRequest(code, "activeUser"), "userB");

            em.flush();
            em.clear();

            TeamMember memberB = teamMemberRepository.findByoAuth2User_ProviderIdAndTeam_TeamId("userB", teamId).orElseThrow();

            assertThat(memberB.getTeamMemberId()).isEqualTo(originalMemberB.getTeamMemberId());
            assertThat(memberB.getDisplayName()).isEqualTo("activeUser");
            assertThat(memberB.getStatus()).isEqualTo(MemberStatusEnum.ACTIVE);
            assertThat(memberB.getRole()).isEqualTo(MemberRoleEnum.MEMBER);
        }

        @Test
        void 초대코드_가입_BANNED면_404() {
            Long teamId = createTeamWithMemberB(MemberStatusEnum.BANNED);

            assertThatThrownBy(() -> teamService.joinTeamByInviteCode(new JoinByInviteCodeRequest(inviteCodeOf(teamId), "userB"), "userB"))
                    .isInstanceOf(NotFoundInviteCodeException.class);
        }
    }
}
