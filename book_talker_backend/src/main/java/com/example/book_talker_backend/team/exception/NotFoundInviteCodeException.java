package com.example.book_talker_backend.team.exception;

import com.example.book_talker_backend.exception.ResourceNotFoundException;

public class NotFoundInviteCodeException extends ResourceNotFoundException {
    public NotFoundInviteCodeException(String message) {
        super(message);
    }
}
