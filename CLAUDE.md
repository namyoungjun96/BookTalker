# BookTalker - Project Context

독서 후기 기록 및 공유 서비스. 이직 포트폴리오 프로젝트.

---

## 기술 스택

| 영역 | 기술 |
|------|------|
| Backend | Spring Boot 3.x, Spring Security, Spring Data JPA, Spring Scheduler |
| Language | Java 17 |
| Database | PostgreSQL |
| Frontend | Vue3, PWA |
| Build | Gradle |
| Infrastructure | OCI Free Tier (WAS 서버 + DB 서버 분리), Nginx |
| Logging | Logback (프로파일별 분리), Discord Webhook Custom Appender |
| External API | 알라딘 API, Naver OAuth2 |

---

## 시스템 아키텍처

```
[브라우저]
  ├── Nginx:80       → Vue3 정적 파일 서빙 (리버스 프록시 아님)
  └── WAS:8010       → Spring Boot API 서버
        ├── PostgreSQL:5432  (OCI Server 2)
        ├── 알라딘 API
        ├── Naver OAuth2
        └── Discord Webhook (ERROR 알림, prod 전용)
```

- 브라우저가 WAS:8010으로 직접 API 요청 → **CORS 설정 필수**
- `java -jar`로 직접 실행 (Docker Compose 미사용)
- 프로파일: `local` (개발) / `prod` (운영), `SPRING_PROFILES_ACTIVE` 환경변수로 전환

---

## 패키지 구조

```
com.example.book_talker_backend
├── book/
│   ├── BookController.java
│   ├── dao/
│   ├── entity/
│   │   ├── Book.java
│   │   ├── conveter/       # JPA AttributeConverter
│   │   └── dto/
│   ├── exception/
│   ├── infrastructure/     # AladinBookMapper.java
│   └── service/            # BookService.java
├── config/
│   ├── SecurityConfig.java
│   ├── WebConfig.java
│   └── DiscordWebhookAppender.java
├── exception/
│   └── GlobalExceptionHandler.java
├── oauth2/
├── review/
│   ├── RankController.java
│   ├── ReviewController.java
│   ├── dao/
│   ├── entity/
│   │   ├── Review.java
│   │   ├── Rank.java
│   │   └── dto/
│   └── service/
│       ├── ReviewService.java
│       └── RankService.java
└── user/
```

---

## 핵심 비즈니스 규칙

### 회독 수 기반 리뷰 시스템
- DB 유니크 제약: `(writer, isbn13, reading_count)`
- `POST /review` → 1회독 리뷰 (공개 여부 선택 가능)
- `POST /review/next-reading` → 2회독 이상 (isPublic 강제 false)
- 2-layer 방어: 서비스 사전 검증 + GlobalExceptionHandler의 DataIntegrityViolationException 처리

### 공개/비공개 설계
- `headline` (한 줄 요약): 필수 입력, 항상 공개
- `content` (본문): `isPublic=true`일 때만 비로그인 노출
- 2회독 이상은 `isPublic` 변경 불가 (강제 비공개 유지)

### 랭킹 시스템
- `@Scheduled(cron = "0 0 4 * * ?")` - 매일 새벽 4시 배치
- 전체 삭제 후 재삽입 방식 (`deleteAll` → `saveAll`)
- `HAVING COUNT(*) >= 3` 조건으로 리뷰 수 미달 책 제외
- 전체 랭킹 + 장르별 그룹 랭킹 지원

---

## DB 스키마

### review
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | 리뷰 ID |
| writer | VARCHAR | 작성자 (Naver provider_id) |
| isbn13 | VARCHAR FK | 책 ISBN-13 |
| reading_count | INT | 회독 수 |
| headline | VARCHAR NOT NULL | 한 줄 요약 (항상 공개) |
| content | TEXT | 본문 |
| rating | INT | 평점 (1~5) |
| is_public | BOOLEAN DEFAULT false | 공개 여부 |
| reg_date | TIMESTAMP | 등록 일시 |
| mod_date | TIMESTAMP | 수정 일시 |

**유니크 제약:** `(writer, isbn13, reading_count)`

### book
| 컬럼 | 타입 | 설명 |
|------|------|------|
| isbn13 | VARCHAR PK | ISBN-13 |
| title | VARCHAR | 제목 |
| author | VARCHAR | 저자 |
| cover | VARCHAR | 표지 URL |
| genre | VARCHAR | 장르 |
| publisher | VARCHAR | 출판사 |

### rank (랭킹 스냅샷)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| isbn13 | VARCHAR | 책 ISBN-13 |
| genre | VARCHAR | 장르 |
| title | VARCHAR | 제목 |
| cover | VARCHAR | 표지 URL |
| avg_rating | DOUBLE | 평균 평점 |
| review_count | INT | 리뷰 수 |
| updated_at | TIMESTAMP | 배치 실행 시각 |

### oauth2_user
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| provider | VARCHAR | OAuth2 제공자 (naver) |
| provider_id | VARCHAR | Naver 사용자 ID |
| provider_email | VARCHAR | 이메일 |
| created_at | TIMESTAMP | |

---

## 예외 처리 전략

`GlobalExceptionHandler` (@RestControllerAdvice):

| 예외 | HTTP | 사유 |
|------|------|------|
| IllegalArgumentException | 400 | 잘못된 입력, 존재하지 않는 리소스 |
| AccessDeniedException | 403 | 리소스 소유권 불일치 |
| DataIntegrityViolationException | 409 | DB 유니크 제약 위반 |
| Exception | 500 | 미처리 예외 |

---

## 로그 전략

**logback-spring.xml** 프로파일별 분리:
- `local`: Console + File, 애플리케이션 DEBUG, Hibernate SQL DEBUG
- `prod`: File + ERROR 전용 파일 + Discord Webhook, 애플리케이션 INFO

**DiscordWebhookAppender**: `AppenderBase<ILoggingEvent>` 직접 구현
- ERROR 레벨만 필터링
- Java `HttpClient`로 Discord Webhook 호출 (타임아웃 5초)
- `webhookUrl`은 `app.discord.webhook-url` 환경변수로 주입

---

## API 엔드포인트 목록

### 인증 불필요
| Method | Path | 설명 |
|--------|------|------|
| GET | /rank | 전체/장르별 랭킹 조회 (`?genre=` 옵션) |
| GET | /rank/genres | 장르별 그룹 랭킹 |
| GET | /rank/reviews | 책별 공개 리뷰 (`?isbn13=`, 페이지네이션) |
| GET | /book/search | 알라딘 책 검색 |
| GET | /book/list | 알라딘 책 목록 (베스트셀러 등) |

### 로그인 필요
| Method | Path | 설명 |
|--------|------|------|
| POST | /review | 1회독 리뷰 작성 |
| POST | /review/next-reading | 2회독 이상 리뷰 작성 |
| PUT | /review | 리뷰 수정 |
| DELETE | /review | 리뷰 삭제 |
| GET | /review/list | 내 리뷰 목록 |
| GET | /review/detail | 리뷰 상세 |

---

## 개발 규칙

- 새 기능 설계 순서: **도메인 규칙 → DB 스키마 → API 설계 → 예외 처리**
- 코드보다 설계 의도와 트레이드오프 설명 우선
- OCI Free Tier (1GB RAM) 환경 고려 → 무거운 인프라 추가 지양
- 포트폴리오 목적: 기술적 의사결정 근거가 중요

## 작업 문서화 규칙

작업 단위마다 HTML 문서를 `document/` 하위 폴더에 남긴다.

### 폴더 구조

도메인별로 관리한다.

```
document/
├── auth/          # OAuth2, 인증 관련 설계·검수·PR 문서
├── book/          # 책 검색, 알라딘 API, 캐시 관련 문서
├── review/        # 리뷰 도메인 설계·검수·PR 문서
├── mypage/        # 마이페이지 설계·검수·PR 문서
├── monitoring/    # 로깅, Discord Webhook, API 모니터링 문서
├── infra/         # Nginx, 배포, 인프라 관련 PR 문서
└── ranking/       # 랭킹 집계 관련 문서
```

### 파일명 규칙
`[타입]_작업명_YYYYMMDD_순번.html`

| 타입 | 용도 |
|------|------|
| `[design]` | 구현 전 설계·검수 요청 |
| `[commit]` | 커밋·PR 내용 정리 |
| `[review]` | 코드 리뷰 결과 |

### 적용 시점
- 설계 검수가 필요한 작업 → 구현 전에 `[design]` 문서 작성 후 사용자 승인 대기
- 커밋·PR 올리기 전 → `[commit]` 문서로 변경 내용 정리 후 사용자 검수
- 코드 리뷰 → `[review]` 문서로 결과 정리

### 커밋 워크플로
코드 작성 완료 → 사용자 검수 → 승인 후 커밋 (검수 없이 커밋 금지)

## 남은 작업
- [ ] 테스트 코드 (단위/통합)
- [ ] 알라딘 API Key 환경변수 분리 (현재 코드에 하드코딩)
- [ ] Caffeine 캐시 (알라딘 API 반복 호출 최소화)
- [ ] Swagger/OpenAPI 문서화
- [ ] Spring Boot Actuator (헬스체크)
- [ ] PostgreSQL 슬로우 쿼리 로그 + 인덱스 튜닝
- [ ] 마이 페이지 완성 (독서 통계, 프로필 수정)
