package com.example.book_talker_backend.team.exception;

public class BannedTeamMemberException extends RuntimeException {
    public BannedTeamMemberException(String message) {
        super(message);
    }
}
