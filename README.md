# 📚 BookTalker

> 책 에세이를 기록하고, 다른 독자들과 나누는 공간

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![Vue3](https://img.shields.io/badge/Vue3-42b883?style=flat-square&logo=vue.js&logoColor=white)
![OCI](https://img.shields.io/badge/Oracle_Cloud-F80000?style=flat-square&logo=oracle&logoColor=white)

<br>

## 프로젝트 소개

BookTalker는 책 한 권을 읽고 난 뒤의 생각과 감상을 **에세이 형태로 기록**하고, 원하는 경우 다른 사람들과 나눌 수 있는 서비스입니다.

단순한 별점·한 줄 평을 넘어, 자신만의 독서 기록을 남기고 싶은 사람들을 위해 만들었습니다.

<br>

## 주요 기능

### 🔐 소셜 로그인
- 네이버 OAuth2 로그인 (Authorization Code Flow)
- HTTP Cookie 기반 세션 관리

### 🔍 책 검색
- 알라딘 API 연동으로 제목·저자 검색
- 책 상세 정보 조회 및 DB 저장 (최초 조회 시 자동 저장)

### 📝 리뷰 (책 에세이)
- 회독 수 기반 리뷰 시스템: 같은 책을 여러 번 읽을수록 새로운 리뷰 작성 가능
- 1회독 리뷰는 공개 여부(`isPublic`) 선택 가능, 2회독 이상은 자동 비공개
- `headline` (한 줄 요약): 필수 입력, 비공개 리뷰도 랭킹 페이지에 노출
- 리뷰 수정·삭제는 본인 리뷰만 가능 (소유권 검증)

### 🏆 책별 랭킹
- 매일 새벽 4시 배치로 책별 평균 평점 랭킹 산출 (`@Scheduled`)
- 리뷰 3개 미만 책은 랭킹에서 제외 (`HAVING COUNT(*) >= 3`)
- 전체 랭킹 및 장르별 랭킹 조회 지원

### 🌐 비로그인 공개 조회
- 로그인 없이 책별 공개 리뷰 열람 가능 (페이지네이션 적용)
- 프라이버시 중심 설계: 사용자가 공개를 선택한 1회독 리뷰만 노출

### 🚨 운영 모니터링
- 프로파일별 로그 분리 (`local` / `prod`)
- `prod` 환경에서 ERROR 로그 발생 시 Discord Webhook으로 실시간 알림
- ERROR 전용 로그 파일 별도 분리, 일별 롤링 정책 (`maxHistory=100`)

<br>

## 기술 스택

| 영역 | 기술 |
|------|------|
| Backend | Spring Boot 3.x, Spring Security, Spring Data JPA, Spring Scheduler |
| Language | Java 17 |
| Database | PostgreSQL |
| Frontend | Vue3, PWA |
| Infrastructure | OCI (Oracle Cloud), Nginx |
| Logging | Logback (프로파일별 분리, Discord Webhook Appender) |
| External API | 알라딘 API, Naver OAuth2 |

<br>

## 시스템 아키텍처

```
[사용자]
  │
  ├──▶ Nginx:80 (Vue3 정적 파일 서빙)
  │
  └──▶ WAS:8010 (Spring Boot, API 서버)   ──▶ PostgreSQL:5432
             │                                   (OCI Server 2)
             ├──▶ 알라딘 API (책 정보)
             ├──▶ Naver OAuth2 (소셜 로그인)
             └──▶ Discord Webhook (ERROR 알림)
```

- **서버 2대 구성** (OCI Free Tier): WAS 서버 + DB 서버 분리
- Nginx는 Vue3 빌드 파일만 서빙 (리버스 프록시 아님)
- 브라우저가 WAS:8010으로 직접 API 요청 → CORS 설정 필요

<br>

## 기술적 의사결정

### 1. 회독 수 기반 리뷰 시스템 설계

**문제:** 같은 책을 여러 번 읽으면 리뷰를 어떻게 관리할 것인가?

**결정:** `(writer, isbn13, reading_count)` 복합 유니크 제약 + 두 가지 별도 엔드포인트로 분리

| 엔드포인트 | 역할 |
|---|---|
| `POST /review` | 1회독 리뷰 작성 |
| `POST /review/next-reading` | 2회독 이상 리뷰 작성 |

**이유:**
- 1회독과 다음 회독은 비즈니스 규칙이 다름 (공개 여부, 선행 조건 검증)
- DB 유니크 제약으로 중복 삽입을 막되, 서비스 레이어에서 사전 검증해 사용자 친화적 에러 메시지 제공 (2-layer 방어)

---

### 2. 예외 처리: HTTP 상태 코드 의미론 확보

**문제:** Spring은 `IllegalArgumentException`에 기본적으로 500을 반환한다.

**결정:** `@RestControllerAdvice` 기반 `GlobalExceptionHandler` 구현

| 예외 | HTTP 상태 | 사유 |
|---|---|---|
| `IllegalArgumentException` | 400 | 잘못된 입력·존재하지 않는 리소스 |
| `AccessDeniedException` | 403 | 리소스 소유자 불일치 |
| `DataIntegrityViolationException` | 409 | DB 유니크 제약 위반 (중복 리뷰) |
| `Exception` | 500 | 그 외 미처리 예외 |

**이유:** 클라이언트가 에러 원인을 상태 코드만으로 구분할 수 있어야 API 설계가 완결됨

---

### 3. 운영 알림: Discord Webhook Custom Appender

**문제:** OCI Free Tier 환경에서 Prometheus + Grafana 같은 모니터링 스택을 올리기엔 메모리 부담이 큼.

**결정:** Logback `AppenderBase`를 직접 구현해 ERROR 로그를 Discord로 실시간 전송

**이유:**
- 추가 인프라 없이 에러 알림 확보
- `logback-spring.xml`의 `springProfile`로 `prod` 환경에서만 활성화 → 로컬 개발에 영향 없음
- 알림에는 타임스탬프(KST), 로거명, 메시지만 포함해 노이즈 최소화

---

### 4. 랭킹 집계: DB 윈도우 함수 활용

**문제:** 매일 책별 랭킹을 산출해야 하는데, 어떻게 구현할 것인가?

**결정:** PostgreSQL `RANK() OVER` + `@Scheduled` 배치로 별도 랭킹 테이블에 저장

```sql
SELECT isbn13, avg_rating, review_count,
       RANK() OVER (ORDER BY avg_rating DESC) AS ranking
FROM (
    SELECT isbn13, AVG(rating) AS avg_rating, COUNT(*) AS review_count
    FROM review
    GROUP BY isbn13
    HAVING COUNT(*) >= 3
) sub
```

**이유:**
- 애플리케이션에서 전수 조회·정렬하는 것보다 DB가 잘하는 일을 DB에 맡기는 게 적합
- 배치로 별도 테이블에 저장해 랭킹 조회 시 집계 연산 없이 단순 SELECT로 응답 가능

---

### 5. 리뷰 공개 설계: `headline`과 `isPublic` 분리

**문제:** 리뷰를 비공개로 두면서도 랭킹 페이지에 존재감을 남길 수 있어야 한다.

**결정:** `headline`(한 줄 요약)은 항상 공개, `content`(본문)만 `isPublic`으로 제어

| 필드 | 특성 | 비로그인 노출 |
|------|------|--------------|
| `headline` | 필수 입력, 항상 공개 | ✅ 항상 |
| `content` | 선택적 공개 (`isPublic`) | `isPublic=true` 일 때만 |

**이유:** "완전 공개 아니면 완전 비공개"가 아닌 중간 상태를 제공해 사용자 선택권 확보

---

### 6. PostgreSQL 선택

**이유:**
- 실무(모비젠 NI팀)에서 PostgreSQL을 사용했고, 심화 학습 목적으로 동일 DB 선택
- MySQL 대비 윈도우 함수, JSON 타입 등 고급 기능이 풍부함
- 랭킹 집계처럼 복잡한 쿼리를 직접 짜볼 수 있는 환경 확보

<br>

## 향후 계획

- [ ] 테스트 코드 작성 (단위 테스트, 통합 테스트)
- [ ] 알라딘 API 응답 캐싱 (Caffeine, 반복 호출 최소화)
- [ ] Swagger/OpenAPI 문서화
- [ ] 마이 페이지 완성 (독서 통계, 프로필 수정)
- [ ] Spring Boot Actuator 도입 (헬스체크, JVM 메트릭)
- [ ] PostgreSQL 슬로우 쿼리 로그 설정 및 인덱스 튜닝

<br>

## 로컬 실행 방법

```bash
# Backend
./gradlew bootRun

# Frontend
npm install
npm run dev
```

> 환경변수 설정 필요: 알라딘 API Key, Naver OAuth2 Client ID/Secret, Discord Webhook URL

<br>

## 프로젝트 배경

SI/공공 분야에서 3년간 백엔드 개발을 하면서 (네트워크 인프라 관리 시스템, 국토교통부 데이터 거래 플랫폼),  
"많은 사용자에게 실제로 가치를 주는 서비스를 만들어보고 싶다"는 목표로 시작한 프로젝트입니다.

실무에서 경험한 배치 처리, PostgreSQL, Spring Boot를 직접 설계 단계부터 적용해보면서, 단순 CRUD를 넘어 **설계 의도가 있는 코드**를 작성하는 데 집중했습니다.