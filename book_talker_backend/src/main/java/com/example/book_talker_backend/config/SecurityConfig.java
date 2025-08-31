package com.example.book_talker_backend.config;

import com.example.book_talker_backend.oauth2.CustomAuthenticationSuccessHandler;
import com.example.book_talker_backend.user.dao.OAuth2UserRepository;
import com.example.book_talker_backend.user.dao.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@EnableWebSecurity
@EnableRedisHttpSession(redisNamespace = "book_talker:session")
@RequiredArgsConstructor
public class SecurityConfig {
    private final OAuth2UserRepository oAuth2UserRepository;
    private final UserRepository userRepository;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorizeRequests ->
                authorizeRequests.requestMatchers("/login").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(login -> login.
                        successHandler(new CustomAuthenticationSuccessHandler(userRepository, oAuth2UserRepository))
                )
                .oauth2Client(Customizer.withDefaults())
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
        ;

        return http.build();
    }

    @Bean
    public OAuth2AuthorizedClientRepository authorizedClientRepository() {
        // HttpSession을 Redis에 저장하므로 OAuth2 정보도 Redis에 저장됨
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    @Bean
    public LettuceConnectionFactory lettuceConnectionFactory() {
        return new LettuceConnectionFactory();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
