package com.example.book_talker_backend.user.entity.dto;

import java.util.Map;
import org.springframework.security.oauth2.core.user.OAuth2User;

public record UserResponse(
    String name,
    String email,
    String birthday,
    String profileImage
) {
    // OAuth2User에서 변환하는 정적 팩토리 메서드
    public static UserResponse from(OAuth2User oauth2User) {
        Map<String, Object> response = oauth2User.getAttribute("response");
        
        return new UserResponse(
            (String) response.get("name"),
            (String) response.get("email"),
            (String) response.get("birthday"),
            (String) response.get("profile_image")
        );
    }
}