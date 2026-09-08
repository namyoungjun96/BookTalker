package com.example.book_talker_backend.team.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.example.book_talker_backend.team.dao.TeamMemberRepository;
import com.example.book_talker_backend.team.dao.TeamRepository;
import com.example.book_talker_backend.team.entity.RoleEnum;
import com.example.book_talker_backend.team.entity.Team;
import com.example.book_talker_backend.team.entity.TeamMember;
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
        oAuth2UserRepository.save(new OAuth2UserEntity("naver", "1234567"));
        teamService.createTeam("mobigen", "mobigen in book", "1234567", "owner");

        em.flush();
        em.clear();

        Team team = teamRepository.findByTeamName("mobigen");

        assertNotNull(team);
        assertEquals("mobigen", team.getTeamName());

        TeamMember owner = teamMemberRepository.findByoAuth2User_ProviderIdAndTeam_TeamId("1234567", team.getTeamId()).get();
        
        assertEquals(RoleEnum.OWNER, owner.getRole());
        assertEquals("owner", owner.getDisplayName());
    }

    @Test
    void 팀_생성_존재하지_않는_사용자의_접근시() {
        assertThatThrownBy(() -> teamService.createTeam("mobigen", "mobigen in book", "1234567", "owner"))
            .isInstanceOf(NotFoundUserException.class);
    }
}
