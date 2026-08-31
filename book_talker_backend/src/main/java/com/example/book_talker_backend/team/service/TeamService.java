package com.example.book_talker_backend.team.service;

import java.time.LocalDateTime;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;

import com.example.book_talker_backend.team.dao.TeamRepository;
import com.example.book_talker_backend.team.entity.RoleEnum;
import com.example.book_talker_backend.team.entity.Team;
import com.example.book_talker_backend.team.entity.TeamMember;
import com.example.book_talker_backend.user.dao.OAuth2UserRepository;
import com.example.book_talker_backend.user.entity.OAuth2UserEntity;
import com.example.book_talker_backend.user.exception.NotFoundUserException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {
    private final TeamRepository teamRepository;
    private final OAuth2UserRepository oAuth2UserRepository;

    @Transactional
    public void createTeam(String teamName, String teamDescription, String providerId, String displayName) {
        OAuth2UserEntity providerUser = oAuth2UserRepository.findByProviderId(providerId);
        if (providerUser == null) {
            throw new NotFoundUserException("[createTeam] 해당 유저는 존재하지 않습니다: " + providerId);
        }

        Team team = new Team();
        team.setTeamName(teamName);
        team.setTeamDescription(teamDescription);
        team.setCreatedAt(LocalDateTime.now());
        team.setUpdatedAt(LocalDateTime.now());

        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setRole(RoleEnum.OWNER);
        member.setOAuth2User(providerUser);
        member.setDisplayName(displayName);
        member.setJoinedAt(LocalDateTime.now());

        team.getTeamMembers().add(member);
        
        teamRepository.save(team);
    }

    public void getMyTeams(String providerId) {}
    public void updateTeam(Long teamId, String teamName, String teamDescription, String providerId) {}
    public void deleteTeam(Long teamId, String providerId) {} 
    public void addMember(Long teamId, String actorProviderId, String requestProviderId) {}
    public void removeMember(Long teamId, Long teamMemberId, String providerId) {}
    private void leaveTeam(Long teamId, Long teamMemberId) {}
    private void kickMember(Long teamId, Long teamMemberId, String requestProviderId) {}
}
