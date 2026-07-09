# 코드 개선 TODO

2026-07-09 소스코드 분석에서 발견된 항목. 급한 작업이 아니라 **순차적으로 검토 후 처리**하기 위해 별도 기재.
완료 시 체크박스 처리하고 커밋 해시 남길 것.

## Backend

- [ ] **[성능]** `StatService.getDailyStats` — 대시보드 일별 통계가 하루당 4개 카운트 쿼리(회원/게시글/댓글/문의)를 개별 호출. 30일 조회 시 ~120쿼리+ 발생. `GROUP BY date_trunc` 등으로 배치 쿼리화 필요.
  `backend/src/main/java/com/corum/backend/service/stats/StatService.java:34-98`
- [ ] **[성능]** `BoardNotificationSubscriptionService.getMySubscriptions` — 구독 게시판 목록을 `findById` 루프로 1건씩 조회 (N+1). `findAllById`로 배치 조회 변경.
  `backend/src/main/java/com/corum/backend/service/notification/BoardNotificationSubscriptionService.java:49-62`
- [ ] **[구조]** `AdminMemberController` 엑셀/PDF 내보내기 — 서비스 계층 없이 컨트롤러에서 직접 `memberRepository.findAll()` 호출(페이지네이션 없음, 전체 회원 로드), PDF 작성기가 컨트롤러 내부 클래스로 구현됨. 서비스 계층으로 이동 검토. ("엑셀" 내보내기가 실제로는 CSV — 네이밍도 확인)
  `backend/src/main/java/com/corum/backend/controller/admin/AdminMemberController.java:288-420`
- [ ] **[중복]** `AuthService` — `MemberResponse` 조립 로직(isAdmin 조회 + groupIds + 게시글/댓글 수)이 `updateProfile`/`getMe`/`uploadProfileImage`에 동일하게 3회 반복. `buildMemberResponse()` 헬퍼로 추출.
  `backend/src/main/java/com/corum/backend/service/auth/AuthService.java:213-283`
- [ ] **[사소]** `WebPushService.generateVapidKeys` — 다른 서비스와 달리 `BusinessException` 대신 raw `RuntimeException` 사용, 구체적 에러 메시지가 일반 "서버 오류가 발생했습니다"로 뭉개짐.
  `backend/src/main/java/com/corum/backend/service/notification/WebPushService.java:62`
- [ ] **[사소]** `MenuService.updateSortOrder`/`.reorder` — 루프 내 `findById` 반복 (관리자 전용·소규모라 우선순위 낮음).
  `backend/src/main/java/com/corum/backend/service/menu/MenuService.java:253-274`

## Frontend

- [ ] **[코드스멜]** 관리자 페이지들의 `onMounted` 비동기 fan-out에 에러 처리 없음 — 개별 fetch 함수 내부 try/catch 없이 여러 호출을 동시 발사, 실패 시 axios 인터셉터의 전역 토스트 외엔 로딩/에러 상태가 없음. `AdminGroupsPage.vue`, `AdminCalendarPage.vue` 등에도 동일 패턴 반복 — 개별 수정보다 공통 처리 방식(로딩/에러 상태 wrapper) 도입 검토.
  `frontend/src/pages/admin/dashboard/AdminDashboardWidgetsPage.vue:803`
- [ ] **[구조]** 관리자 목록 페이지(약 11개: `AdminMembersPage.vue`, `AdminBoardsPage.vue`, `AdminCommentsPage.vue`, `AdminInquiriesPage.vue`, `AdminPostsPage.vue`, `AdminStatsPage.vue` 등)에 페이지네이션(`page/size/total` + `el-pagination`) 로직이 동일하게 복붙되어 있음. `composables/usePagedFetch.js` 같은 공용 컴포저블로 추출 검토 (현재 `composables/` 디렉토리 자체가 없음).
- [ ] **[참고, 조치 불필요]** 대형 단일 파일 컴포넌트: `AdminDashboardWidgetsPage.vue`(1044줄), `AdminMenusPage.vue`(1001줄), `AdminContentPagesPage.vue`(735줄), `AdminSettingsPage.vue`(637줄), `AdminBoardsPage.vue`(618줄) — 다음에 손댈 일이 생기면 서브 컴포넌트로 분리 고려.

## 기존 문서에 이미 남아있는 항목 (참고용, 여기서 중복 관리 안 함)

- 보안 하드닝 잔여 항목(운영 가이드 성격 포함) → `CLAUDE.md` "보안 상태" 섹션 참고
- 게시판 이관 남은 작업 → `docs/migration/게시판_이관_가이드.md` "6. 남은 작업 / TODO" 참고
- 백엔드/프론트엔드 테스트 코드 부재(0%) → `CLAUDE.md` 진행 상태 참고
