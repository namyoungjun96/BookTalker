package com.example.book_talker_backend.book.exception;

public class KakaoApiException extends RuntimeException {
    public KakaoApiException(String message) {
        super(message);
    }
}
