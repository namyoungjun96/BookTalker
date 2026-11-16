# BookTalker - Project Context

독서 후기 기록 및 공유 서비스. 이직 포트폴리오 프로젝트.

---

## 기술 스택

| 영역 | 기술 |
|------|------|
| Backend | Spring Boot 3.x, Spring Security, Spring Data JPA, Spring Scheduler |
| Language | Java 17 |
| Database | PostgreSQL, Flyway (스키마 변경은 마이그레이션 파일로만, `ddl-auto: validate`) |
| Frontend | Vue3, PWA |
| Build | Gradle |
| Infrastructure | OCI Free Tier (WAS 서버 + DB 서버 분리), Nginx, GitHub Actions (빌드·테스트·배포) |
| Logging | Logback (프로파일별 분리), Discord Webhook Custom Appender |
| External API | 카카오 책 검색 API, Naver OAuth2 |

---

## 시스템 아키텍처

```
[운영]
브라우저 → https://booktalker.co.kr (Nginx, HTTPS)
  ├── /      → Vue3 정적 파일 서빙
  └── /api   → WAS (리버스 프록시, `/api` 접두사를 떼고 전달 — same-origin)
        ├── PostgreSQL:5432  (OCI Server 2)
        ├── 카카오 책 검색 API
        ├── Naver OAuth2
        └── Discord Webhook (ERROR 알림, prod 전용)

[로컬]
Vite 개발 서버(:5173) → WAS:8010 직접 요청 (cross-origin → CORS 필요)
```

- 운영은 Nginx가 `/api`를 프록시해 same-origin이라 CORS가 필요 없고, 로컬만 CORS를 쓴다 (`SecurityConfig`의 CORS 설정은 로컬용)
- 무중단 배포: WAS를 8010 ⇄ 8011 포트로 교대하며 Nginx 설정을 전환 (`deploy/deploy.sh`), 헬스체크 실패 시 자동 롤백
- `java -jar`로 직접 실행 (Docker Compose 미사용)
- 프로파일: `local` (개발) / `prod` (운영), `SPRING_PROFILES_ACTIVE` 환경변수로 전환

---

## 패키지 구조

```
com.example.book_talker_backend
├── Controller.java         # /user/me, /auth/session
├── book/
│   ├── BookController.java
│   ├── cache/              # SearchCacheKey (검색 캐시)
│   ├── dao/
│   ├── entity/
│   │   ├── Book.java
│   │   └── dto/
│   ├── exception/
│   ├── infrastructure/
│   │   ├── BookSearchClient.java   # 책 검색 포트(인터페이스)
│   │   ├── IsbnParser.java
│   │   └── kakao/                  # 카카오 어댑터 (KakaoBookSearchClient, KakaoBookMapper)
│   └── service/            # BookService.java
├── config/
│   ├── SecurityConfig.java
│   ├── WebConfig.java
│   ├── CacheConfiguration.java
│   └── DiscordWebhookAppender.java
├── exception/
│   └── GlobalExceptionHandler.java
├── monitoring/             # API 호출량 집계·일일 리포트 (ApiUsage*)
├── oauth2/
├── review/
│   ├── RankController.java
│   ├── ReviewController.java
│   ├── TeamReviewController.java   # 모임 피드
│   ├── WeightedScoreCalculator.java
│   ├── dao/
│   ├── entity/
│   │   ├── Review.java
│   │   ├── Rank.java
│   │   └── dto/
│   ├── exception/
│   └── service/
│       ├── ReviewService.java
│       └── RankService.java
├── team/
│   ├── TeamController.java
│   ├── dao/
│   ├── entity/             # Team, TeamMember, MemberRoleEnum, MemberStatusEnum, dto/
│   ├── exception/
│   └── service/            # TeamService.java
├── test/                   # TestAuthController (local 프로파일 전용 로그인)
└── user/
```

---

## 핵심 비즈니스 규칙

### 회독 수 기반 리뷰 시스템
- DB 유니크 제약: `(writer, isbn13, reading_count)`
- `POST /review` → 1회독 리뷰
- `POST /review/next-reading` → 2회독 이상 (해당 책의 기존 회독이 있어야 작성 가능, 회차는 최대 회차 + 1)
- 회차와 관계없이 공개 범위(`visibility`) 선택·변경 가능
- 2-layer 방어: 서비스 사전 검증 + GlobalExceptionHandler의 DataIntegrityViolationException 처리

### 공개 범위 설계 (`visibility`)
| 값 | 화면 표기 | 본문(`content`) 열람 범위 |
|----|-----------|---------------------------|
| `PUBLIC` | 전체공개 | 비로그인 포함 전체 |
| `GROUP` | 모임공개 | 같은 모임의 ACTIVE 모임원 (모임 피드 `GET /teams/{teamId}/reviews`와 상세에서 열람) |
| `PRIVATE` | 비공개 | 작성자 본인만 (모임 피드에서 제외) |

- `headline` (한 줄 요약): 필수 입력, 항상 공개
- `visibility`: 필수 (null이면 400), 프론트 기본값은 `PRIVATE`
- 책별 독후감 목록(`GET /rank/reviews`): 공개 범위·회차와 관계없이 한 줄 요약 노출, 본문 상세(`GET /rank/reviews/{reviewId}`)는 `PUBLIC`만
- 이전 정책(`is_public` Boolean, 2회독 이상 강제 비공개)은 폐기

### 모임(팀) 시스템
- 모임 생성자는 `OWNER`, 초대코드로 가입한 사용자는 `MEMBER`. 모임원 상태: `PENDING` / `ACTIVE` / `BANNED` / `INACTIVE`
- 초대코드는 `team.code` (VARCHAR(13), NOT NULL + UNIQUE). 생성 시 충돌하면 최대 3회 재생성, 모두 실패하면 500
- `(provider_id, team_id)` 복합 UNIQUE — 한 사용자는 한 모임에 한 번만 가입
- 모임 존재 여부 숨기기: "모임 없음"과 "모임원 아님(`INACTIVE`·`BANNED` 포함)"을 모두 404로 응답. 모임원인데 권한이 부족하면 403
- 모임 피드: 모임원의 `PUBLIC`·`GROUP` 독후감 (가입 전 리뷰 포함, `INACTIVE`·`BANNED` 사용자 제외), 수정일 최신순, 4개 단위
- 책 검색은 `BookSearchClient` 인터페이스 뒤에 두고 구현체는 `book/infrastructure/kakao/`. 알라딘 시절 인터페이스가 없어 서비스까지 고쳐야 했던 경험으로, 카카오로 교체하며 도입
- 근거: `docs/decision/팀 기능 의사결정.md`, `docs/spec/team/`

### 랭킹 시스템
- `@Scheduled(cron = "0 0 4 * * ?")` - 매일 새벽 4시 배치
- 전체 삭제 후 재삽입 방식 (`deleteAll` → `saveAll`)
- **1회독 리뷰만 집계** — 같은 사람이 회독마다 평점을 남겨 랭킹을 왜곡하는 것 방지 (공개 범위와 무관)
- `HAVING COUNT(*) >= 3` 조건으로 리뷰 수 미달 책 제외
- **베이즈 가중 평점(`weighted_score`)** — 리뷰가 적은 책이 평균만으로 상위에 오르는 것을 방지 (`WeightedScoreCalculator`). 랭킹 조회 정렬도 이 값 기준 (PR #39)
- 전체 랭킹 + 장르별 그룹 랭킹 지원

---

## DB 스키마

### review
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | 리뷰 ID |
| writer | VARCHAR | 작성자 (Naver provider_id) |
| isbn13 | VARCHAR FK | 책 ISBN-13 |
| reading_count | INT NOT NULL | 회독 수 |
| headline | VARCHAR NOT NULL | 한 줄 요약 (항상 공개) |
| content | TEXT NOT NULL | 본문 |
| rating | INT | 평점 (1~5) |
| visibility | VARCHAR NOT NULL | 공개 범위 (`PUBLIC` / `GROUP` / `PRIVATE`) |
| reg_date | TIMESTAMP | 등록 일시 |
| mod_date | TIMESTAMP | 수정 일시 |

**유니크 제약:** `(writer, isbn13, reading_count)`

### team
| 컬럼 | 타입 | 설명 |
|------|------|------|
| team_id | BIGINT PK | 모임 ID |
| team_name | VARCHAR | 모임 이름 (1~20자) |
| team_description | VARCHAR | 모임 소개 (1~100자) |
| code | VARCHAR(13) NOT NULL UNIQUE | 초대코드 |
| created_at / updated_at | TIMESTAMP | |

### team_member
| 컬럼 | 타입 | 설명 |
|------|------|------|
| team_member_id | BIGINT PK | |
| team_id | BIGINT FK | 모임 |
| provider_id | VARCHAR FK | 사용자 (Naver provider_id) |
| role | VARCHAR NOT NULL | `OWNER` / `MEMBER` |
| status | VARCHAR NOT NULL | `PENDING` / `ACTIVE` / `BANNED` / `INACTIVE` |
| display_name | VARCHAR | 모임 닉네임 (1~10자) |
| joined_at | TIMESTAMP | |

**유니크 제약:** `(provider_id, team_id)`

### book
| 컬럼 | 타입 | 설명 |
|------|------|------|
| isbn13 | VARCHAR PK | ISBN-13 |
| title | VARCHAR | 제목 |
| author | VARCHAR | 저자 |
| cover | VARCHAR | 표지 URL |
| genre | VARCHAR NOT NULL | 장르 (V4로 null 차단) |
| publisher | VARCHAR | 출판사 |

### book_rank (랭킹 스냅샷)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| isbn13 | VARCHAR PK | 책 ISBN-13 |
| genre | VARCHAR | 장르 |
| title | VARCHAR | 제목 |
| cover | VARCHAR | 표지 URL |
| avg_rating | DOUBLE | 평균 평점 |
| weighted_score | DOUBLE NOT NULL | 베이즈 가중 평점 (랭킹 정렬 기준) |
| review_count | INT | 리뷰 수 |
| updated_at | TIMESTAMP | 배치 실행 시각 |

> 스키마는 Flyway(`db/migration` V1 baseline ~ V4)로만 변경한다.

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
| IllegalArgumentException, MethodArgumentNotValidException, MethodArgumentTypeMismatchException, UnknownEnumValueException | 400 | 잘못된 입력, 검증 실패 |
| AccessDeniedException, TeamAccessDeniedException | 403 | 리소스 소유권 불일치, 모임원이지만 모임장 권한 없음 |
| ResourceNotFoundException (및 하위) | 404 | 없는 리소스, 모임 없음/모임원 아님(존재 숨김) |
| DataIntegrityViolationException, DuplicateTeamMemberException, BannedTeamMemberException | 409 | DB 유니크 제약 위반, 이미 모임원 / 추방됨 |
| InviteCodeGenerationFailedException | 500 | 초대코드 3회 충돌 (사용자용 메시지로 변환) |
| Exception | 500 | 미처리 예외 (응답은 고정 문구, 상세는 로그) |

> 응답 메시지에 내부 메서드명 등이 섞이는 문제와 Spring 기본 예외의 500 처리는 예외 처리 리팩토링(`FIX`)에서 정리 예정

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
| GET | /rank/reviews | 책별 독후감 목록 — 한 줄 요약, 공개 범위 무관 (`?isbn13=`, 페이지네이션) |
| GET | /rank/reviews/{reviewId} | 공개 독후감 상세 (`PUBLIC`만, 아니면 404) |
| GET | /teams/join/{code} | 초대코드로 모임 미리보기 (가입 전) |
| GET | /auth/session | 로그인 상태 확인 (로그인 200 / 비로그인 401, FE 세션 체크용) |

### 로그인 필요
| Method | Path | 설명 |
|--------|------|------|
| POST | /review | 1회독 리뷰 작성 |
| POST | /review/next-reading | 다음 회독 리뷰 작성 (2회독 이상) |
| PUT | /review | 리뷰 수정 |
| DELETE | /review | 리뷰 삭제 |
| GET | /review/list | 내 리뷰 목록 |
| GET | /review/detail | 리뷰 상세 |
| POST | /teams | 모임 생성 |
| GET | /teams | 내 모임 목록 |
| GET / PUT / DELETE | /teams/{teamId} | 모임 상세 / 수정 / 삭제 (수정·삭제는 모임장) |
| GET | /teams/{teamId}/members | 모임원 목록 |
| GET | /teams/{teamId}/code | 초대코드 조회 |
| POST | /teams/join | 초대코드로 가입 |
| GET | /teams/{teamId}/reviews | 모임 피드 |
| GET | /teams/{teamId}/reviews/{reviewId} | 모임 독후감 상세 |
| GET | /user/me | 내 정보 |
| GET | /book/search | 카카오 책 검색 (로그인 필요 — 외부 API 쿼터를 비로그인 반복 호출로 소진하는 것을 막기 위함) |

---

## 개발 규칙

- 새 기능 설계 순서: **도메인 규칙 → DB 스키마 → API 설계 → 예외 처리**
- 코드보다 설계 의도와 트레이드오프 설명 우선
- OCI Free Tier (1GB RAM) 환경 고려 → 무거운 인프라 추가 지양
- 포트폴리오 목적: 기술적 의사결정 근거가 중요

### 테스트
- 배포 전 CI(GitHub Actions)가 테스트를 실행한다 (`build -x test` 제거, PR #40)
- DB가 필요한 테스트는 Testcontainers(PostgreSQL), 성능 측정 테스트는 `perf` 태그로 분리해 기본 `test`에서 제외하고 `./gradlew perfTest`(로컬 DB 필요)로만 실행

### 역할 분담
| 영역 | 코드 작성 | 커밋·PR |
|------|-----------|------|
| 백엔드 (`book_talker_backend`) | **개발자가 직접 작성** — Claude는 조언·검토만 (라이브 코딩 대비) | Claude (검수 후 개발자 승인을 받아 진행) |
| 프론트엔드 (`book_talker_frontend`) | Claude | Claude (개발자 승인 후, 새 파일 포함) |

## 커밋 메시지 규칙

형식: `타입(영역): [도메인] 요약`

| 영역 | 대상 |
|------|------|
| `FE` | 프론트엔드 (`book_talker_frontend`) |
| `BE` | 백엔드 (`book_talker_backend`) |

- 타입: `FEAT`, `FIX`, `REFACTOR`, `DOCS` 등
- 예: `FEAT(FE): [팀] 내 독서 모임 목록 화면 추가`, `FIX(BE): [팀] 팀 조회 응답 DTO 변경`

## 작업 문서화 규칙

작업 단위마다 마크다운 문서를 `docs/`에 남긴다. `docs/`는 git에 올리지 않는 작업용 폴더이고, 공개용으로 정리한 문서만 `document/`(`infra/`·`review/`·`team/`)에 두어 README에서 링크한다.

### 폴더 구조

| 폴더 | 내용 |
|------|------|
| `docs/design/` | `[design]` 설계 문서. 맨 위에 `**상태: 검토 중**` / `**상태: 결정됨 (날짜, 선택안)**`을 적고, 결정돼도 파일은 옮기지 않는다 |
| `docs/spec/<기능>/` | 기능 하나당 폴더 하나(영문명). `<기능> 스펙 정의서.md` + `<기능> API 명세서.md`, 기능 완료 시 고정된 기록 |
| `docs/pr/` | `[commit]` PR 문서 |
| `docs/decision/` | 의사결정 기록, 월별 로드맵 |
| `docs/arguments/` | 코드·스펙 현황 분석 |
| `docs/unresolved/` | 설계까지는 아닌 메모·유의점 |
| `docs/incident/` · `docs/review/` · `docs/loadtest/` · `docs/release/` | 장애 보고서 · 리뷰 결과 · 부하 테스트 · 릴리스 기록 |

### 파일명 규칙
`[타입]_제목.md`

PR 문서(`[commit]`)는 실제 PR 제목을 그대로 파일명으로 사용한다.
예: `[commit]_랭킹 조회 정렬을 베이즈 가중 점수로 변경.md`

| 타입 | 용도 |
|------|------|
| `[design]` | 구현 전 설계·검수 요청 |
| `[commit]` | 커밋·PR 내용 정리 |
| `[review]` | 코드 리뷰 결과 |
| `[incident]` | 장애 보고서 |

### 작업 워크플로 (전체 흐름)

1. **요청 접수** — 개발자가 변경/수정/추가 사항 전달
2. **분석 및 설계** — Claude가 코드 분석 후 구현 방식을 `[design]` 문서로 작성하여 개발자에게 전달
3. **설계 승인** — 개발자가 설계 문서 검토 후 승인 (승인 전 구현 시작 금지)
4. **구현** — 승인된 설계대로 구현 (프론트: Claude, 백엔드: 개발자 — [역할 분담](#역할-분담) 참고)
5. **구현 검수** — 개발자가 구현 결과 검토 (백엔드는 Claude가 검토 의견 제시)
6. **브랜치 생성 및 커밋** — 승인 시 작업 단위에 맞는 브랜치 생성 후 커밋 (검수 없이 커밋 금지, FE/BE 커밋 분리)
7. **PR 문서 작성** — Claude가 PR 내용을 `[commit]` 문서로 작성하여 개발자에게 전달
8. **PR 진행** — 개발자가 PR 문서 검토 후 직접 PR 생성

### [design] 문서 작성 기준
해결 방식이 **여러 가지**인지 여부로 판단한다.

| 상황 | [design] 문서 | 흐름 |
|------|--------------|------|
| 해결 방식이 여러 가지 (트레이드오프 존재) | ✅ 작성 | 설계 문서 → 승인 → 구현 → 검수 → 커밋 → PR 문서 |
| 해결 방식이 명확히 하나 (단순 수정) | ❌ 생략 | 구현 → 검수 → 커밋 → PR 문서 |

예시:
- 네비게이션 탭 활성화 버그 → 3가지 방법 존재 → [design] 작성
- CSS 여백 수정, 오타 수정 → 선택지 없음 → [design] 생략

## 남은 작업
> 전체 일정과 완료 현황은 `docs/decision/11월 로드맵.md`가 기준이다.

- [ ] 테스트 정비 — Testcontainers PostgreSQL 16, `application-test.yml`, 작성 API 검증 누락 테스트 (CI는 배포 전 테스트를 이미 실행 중, PR #40)
- [ ] Swagger/OpenAPI 문서화
- [ ] PostgreSQL 슬로우 쿼리 로그 + 인덱스 튜닝
- [ ] 마이 페이지 완성 (독서 통계, 프로필 수정)
- [ ] 외부 API 사용자별 호출 제한 (카카오 쿼터 한도에 근접할 때)
- [ ] `review.writer` FK 제약 (Flyway 마이그레이션으로)
- [ ] 예외 처리 리팩토링 — 팀 기능 완료 후 `FIX`로 진행 (`docs/unresolved/[design]_리뷰 예외 처리 방향 논의.md`)
