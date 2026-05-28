package com.example.book_talker_backend.user.dao;

import com.example.book_talker_backend.user.entity.OAuth2UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuth2UserRepository extends JpaRepository<OAuth2UserEntity, Long> {
    OAuth2UserEntity findByProviderId(String providerId);
}
