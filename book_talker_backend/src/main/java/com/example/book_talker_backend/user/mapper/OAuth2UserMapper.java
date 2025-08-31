package com.example.book_talker_backend.user.mapper;

import com.example.book_talker_backend.user.entity.LoginType;
import com.example.book_talker_backend.user.entity.OAuth2UserEntity;
import com.example.book_talker_backend.user.entity.Person;

import java.time.LocalDateTime;
import java.util.Map;

public class OAuth2UserMapper {
    public static Person toPerson(Map<String, Object> oAuth2User) {
        Person person = new Person();
        person.setEmail(oAuth2User.get("email").toString());
        person.setCreatedAt(LocalDateTime.now());
        person.setLoginType(LoginType.SOCIAL);

        return person;
    }

    public static OAuth2UserEntity toOAuth2UserEntity(Map<String, Object> oAuth2User, String provider) {
        OAuth2UserEntity oAuth2UserEntity = new OAuth2UserEntity();
        oAuth2UserEntity.setProviderId((String) oAuth2User.get("id"));
        oAuth2UserEntity.setProvider(provider);
        oAuth2UserEntity.setProviderEmail((String) oAuth2User.get("email"));
        oAuth2UserEntity.setCreatedAt(LocalDateTime.now());

        return oAuth2UserEntity;
    }
}
