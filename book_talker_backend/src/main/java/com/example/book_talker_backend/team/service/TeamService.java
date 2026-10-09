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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final OAuth2UserRepository oAuth2UserRepository;


    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_ATTEMPTS = 3;

    @Transactional
    public CreateTeamResponse createTeam(CreateTeamRequest request, String providerId) {
        OAuth2UserEntity providerUser = oAuth2UserRepository.findByProviderId(providerId);

        if (providerUser == null)
            throw new NotFoundUserException("[createTeam] 존재하지 않는 사용자 입니다: " + providerId);

        Team team = new Team();
        team.setTeamName(request.teamName());
        team.setTeamDescription(request.teamDesc());
        team.setCode(issueUniqueCode());

        TeamMember member = new TeamMember(team, providerUser, MemberRoleEnum.OWNER, MemberStatusEnum.ACTIVE, request.displayName());

        team.getTeamMembers().add(member);
        
        Long teamId = teamRepository.save(team).getTeamId();

        return new CreateTeamResponse(teamId);
    }

    String issueUniqueCode() {
        String code;

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            code = generateRandomCode();

            if (!teamRepository.existsByCode(code))
                return code;
        }

        throw new InviteCodeGenerationFailedException("[createTeam] 모임 초대 코드 생성 문제가 생겼습니다.");
    }

    @Transactional(readOnly = true)
    public List<TeamListResponse> getMyTeams(String providerId) {
        return teamMemberRepository.findByActiveTeamFromProviderId(providerId);
    }

    @Transactional
    public void updateTeam(Long teamId, UpdateTeamRequest request, String providerId) {
        TeamMember member = teamMemberRepository
                .findByoAuth2User_ProviderIdAndTeam_TeamId(providerId, teamId)
                .filter(m -> m.getStatus() == MemberStatusEnum.ACTIVE)
                .orElseThrow(() -> new NotFoundTeamException("[updateTeam] 모임을 찾을 수 없습니다." + teamId + " / " + providerId));   // 404: 팀 없음 + 모임원 아님

        if (member.getRole() != MemberRoleEnum.OWNER)
            throw new TeamAccessDeniedException("[updateTeam] 모임장만 수정할 수 있습니다." + teamId + " / " + providerId);                // 403: 모임장 아님

        Team team = member.getTeam();

        team.setTeamName(request.teamName());
        team.setTeamDescription(request.teamDesc());
    }

    @Transactional
    public void deleteTeam(Long teamId, String providerId) {
        TeamMember member = teamMemberRepository
                .findByoAuth2User_ProviderIdAndTeam_TeamId(providerId, teamId)
                .filter(m -> m.getStatus() == MemberStatusEnum.ACTIVE)
                .orElseThrow(() -> new NotFoundTeamException("[deleteTeam] 모임을 찾을 수 없습니다." + teamId + " / " + providerId));   // 404: 팀 없음 + 모임원 아님

        if (member.getRole() != MemberRoleEnum.OWNER)
            throw new TeamAccessDeniedException("[deleteTeam] 모임장만 삭제할 수 있습니다." + teamId + " / " + providerId);                // 403: 모임장 아님

        Team team = member.getTeam();

        teamRepository.delete(team);
    } 

    @Transactional
    public void addMember(Long teamId, String newMemberProviderId, String ownerProviderId, String displayName) {
        TeamMember owner = teamMemberRepository
                .findByoAuth2User_ProviderIdAndTeam_TeamId(ownerProviderId, teamId)
                .filter(m -> m.getStatus() == MemberStatusEnum.ACTIVE)
                .orElseThrow(() -> new TeamAccessDeniedException("[addMember] 해당 모임, 유저는 존재하지 않습니다: " + ownerProviderId + " / " + teamId));

        if (owner.getRole() != MemberRoleEnum.OWNER) {
            throw new TeamAccessDeniedException("[addMember] 해당 모임, 유저는 존재하지 않습니다: " + ownerProviderId + " / " + teamId);
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
            else if (MemberStatusEnum.INACTIVE == checkedMember.getStatus()) {
                checkedMember.rejoin(displayName);

                return ;
            }
            else if (MemberStatusEnum.BANNED == checkedMember.getStatus())
                throw new BannedTeamMemberException("[addMember] 차단된 사용자 입니다: " + newMemberProviderId);
        }
        
        saveMember(owner.getTeam(), newUser, displayName);
    }

    public void removeMember(Long teamId, Long teamMemberId, String providerId) {}
    private void leaveTeam(Long teamId, Long teamMemberId) {}
    private void kickMember(Long teamId, Long teamMemberId, String requestProviderId) {}

    @Transactional(readOnly = true)
    public InviteCodeResponse getTeamInviteCode(Long teamId, String providerId) {
        TeamMember owner = teamMemberRepository
                .findByoAuth2User_ProviderIdAndTeam_TeamId(providerId, teamId)
                .filter(m -> m.getStatus() == MemberStatusEnum.ACTIVE)
                .orElseThrow(() -> new NotFoundTeamException("[getTeamInviteCode] 해당 모임, 유저는 존재하지 않습니다: " + providerId + " / " + teamId));

        if (owner.getRole() != MemberRoleEnum.OWNER) {
            throw new TeamAccessDeniedException("[getTeamInviteCode] 모임장만 조회할 수 있습니다: " + providerId);
        }

        return new InviteCodeResponse(owner.getTeam().getCode());
    }

    @Transactional
    public JoinTeamResponse joinTeamByInviteCode(JoinByInviteCodeRequest request, String providerId) {
        Team team = teamRepository.findByCode(request.code()).orElseThrow(
                () -> new NotFoundInviteCodeException("[joinTeamByInviteCode] 유효하지 않은 코드입니다: " + request.code()));
        
        OAuth2UserEntity providerUser = oAuth2UserRepository.findByProviderId(providerId);

        if (providerUser == null)
            throw new NotFoundUserException("[joinTeamByInviteCode] 존재하지 않는 사용자 입니다: " + providerId);

        Optional<TeamMember> uncheckedMember = teamMemberRepository.findByoAuth2User_ProviderIdAndTeam_TeamId(providerId, team.getTeamId());

        if (uncheckedMember.isPresent()) {
            TeamMember checkedMember = uncheckedMember.get();
            
            if (MemberStatusEnum.ACTIVE == checkedMember.getStatus())
                throw new DuplicateTeamMemberException("[joinTeamByInviteCode] 해당 유저는 이미 존재 합니다: " + providerId);
            else if (MemberStatusEnum.INACTIVE == checkedMember.getStatus()) {
                checkedMember.rejoin(request.displayName());

                return new JoinTeamResponse(team.getTeamId());
            }
            else if (MemberStatusEnum.BANNED == checkedMember.getStatus())
                throw new NotFoundInviteCodeException("[joinTeamByInviteCode] 유효하지 않은 코드입니다: " + request.code());
        }
        
        saveMember(team, providerUser, request.displayName());

        return new JoinTeamResponse(team.getTeamId());
    }

    void saveMember(Team team, OAuth2UserEntity providerUser, String displayName) {
        TeamMember member = new TeamMember(team, providerUser, MemberRoleEnum.MEMBER, MemberStatusEnum.ACTIVE, displayName);

        teamMemberRepository.save(member);
    }

    String generateRandomCode() {
        int length = 13;
        String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();

        for(int i=0; i<length; i++) {
            sb.append(characters.charAt(RANDOM.nextInt(characters.length())));
        }

        return sb.toString();
    }
}
