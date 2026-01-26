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
    @Column(name = "provider_id")
    String providerId; // OAuth2 제공자에서의 사용자 ID
    @Column(name = "provider_email")
    String providerEmail;
    @Column(name = "created_at")
    LocalDateTime createdAt;
}
