package com.example.book_talker_backend.team.dao;

import java.util.List;
import java.util.Optional;

import com.example.book_talker_backend.team.entity.dto.TeamListResponse;
import com.example.book_talker_backend.team.entity.dto.TeamMemberListResponse;
import com.example.book_talker_backend.team.entity.dto.TeamResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.book_talker_backend.team.entity.MemberStatusEnum;
import com.example.book_talker_backend.team.entity.TeamMember;
import org.springframework.data.jpa.repository.Query;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findAllByoAuth2User_ProviderId(String providerId);
    Optional<TeamMember> findByoAuth2User_ProviderIdAndTeam_TeamId(String providerId, Long teamId);
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
        ORDER BY m.joinedAt DESC
    """)
    List<TeamListResponse> findByActiveTeamFromProviderId(String providerId);

    @Query("""
        SELECT new com.example.book_talker_backend.team.entity.dto.TeamResponse(
            tm.teamId, tm.teamName, tm.teamDescription,
            (SELECT COUNT(tm2) FROM TeamMember tm2
              WHERE tm2.team.teamId = tm.teamId AND tm2.status = 'ACTIVE'),
            m.role)
        FROM TeamMember m JOIN m.team tm
        WHERE m.oAuth2User.providerId = :providerId
          AND m.status = 'ACTIVE'
          AND tm.teamId = :teamId
    """)
    Optional<TeamResponse> findActiveTeamDetail(String providerId, Long teamId);

    @Query("""
        SELECT new com.example.book_talker_backend.team.entity.dto.TeamMemberListResponse(
            m.teamMemberId, m.displayName, m.role, m.joinedAt,
                CASE WHEN m.oAuth2User.providerId = :providerId THEN true ELSE false END
            )
        FROM TeamMember m
        WHERE m.team.teamId = :teamId AND m.status = 'ACTIVE'
        ORDER BY CASE WHEN m.role = 'OWNER' THEN 0 ELSE 1 END, m.joinedAt ASC, m.teamMemberId ASC
    """)
    List<TeamMemberListResponse> findActiveMembersByTeamId(String providerId, Long teamId);

    boolean existsByoAuth2User_ProviderIdAndTeam_TeamIdAndStatus(String providerId, Long teamId, MemberStatusEnum status);
}
