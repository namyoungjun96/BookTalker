package com.example.book_talker_backend.user.exception;

import com.example.book_talker_backend.exception.ResourceNotFoundException;

public class NotFoundUserException extends ResourceNotFoundException {
    public NotFoundUserException(String message) {
        super(message);
    }
}
