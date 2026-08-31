package com.example.book_talker_backend.team.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.book_talker_backend.team.entity.TeamMember;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    List<TeamMember> findAllByoAuth2User_ProviderId(String providerId);
    Optional<TeamMember> findByoAuth2User_ProviderIdAndTeam_TeamId(String providerId, Long teamId);
    List<TeamMember> findAllByTeam_TeamId(Long teamId);
}
