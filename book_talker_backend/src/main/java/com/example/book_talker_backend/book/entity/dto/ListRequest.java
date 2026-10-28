package com.example.book_talker_backend.book.entity.dto;

import jakarta.validation.constraints.Min;

public record ListRequest(
        QueryType queryType,
        @Min(1)
        int start
) {
}