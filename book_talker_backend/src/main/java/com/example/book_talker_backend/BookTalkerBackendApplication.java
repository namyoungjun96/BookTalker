package com.example.book_talker_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class BookTalkerBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookTalkerBackendApplication.class, args);
    }

}
