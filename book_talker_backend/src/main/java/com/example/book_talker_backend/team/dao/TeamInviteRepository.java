package com.example.book_talker_backend.team.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.book_talker_backend.team.entity.TeamInvite;

public interface TeamInviteRepository extends JpaRepository<TeamInvite, Long> {
    Optional<TeamInvite> findByCode(String code);
}
