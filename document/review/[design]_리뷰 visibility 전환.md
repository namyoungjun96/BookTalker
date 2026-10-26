# 리뷰 visibility 전환

- 작성일: 2026-10-03 (분리: 2026-10-05)
- 브랜치: `feat/team-domain` (FEAT `1646acd`, 2026-10-05)
- 상태: **완료** — 구현 2026-10-05, 운영 DB 마이그레이션 2026-10-15, `is_public` 컬럼 삭제 2026-10-20(Flyway V3). 아래 4~5장은 작업 당시 기록
- 작업 원칙: 백엔드 코드는 직접 작성, 프론트엔드 구현과 설계 검토는 AI(Claude) 활용
- 분리 이력: 기존 `[design]_리뷰 visibility 전환 및 예외 처리 방향 논의`에서 분리.
  예외 처리(A~E)는 `[design]_리뷰 예외 처리 방향 논의`로 이관

---

## 1. 배경

review 테이블의 `is_public`(Boolean)을 `visibility`(enum) 컬럼으로 대체한다.

| 값 | 화면 표기 | 의미 |
|----|-----------|------|
| `PUBLIC` | 전체공개 | 비로그인 포함 전체 공개 |
| `GROUP` | 모임공개 | 속한 모임원에게만 공개 |
| `PRIVATE` | 비공개 | 본인만 열람 |

- 기존: 2회독 이상은 `isPublic` 강제 false, 변경 불가
- 변경: **모든 회독에서 공개 범위 선택 가능**

### 분리 이유
- visibility 전환은 **도메인 정책 변경**, 예외 처리는 **API 응답 규약 변경**으로 성격이 다름
- 한 PR에 섞이면 리뷰 범위가 커지고, 변경 근거 설명이 흐려짐

---

## 2. 범위 경계 — **결정: ① Bean Validation을 2차로 이관**

`@Valid` + `@NotNull`을 이 PR에 남기면 예외 처리가 함께 따라온다.

| 안 | 내용 | 결과 |
|---|---|---|
| **① (채택)** | `@Valid`/`@NotNull` 제거, null은 기본값 처리 | 예외 처리 코드 무변경. Bean Validation은 B(검증 위치) 논의와 함께 2차로 |
| ② | `@Valid` 유지 + `@ExceptionHandler(MethodArgumentNotValidException)` 하나 추가, 기존 `{message}` 형식 | 동작은 하나 2차에서 다시 제거됨 |
| ③ | 현재 상태(상속 + 중간 단계 override) 그대로 | 응답 형식 혼재(`detail` / `message`), 주석 코드 잔존 |

**①을 고르지 않고 핸들러만 되돌릴 경우의 문제:** visibility 누락 요청 →
`MethodArgumentNotValidException` → `Exception` 핸들러 → **500 + Discord ERROR 알림**

### null 처리 규칙 (기존 `isPublic` 처리 방식과 동일한 구조)
| 경로 | visibility = null 일 때 |
|------|------------------------|
| `POST /review`, `POST /review/next-reading` (`ReviewRequest.to()`) | `PRIVATE` (프론트 `DEFAULT_VISIBILITY`와 일치, 안전한 쪽 기본값) |
| `PUT /review` (`ReviewService.updateReview`) | 기존 값 유지 |

- 정상 클라이언트(프론트)는 항상 `visibility`를 전송하므로 null 경로는 방어용

---

## 3. 완료된 작업

### 프론트엔드 (AI 구현)
- `src/constants/reviewVisibility.js` 신규: 선택지(값/표기/스타일 클래스), 기본값 `PRIVATE`
- `ReviewCreateView.vue`: 체크박스 토글 → 3단 선택, 2회독+ 잠금 제거, `visibility` 전송
- `ReviewDetailView.vue`: 조회 배지 `visibility` 기반 표시, 수정 시 3단 선택, 2회독+ 잠금 제거
- 표기는 팀 목업(`TeamDetailView.vue`)에 맞춰 **전체공개 / 모임공개 / 비공개**, API 값은 영문 enum

### 백엔드 (개발자 구현)
- `ReviewVisibilityEnum` (`PUBLIC`, `GROUP`, `PRIVATE`)
- `Review.visibility`: `@Enumerated(STRING)`, `nullable = false`
- `ReviewRequest`, `ReviewDetailResponse`: `isPublic` → `visibility`
- `ReviewService`: 2회독+ 강제 false 로직 제거 (insertNextReading, updateReview)
- `findPublicReviews`: `visibility = PUBLIC` 조건만 사용 (2회독+ 공개 리뷰도 노출)
- 테스트: isPublic 강제 false 테스트 2개 삭제, 나머지 enum으로 교체
- `Review.java`, `ReviewRepository` 주석 갱신 완료

### 결정 내역
| # | 내용 | 결정 |
|---|------|------|
| 1 | 2회독+ 공개 리뷰 미노출 | 반영 완료 (`readingCount = 1` 조건 제거) |
| 2 | 랭킹 집계 대상 | **1회독 한정 유지** — 원치 않는 평점 어뷰징 방지, 신뢰성 |
| 3 | visibility null 처리 | **기본값 처리(①)**. Bean Validation은 2차 문서 B로 이관 |
| 4 | GROUP 리뷰 팀원 열람 권한 | 미개발 (모임 피드 API 개발 시) |
| 5 | 팀 목업 표기 불일치 | 목업 표기가 정답 → 프론트 표기 수정 완료 |
| 6 | 정책 변경 후 남은 주석·CLAUDE.md | visibility 관련분은 **이 PR에 포함**, 예외 처리 표는 2차 |
| 7 | `ddl-auto: validate` | 로컬 테스트용 의도된 변경 → **커밋 제외** |

---

## 4. 정책 수정 — **논의 예정**

> 개발자 요청으로 정책 수정 사항을 별도 논의 후 이 절에 정리한다.

- [ ] 수정 대상 정책 확정
- [ ] 영향 범위(프론트/백엔드/테스트/CLAUDE.md) 반영

---

## 5. 남은 작업

### 백엔드 (개발자 구현)
- [ ] `ReviewController` 3곳 `@Valid` 제거
- [ ] `ReviewRequest.visibility`의 `@NotNull` 제거
- [ ] `ReviewRequest.to()`: null이면 `PRIVATE`
- [ ] `ReviewService.updateReview`: null이면 기존 값 유지
- [ ] `GlobalExceptionHandler`: 이번 브랜치 기준 변경 전 상태로 복원
  (현재 작업분은 2차 브랜치로 이관 — stash 등)
- [ ] (권장) 새 정책 테스트 추가
  - 2회독+ 리뷰도 `PUBLIC`으로 저장됨
  - visibility null 요청 시 insert → `PRIVATE`, update → 기존 값 유지

### 마이그레이션 (배포 차단 요인)
- prod는 `ddl-auto: update` → 데이터가 있는 `review` 테이블에 Hibernate가
  `visibility ... NOT NULL` 컬럼 추가 시 PostgreSQL에서 실패
- 운영 반영 전 수동 실행 스크립트 필요

```sql
ALTER TABLE review ADD COLUMN visibility VARCHAR(255);
UPDATE review SET visibility = CASE WHEN is_public THEN 'PUBLIC' ELSE 'PRIVATE' END;
ALTER TABLE review ALTER COLUMN visibility SET NOT NULL;
-- is_public 컬럼 삭제는 배포 안정화 확인 후 별도 진행 (롤백 대비)
```
- `is_public`이 null인 행은 `ELSE` 분기로 `PRIVATE` 처리됨

### 문서 (AI)
- [ ] `CLAUDE.md`
  - 핵심 비즈니스 규칙: 2회독+ `isPublic` 강제 false → visibility 정책으로 교체
  - 공개/비공개 설계: `content` 노출 조건 `isPublic=true` → `visibility=PUBLIC`
  - DB 스키마: `is_public` → `visibility`
  - API 목록: `POST /review/next-reading` 설명 갱신
  - 개발 규칙: **백엔드 코드는 개발자가 직접 작성, AI는 조언·검토만** 추가

---

## 6. 커밋 범위

### 포함
- 백엔드: `ReviewVisibilityEnum`, `Review`, `ReviewRequest`, `ReviewDetailResponse`,
  `ReviewService`, `ReviewRepository`, `ReviewController`(최종적으로 변경 없을 수 있음), 테스트 3종
- 프론트: `constants/reviewVisibility.js`, `ReviewCreateView.vue`, `ReviewDetailView.vue`
- 마이그레이션 SQL, `CLAUDE.md`

### 제외
- `build.gradle` (Testcontainers 토글, devtools 제거 — 제거 의도 확인 필요)
- `application-local.yml` (`ddl-auto: validate`)
- `GlobalExceptionHandler.java` (2차로 이관)
- `loadtest/`, `logs/`

---

## 7. 이후 과제 (이번 PR 범위 밖)
- GROUP 리뷰 팀원 열람 권한 (`getReviewDetail` 본인 전용) — 모임 피드 API 개발 시
- `is_public` 컬럼 삭제 — 배포 안정화 후
