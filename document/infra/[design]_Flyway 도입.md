# [design] Flyway 도입

2026년 10월 20일 · 로드맵 3주차 (10/24 → 10/21로 당김, 실제 시작 10/20) · 범위: Flyway 도입, `V1__baseline`, `V2__add_not_null_constraints`, `V3__drop_review_is_public`, `ddl-auto` → `validate`

> 설계 검토에 AI(Claude)를 활용했고, 선택·결정과 백엔드 구현은 직접 했습니다.

**상태: 구현·배포 완료 (2026-10-20, PR #21) — D1-a(운영 덤프 baseline), D2·D3 추천안 채택, D4는 V1 대조 결과로 범위 확장(아래 D4·5장), V2에 `headline` 추가**

- 관련 문서: `document/infra/[design]_운영 DB 마이그레이션 (팀 기능 1차 배포).md` (10/15 수동 SQL), `docs/decision/10월 로드맵.md` 3주차 "Flyway 도입"

---

# 1. 문제

- 운영 스키마가 `ddl-auto: update` + 10/15 수동 SQL로 만들어져 **어떤 변경이 언제 들어갔는지 기록이 없다.** 1차 배포 때 UNIQUE 누락·NOT NULL 전환을 손으로 처리했다.
- `update`는 컬럼 추가만 하고 제약(NOT NULL, UNIQUE) 변경·삭제는 하지 않는다 → 엔티티와 운영 스키마가 조용히 어긋난다.
- 10/26 실사용 시작 후 스키마 변경(모임원 제거, 엑셀 산출물, 독후감 현황)이 이어진다 → **그 전에** 변경을 파일로 남기고 리뷰할 수 있게 한다.

목표: 이후 모든 스키마 변경은 `src/main/resources/db/migration/V*.sql`로만 하고, Hibernate는 `validate`(검사만).

---

# 2. 선택지와 추천

## D1. `V1__baseline.sql`을 무엇으로 만드나

| 안 | 방식 | 장점 | 단점 |
| --- | --- | --- | --- |
| **D1-a (추천)** | 운영 DB에서 `pg_dump --schema-only`로 추출 후 정리 | **운영 실제 상태와 일치** — baseline의 정의가 "지금 운영 그대로" | 덤프 정리 필요 (`SET`·owner·grant 제거) |
| D1-b | Hibernate가 생성하는 DDL(엔티티 기준) | 깔끔함 | 운영과 다를 수 있음 (`update` 누적, 10/15 수동 SQL, 남겨 둔 `is_public`) → 새로 만든 DB와 운영이 달라짐 |
| D1-c | 손으로 작성 | — | 누락 위험 |

- 운영에는 `review.is_public`(NOT NULL 해제, 미사용)이 남아 있다. D1-a면 V1에도 들어간다 → 삭제 여부는 D4.
- `pg_dump` 클라이언트 버전은 운영 PostgreSQL 이상이어야 한다 (`SELECT version();`으로 먼저 확인).

## D2. 이미 테이블이 있는 DB(운영·로컬)를 어떻게 처리하나

공통: `spring.flyway.baseline-on-migrate: true`, `baseline-version: 1`
→ 테이블은 있는데 `flyway_schema_history`가 없는 DB는 **V1을 실행하지 않고 "적용됨"으로 표시**한 뒤 V2부터 실행. 빈 DB(테스트 컨테이너)는 V1부터 실행.

| 대상 | 경로 | 의미 |
| --- | --- | --- |
| 운영 | baseline(V1 표시) → V2 실행 | 실제 배포 경로 |
| **로컬 (추천: 운영과 같이 baseline)** | baseline → V2 실행 | **운영 배포 리허설** — 같은 경로를 먼저 밟아 봄. 로컬 테스트 데이터도 유지 |
| 테스트 (Testcontainers) | V1 → V2 실행 | **빈 DB 경로 검증** — 새 서버를 만들 때 이 파일들만으로 스키마가 재현되는지 |

- 대안: 로컬 DB를 지우고 V1부터 → 테스트가 이미 같은 경로를 검증하므로 리허설 가치가 더 큼

## D3. `ddl-auto`

| 프로파일 | 지금 | 변경 | 이유 |
| --- | --- | --- | --- |
| prod | `update` | **`validate`** | 스키마 변경은 Flyway만. 엔티티와 테이블·컬럼·타입이 안 맞으면 기동 실패 → red-green 배포에서 새 버전이 안 뜨고 기존 버전 유지 |
| local | `update` | **`validate` (추천)** | 로컬이 `update`면 엔티티만 고쳐도 로컬에선 돌아가고 **운영에서 처음 깨짐**. 로컬에서 먼저 깨지게 |
| 테스트 | local 프로파일을 따라감 (`application.yml`의 `active: local`) | 자동으로 `validate` | `@DataJpaTest`에도 Flyway가 돌아서 **V1→V2 결과가 엔티티와 맞는지를 기존 Repository 테스트가 함께 검증** |

- `validate`가 검사하지 않는 것: NOT NULL·UNIQUE·FK·CHECK (10/15 문서 1장과 같음) → 제약은 마이그레이션 SQL 리뷰로 챙김

## D4. 이번에 넣을 마이그레이션 범위

| 안 | 내용 | 비고 |
| --- | --- | --- |
| ~~D4-a~~ (V1 대조 후 확장) | V2 `review.content` NOT NULL + **V3 `review.is_public` 삭제** | `is_public`은 "안정화 후 삭제"로 남겨 둔 것(10/15). 운영 버전(`d30cfb1`)부터 이미 안 씀 → red-green 중 기존 버전과도 충돌 없음. 1차 배포 후 5일간 롤백 필요 없었음 |
| D4-b | V2만 | `is_public`은 나중에 — 대신 V1에 쓰지 않는 컬럼이 계속 남음 |
| **D4-c (채택)** | **V2 제약 보강**: `review.content`·`review.headline`·`review.visibility`·`team_member.team_id` NOT NULL + `review.visibility` 기본값 제거 / **V3 컬럼 삭제**: `review.is_public` | V1 대조(5장)에서 `visibility`·`team_id`가 nullable로 발견됨. `headline`은 V2 작성 중 개발자가 추가로 발견. NOT NULL 4개는 "엔티티 규칙을 DB에도 건다"는 같은 이유라 한 버전, 컬럼 삭제는 되돌릴 수 없는 다른 성격이라 별도 버전 — **버전 하나 = 같은 이유로 바뀌고 같이 되돌릴 변경 하나** (커밋 단위와 같은 기준) |

- 요청 DTO `@Valid` 일괄 적용은 **이번 범위 아님** — 예외 처리 리팩토링과 함께 (로드맵 위험 요소: 밀리면 4주차)

---

# 3. 그 외 (선택지 없음)

| 항목 | 내용 |
| --- | --- |
| 의존성 | `org.flywaydb:flyway-core` + **`org.flywaydb:flyway-database-postgresql`** (Flyway 10부터 PostgreSQL 지원이 별도 모듈 — 빠뜨리면 기동 시 "Unsupported Database" 오류). 버전은 Spring Boot 관리값 사용 |
| 위치 | `src/main/resources/db/migration/` (기본값) |
| 설정 위치 | `baseline-on-migrate`, `baseline-version`은 프로파일 공통이라 `application.yml` |
| V1 정리 | `pg_dump --schema-only --no-owner --no-privileges` 후 `\restrict`·`\unrestrict`(psql 메타 명령 — Flyway 실행 시 문법 오류), `SET ...`, `SELECT pg_catalog.set_config('search_path', '', false);` 줄 제거. `CREATE TABLE`·`SEQUENCE`·`ALTER ... ADD CONSTRAINT`·`CREATE INDEX`만 남김 |
| V2 사전 확인 | 운영에서 아래가 모두 0 — `SELECT count(*) FROM review WHERE content IS NULL;` (빈 문자열은 10/16 확인), `SELECT count(*) FROM review WHERE headline IS NULL;`, `SELECT count(*) FROM review WHERE visibility IS NULL;`, `SELECT count(*) FROM team_member WHERE team_id IS NULL;`. 0이 아니면 V2가 실패해 앱 기동 실패(기존 버전 유지) |
| V2 엔티티 | `Review.content` `@Column(nullable = false, columnDefinition = "TEXT")`, `Review.headline` `@Column(nullable = false)`, `TeamMember.team` `@JoinColumn(name = "team_id", nullable = false)` — `validate`는 안 보지만 엔티티를 스키마와 맞춰 둠 (`visibility`는 이미 `nullable = false`) |
| 실패 시 | PostgreSQL은 DDL도 트랜잭션 → 마이그레이션이 실패하면 그 버전 전체 롤백, 앱 기동 실패 → red-green이 기존 버전 유지 |
| 되돌리기 | Flyway 무료판은 undo 없음 → 되돌릴 땐 **새 버전**으로 (예: `V4__review_content_nullable.sql`). 적용된 V 파일은 수정 금지 (체크섬 불일치로 기동 실패) |
| red-green 호환 | 새 버전이 8011에서 기동하며 마이그레이션 → 그동안 기존 버전(8010)이 바뀐 스키마로 동작. V2(NOT NULL)·V3(미사용 컬럼 삭제) 모두 기존 버전과 호환 |

---

# 4. 작업 순서 (개발자)

1. 운영 `SELECT version();` → `pg_dump --schema-only --no-owner --no-privileges`로 V1 추출·정리
2. `build.gradle` 의존성 2개, `application.yml`에 Flyway 설정, local·prod `ddl-auto: validate`
3. `V2__add_not_null_constraints.sql`, `V3__drop_review_is_public.sql`, 엔티티 `nullable = false` 2곳
4. **로컬 기동** → 운영 경로 리허설: `SELECT version, description, type, success FROM flyway_schema_history;`에 `1 BASELINE`, `2 SQL`, `3 SQL` 확인
5. **전체 테스트** (`./gradlew test`) → 빈 DB 경로(V1부터) + `validate` 통과 확인
6. 운영 배포 직전: V2 사전 확인 쿼리

커밋 제안: `FEAT(BE): [인프라] Flyway 도입 및 운영 스키마 baseline` / `FIX(BE): [DB] review·team_member NOT NULL 제약 보강 및 visibility 기본값 제거` / `REFACTOR(BE): [리뷰] 미사용 review.is_public 컬럼 삭제`

---

# 5. V1 덤프 대조 결과 (2026-10-20)

운영 PostgreSQL **16.15** 덤프 기준. 테이블 7 · IDENTITY 5 · PK 7 · UNIQUE 4 · FK 3 · CHECK 3.

**`validate` 대상(테이블·컬럼·타입): 엔티티와 전부 일치** — `Team`·`TeamMember`·`OAUTH2_USER`의 `@Table` 이름도 Spring 이름 규칙으로 `team`·`team_member`·`oauth2_user`. `person`은 로그인 시 저장 중이라 유지.

**`validate`가 보지 않는 부분에서 발견**

| # | 컬럼 | 운영 실제 | 엔티티·문서 | 조치 |
| --- | --- | --- | --- | --- |
| 1 | `review.visibility` | nullable, **`DEFAULT 'PUBLIC'`** | 엔티티 `nullable = false`, 10/15 설계·1차 PR 문서는 `DEFAULT 'PRIVATE'` + NOT NULL | V2: NOT NULL + **기본값 제거**. 기본값 `PUBLIC`은 컬럼을 빠뜨린 INSERT(손 SQL·이관·마이그레이션)가 **조용히 전체공개**가 됨 → 빠뜨리면 실패하는 쪽이 안전. Hibernate는 INSERT에 모든 컬럼을 명시해 앱 경로로는 발동 안 함. 10/15 실제 실행 SQL이 문서와 달랐던 것으로 추정 |
| 2 | `review.content` | nullable | 계획대로 | V2: NOT NULL |
| 2-1 | `review.headline` | nullable | 엔티티에 `nullable = false` 없음, 규칙은 "필수 입력" | V2: NOT NULL + 엔티티 수정 (V2 작성 중 추가 발견 — 첫 대조에서 누락) |
| 3 | `team_member.team_id` | nullable | 엔티티 `@JoinColumn`에도 `nullable = false` 없음 | V2: NOT NULL + 엔티티 수정 |
| - | `review.is_public` | nullable, 미사용 | 엔티티에 없음 (`validate`는 문제 삼지 않음) | V3: 삭제 |

- 테스트 컨테이너 `postgres:13.10` ≠ 운영 16.15 → **지원 이후**로 미룸 (로드맵 "지원 이후로 미룬 것"). 현재 V1 문법은 13에서도 실행 가능
