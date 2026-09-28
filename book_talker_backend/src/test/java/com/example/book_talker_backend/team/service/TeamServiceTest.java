package com.example.book_talker_backend.team.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.UnexpectedRollbackException;

import com.example.book_talker_backend.team.dao.TeamInviteRepository;
import com.example.book_talker_backend.team.dao.TeamMemberRepository;
import com.example.book_talker_backend.team.dao.TeamRepository;
import com.example.book_talker_backend.team.entity.MemberRoleEnum;
import com.example.book_talker_backend.team.entity.MemberStatusEnum;
import com.example.book_talker_backend.team.entity.Team;
import com.example.book_talker_backend.team.entity.TeamInvite;
import com.example.book_talker_backend.team.entity.TeamMember;
import com.example.book_talker_backend.team.exception.BannedTeamMemberException;
import com.example.book_talker_backend.team.exception.DuplicateTeamMemberException;
import com.example.book_talker_backend.team.exception.NotFoundInviteCodeException;
import com.example.book_talker_backend.team.exception.TeamAccessDeniedException;
import com.example.book_talker_backend.user.dao.OAuth2UserRepository;
import com.example.book_talker_backend.user.entity.OAuth2UserEntity;
import com.example.book_talker_backend.user.exception.NotFoundUserException;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@DataJpaTest
@Import(TeamService.class)
@Testcontainers
public class TeamServiceTest {
    @Autowired TeamService teamService;
    @Autowired TeamRepository teamRepository;
    @Autowired TeamMemberRepository teamMemberRepository;
    @Autowired OAuth2UserRepository oAuth2UserRepository;
    @Autowired TeamInviteRepository teamInviteRepository;
    @Autowired TestEntityManager em;

    @ServiceConnection
    static PostgreSQLContainer postgreSQLContainer =
            new PostgreSQLContainer(DockerImageName.parse("postgres:13.10-alpine"));

    @BeforeEach
    void setUp() {
        oAuth2UserRepository.save(new OAuth2UserEntity("naver", "userA"));
        oAuth2UserRepository.save(new OAuth2UserEntity("naver", "userB"));

        em.flush();
        em.clear();
    }

    @Test
    void 팀_생성_정상적() {
        teamService.createTeam("mobigen", "mobigen in book", "userA", "owner");

        em.flush();
        em.clear();

        Team team = teamRepository.findByTeamName("mobigen");

        assertNotNull(team);
        assertEquals("mobigen", team.getTeamName());

        TeamMember owner = teamMemberRepository.findByoAuth2User_ProviderIdAndTeam_TeamId("userA", team.getTeamId()).get();
        
        assertEquals(MemberRoleEnum.OWNER, owner.getRole());
        assertEquals(MemberStatusEnum.ACTIVE, owner.getStatus());
        assertEquals("owner", owner.getDisplayName());
    }

    @Test 
    void 팀_조회_ACTIVE만_뜨는지() {
        teamService.createTeam("mobigen", "mobigen in book", "userA", "owner");
        teamService.createTeam("naver", "naver in book", "userA", "owner");
        teamService.createTeam("line", "line in book", "userA", "owner");

        Team teamNaver = teamRepository.findByTeamName("naver");
        Team teamLine = teamRepository.findByTeamName("line");
        Team teamMobigen = teamRepository.findByTeamName("mobigen");
        
        OAuth2UserEntity userB = oAuth2UserRepository.findByProviderId("userB");

        teamMemberRepository.save(new TeamMember(teamNaver, userB, MemberRoleEnum.MEMBER, MemberStatusEnum.ACTIVE, "naverUserB"));
        teamMemberRepository.save(new TeamMember(teamLine, userB, MemberRoleEnum.MEMBER, MemberStatusEnum.PENDING, "lineUserB"));
        teamMemberRepository.save(new TeamMember(teamMobigen, userB, MemberRoleEnum.MEMBER, MemberStatusEnum.BANNED, "mobigenUserB"));

        em.flush();
        em.clear();

        List<Team> teams = teamService.getMyTeams("userB");

        assertNotNull(teams);
        assertEquals(1, teams.size());
        assertEquals("naver", teams.get(0).getTeamName());
    }

    @Test 
    void 팀_아무것도_없을때는_빈_리스트() {
        List<Team> teams = teamService.getMyTeams("userB");

        assertTrue(teams.isEmpty());
    }

    @Test 
    void 팀_수정_getReferenceById_검증() {
        teamService.createTeam("naver", "naver in book", "userA", "owner");

        em.flush();
        em.clear();

        Team teamNaver = teamRepository.findByTeamName("naver");
        
        teamService.updateTeam(teamNaver.getTeamId(), "naver", "book in naver", "userA");
        
        em.flush();
        em.clear();

        teamNaver = teamRepository.findByTeamName("naver");
        
        assertEquals("book in naver", teamNaver.getTeamDescription());
    }

    @Test 
    void 팀_팀에_속하지_않은_유저가_수정할_때() {
        teamService.createTeam("naver", "naver in book", "userA", "owner");

        em.flush();
        em.clear();

        Team teamNaver = teamRepository.findByTeamName("naver");

        assertThatThrownBy(() -> teamService.updateTeam(teamNaver.getTeamId(), "ver", "book in ver", "userB"))
                    .isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test 
    void 팀_수정_허가된_롤만_가능() {
        teamService.createTeam("naver", "naver in book", "userA", "owner");

        Team teamNaver = teamRepository.findByTeamName("naver");
        
        OAuth2UserEntity userB = oAuth2UserRepository.findByProviderId("userB");

        teamMemberRepository.save(new TeamMember(teamNaver, userB, MemberRoleEnum.MEMBER, MemberStatusEnum.ACTIVE, "naverUserB"));

        em.flush();
        em.clear();

        assertThatThrownBy(() -> teamService.updateTeam(teamNaver.getTeamId(), "ver", "book in ver", "userB"))
                    .isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test 
    void 팀_삭제시_팀원도_삭제되는지_검증() {
        teamService.createTeam("naver", "naver in book", "userA", "owner");
        
        Team teamNaver = teamRepository.findByTeamName("naver");
        OAuth2UserEntity userB = oAuth2UserRepository.findByProviderId("userB");

        teamMemberRepository.save(new TeamMember(teamNaver, userB, MemberRoleEnum.MEMBER, MemberStatusEnum.ACTIVE, "naverUserB"));

        em.flush();
        em.clear();

        teamService.deleteTeam(teamNaver.getTeamId(), "userA");

        em.flush();
        em.clear();

        assertTrue(teamRepository.findById(teamNaver.getTeamId()).isEmpty());
        assertTrue(teamMemberRepository.findByoAuth2User_ProviderIdAndTeam_TeamId("userA", teamNaver.getTeamId()).isEmpty());
        assertTrue(teamMemberRepository.findByoAuth2User_ProviderIdAndTeam_TeamId("userB", teamNaver.getTeamId()).isEmpty());
    }

    @Test 
    void 팀_팀에_속하지_않은_유저가_삭제할_때() {
        teamService.createTeam("naver", "naver in book", "userA", "owner");

        em.flush();
        em.clear();
        
        Team teamNaver = teamRepository.findByTeamName("naver");
        
        assertThatThrownBy(() -> teamService.deleteTeam(teamNaver.getTeamId(), "userB"))
                    .isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test 
    void 팀_삭제_허가된_롤만_가능() {
        teamService.createTeam("naver", "naver in book", "userA", "owner");
        
        Team teamNaver = teamRepository.findByTeamName("naver");
                    
        OAuth2UserEntity userB = oAuth2UserRepository.findByProviderId("userB");

        teamMemberRepository.save(new TeamMember(teamNaver, userB, MemberRoleEnum.MEMBER, MemberStatusEnum.ACTIVE, "naverUserB"));

        em.flush();
        em.clear();

        assertThatThrownBy(() -> teamService.deleteTeam(teamNaver.getTeamId(), "userB"))
                    .isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test
    void 롤백온리_실험() {
        teamService.createTeam("naver", "naver in book", "userA", "owner");
        em.flush();
        em.clear();

        Team teamNaver = teamRepository.findByTeamName("naver");

        assertThatThrownBy(() -> teamService.updateTeam(teamNaver.getTeamId(), "ver", "book in ver", "userB"))
                .isInstanceOf(TeamAccessDeniedException.class);

        TestTransaction.flagForCommit();
        assertThatThrownBy(() -> TestTransaction.end())
                .isInstanceOf(UnexpectedRollbackException.class);
    }

    @Test 
    void addMember_정상_동작() {
        teamService.createTeam("naver", "naver in book", "userA", "owner");

        Team teamNaver = teamRepository.findByTeamName("naver");
        
        teamService.addMember(teamNaver.getTeamId(), "userB", "userA", "displayName");
        em.flush();
        em.clear();

        TeamMember member = teamMemberRepository.findByoAuth2User_ProviderIdAndTeam_TeamId("userB", teamNaver.getTeamId()).get();
        
        assertEquals("displayName", member.getDisplayName());
        assertEquals(MemberRoleEnum.MEMBER, member.getRole());
        assertEquals(MemberStatusEnum.ACTIVE, member.getStatus());
    }

    @Test 
    void addMember_추가하려는_사용자가_없으면_에러() {
        teamService.createTeam("naver", "naver in book", "userA", "owner");
        em.flush();
        em.clear();

        Team teamNaver = teamRepository.findByTeamName("naver");
        
        assertThatThrownBy(() -> teamService.addMember(teamNaver.getTeamId(), "userC", "userA", "displayName"))
                .isInstanceOf(NotFoundUserException.class); 
    }

    @Test 
    void addMember_이미_등록된_사용자면_에러() {
        teamService.createTeam("naver", "naver in book", "userA", "owner");

        Team teamNaver = teamRepository.findByTeamName("naver");
        
        teamService.addMember(teamNaver.getTeamId(), "userB", "userA", "displayName");
        em.flush();
        em.clear();
        
        assertThatThrownBy(() -> teamService.addMember(teamNaver.getTeamId(), "userB", "userA", "displayName"))
                .isInstanceOf(DuplicateTeamMemberException.class); 
    }

    @Test 
    void addMember_차단된_사용자면_에러() {
        teamService.createTeam("naver", "naver in book", "userA", "owner");

        Team teamNaver = teamRepository.findByTeamName("naver");
        
        teamService.addMember(teamNaver.getTeamId(), "userB", "userA", "displayName");
        
        TeamMember member = teamMemberRepository.findByoAuth2User_ProviderIdAndTeam_TeamId("userB", teamNaver.getTeamId()).get();

        member.setStatus(MemberStatusEnum.BANNED);
        em.flush();
        em.clear();
        
        assertThatThrownBy(() -> teamService.addMember(teamNaver.getTeamId(), "userB", "userA", "displayName"))
                .isInstanceOf(BannedTeamMemberException.class); 
    }

    @Test 
    void createInviteCode_정상_동작() {
        teamService.createTeam("naver", "naver in book", "userA", "owner");
        Team teamNaver = teamRepository.findByTeamName("naver");
        String code = teamService.createInviteCode(teamNaver.getTeamId(), "userA");
        
        em.flush();
        em.clear();

        TeamInvite inviteCode = teamInviteRepository.findByCode(code).get();
        assertEquals(teamNaver.getTeamId(), inviteCode.getTeam().getTeamId());
        assertTrue(inviteCode.isActive());
    }

    @Test 
    void createInviteCode_생성자가_owner가_아닐때() {
        teamService.createTeam("naver", "naver in book", "userA", "owner");
        Team teamNaver = teamRepository.findByTeamName("naver");
        teamService.addMember(teamNaver.getTeamId(), "userB", "userA", "userB");
        
        em.flush();
        em.clear();
        
        assertThatThrownBy(() -> teamService.createInviteCode(teamNaver.getTeamId(), "userB"))
                .isInstanceOf(TeamAccessDeniedException.class);
    }

    @Test 
    void revokeInviteCode_정상_동작() {
        teamService.createTeam("naver", "naver in book", "userA", "owner");
        Team teamNaver = teamRepository.findByTeamName("naver");
        String code = teamService.createInviteCode(teamNaver.getTeamId(), "userA");
        teamService.revokeInviteCode(code, "userA");
        
        em.flush();
        em.clear();

        TeamInvite inviteCode = teamInviteRepository.findByCode(code).get();
        assertFalse(inviteCode.isActive());
    }

    @Test 
    void revokeInviteCode_존재하지_않는_코드일때() {
        assertThatThrownBy(() -> teamService.revokeInviteCode("revokeTestCode", "userA"))
                .isInstanceOf(NotFoundInviteCodeException.class);
    }

    @Test 
    void revokeInviteCode_이미_비활성화된_경우() {
        teamService.createTeam("naver", "naver in book", "userA", "owner");
        Team teamNaver = teamRepository.findByTeamName("naver");
        String code = teamService.createInviteCode(teamNaver.getTeamId(), "userA");
        TeamInvite inviteCode = teamInviteRepository.findByCode(code).get();
        inviteCode.setActive(false);
        
        em.flush();
        em.clear();
        
        teamService.revokeInviteCode(code, "userA");

        inviteCode = teamInviteRepository.findByCode(code).get();
        assertFalse(inviteCode.isActive());
    }

    @Test 
    void joinTeamByInviteCode_정상_동작() {
        teamService.createTeam("naver", "naver in book", "userA", "owner");
        Team teamNaver = teamRepository.findByTeamName("naver");
        String code = teamService.createInviteCode(teamNaver.getTeamId(), "userA");
        teamService.joinTeamByInviteCode(code, "userB", "userB");
        
        em.flush();
        em.clear();

        TeamMember member = teamMemberRepository.findByoAuth2User_ProviderIdAndTeam_TeamId("userB", teamNaver.getTeamId()).get();
        assertEquals(MemberRoleEnum.MEMBER, member.getRole());
        assertEquals(MemberStatusEnum.ACTIVE, member.getStatus());
    }
}
