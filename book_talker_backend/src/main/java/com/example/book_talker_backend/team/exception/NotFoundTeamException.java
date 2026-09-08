package com.example.book_talker_backend.team.exception;

import com.example.book_talker_backend.exception.ResourceNotFoundException;

public class NotFoundTeamException extends ResourceNotFoundException {
    public NotFoundTeamException(String message) {
        super(message);
    }
}
