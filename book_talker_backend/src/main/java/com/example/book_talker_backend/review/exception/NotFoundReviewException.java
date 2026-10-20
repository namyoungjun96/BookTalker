package com.example.book_talker_backend.review.exception;

import com.example.book_talker_backend.exception.ResourceNotFoundException;

public class NotFoundReviewException extends ResourceNotFoundException {
    public NotFoundReviewException(String message) {
        super(message);
    }
}
