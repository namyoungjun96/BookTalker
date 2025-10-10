package com.example.book_talker_backend.book.entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;

public record ListRequest(
        QueryType queryType,
        @NotBlank
        String start,
        @NotBlank
        @Max(100)
        String maxResults,
        CoverSizeEnum cover
) {
}