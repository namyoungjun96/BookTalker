package com.example.book_talker_backend.oauth2;

import com.example.book_talker_backend.user.dao.OAuth2UserRepository;
import com.example.book_talker_backend.user.dao.UserRepository;
import com.example.book_talker_backend.user.mapper.OAuth2UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    @Value("${base-url.frontend}")
    private String BASE_URL;

    private final UserRepository userRepository;
    private final OAuth2UserRepository oAuth2UserRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauth2Token.getPrincipal();
        Map<String, Object> userInfo = (Map<String, Object>) oauth2User.getAttributes().get("response");
        String naverId = (String) userInfo.get("id");

        log.debug("naver login: naverId={}", naverId);
        if (oAuth2UserRepository.findByProviderId(naverId) == null) {
            log.debug("new user, saving: naverId={}", naverId);
            userRepository.save(OAuth2UserMapper.toPerson());
            oAuth2UserRepository.save(OAuth2UserMapper.toOAuth2UserEntity(naverId, oauth2Token.getAuthorizedClientRegistrationId()));
        }

        response.sendRedirect(BASE_URL);
    }
}
