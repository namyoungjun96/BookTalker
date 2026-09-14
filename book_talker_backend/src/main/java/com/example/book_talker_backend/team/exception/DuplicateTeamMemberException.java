package com.example.book_talker_backend.team.exception;

public class DuplicateTeamMemberException extends RuntimeException {
    public DuplicateTeamMemberException(String message) {
        super(message);
    }
}
