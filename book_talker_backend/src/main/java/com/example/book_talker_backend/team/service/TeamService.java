package com.example.book_talker_backend.team.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

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

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamInviteRepository teamInviteRepository;
    private final OAuth2UserRepository oAuth2UserRepository;

    @Transactional
    public void createTeam(String teamName, String teamDescription, String providerId, String displayName) {
        OAuth2UserEntity providerUser = oAuth2UserRepository.findByProviderId(providerId);

        Team team = new Team();
        team.setTeamName(teamName);
        team.setTeamDescription(teamDescription);

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
        List<TeamMember> activeMemberships = teamMemberRepository.findAllByoAuth2User_ProviderIdAndStatus(providerId, MemberStatusEnum.ACTIVE);

        return activeMemberships
                .stream()
                .map(TeamMember::getTeam)
                .toList();
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
                .orElseThrow(() -> new IllegalStateException("[deleteTeam] 불변조건 위반 — OWNER 체크 통과했는데 팀이 없음: teamId=" + teamId));

        teamRepository.delete(team);
    } 

    public void addMember(Long teamId, String newMemberProviderId, String ownerProviderId, String displayName) {
        TeamMember owner = teamMemberRepository
                .findByoAuth2User_ProviderIdAndTeam_TeamId(ownerProviderId, teamId)
                .orElseThrow(() -> new TeamAccessDeniedException("[addMember] 해당 팀, 유저는 존재하지 않습니다: " + ownerProviderId + " / " + teamId));

        if (owner.getRole() != MemberRoleEnum.OWNER) {
            throw new TeamAccessDeniedException("[addMember] 해당 팀, 유저는 존재하지 않습니다: " + ownerProviderId + " / " + teamId);
        }

        OAuth2UserEntity newUser = oAuth2UserRepository.findByProviderId(newMemberProviderId);

        if (newUser == null) {
            throw new NotFoundUserException("[addMember] 해당 사용자를 찾을 수 없습니다. " + newMemberProviderId);
        }

        Optional<TeamMember> uncheckedMember = teamMemberRepository.findByoAuth2User_ProviderIdAndTeam_TeamId(newMemberProviderId, teamId);

        if (uncheckedMember.isPresent()) {
            TeamMember checkedMember = uncheckedMember.get();
            
            if (MemberStatusEnum.ACTIVE == checkedMember.getStatus())
                throw new DuplicateTeamMemberException("[addMember] 해당 유저는 이미 존재 합니다: " + newMemberProviderId);
            else if (MemberStatusEnum.BANNED == checkedMember.getStatus())
                throw new BannedTeamMemberException("[addMember] 차단된 사용자 입니다: " + newMemberProviderId);
        }
        
        saveMember(teamId, newUser, displayName, MemberRoleEnum.MEMBER, MemberStatusEnum.ACTIVE);
    }

    public void removeMember(Long teamId, Long teamMemberId, String providerId) {}
    private void leaveTeam(Long teamId, Long teamMemberId) {}
    private void kickMember(Long teamId, Long teamMemberId, String requestProviderId) {}

    @Transactional
    public String createInviteCode(Long teamId, String providerId) {
        TeamMember owner = teamMemberRepository
                .findByoAuth2User_ProviderIdAndTeam_TeamId(providerId, teamId)
                .orElseThrow(() -> new TeamAccessDeniedException("[createInviteCode] 해당 팀, 유저는 존재하지 않습니다: " + providerId + " / " + teamId));

        if (owner.getRole() != MemberRoleEnum.OWNER) {
            throw new TeamAccessDeniedException("[createInviteCode] 해당 팀, 유저는 존재하지 않습니다: " + providerId + " / " + teamId);
        }

        TeamInvite invite = new TeamInvite();
        invite.setActive(true);
        invite.setTeam(teamRepository.getReferenceById(teamId));

        String code = generateRandomCode();
        invite.setCode(code);

        teamInviteRepository.save(invite);

        return code;
    }

    @Transactional
    public void revokeInviteCode(String code, String providerId) {
        TeamInvite inviteCode = teamInviteRepository
                .findByCode(code)
                .orElseThrow(() -> new NotFoundInviteCodeException("[revokeInviteCode] 유효하지 않은 코드입니다: " + code));
        Long teamId = inviteCode.getTeam().getTeamId();

        TeamMember owner = teamMemberRepository
                .findByoAuth2User_ProviderIdAndTeam_TeamId(providerId, teamId)
                .orElseThrow(() -> new TeamAccessDeniedException("[revokeInviteCode] 해당 팀, 유저는 존재하지 않습니다: " + providerId + " / " + teamId));

        if (owner.getRole() != MemberRoleEnum.OWNER) {
            throw new TeamAccessDeniedException("[revokeInviteCode] 해당 팀, 유저는 존재하지 않습니다: " + providerId + " / " + teamId);
        }

        if (!inviteCode.isActive()) {
            return ;
        }

        inviteCode.setActive(false);
    }

    @Transactional
    public void joinTeamByInviteCode(String code, String providerId, String displayName) {
        TeamInvite inviteCode = teamInviteRepository
                .findByCode(code)
                .orElseThrow(() -> new NotFoundInviteCodeException("[joinTeamByInviteCode] 유효하지 않은 코드입니다: " + code));

        if (!inviteCode.isActive())
            throw new NotFoundInviteCodeException("[joinTeamByInviteCode] 유효하지 않은 코드입니다: " + code);

        Long teamId = inviteCode.getTeam().getTeamId();
        
        OAuth2UserEntity user = oAuth2UserRepository.findByProviderId(providerId);

        Optional<TeamMember> uncheckedMember = teamMemberRepository.findByoAuth2User_ProviderIdAndTeam_TeamId(providerId, teamId);

        if (uncheckedMember.isPresent()) {
            TeamMember checkedMember = uncheckedMember.get();
            
            if (MemberStatusEnum.ACTIVE == checkedMember.getStatus())
                throw new DuplicateTeamMemberException("[joinTeamByInviteCode] 해당 유저는 이미 존재 합니다: " + providerId);
            else if (MemberStatusEnum.BANNED == checkedMember.getStatus())
                throw new NotFoundInviteCodeException("[joinTeamByInviteCode] 유효하지 않은 코드입니다: " + code);
        }
        
        saveMember(teamId, user, displayName, MemberRoleEnum.MEMBER, MemberStatusEnum.ACTIVE);
    }

    void saveMember(Long teamId, OAuth2UserEntity user, String displayName, MemberRoleEnum role, MemberStatusEnum status) {
        TeamMember member = new TeamMember();
        member.setTeam(teamRepository.getReferenceById(teamId));
        member.setRole(role);
        member.setStatus(status);
        member.setOAuth2User(user);
        member.setDisplayName(displayName);
        member.setJoinedAt(LocalDateTime.now());

        teamMemberRepository.save(member);
    }

    public String generateRandomCode() {
        int length = 13;
        String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        SecureRandom random = new SecureRandom();

        for(int i=0; i<length; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }

        return sb.toString();
    }
}
