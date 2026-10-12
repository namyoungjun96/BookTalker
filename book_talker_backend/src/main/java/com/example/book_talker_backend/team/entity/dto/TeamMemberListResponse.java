package com.example.book_talker_backend.team.entity.dto;

import com.example.book_talker_backend.team.entity.MemberRoleEnum;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record TeamMemberListResponse(
        Long memberId,
        String displayName,
        MemberRoleEnum role,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDateTime joinedAt,
        Boolean me
) {
}
