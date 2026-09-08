package com.example.book_talker_backend.team.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;

import com.example.book_talker_backend.team.dao.TeamMemberRepository;
import com.example.book_talker_backend.team.dao.TeamRepository;
import com.example.book_talker_backend.team.entity.MemberRoleEnum;
import com.example.book_talker_backend.team.entity.MemberStatusEnum;
import com.example.book_talker_backend.team.entity.Team;
import com.example.book_talker_backend.team.entity.TeamMember;
import com.example.book_talker_backend.team.exception.NotFoundTeamException;
import com.example.book_talker_backend.team.exception.TeamAccessDeniedException;
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
    private final TeamMemberRepository teamMemberRepository;
    private final TeamMemberRepository teamMemberRepository;
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
        member.setRole(MemberRoleEnum.OWNER);
        member.setStatus(MemberStatusEnum.ACTIVE);
        member.setOAuth2User(providerUser);
        member.setDisplayName(displayName);
        member.setJoinedAt(LocalDateTime.now());

        team.getTeamMembers().add(member);
        
        teamRepository.save(team);
    }

    public List<Team> getMyTeams(String providerId) {
        List<TeamMember> memberList = teamMemberRepository.findAllByoAuth2User_ProviderId(providerId);
        List<Team> teams = new ArrayList<>();
        
        for (TeamMember member: memberList) {
            if (member.getStatus() == MemberStatusEnum.ACTIVE)
                teams.add(member.getTeam());
        }

        return teams;
    }

    @Transactional
    public void updateTeam(Long teamId, String teamName, String teamDescription, String providerId) {
        if (teamMemberRepository.findByoAuth2User_ProviderIdAndTeam_TeamId(providerId, teamId)
                .orElseThrow(() -> new TeamAccessDeniedException("[updateTeam] 권한이 맞지 않습니다: " + providerId))
                .getRole() 
            != MemberRoleEnum.OWNER)
            throw new TeamAccessDeniedException("[updateTeam] 권한이 맞지 않습니다: " + providerId);

        Team team = teamRepository.getReferenceById(teamId);

        team.setTeamName(teamName);
        team.setTeamDescription(teamDescription);
    }

    @Transactional
    public void deleteTeam(Long teamId, String providerId) {
        if (teamMemberRepository.findByoAuth2User_ProviderIdAndTeam_TeamId(providerId, teamId)
                .orElseThrow(() -> new TeamAccessDeniedException("[deleteTeam] 권한이 맞지 않습니다: " + providerId))
                .getRole() 
            != MemberRoleEnum.OWNER)
            throw new TeamAccessDeniedException("[deleteTeam] 권한이 맞지 않습니다: " + providerId);

        Team team = teamRepository
                .findById(teamId)
                .orElseThrow(() ->  NotFoundTeamException("[deleteTeam] 권한이 맞지 않습니다: " + providerId));

        teamRepository.delete(team);
    } 

    public void getMyTeams(String providerId) {}
    public void updateTeam(Long teamId, String teamName, String teamDescription, String providerId) {}
    public void deleteTeam(Long teamId, String providerId) {} 
    public void addMember(Long teamId, String actorProviderId, String requestProviderId) {}

    public void removeMember(Long teamId, Long teamMemberId, String providerId) {}
    private void leaveTeam(Long teamId, Long teamMemberId) {}
    private void kickMember(Long teamId, Long teamMemberId, String requestProviderId) {}
}
