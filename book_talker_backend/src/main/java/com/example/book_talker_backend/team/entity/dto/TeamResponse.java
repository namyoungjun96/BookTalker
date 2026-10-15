package com.example.book_talker_backend.team.entity.dto;

import com.example.book_talker_backend.team.entity.MemberRoleEnum;

public record TeamResponse(
        Long teamId,
        String teamName,
        String teamDesc,
        long memberCount,
        MemberRoleEnum role
) {
}
