# BookTalker Backend 경량화 방안

## 서버 환경
- **CPU**: 1 Core
- **RAM**: 1GB
- **OS**: OCI Instance (Oracle Cloud Infrastructure)

## 현재 프로젝트 상태
- **Framework**: Spring Boot 3.5.5-SNAPSHOT
- **Java**: 17
- **Database**: PostgreSQL (외부 서버)
- **주요 기능**: OAuth2 인증, 책 검색/조회, 리뷰 관리

---

## 경량화 방안 리스트

### 1. 내장 웹 서버를 Undertow로 변경 ⭐⭐⭐

#### 적용 방법
```gradle
dependencies {
    implementation('org.springframework.boot:spring-boot-starter-web') {
        exclude group: 'org.springframework.boot', module: 'spring-boot-starter-tomcat'
    }
    implementation 'org.springframework.boot:spring-boot-starter-undertow'
}
```

#### 선정 이유
- **메모리 효율**: Tomcat 대비 **30-40% 적은 메모리** 사용
- **스레드 관리**: Undertow는 Non-blocking I/O 기반으로 **적은 스레드**로 더 많은 요청 처리
- **경량성**: Undertow의 기본 메모리 footprint가 Tomcat보다 훨씬 작음
- **성능**: 1코어 환경에서 context switching 오버헤드 감소
- **호환성**: Spring Boot에서 바로 지원하여 코드 변경 불필요

#### 예상 효과
- Tomcat: 초기 메모리 ~200MB → Undertow: ~120-150MB
- 메모리 절약: **약 50-80MB**

---

### 2. JVM Heap 메모리 최적화 ⭐⭐⭐

#### 적용 방법
```bash
java -Xms256m -Xmx512m \
     -XX:+UseSerialGC \
     -XX:MaxMetaspaceSize=128m \
     -XX:CompressedClassSpaceSize=32m \
     -XX:ReservedCodeCacheSize=32m \
     -Xss256k \
     -jar book_talker_backend.jar
```

#### 선정 이유
- **적정 Heap 크기**: 1GB RAM 중 약 512MB를 Heap으로 할당 (나머지는 OS, Native Memory, Metaspace용)
- **Serial GC**: 단일 스레드 GC로 CPU 1코어 환경에 최적화
  - G1GC/Parallel GC는 멀티코어 환경에 최적화되어 있어 1코어에서는 오버헤드만 발생
  - Serial GC는 메모리 오버헤드가 가장 적음
- **Metaspace 제한**: Spring Boot는 기본적으로 Metaspace를 무제한으로 사용하므로 명시적 제한 필요
- **Stack Size 축소**: 기본 1MB → 256KB로 축소 (스레드당 메모리 절약)

#### 예상 효과
- 전체 JVM 메모리 사용량: **약 600-700MB**로 제한
- OOM(Out Of Memory) 방지 및 안정적인 운영

---

### 3. HikariCP 데이터베이스 연결 풀 최적화 ⭐⭐⭐

#### 적용 방법
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 3          # 최대 연결 수 (기본값: 10)
      minimum-idle: 1                # 최소 유휴 연결 수
      connection-timeout: 10000      # 연결 타임아웃 (10초)
      idle-timeout: 300000           # 유휴 연결 타임아웃 (5분)
      max-lifetime: 600000           # 연결 최대 수명 (10분)
      leak-detection-threshold: 60000 # 연결 누수 감지 (60초)
```

#### 선정 이유
- **과도한 연결 수**: 기본값 10개는 소규모 서비스에 과도함
- **메모리 절약**: 각 DB 연결당 약 **5-10MB** 메모리 사용
  - 10개 → 3개로 줄이면 **약 35-70MB 절약**
- **동시 사용자 고려**: 책 검색/리뷰 작성 서비스는 동시 요청이 많지 않음
- **외부 DB 사용**: PostgreSQL이 외부 서버에 있으므로 네트워크 레이턴시가 있어 많은 연결이 불필요
- **유휴 연결 관리**: idle-timeout으로 사용하지 않는 연결 자동 정리

#### 예상 효과
- 메모리 절약: **약 50-70MB**
- DB 서버 부하 감소

---

### 4. Spring Boot DevTools 제거 ⭐⭐

#### 적용 방법
```gradle
dependencies {
    // developmentOnly 'org.springframework.boot:spring-boot-devtools'  // 주석 처리 또는 삭제
}
```

#### 선정 이유
- **프로덕션 불필요**: DevTools는 개발 시 Hot Reload 등을 위한 도구로 배포 환경에서는 불필요
- **메모리 오버헤드**: 클래스 로더 이중화, 파일 감시 등으로 **약 20-30MB** 메모리 사용
- **성능 영향**: 불필요한 백그라운드 작업으로 CPU 사이클 낭비
- **보안**: 프로덕션에서 DevTools 엔드포인트 노출 위험

#### 예상 효과
- 메모리 절약: **약 20-30MB**
- 시작 시간 단축

---

### 5. 로깅 레벨 최적화 및 Async Logging 적용 ⭐⭐

#### 적용 방법

**application-prod.yml**
```yaml
logging:
  level:
    root: WARN                                              # 기본 로그 레벨
    com.example.book_talker_backend: INFO                   # 애플리케이션 로그
    org.springframework.web: WARN                           # Spring Web 로그
    org.springframework.security: WARN                      # Security 로그
    org.hibernate: ERROR                                    # Hibernate 로그
    org.hibernate.SQL: ERROR                                # SQL 쿼리 로그 (프로덕션에서 비활성화)
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"  # 간소화된 패턴
```

**logback-spring.xml** (새로 생성)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="CONSOLE" />
        <queueSize>256</queueSize>
        <discardingThreshold>0</discardingThreshold>
    </appender>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="WARN">
        <appender-ref ref="ASYNC" />
    </root>
</configuration>
```

#### 선정 이유
- **과도한 로깅**: DEBUG/TRACE 레벨 로깅은 엄청난 CPU와 I/O 사용
  - 특히 Hibernate SQL 로깅은 매 쿼리마다 로그 생성
- **I/O Blocking**: 동기 로깅은 I/O 작업이 완료될 때까지 스레드 블로킹
- **CPU 사용**: 문자열 포맷팅, 스택 트레이스 생성 등으로 CPU 사용
- **Async Logging**: 별도 스레드에서 로깅 처리하여 애플리케이션 스레드 블로킹 방지

#### 예상 효과
- CPU 사용량: **10-20% 감소**
- 응답 시간 개선
- 디스크 I/O 감소

---

### 6. JPA 최적화 설정 ⭐⭐

#### 적용 방법
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 10                    # Batch Insert/Update
          fetch_size: 20                    # Fetch Size
        order_inserts: true                 # Insert 순서 최적화
        order_updates: true                 # Update 순서 최적화
        query.in_clause_parameter_padding: true  # IN 절 파라미터 패딩
    open-in-view: false                     # OSIV 비활성화
```

#### 선정 이유
- **OSIV(Open Session In View) 비활성화**
  - OSIV는 HTTP 요청 전체에서 영속성 컨텍스트 유지 → **불필요한 DB 연결 점유**
  - 비활성화 시 트랜잭션 범위에서만 연결 사용 → 연결 풀 효율 증가
- **Batch Processing**: 여러 쿼리를 묶어 실행하여 DB 왕복 횟수 감소
- **Fetch Size 제한**: 한 번에 가져오는 데이터 양 제한으로 메모리 사용 감소
- **현재 사용 사례**: Review 조회, Book 조회 등에서 불필요한 연결 점유 가능성 높음

#### 예상 효과
- DB 연결 점유 시간 **30-50% 감소**
- 메모리 사용 효율 증가

---

### 7. Spring Security 세션 타임아웃 단축 ⭐⭐

#### 적용 방법
```yaml
server:
  servlet:
    session:
      timeout: 10m                # 기본 30분 → 10분으로 단축
      cookie:
        max-age: 600              # 쿠키 만료 시간 (10분)
        http-only: true
        secure: true              # HTTPS 사용 시
```

#### 선정 이유
- **메모리 누적**: 세션은 서버 메모리에 저장되며, 타임아웃이 길수록 메모리 누적
- **비활성 사용자**: 로그인 후 활동하지 않는 사용자의 세션이 메모리 점유
- **서비스 특성**: 책 검색/리뷰 작성은 장시간 세션 유지가 불필요
- **세션 크기**: OAuth2 인증 정보 포함 시 각 세션당 **약 5-10KB**
  - 100명 동시 사용자 가정 시: 30분 타임아웃 = 수백 MB, 10분 = 100MB 이하

#### 예상 효과
- 동시 세션 수 감소로 **메모리 50-100MB 절약** (사용자 수에 따라)

---

### 8. 불필요한 Auto Configuration 비활성화 ⭐

#### 적용 방법
```java
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,        // 제외하지 않음 (DB 사용)
    RedisAutoConfiguration.class,             // Redis 미사용
    SessionAutoConfiguration.class,           // 사용 중이므로 제외하지 않음
    // 필요 시 추가
})
public class BookTalkerBackendApplication {
    // ...
}
```

또는 application.yml에서:
```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
      - org.springframework.boot.autoconfigure.session.SessionAutoConfiguration  # 주의: 세션 사용 중이면 제외하지 말 것
```

#### 선정 이유
- **불필요한 빈 생성**: Spring Boot는 기본적으로 많은 Auto Configuration 실행
- **클래스 로딩**: 사용하지 않는 라이브러리도 클래스 로드 → Metaspace 낭비
- **초기화 시간**: 불필요한 빈 초기화로 시작 시간 증가
- **현재 프로젝트**: Redis 의존성은 주석 처리되어 있지만 Auto Configuration은 실행될 수 있음

#### 예상 효과
- 시작 시간 **2-5초 단축**
- Metaspace 메모리 **10-20MB 절약**

---

### 9. Aladin API 응답 캐싱 적용 ⭐⭐

#### 적용 방법

**build.gradle 의존성 추가**
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-cache'
    implementation 'com.github.ben-manes.caffeine:caffeine'  // 경량 캐시 라이브러리
}
```

**CacheConfig.java 생성**
```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "aladinBooks", "aladinSearch", "aladinList"
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(100)           // 최대 100개 항목
            .expireAfterWrite(1, TimeUnit.HOURS)  // 1시간 후 만료
            .recordStats());            // 통계 기록
        return cacheManager;
    }
}
```

**BookService.java 수정**
```java
@Cacheable(value = "aladinBooks", key = "#isbn13")
public AladinBook getBookByIsbn13WithApi(String isbn13) {
    // 기존 코드
}

@Cacheable(value = "aladinSearch", key = "#request")
public AladinResponse search(SearchRequest request) {
    // 기존 코드
}

@Cacheable(value = "aladinList", key = "#request")
public AladinResponse list(ListRequest request) {
    // 기존 코드
}
```

#### 선정 이유
- **외부 API 호출 비용**: Aladin API 호출은 네트워크 I/O 발생 → 응답 시간 증가
- **동일 요청 반복**: 인기 도서, 베스트셀러 등은 여러 사용자가 반복 조회
- **Caffeine 선택**
  - Redis보다 **훨씬 경량** (인메모리 라이브러리)
  - 외부 서버 불필요 → 네트워크 오버헤드 없음
  - 메모리 사용량 제한 가능 (maximumSize)
- **메모리 트레이드오프**: 약 10-20MB 메모리 사용하지만, 네트워크 I/O 및 CPU 절약 효과가 더 큼

#### 예상 효과
- API 호출 **70-80% 감소** (캐시 히트율에 따라)
- 응답 시간 **50-90% 개선** (캐시 히트 시)
- CPU 사용량 감소

---

### 10. Docker 이미지 최적화 (배포 방식에 따라) ⭐⭐

#### 적용 방법

**Dockerfile (Multi-stage build + Alpine)**
```dockerfile
# Build stage
FROM gradle:8.5-jdk17-alpine AS builder
WORKDIR /app
COPY book_talker_backend/ .
RUN gradle clean bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 비root 사용자 생성
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# JVM 옵션 설정
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m"

EXPOSE 8010

ENTRYPOINT exec java $JAVA_OPTS -jar app.jar
```

#### 선정 이유
- **Alpine Linux**: Ubuntu 기반(~200MB) 대비 Alpine(~5MB)은 **약 195MB 경량**
- **JRE vs JDK**: 런타임에는 JRE만 필요 (JDK는 빌드 시에만)
  - JDK: ~400MB, JRE: ~150-200MB → **약 200MB 절약**
- **Multi-stage build**: 빌드 도구(Gradle)를 최종 이미지에 포함하지 않음
- **계층 캐싱**: Docker 계층 캐싱으로 재빌드 시간 단축
- **보안**: 비root 사용자로 실행

#### 예상 효과
- Docker 이미지 크기: **600MB → 250-300MB**
- 배포 시간 단축
- 메모리 절약 (OS 오버헤드 감소)

---

### 11. Lazy Loading 및 Entity 최적화 ⭐

#### 적용 방법

**기존 코드 검토 및 수정**

```java
// Review Entity - Book 관계 Lazy Loading 확인
@Entity
public class Review {
    @ManyToOne(fetch = FetchType.LAZY)  // Lazy Loading 확인
    @JoinColumn(name = "isbn13")
    private Book book;

    // DTO 변환 메서드 추가 (필요한 필드만)
    public ReviewResponse toResponse() {
        return ReviewResponse.builder()
            .id(this.id)
            .writer(this.writer)
            .content(this.content)
            .rating(this.rating)
            .isbn13(this.book.getIsbn13())  // Book 전체가 아닌 필요한 필드만
            .build();
    }
}
```

**쿼리 최적화**
```java
// ReviewRepository.java
@EntityGraph(attributePaths = {"book"})  // N+1 문제 해결
List<Review> findByWriter(String writer);

@Query("SELECT r FROM Review r JOIN FETCH r.book WHERE r.writer = :writer")  // 명시적 JOIN FETCH
List<Review> findByWriterWithBook(@Param("writer") String writer);
```

#### 선정 이유
- **N+1 쿼리 문제**: Lazy Loading 시 각 Review마다 Book 조회 쿼리 발생 → DB 부하 및 메모리 낭비
- **불필요한 데이터 로딩**: Entity 전체를 로드하지 않고 필요한 필드만 DTO로 변환
- **메모리 효율**: Entity는 영속성 컨텍스트에 캐시되어 메모리 점유
- **현재 코드**: Review에서 Book을 ManyToOne으로 참조하고 있어 최적화 필요

#### 예상 효과
- DB 쿼리 수 **50-90% 감소** (N+1 문제 해결)
- 메모리 사용량 감소 (불필요한 Entity 로딩 방지)

---

### 12. Spring Boot Actuator 비활성화 또는 최소화 ⭐

#### 적용 방법

**Actuator 미사용 시 (build.gradle에서 제거)**
```gradle
dependencies {
    // implementation 'org.springframework.boot:spring-boot-starter-actuator'  // 제거
}
```

**Actuator 사용 시 (최소화)**
```yaml
management:
  endpoints:
    enabled-by-default: false      # 모든 엔드포인트 기본 비활성화
    web:
      exposure:
        include: health              # health만 활성화
  endpoint:
    health:
      enabled: true
      show-details: never            # 상세 정보 비활성화
```

#### 선정 이유
- **불필요한 기능**: 현재 build.gradle에 Actuator가 없는 것으로 보이지만, 추가 시 고려
- **메모리 오버헤드**: Actuator는 메트릭 수집, 엔드포인트 등으로 **약 20-30MB** 메모리 사용
- **보안**: 프로덕션에서 불필요한 엔드포인트 노출 위험
- **모니터링 대안**: 외부 모니터링 도구 사용 시 Actuator 불필요

#### 예상 효과
- 메모리 절약: **약 20-30MB** (사용 시)
- 보안 강화

---

### 13. Jackson JSON 처리 최적화 ⭐

#### 적용 방법
```yaml
spring:
  jackson:
    default-property-inclusion: non_null  # null 필드 제외
    serialization:
      write-dates-as-timestamps: true     # 날짜를 timestamp로 (문자열보다 경량)
      fail-on-empty-beans: false
    deserialization:
      fail-on-unknown-properties: false   # 알 수 없는 속성 무시 (Aladin API)
```

#### 선정 이유
- **직렬화 비용**: JSON 직렬화/역직렬화는 CPU 집약적
- **불필요한 필드**: null 필드 직렬화는 네트워크 대역폭 및 메모리 낭비
- **Aladin API**: 외부 API 응답에 불필요한 필드가 많을 수 있음 → 무시하여 메모리 절약
- **날짜 처리**: ISO 8601 문자열보다 timestamp가 처리 속도 및 크기 면에서 유리

#### 예상 효과
- JSON 처리 속도 **10-20% 개선**
- 네트워크 페이로드 크기 감소

---

### 14. OAuth2 설정 최적화 ⭐

#### 적용 방법
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          naver:
            # 필요한 scope만 요청 (현재는 너무 많음)
            scope:
              - id
              - email
              - name
              # 불필요한 항목 제거: gender, age, birthday, profile_image, birthyear, mobile
```

**SecurityConfig.java 수정**
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)  // ALWAYS → IF_REQUIRED
            .maximumSessions(1)                                        // 동일 사용자 최대 세션 수
            .maxSessionsPreventsLogin(false)                          // 새 로그인 시 기존 세션 무효화
        );
    // 기존 설정...
}
```

#### 선정 이유
- **과도한 Scope**: 현재 10개 항목 요청 중 실제 사용하는 것은 id, email, name 정도
  - 불필요한 데이터 전송 및 저장 → 메모리 낭비
- **세션 정책**: ALWAYS는 매 요청마다 세션 생성 → IF_REQUIRED로 필요 시에만 생성
- **동시 세션 제한**: 동일 사용자의 다중 로그인 방지 → 메모리 사용량 감소

#### 예상 효과
- 세션 크기 **30-40% 감소**
- 네트워크 트래픽 감소

---

### 15. 빌드 최적화 (Optional) ⭐

#### 적용 방법

**build.gradle**
```gradle
tasks.named('bootJar') {
    layered {
        enabled = true  // 레이어드 JAR 활성화
    }
}

// 불필요한 의존성 제외
configurations.all {
    exclude group: 'org.springframework.boot', module: 'spring-boot-starter-logging'  // logback 대신 log4j2 사용 시
    exclude group: 'junit', module: 'junit'  // JUnit4 제외 (JUnit5 사용)
}
```

**Spring Boot 3.x 최적화 플래그**
```properties
# application-prod.yml
spring:
  main:
    lazy-initialization: true       # Lazy Bean 초기화 (시작 시간 단축, 메모리 절약)
    # 주의: 첫 요청 시 초기화로 첫 응답이 느릴 수 있음
```

#### 선정 이유
- **Lazy Initialization**: 빈을 사용 시점에 초기화 → 시작 시 메모리 사용량 감소
  - 사용하지 않는 빈은 초기화하지 않음
- **레이어드 JAR**: Docker 이미지 빌드 시 캐싱 효율 증가
- **주의사항**: Lazy Initialization은 첫 요청 시 지연 발생 가능

#### 예상 효과
- 시작 시간 **20-30% 단축**
- 초기 메모리 사용량 **30-50MB 감소**
- 단, 첫 API 호출 시 200-500ms 지연 가능

---

## 우선순위별 적용 순서

### Phase 1: 즉시 적용 (필수) - 예상 메모리 절약: ~200-300MB
1. **JVM Heap 메모리 최적화** (⭐⭐⭐)
2. **Undertow 웹 서버 변경** (⭐⭐⭐)
3. **HikariCP 연결 풀 최적화** (⭐⭐⭐)
4. **DevTools 제거** (⭐⭐)
5. **로깅 레벨 최적화** (⭐⭐)

### Phase 2: 성능 개선 (중요) - 예상 CPU/IO 절약: ~30-40%
6. **Aladin API 캐싱** (⭐⭐)
7. **JPA 최적화 설정** (⭐⭐)
8. **세션 타임아웃 단축** (⭐⭐)
9. **Docker 이미지 최적화** (⭐⭐)

### Phase 3: 세부 최적화 (선택) - 추가 메모리 절약: ~50-100MB
10. **불필요한 Auto Configuration 비활성화** (⭐)
11. **Lazy Loading 및 Entity 최적화** (⭐)
12. **OAuth2 설정 최적화** (⭐)
13. **Jackson JSON 최적화** (⭐)
14. **Actuator 비활성화** (⭐)
15. **빌드 최적화** (⭐)

---

## 예상 최종 메모리 사용량

| 항목 | 최적화 전 | 최적화 후 | 절감량 |
|------|---------|----------|--------|
| JVM Heap | ~800MB | ~512MB | ~288MB |
| Metaspace | ~150MB | ~128MB | ~22MB |
| Native Memory | ~100MB | ~70MB | ~30MB |
| Thread Stack | ~50MB | ~30MB | ~20MB |
| DB Connection Pool | ~100MB | ~30MB | ~70MB |
| 세션 저장소 | ~100MB | ~50MB | ~50MB |
| 기타 | ~100MB | ~80MB | ~20MB |
| **합계** | **~1400MB** | **~900MB** | **~500MB** |

**결론**: 1GB RAM에서 안정적으로 운영 가능 (약 100MB 여유)

---

## 추가 고려사항

### 1. 모니터링
- **메모리 모니터링**: `jstat`, `jmap` 등으로 주기적으로 메모리 사용량 확인
- **OOM Killer 방지**:
  ```bash
  echo "vm.overcommit_memory=1" >> /etc/sysctl.conf
  ```

### 2. Swap 설정
- 1GB RAM 환경에서는 Swap 필수
- 최소 2GB Swap 설정 권장 (느리지만 OOM 방지)

### 3. 프론트엔드 분리
- 프론트엔드는 별도 정적 호스팅 (Netlify, Vercel 등) 권장
- 백엔드는 API 서버로만 사용

### 4. 데이터베이스 인덱스
- Review.writer, Review.isbn13에 인덱스 생성
- 쿼리 성능 개선 → CPU 사용량 감소

### 5. 향후 스케일링
- 트래픽 증가 시 PostgreSQL 쿼리 최적화
- Redis 추가 시 세션 외부화 고려 (단, 메모리 추가 필요)

---

## 참고 자료

1. Spring Boot Memory Optimization: https://spring.io/blog/2015/12/10/spring-boot-memory-performance
2. HikariCP Configuration: https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby
3. Serial GC vs G1GC: https://www.oracle.com/technical-resources/articles/java/g1gc.html
4. Docker Multi-stage Builds: https://docs.docker.com/build/building/multi-stage/
