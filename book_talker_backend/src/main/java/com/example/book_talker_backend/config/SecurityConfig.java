package com.example.book_talker_backend.config;

import com.example.book_talker_backend.oauth2.CustomAuthenticationSuccessHandler;
import com.example.book_talker_backend.user.dao.OAuth2UserRepository;
import com.example.book_talker_backend.user.dao.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
// @EnableRedisHttpSession(redisNamespace = "book_talker:session")
@RequiredArgsConstructor
public class SecurityConfig implements WebMvcConfigurer {
    @Value("${base-url.frontend}")
    private String BASE_URL;
    private final OAuth2UserRepository oAuth2UserRepository;
    private final UserRepository userRepository;

    @Value("${app.admin.id}")
    private String ADMIN_USER_ID;
    @Value("${app.admin.password}")
    private String ADMIN_USER_PASSWORD;

    @Bean
    @Order(1)
    SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .securityMatcher("/actuator/**")
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests.requestMatchers("/actuator/**").authenticated())
                .httpBasic(Customizer.withDefaults())
        ;

        return http.build();
    }

    @Bean
    UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
                .username(ADMIN_USER_ID)
                .password(passwordEncoder().encode(ADMIN_USER_PASSWORD))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(authorizeRequests ->
                authorizeRequests.requestMatchers("/login", "/logout", "/auth/session", "/rank/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(login -> login
                        .successHandler(customAuthenticationSuccessHandler())
                )
                .oauth2Client(Customizer.withDefaults())
                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
                .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessHandler((request, response, authentication) -> {
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().write("{\"message\": \"로그아웃 성공\"}");
                    })
                    .invalidateHttpSession(true)
                    .deleteCookies("SESSION"))
            ;

        return http.build();
    }

    // ⭐ CORS 설정 Bean
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin(BASE_URL);
        configuration.addAllowedMethod("*"); // GET, POST, PUT, DELETE 등 모두 허용
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new CustomAuthenticationSuccessHandler(userRepository, oAuth2UserRepository);
    }

    @Bean
    public OAuth2AuthorizedClientRepository authorizedClientRepository() {
        // HttpSession을 Redis에 저장하므로 OAuth2 정보도 Redis에 저장됨
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    // @Bean
    // public LettuceConnectionFactory lettuceConnectionFactory() {
    //     return new LettuceConnectionFactory();
    // }

    // @Bean
    // public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    //     RedisTemplate<String, Object> template = new RedisTemplate<>();
    //     template.setConnectionFactory(connectionFactory);
    //     template.setKeySerializer(new StringRedisSerializer());
    //     template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    //     return template;
    // }
}
