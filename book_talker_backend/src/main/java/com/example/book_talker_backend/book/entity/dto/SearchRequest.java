package com.example.book_talker_backend.book.entity.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SearchRequest(
        @NotBlank
        String query,
        @Min(1)
        int start
) {
}
