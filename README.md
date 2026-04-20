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
- 책 상세 정보 조회 및 저장

### 📝 리뷰 (책 에세이)
- 리뷰 작성·수정·삭제 (본인 리뷰만 수정/삭제 가능)
- `headline`: 한 줄 요약 (필수 입력, 항상 공개)
- `isPublic`: 전체 공개 여부 선택 (기본값: 비공개)
- 비공개 리뷰도 `headline`은 랭킹 페이지에 노출

### 🏆 책별 랭킹
- 매일 자정 배치로 책별 평균 평점 랭킹 산출
- 리뷰 3개 미만 책은 랭킹에서 제외
- 과거 랭킹 이력 보존 (`base_date` 기반)

### 🌐 비로그인 공개 조회
- 로그인 없이 `GET /api/review/public?isbn13=` 로 책별 공개 리뷰 열람
- 프라이버시 중심 설계: 사용자가 공개를 선택한 리뷰만 노출

<br>

## 기술 스택

| 영역 | 기술 |
|------|------|
| Backend | Spring Boot 3.x, Spring Security, Spring Data JPA |
| Language | Java 17 |
| Database | PostgreSQL |
| Frontend | Vue3, PWA |
| Infrastructure | OCI (Oracle Cloud), Nginx, Docker |
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
             └──▶ Naver OAuth2 (소셜 로그인)
```

- **서버 2대 구성** (OCI Free Tier): WAS 서버 + DB 서버 분리
- Nginx는 Vue3 빌드 파일만 서빙 (리버스 프록시 아님)
- 브라우저가 WAS:8010으로 직접 API 요청 → CORS 설정 필요

<br>

## 기술적 의사결정

### 1. 랭킹 집계: 애플리케이션 레이어 vs DB 윈도우 함수

**문제:** 매일 책별 랭킹을 산출해야 하는데, 어떻게 구현할 것인가?

**선택지:**
- 애플리케이션에서 리뷰를 전부 불러와 정렬 → 데이터가 많아질수록 메모리 부담
- DB `RANK() OVER` 윈도우 함수로 DB 레벨에서 산출 → 단일 쿼리로 처리 가능

**결정:** PostgreSQL `RANK() OVER (PARTITION BY isbn13 ORDER BY avg_rating DESC)` 사용

**이유:**
- 실무에서 PostgreSQL을 사용했고, 윈도우 함수가 이런 집계에 적합하다는 걸 알고 있었음
- 애플리케이션 레이어에서 처리하는 것보다 DB가 잘하는 일을 DB에 맡기는 것이 맞다고 판단
- `HAVING COUNT(*) >= 3` 조건으로 데이터 신뢰성 확보 (리뷰가 너무 적은 책은 제외)

```sql
SELECT
    isbn13,
    avg_rating,
    review_count,
    RANK() OVER (ORDER BY avg_rating DESC) AS ranking
FROM (
    SELECT isbn13, AVG(rating) AS avg_rating, COUNT(*) AS review_count
    FROM review
    GROUP BY isbn13
    HAVING COUNT(*) >= 3
) sub
```

---

### 2. 리뷰 공개 설계: headline과 isPublic 분리

**문제:** 리뷰를 공개/비공개로 관리하면서도, 랭킹 페이지에는 뭔가를 보여줘야 한다.

**고민:** `isPublic = false`이면 랭킹 페이지에 아무것도 안 보여줘야 하는가?

**결정:** `headline`(한 줄 요약)과 `content`(본문)을 분리

| 필드 | 특성 | 비로그인 노출 |
|------|------|--------------|
| `headline` | 필수 입력, 항상 공개 | ✅ 항상 |
| `content` | 선택적 공개 (`isPublic`) | `isPublic=true` 일 때만 |

**이유:**
- 사용자가 리뷰 본문은 비공개로 두면서도, 랭킹 페이지에 존재감을 남길 수 있음
- "완전 공개 아니면 완전 비공개"가 아닌 중간 상태를 제공해 사용자 선택권 확보

---

### 3. PostgreSQL 선택

**이유:**
- 실무(모비젠 NI팀)에서 PostgreSQL을 사용했고, 심화 학습 목적으로 동일 DB 선택
- MySQL 대비 윈도우 함수, JSON 타입 등 고급 기능이 풍부함
- 랭킹 집계처럼 복잡한 쿼리를 직접 짜볼 수 있는 환경 확보

<br>

## 향후 계획

- [ ] 테스트 코드 작성 (단위 테스트, 통합 테스트)
- [ ] 알라딘 API 응답 캐싱 (반복 호출 최소화)
- [ ] Swagger/OpenAPI 문서화
- [ ] 마이 페이지 완성 (독서 통계, 프로필 수정)
- [ ] 에러 핸들링 고도화 (전역 예외 처리, 사용자 친화적 메시지)

<br>

## 로컬 실행 방법

```bash
# Backend
./gradlew bootRun

# Frontend
npm install
npm run dev
```

> 환경변수 설정 필요: 알라딘 API Key, Naver OAuth2 Client ID/Secret

<br>

## 프로젝트 배경

SI/공공 분야에서 3년간 백엔드 개발을 하면서 (네트워크 인프라 관리 시스템, 국토교통부 데이터 거래 플랫폼),
"많은 사용자에게 실제로 가치를 주는 서비스를 만들어보고 싶다"는 목표로 시작한 프로젝트입니다.

실무에서 경험한 배치 처리, PostgreSQL, Spring Boot를 직접 설계 단계부터 적용해보면서, 단순 CRUD를 넘어 **설계 의도가 있는 코드**를 작성하는 데 집중했습니다.