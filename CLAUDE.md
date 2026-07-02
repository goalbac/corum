# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Corum 프로젝트

사단법인 단체 내부용 게시판 중심 웹사이트. 개인 포트폴리오 겸용.

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Spring Boot 3.3.0, Spring Security (JWT), Spring Data JPA, QueryDSL 5.1.0 |
| Frontend | Vue 3 + Vite, Vue Router 4, Pinia, Axios, Element Plus |
| DB | PostgreSQL 16 |
| 파일 스토리지 | MinIO (S3 호환, 로컬) → 운영 시 AWS S3 / Cloudflare R2 |
| 로컬 인프라 | Docker Compose (PostgreSQL + pgAdmin + MinIO) |
| 위지윅 에디터 | TipTap |
| 캘린더 | FullCalendar |

---

## 개발 환경

| 항목 | 내용 |
|------|------|
| OS | Windows + WSL Ubuntu |
| 컨테이너 | Docker Desktop |
| Backend IDE | IntelliJ IDEA 2024.1 |
| Frontend IDE | WebStorm 2024.1.1 |
| JDK | OpenJDK 21 (JAVA_HOME=C:\Program Files\Java\jdk-21.0.1) |
| Node.js | v22.22.3 |
| WSL alias | `idea .` / `webstorm .` |

---

## 빌드 및 실행 명령어

### Backend (PowerShell — D:\workspace\corum\backend)

```powershell
# 빌드 (QueryDSL Q클래스 포함)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.1"; .\gradlew build

# 컴파일만 (Q클래스 생성 포함)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.1"; .\gradlew compileJava

# 테스트 전체 실행
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.1"; .\gradlew test

# 특정 테스트 클래스 실행
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.1"; .\gradlew test --tests "com.corum.backend.SomeTest"

# 빌드 없이 bootRun (개발 시)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.1"; .\gradlew bootRun
```

> **주의:** JAVA_HOME 없이 실행하면 시스템 기본 JDK로 실행될 수 있음. 항상 명시적으로 지정.
> **참고:** `backend/src/test` 디렉토리 없음 — 테스트 코드 미작성 상태 (커버리지 0%). Frontend도 lint·test 스크립트 없음.

### Frontend (PowerShell — D:\workspace\corum\frontend)

```powershell
# 개발 서버 (localhost:5173, /api → localhost:8080 프록시)
npm run dev

# 프로덕션 빌드
npm run build

# 빌드 결과물 미리보기
npm run preview
```

> Frontend는 lint·test 스크립트 없음 (현재 설정 안 됨).

### 인프라 (Docker)

```bash
# 로컬 Docker 시작 (PostgreSQL :5432 / pgAdmin :5050 / MinIO :9000 API, :9001 Console)
cd ~/corum && docker compose up -d

# 로컬 Docker 종료
cd ~/corum && docker compose down

# 볼륨까지 삭제 (DB 초기화)
docker compose down -v
```

---

## 아키텍처 개요

### Backend 레이어 구조

```
Controller → Service → Repository (JPA / QueryDSL)
                ↓
            Domain (JPA 엔티티, BaseEntity 상속)
                ↓
            DTO (Request/Response 분리)
```

- **공통 응답:** 모든 API는 `ApiResponse<T>` 래퍼 반환 (`success`, `message`, `data` 필드)
- **예외 처리:** `BusinessException` 사용 (`notFound`, `forbidden`, `unauthorized` 정적 팩토리) → `GlobalExceptionHandler`가 일관된 에러 응답 생성
- **엔티티:** `BaseEntity` 상속으로 `createdAt`, `updatedAt` 자동 관리
- **동적 쿼리:** QueryDSL Q클래스는 `build/generated/querydsl`에 생성됨 (빌드 후 사용 가능)

### Security / JWT 흐름

1. `POST /api/auth/login` → `JwtProvider`가 accessToken 발급 (refresh token 설정값은 존재하나 실제 재발급 로직 미구현 — 만료 시 재로그인 필요)
2. 이후 요청: `JwtAuthFilter`(OncePerRequestFilter)가 `Authorization: Bearer <token>` 파싱 → `CustomUserDetailsService`로 사용자 조회(그룹 소속 기반 `ROLE_ADMIN`/`ROLE_SUPER_ADMIN` 부여 포함) → SecurityContext 설정
3. 로그아웃/강제 만료: `TokenSessionService`가 JWT 블랙리스트 관리 — **인메모리(ConcurrentHashMap)라 서버 재배포/재시작 시 유실됨** (다중 인스턴스 미대응)
4. 공개 API 경로는 `SecurityConfig`에서 `permitAll()` 명시
5. `/api/admin/**`는 `SecurityConfig`에서 `hasRole("ADMIN")` 전역 게이트, 그룹/메뉴/게시판 구조 CUD는 각 컨트롤러에 `@PreAuthorize`로 개별 적용 (2026-07-02, `45ee800`). `@EnableMethodSecurity` 활성화됨 — 새 관리 기능 추가 시 `@PreAuthorize` 누락하지 않도록 주의 (상세: [보안 상태](#보안-상태-2026-07-02-점검))

### Frontend 상태 관리

- **인증:** `stores/auth.js` — accessToken을 `localStorage`에 보관, 사용자 정보 캐시
- **메뉴:** `stores/menu.js` — 백엔드에서 받아온 메뉴 트리를 저장
- **알림:** `stores/notification.js`
- **사이트 설정:** `stores/site.js`
- **API 호출:** 반드시 `src/api/axios.js` 인스턴스 사용 (토큰 자동 첨부, 401 시 로그인 리다이렉트, 에러 메시지 자동 표시)

### 라우팅 규칙

- `meta: { guest: true }` — 비로그인 전용 (로그인 상태면 `/` 리다이렉트)
- `meta: { requiresAuth: true }` — 로그인 필수
- `meta: { requiresAdmin: true }` — 관리자 전용 (`/admin/**`)
- 동적 메뉴 경로: `/:pathMatch(.*)` → `MenuPage.vue`에서 백엔드 메뉴 트리 기반으로 페이지 유형 판별 후 렌더링

### Vite 프록시

개발 중 `/api/*` 요청은 자동으로 `http://localhost:8080`으로 프록시됨. 프론트에서 별도 baseURL 설정 불필요.

---

## DB 설계 원칙

- **FK 제약조건 없음** — 정합성은 JPA + 서비스 레이어에서 보장
- **CHECK 제약조건 없음** — 값 유형은 Java enum으로 관리
- **ENUM 타입 없음** — varchar + Java enum 조합
- 참조 컬럼에는 인덱스 필수
- `before_value` / `after_value`는 JSON 문자열로 저장
- `ddl-auto: none` — DDL은 `init/01_init.sql`로 직접 관리 (스키마 변경 시 이 파일 수정)

---

## 코딩 컨벤션

- API 응답은 항상 `ApiResponse<T>` 래퍼 사용
- 예외는 `BusinessException` 사용 (`notFound`, `forbidden`, `unauthorized` 정적 팩토리)
- 엔티티는 `BaseEntity` 상속 (`createdAt`, `updatedAt` 자동 관리)
- Lombok 적극 사용 (`@Getter`, `@Builder`, `@RequiredArgsConstructor`)
- REST API prefix: `/api`
- 프론트 API 호출은 `src/api/axios.js` 인스턴스 사용

---

## 로컬 개발 실행

```bash
# 1. Docker 컨테이너 시작
cd ~/corum && docker compose up -d

# 2. Spring Boot 실행 (IntelliJ에서 CorumApplication.java 실행)
# 확인: http://localhost:8080/api/health

# 3. Vue 개발 서버 실행
cd ~/corum/frontend && npm run dev
# 확인: http://localhost:5173
```

---

## 운영 서버

- **Docker Compose 파일:** `docker-compose.prod.yml`
- **프로젝트명:** `corum-prod` (`-p corum-prod` 옵션 필수)
- **환경변수 파일:** `.env.prod` (gitignore 처리, 서버에만 존재)
- **컨테이너 구성:** postgres / minio / backend / frontend(nginx)
- **포트:** 80 (nginx → frontend + `/api` proxy → backend:8080)

> **주의:** `--env-file .env.prod` 없이 실행하면 `JWT_SECRET`이 비어 `WeakKeyException`으로 backend가 시작되지 않음.

> **배포 규칙:** 운영 서버 배포는 사용자가 명시적으로 요청한 경우에만 실행한다.

```bash
# 운영 서버 시작 / 재배포
cd ~/corum
git pull
docker compose -p corum-prod -f docker-compose.prod.yml --env-file .env.prod up -d --build

# 운영 서버 종료
docker compose -p corum-prod -f docker-compose.prod.yml down

# 로그 확인
docker compose -p corum-prod -f docker-compose.prod.yml logs -f backend
```

---

## 레거시 게시판 이관 (진행 중)

- **위치:** `backend/src/main/java/com/corum/backend/migration/` (`BoardMigrationRunner`, `LegacyFtpClient`, `LegacyDataSourceConfig` 등)
- **대상:** 구 홈페이지(hanwoolin.com, MS SQL Server + FTP 첨부파일) → Corum(PostgreSQL + MinIO)로 게시판 데이터 1회성 이관
- **실행 방식:** `migration` Spring 프로파일에서만 동작 (`@Profile("migration")`), 운영(`prod`) 프로파일로 기동 시 관련 빈은 로드되지 않음
- **설정:** 레거시 DB/FTP 접속 정보는 `application-migration.yml`에 환경변수로 분리, git에 커밋되지 않음 (안전 확인됨)
- **이관 대상 게시판 목록:** `BoardMigrationRunner`의 `SPECS` 리스트에 하드코딩 (게시판별 레거시 테이블명 매핑)
- **진행 이력:** 최근 커밋들이 게시판을 순차적으로 추가 이관 중 (예: `b2e5b6e` 러브레터 외 10개 게시판 추가)
- **참고:** mssql-jdbc/commons-net 의존성이 별도 소스셋 분리 없이 메인 프로덕션 JAR에 함께 빌드됨 (런타임 활성화는 안 되지만 불필요한 공격 표면 — 이관 완료 후 정리 권장)

---

## 보안 상태 (2026-07-02 점검)

운영 배포 전 반드시 확인. Cloudflare를 앞단에 둘 예정이나, 아래는 전부 **애플리케이션 레벨 결함**이라 Cloudflare(WAF/DDoS 방어)로는 막을 수 없음.

**🔴 Critical — 배포 전 필수 수정**
1. ~~공개 GitHub 저장소(`goalbac/corum`) 히스토리에 실제 JWT_SECRET 값 노출~~ **[해결됨 2026-07-02]** JWT_SECRET 로테이션 + `git filter-branch`로 전체 히스토리(405커밋)에서 `.env.prod`/`.env.prod.local` 제거 후 `origin/main` force-push 완료
2. ~~Admin 컨트롤러 전체(13개)에 서버 사이드 권한 체크 없음~~ **[해결됨 2026-07-02, `45ee800`]** JWT 인증 시 그룹 소속(ADMIN/SUPER_ADMIN)을 `GrantedAuthority`로 부여, `SecurityConfig`에 `/api/admin/**` → `hasRole("ADMIN")` 전역 게이트 추가. 동일한 갭이 있던 그룹 CUD·회원 그룹 부여(SUPER_ADMIN), 메뉴 CUD·게시판 구조 CUD(ADMIN)에도 `@PreAuthorize` 적용. `AccessDeniedException`은 `ApiResponse` 형식 403으로 응답
3. ~~파일 다운로드가 `fileId`만으로 접근 가능한 IDOR~~ **[해결됨 2026-07-02, `a2dd282`]** `FileStorageService.canAccessFile()` 추가 — target_type=POST면 게시판 권한(READ/DOWNLOAD), MESSAGE면 발신·수신자 여부를 확인. `download`/`view`/`thumbnail`/`small-thumb` 4개 엔드포인트에 적용. 이 과정에서 `BoardService.hasPermission()`이 애초에 어디서도 호출되지 않아 게시판 read/write/comment/download 권한이 **전혀 강제되지 않고 있던 것**을 추가로 발견 — 그룹 미설정 게시판은 공개, 설정된 게시판은 그룹별 권한을 실제로 따르도록 로직을 재작성함. (단, `getPost`/댓글/게시글 목록 등 파일이 아닌 경로에서의 board READ 권한 강제는 이번 수정 범위 밖 — 별도 후속 조치 필요, 아래 11번 참고)
4. ~~파일 업로드 확장자 검증 로직 없음~~ **[해결됨 2026-07-02, `a2dd282`]** `FileStorageService.validateUpload()` 추가 — jsp/php/exe/svg/html 등은 항상 차단, 게시판별/전역 `file_allowed_extensions`·`file_max_size_mb` 실제 적용. 파일 서빙 응답에 `X-Content-Type-Options: nosniff` 추가
5. ~~TipTap 콘텐츠(게시글/안내페이지/팝업/약관)에 XSS sanitize 없음~~ **[해결됨 2026-07-02, `b1c6ecd`]** 백엔드에 Jsoup 기반 `HtmlSanitizer` 추가해 저장 시점에 정제(post/content_page/popup/banner/terms/footerHtml), 프론트엔드는 DOMPurify로 `v-html` 렌더링 지점 11곳에 방어선 추가. 대시보드 CUSTOM 위젯(extra_config JSON 내 HTML)은 백엔드 저장 시점 sanitize는 아직 미적용(JSON 필드 파싱 필요) — 프론트 DOMPurify로만 방어 중, ADMIN 전용 입력이라 낮은 우선순위로 남김

**🟠 High**
6. JWT 블랙리스트가 인메모리 — 재배포/재시작마다 강제로그아웃·계정잠금 상태 유실
7. ~~파일 `/view` 인라인 엔드포인트가 Content-Type을 그대로 반영~~ **[완화됨 2026-07-02]** 위험 확장자 업로드 차단(#4) + `nosniff` 헤더로 신규 업로드분은 방어됨. 기존에 이미 업로드된 위험 확장자 파일이 있다면 여전히 남아있을 수 있음 — 재점검 권장
8. 로그인/회원가입/문의하기/비밀번호 재설정에 IP 기반 rate limiting 전무
9. 로컬 `docker-compose.yml`(DB/MinIO 기본 비밀번호 + 포트 오픈)을 운영에 절대 재사용 금지 — 운영은 반드시 `docker-compose.prod.yml` + `.env.prod` 사용
10. `/api/inquiries` permitAll 범위에 GET(목록 조회)도 포함될 가능성 — 컨트롤러 매핑 재확인 필요
11. ~~`getPost`(게시글 상세)·게시글 목록·댓글 조회 등 파일이 아닌 일반 API 경로의 board READ 권한 미강제~~ **[해결됨 2026-07-02, `4eee876`]** `PostService.getPosts/getPost/getAdjacentPosts`, `CommentService.getComments`에 `boardService.hasPermission(READ)` 체크 추가. 권한 미설정 게시판은 기존과 동일하게 공개 유지. (대시보드 "최신글" 위젯의 `getLatestPosts`는 admin이 board를 직접 지정하는 경로라 이번 범위에서 제외)

**🟡 Medium**: 비밀번호 재설정/이메일 인증 토큰 재사용 가능(1회성 미보장) · 관리자 발급 임시 비밀번호 이메일 평문 전송 · `docker-compose.prod.yml`의 MinIO 자격증명 약한 fallback(`:-minioadmin123`) · CORS 허용 목록에 개발용 `trycloudflare.com` 잔존

**확인된 안전한 부분**: SQL Injection(파라미터 바인딩 전면 사용) · Path traversal(UUID 파일명) · CSRF(STATELESS+JWT라 해당 없음) · 이관 도구 자격증명 관리 · audit_logs 민감정보 미기록

---

## 전체 기능 명세

### 1. 메뉴 구조

- 상단 고정 메뉴 4개. 클릭 시 왼쪽 사이드바에 하위 전체 메뉴 표시
- 2depth까지 상단 노출, 사이드바는 전체 depth 표시
- 현재 위치(active) 강조 표시
- 새 글/변경 후 3일간 NEW 뱃지 표시

**메뉴 속성:**
- 메뉴명, 메뉴 설명 (해당 페이지 상단 표시)
- 메뉴 유형: PAGE / LINK (새창 여부 선택) / GROUP(폴더)
- URL 직접 지정 또는 미지정 시 자동 넘버링 (예: /12)
- 접근 권한: ALL(모든 사용자) / LOGIN(로그인 사용자) / GROUP(그룹 지정)
- 메뉴 숨김: 완전 숨김 / 권한 없을 때 숨김 / 권한 없을 때 잠금
- 관리자가 메뉴 추가·삭제·순서 변경 가능

### 2. 그룹(권한) 구조

```
최고관리자 (SUPER_ADMIN) — 시스템 고정, 모든 권한
운영 그룹 (type: ADMIN) — 관리자 패널 접근 가능
  └─ 하위 그룹 자유 생성 (예: 회원관리팀, 게시판관리팀, 콘텐츠팀)
일반 그룹 (type: NORMAL) — 일반 페이지 접근
  └─ 정회원, 준회원, 일반인 (가입 기본값)
```

- 한 사용자가 여러 그룹 소속 가능 (다대다)
- 가입 시 자동으로 '일반인' 그룹 배정
- SUPER_ADMIN이 그룹 추가·수정·삭제 (최상위 운영/일반 그룹은 삭제 불가)
- 운영 그룹 하위 그룹별로 관리자 패널 조회/편집 권한 분리
- 메뉴/게시판 접근 권한은 그룹 단위로 설정

### 3. 회원

**회원 필드:**
이름, 이메일, 아이디(로그인), 비밀번호, 성별, 연락처, 집주소, 생년월일,
자택전화, 하는 일, 일하는곳 전화, 뉴스레터 수신여부, 프로필 사진, 가입일,
관리자 메모(관리자만 — admin_member_memos 테이블)

**회원 기능:**
- 자유 가입 → 이메일 인증 → 기본 '일반인' 그룹 배정
- 로그인 실패 N회 시 계정 잠금 (임계값 관리자 설정, site_settings.login_fail_limit)
- 비밀번호 찾기 (이메일 재설정 링크 발송)
- 약관 동의: 최초 로그인/약관 변경 시 동의 화면, 동의 일시·IP 기록
- 동시 로그인 제한 옵션 (site_settings.allow_concurrent_login)
- 마이페이지: 내 정보 수정, 비밀번호 변경, 회원 탈퇴 (개인정보 보관 기간 설정)
- 관리자: 회원 명부 엑셀/PDF 출력, 강제 로그아웃 (JWT 블랙리스트)

### 4. 게시판

**게시판 유형 (board_type):**
- POST — 일반 게시판
- GALLERY — 이미지 중심 썸네일 그리드
- DOCUMENT — 파일 중심, 폴더/카테고리 구조, 버전 관리

**게시글 기능:**
- 제목, 글쓴이(writer_name 표시용 별도), 작성시간, 조회수, 좋아요, 본문(위지윅), 첨부파일, 출력
- 공지 고정 (is_notice, 게시판별 공지 수 제한 — boards.notice_count_limit)
- 검색 (제목/본문/글쓴이)
- 관리자가 게시판별 목록 표시 컬럼 선택 가능 (조회수·좋아요 숨김 등)
- 관리자는 관리자 패널에서 모든 필드 수정 가능 (updated_by로 추적)
- 본문은 일반 페이지에서도 수정 가능 (권한 보유자)

**댓글 (3뎁스):**
- parent_id 자기참조, depth 컬럼으로 뎁스 제한 (max 2, 0-based)
- 로그인 사용자만 작성
- 게시판별 댓글 권한 별도 설정 (board_group_permissions.can_comment)
- is_deleted=true 시 '삭제된 댓글입니다' 표시 (대댓글 있는 경우)

**파일 첨부:**
- S3 호환 스토리지 (로컬: MinIO, target_type='POST')
- 게시판별 허용 용량(file_max_size_mb)·확장자(file_allowed_extensions)·최대 개수(file_max_count) 설정
- 전역 기본값은 site_settings, 게시판별 null이면 전역값 사용
- 다운로드 횟수·이력 기록 (file_download_logs)

**게시판 권한 (board_group_permissions):**
- can_read, can_write, can_comment, can_download — 그룹별 독립 설정

### 5. 캘린더

- 월·주·일 뷰 (FullCalendar)
- 일정 유형/카테고리 색상 태그 (calendars.color)
- 반복 일정 (recurrence_type: NONE/DAILY/WEEKLY/MONTHLY, recurrence_rule JSON)
- 여러 캘린더 동시 운영 가능 (색상 구분)
- 그룹별 읽기/쓰기 권한 (calendar_group_permissions)
- 일정 등록·수정·삭제는 can_write 권한 필요

### 6. 대시보드

- 최신글 목록 (전체/게시판별, post_count 설정)
- 이미지 슬라이더
- 링크 모음
- 위젯 노출 순서·표시 여부 관리자 설정 (dashboard_widgets)
- extra_config JSON으로 위젯별 추가 설정 저장
- 회원 현황 통계, 접속 통계 (visit_stats)
- 대시보드는 로그인 후 기본 홈

### 7. 안내 페이지

- 관리자가 TipTap 위지윅 에디터로 직접 편집
- 비로그인 공개 여부 설정 (menus.access_type)
- 편집 이력 보관 (content_page_histories — 전체 content 스냅샷)
- 이전 버전 복원 가능

### 8. 문의하기

- 비로그인 사용자도 접수 가능 (member_id nullable)
- 로그인 시 이메일·연락처 자동 입력
- 접수 항목: 제목, 본문, 연락처, 이메일, IP(자동)
- status: RECEIVED(접수) / CHECKING(확인중) / DONE(처리완료)
- 관리자: 목록 조회, 문의별 메모(inquiry_memos), 처리 상태 변경

### 9. 쪽지

- 회원 간 쪽지 발송
- 받은·보낸 쪽지함, 읽음 여부 (is_read, read_at)
- 관리자 전체 공지 쪽지 (is_notice=true)
- 발신자/수신자 각각 독립 삭제 (is_deleted_by_sender, is_deleted_by_recipient)
- 둘 다 삭제해야 실제 데이터 정리
- SMTP 연동 시 수신 알림 외부 메일 발송 (사용자 선택)

### 10. 팝업 / 배너

**팝업:**
- content_type: IMAGE / HTML
- 노출 페이지: target_type=ALL(전체) 또는 MENU(특정 메뉴)
- 기간 설정 (start_at, end_at), 복수 팝업 동시 운영
- position: LEFT / RIGHT / CENTER, priority로 순서 제어
- 클릭 링크 이동 (link_url, link_new_window)
- 3일간 보지 않기 (쿠키)

**상단 공지 배너:**
- 중요 공지 띠 형태 상단 표시
- 기간 설정, 닫기 버튼
- 팝업과 별도 테이블 (banners)

### 11. 관리자 기능

**사이트 기본 설정 (site_settings):**
- 사이트명, 설명, 파비콘
- 점검 모드 (maintenance_mode, maintenance_message, maintenance_until)
- 로그인 실패 임계값 (login_fail_limit)
- 세션 타임아웃 (session_timeout_min)
- 동시 로그인 제한 (allow_concurrent_login)

**파일/스토리지:**
- 전역 파일 크기 제한 (file_max_size_mb)
- 전역 허용 확장자 (file_allowed_extensions)
- S3/MinIO 연결 설정

**SMTP 설정:**
- smtp_host, smtp_port, smtp_username, smtp_password_enc(암호화), smtp_use_tls
- 테스트 발송 기능
- 발송 이력 (smtp_send_logs: send_type, success, error_message)
- send_type: EMAIL_VERIFY / PASSWORD_RESET / INQUIRY_NOTIFY / MESSAGE_NOTIFY / NEWSLETTER

**약관 관리:**
- type: SERVICE(이용약관) / PRIVACY(개인정보처리방침)
- 버전 관리 (version), 활성화 여부 (is_active)
- require_reagree=true 시 변경 후 재동의 요청

**감사 로그 (audit_logs):**
- action_type: LOGIN / LOGOUT / CREATE / UPDATE / DELETE 등
- target_table, target_id, before_value(JSON), after_value(JSON)
- member_id, ip_address, created_at

**로그인 보안:**
- 실패 N회 계정 잠금 (is_locked, login_fail_count, locked_at)
- 관리자 강제 로그아웃 (JWT 블랙리스트)

### 12. 이메일 (SMTP)

- Google Workspace SMTP 연동 (spring-boot-starter-mail)
- 용도: 이메일 인증, 비밀번호 재설정, 문의 알림, 쪽지 알림, 뉴스레터(추후)
- 관리자 패널에서 SMTP 설정값 입력·테스트
- 발송 이력 보관

---

## 전체 테이블 목록 (39개)

### 회원/그룹/약관 (7개)
| 테이블 | 주요 컬럼 |
|--------|----------|
| members | id, username, email, password_hash, name, gender, phone, address, birth_date, home_phone, occupation, work_phone, newsletter_yn, profile_image_url, is_active, is_locked, login_fail_count, locked_at, joined_at, withdrawn_at |
| groups | id, parent_id, name, description, type, sort_order, is_system |
| member_groups | id, member_id, group_id, assigned_by, assigned_at |
| terms | id, type, version, content, is_active, require_reagree |
| member_terms_agreements | id, member_id, terms_id, agreed_at, agreed_ip |
| admin_member_memos | id, member_id, created_by, memo |
| member_login_logs | id, member_id, action, ip_address, user_agent, success |

### 메뉴/관리자/설정 (6개)
| 테이블 | 주요 컬럼 |
|--------|----------|
| menus | id, parent_id, name, description, menu_type, page_type, target_id, url, url_auto, new_window, sort_order, is_hidden, hide_if_no_permission, access_type, is_active |
| menu_group_permissions | id, menu_id, group_id |
| admin_menus | id, parent_id, name, url, icon, sort_order, is_active |
| admin_menu_group_permissions | id, admin_menu_id, group_id, can_view, can_edit |
| site_settings | site_name, site_description, favicon_url, maintenance_mode, maintenance_message, maintenance_until, login_fail_limit, session_timeout_min, allow_concurrent_login, file_max_size_mb, file_allowed_extensions, smtp_host, smtp_port, smtp_username, smtp_password_enc, smtp_use_tls |
| audit_logs | id, member_id, action_type, target_table, target_id, before_value, after_value, ip_address |

### 게시판/댓글/파일 (9개)
| 테이블 | 주요 컬럼 |
|--------|----------|
| boards | id, menu_id, name, board_type, use_comment, use_like, use_anonymous, use_notice, notice_count_limit, file_max_size_mb, file_allowed_extensions, file_max_count, is_active |
| board_group_permissions | id, board_id, group_id, can_read, can_write, can_comment, can_download |
| posts | id, board_id, member_id, title, content, writer_name, is_notice, is_hidden, view_count, like_count, client_ip, updated_by |
| post_likes | id, post_id, member_id |
| comments | id, post_id, parent_id, member_id, writer_name, content, depth, sort_order, is_deleted, client_ip |
| files | id, target_type, target_id, original_name, stored_name, storage_path, mime_type, file_size, download_count, uploaded_by |
| file_download_logs | id, file_id, member_id, ip_address |
| document_folders | id, board_id, parent_id, name, sort_order |
| document_versions | id, post_id, version_number, file_id, change_note, created_by |

### 쪽지/문의/캘린더 (7개)
| 테이블 | 주요 컬럼 |
|--------|----------|
| messages | id, sender_id, title, content, is_notice |
| message_recipients | id, message_id, recipient_id, is_read, read_at, is_deleted_by_sender, is_deleted_by_recipient |
| inquiries | id, member_id, title, content, contact_phone, contact_email, client_ip, status |
| inquiry_memos | id, inquiry_id, created_by, memo |
| calendars | id, name, color, description, is_active |
| calendar_events | id, calendar_id, title, description, location, start_at, end_at, is_all_day, recurrence_type, recurrence_rule, created_by, updated_by |
| calendar_group_permissions | id, calendar_id, group_id, can_read, can_write |

### 팝업/배너/페이지/통계 (10개)
| 테이블 | 주요 컬럼 |
|--------|----------|
| popups | id, title, content_type, content, image_url, link_url, link_new_window, position, priority, start_at, end_at, is_active |
| popup_target_pages | id, popup_id, target_type, target_menu_id |
| banners | id, title, content, link_url, link_new_window, start_at, end_at, is_active |
| content_pages | id, menu_id, title, content, created_by, updated_by |
| content_page_histories | id, content_page_id, content, created_by |
| dashboard_widgets | id, widget_type, title, target_board_id, post_count, sort_order, is_active, extra_config |
| visit_logs | id, member_id, ip_address, user_agent, request_uri, visit_date |
| visit_stats | id, stat_date, total_visits, unique_visits, login_visits |
| search_logs | id, member_id, keyword, search_type, result_count, ip_address |
| smtp_send_logs | id, member_id, to_email, subject, send_type, success, error_message |

---

## 현재 개발 진행 상태

- [x] Docker Compose 구성 (PostgreSQL + pgAdmin + MinIO)
- [x] DB 스키마 39개 테이블 + 기본 데이터 (init/01_init.sql)
- [x] Spring Boot 초기 세팅 (`/api/health` 확인)
- [x] Vue 3 + Vite 초기 세팅 (localhost:5173 확인)
- [x] 인증 구현 (JWT, 회원가입, 로그인, 이메일 인증, 비밀번호 재설정)
- [x] 그룹/권한 구현
- [x] 메뉴 구현
- [x] 게시판 구현 (POST/GALLERY/DOCUMENT)
- [x] 캘린더 구현
- [x] 쪽지 구현
- [x] 문의하기 구현
- [x] 팝업/배너 구현
- [x] 안내 페이지 구현
- [x] 대시보드 구현
- [x] 관리자 패널 구현
- [x] 통계/감사로그 구현 (`AuditLog`, `VisitStats`, `AdminStatsController`)
- [x] SMTP 연동 (`SmtpSendLog`, `application-prod.yml` mail 설정, `MailService`)
- [x] 레거시 게시판(hanwoolin.com) 이관 도구 구축, 순차 이관 진행 중
- [x] 보안 하드닝 — Critical 5건(JWT_SECRET 노출/Admin API 권한/파일 IDOR/업로드 확장자/XSS) 전부 해결. High/Medium 잔여 항목은 [보안 상태](#보안-상태-2026-07-02-점검) 참고 — 운영 배포 전 재확인 권장
- [ ] 백엔드/프론트엔드 테스트 코드 작성 (현재 0%)
- [ ] 운영 환경 배포
