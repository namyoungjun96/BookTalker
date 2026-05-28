package com.example.book_talker_backend.user.entity.dto;

import java.util.Map;
import org.springframework.security.oauth2.core.user.OAuth2User;

public record UserResponse(String naverId) {
    public static UserResponse from(OAuth2User oauth2User) {
        Map<String, Object> response = oauth2User.getAttribute("response");
        return new UserResponse((String) response.get("id"));
    }
}
