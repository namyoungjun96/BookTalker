package com.example.book_talker_backend.book.exception;

public class UnknownEnumValueException extends RuntimeException {
    public UnknownEnumValueException(String message) {
        super(message);
    }
}
