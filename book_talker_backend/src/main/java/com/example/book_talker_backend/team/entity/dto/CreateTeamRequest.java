package com.example.book_talker_backend.team.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(
        @Size(max = 20, message = "모임 이름을 1~20자로 입력해주세요") @NotBlank(message = "모임 이름을 입력해주세요") String teamName,
        @Size(max = 100, message = "모임 소개를 1~100자로 입력해주세요") @NotBlank(message = "모임 소개를 입력해주세요") String teamDesc,
        @Size(max = 10, message = "닉네임을 1~10자로 입력해주세요") @NotBlank(message = "닉네임을 입력해주세요") String displayName
) {
}
