package com.example.book_talker_backend.user.mapper;

import com.example.book_talker_backend.user.entity.LoginType;
import com.example.book_talker_backend.user.entity.OAuth2UserEntity;
import com.example.book_talker_backend.user.entity.Person;

import java.time.LocalDateTime;

public class OAuth2UserMapper {
    public static Person toPerson() {
        Person person = new Person();
        person.setCreatedAt(LocalDateTime.now());
        person.setLoginType(LoginType.SOCIAL);

        return person;
    }

    public static OAuth2UserEntity toOAuth2UserEntity(String naverId, String provider) {
        OAuth2UserEntity oAuth2UserEntity = new OAuth2UserEntity();
        oAuth2UserEntity.setProviderId(naverId);
        oAuth2UserEntity.setProvider(provider);
        oAuth2UserEntity.setCreatedAt(LocalDateTime.now());

        return oAuth2UserEntity;
    }
}
