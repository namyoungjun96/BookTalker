package com.example.book_talker_backend.team.entity.dto;

public record JoinByInviteCodeRequest(
    String code,
    String displayName
) {
}
