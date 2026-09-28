package com.example.book_talker_backend.team.entity.dto;

public record CreateTeamRequest (
    String teamName,
    String teamDescription,
    String displayName
) {
}
