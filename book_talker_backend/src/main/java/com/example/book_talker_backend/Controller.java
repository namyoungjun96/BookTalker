package com.example.book_talker_backend;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class Controller {
    @GetMapping(value = "/")
    public String userInfo(@AuthenticationPrincipal OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();

        return attributes.toString();
    }

    public boolean isProfileComplete(OAuth2AuthenticationToken authentication) {
        OAuth2User oauth2User = authentication.getPrincipal();

        boolean hasEmail = oauth2User.getAttribute("email") != null;
        boolean hasName = oauth2User.getAttribute("name") != null;

        return hasEmail && hasName;
    }
}
