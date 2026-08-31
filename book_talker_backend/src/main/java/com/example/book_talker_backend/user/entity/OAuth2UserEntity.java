package com.example.book_talker_backend.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "OAUTH2_USER")
@Setter
@Getter
public class OAuth2UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    String provider; // 'google', 'kakao', 'naver'
    @Column(name = "provider_id", unique = true)
    String providerId;
    @Column(name = "created_at")
    LocalDateTime createdAt;

    public OAuth2UserEntity (String provider, String providerId) {
        this.provider = provider;
        this.providerId = providerId;
        this.createdAt = LocalDateTime.now();
    }
}
