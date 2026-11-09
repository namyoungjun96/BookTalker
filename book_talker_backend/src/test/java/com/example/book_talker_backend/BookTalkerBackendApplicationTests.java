package com.example.book_talker_backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

// 로컬 DB 없이(CI 포함) 전체 컨텍스트가 뜨는지 확인 — 빈 컨테이너에 Flyway가 스키마를 만듦
@Testcontainers
@SpringBootTest
class BookTalkerBackendApplicationTests {

    @ServiceConnection
    static PostgreSQLContainer postgreSQLContainer =
            new PostgreSQLContainer(DockerImageName.parse("postgres:13.10-alpine"));

    @Test
    void contextLoads() {
    }

}
