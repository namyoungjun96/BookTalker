package com.example.book_talker_backend;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import com.example.book_talker_backend.user.entity.dto.UserResponse;

@RestController
@RequiredArgsConstructor
@Slf4j
public class Controller {
    @GetMapping(value = "/")
    public String userInfo(@AuthenticationPrincipal OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();

        return attributes.toString();
    }

    @GetMapping("/api/user/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(UserResponse.from(oauth2User));
    }

    @GetMapping(value = "/api/auth/session")
    public ResponseEntity<Void> checkSession(@AuthenticationPrincipal OAuth2User authentication) {
        log.debug("Auth info: {}", authentication);

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok().build();
    }

    public boolean isProfileComplete(OAuth2AuthenticationToken authentication) {
        OAuth2User oauth2User = authentication.getPrincipal();

        boolean hasEmail = oauth2User.getAttribute("email") != null;
        boolean hasName = oauth2User.getAttribute("name") != null;

        return hasEmail && hasName;
    }
}
