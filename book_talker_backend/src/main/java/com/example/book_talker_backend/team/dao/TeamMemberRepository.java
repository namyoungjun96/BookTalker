package com.example.book_talker_backend.team.dao;

import java.util.List;
import java.util.Optional;

import com.example.book_talker_backend.team.entity.dto.TeamListResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.book_talker_backend.team.entity.MemberStatusEnum;
import com.example.book_talker_backend.team.entity.TeamMember;
import org.springframework.data.jpa.repository.Query;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findAllByoAuth2User_ProviderId(String providerId);
    Optional<TeamMember> findByoAuth2User_ProviderIdAndTeam_TeamId(String providerId, Long teamId);
    List<TeamMember> findAllByTeam_TeamId(Long teamId);
    List<TeamMember> findAllByoAuth2User_ProviderIdAndStatus(String providerId, MemberStatusEnum status);

    @Query("""
        SELECT new com.example.book_talker_backend.team.entity.dto.TeamListResponse(
            tm.teamId, tm.teamName, tm.teamDescription,
                (
                    SELECT COUNT(tm2)
                        FROM TeamMember tm2
                    WHERE tm2.team.teamId = tm.teamId
                    AND tm2.status = 'ACTIVE'
                    ),
                m.role
            )
        FROM TeamMember m
        JOIN m.team tm
        WHERE m.oAuth2User.providerId = :providerId and m.status = 'ACTIVE'
    """)
    List<TeamListResponse> findByActiveTeamFromProviderId(String providerId);
}
