package com.example.book_talker_backend.team.dao;

import com.example.book_talker_backend.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    Team findByTeamName(String teamName);

    boolean existsByCode(String code);

    Optional<Team> findByCode(String code);
}
