package com.example.book_talker_backend.team.exception;

public class TeamAccessDeniedException extends RuntimeException {
    public TeamAccessDeniedException(String message) {
        super(message);
    }
}
