package com.example.book_talker_backend.team.entity.dto;

public record UpdateTeamRequest(
        String teamName,
        String teamDesc
) {
}
