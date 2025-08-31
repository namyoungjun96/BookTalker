package com.example.book_talker_backend.user.dao;

import com.example.book_talker_backend.user.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Person, Long> {
    boolean existsByEmail(String email);
}
