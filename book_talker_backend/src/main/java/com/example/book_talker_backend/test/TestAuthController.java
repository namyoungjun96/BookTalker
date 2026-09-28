package com.example.book_talker_backend.test;

import com.example.book_talker_backend.user.dao.OAuth2UserRepository;
import com.example.book_talker_backend.user.dao.UserRepository;
import com.example.book_talker_backend.user.mapper.OAuth2UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@Profile("!prod")
@RequiredArgsConstructor
public class TestAuthController {
    private final OAuth2UserRepository oAuth2UserRepository;
    private final UserRepository userRepository;

    @PostMapping("/test/login")
    public ResponseEntity<Void> testLogin(@RequestParam String naverId,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {
        if (oAuth2UserRepository.findByProviderId(naverId) == null) {
            userRepository.save(OAuth2UserMapper.toPerson());
            oAuth2UserRepository.save(OAuth2UserMapper.toOAuth2UserEntity(naverId, "naver"));
        }

        Map<String, Object> attributes = Map.of("response", Map.of("id", naverId));
        OAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("OAUTH2_USER")),
                attributes,
                "response"
        );

        OAuth2AuthenticationToken token = new OAuth2AuthenticationToken(
                principal, principal.getAuthorities(), "naver"
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(token);
        SecurityContextHolder.setContext(context);

        new HttpSessionSecurityContextRepository().saveContext(context, request, response);

        return ResponseEntity.ok().build();
    }
}
