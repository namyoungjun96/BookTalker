package com.example.book_talker_backend.team.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.book_talker_backend.team.entity.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {
    public Team findByTeamName(String teamName);
}
