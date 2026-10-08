package com.example.book_talker_backend.team.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinByInviteCodeRequest(
        @NotBlank(message = "초대 코드를 입력해주세요") String code,
        @Size(max = 10, message = "닉네임을 1~10자로 입력해주세요") @NotBlank(message = "닉네임을 입력해주세요") String displayName
) {
}
