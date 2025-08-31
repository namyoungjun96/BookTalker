package com.example.book_talker_backend.user.dao;

import com.example.book_talker_backend.user.entity.OAuth2UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OAuth2UserRepository extends JpaRepository<OAuth2UserEntity, Long> {
    List<OAuth2UserEntity> findByUserId(String userId);
}
