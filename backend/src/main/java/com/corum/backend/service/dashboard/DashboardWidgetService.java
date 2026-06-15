package com.corum.backend.service.dashboard;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.board.Board;
import com.corum.backend.domain.board.BoardRepository;
import com.corum.backend.domain.calendar.CalendarEntity;
import com.corum.backend.domain.calendar.CalendarEventRepository;
import com.corum.backend.domain.calendar.CalendarRepository;
import com.corum.backend.service.calendar.CalendarService;
import com.corum.backend.domain.dashboard.DashboardWidget;
import com.corum.backend.domain.dashboard.DashboardWidgetRepository;
import com.corum.backend.domain.dashboard.VisitStats;
import com.corum.backend.domain.dashboard.VisitStatsRepository;
import com.corum.backend.domain.file.UploadFile;
import com.corum.backend.domain.file.UploadFileRepository;
import com.corum.backend.domain.inquiry.InquiryRepository;
import com.corum.backend.domain.member.MemberRepository;
import com.corum.backend.domain.menu.Menu;
import com.corum.backend.domain.menu.MenuRepository;
import com.corum.backend.domain.post.Post;
import com.corum.backend.domain.post.PostRepository;
import com.corum.backend.dto.dashboard.DashboardCalendarEventResponse;
import com.corum.backend.dto.dashboard.DashboardInfoResponse;
import com.corum.backend.dto.dashboard.DashboardPostResponse;
import com.corum.backend.dto.dashboard.DashboardStatsResponse;
import com.corum.backend.dto.dashboard.DashboardWidgetRequest;
import com.corum.backend.dto.dashboard.DashboardWidgetResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardWidgetService {

    private final DashboardWidgetRepository widgetRepository;
    private final BoardRepository boardRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final InquiryRepository inquiryRepository;
    private final VisitStatsRepository visitStatsRepository;
    private final CalendarRepository calendarRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final UploadFileRepository uploadFileRepository;
    private final MenuRepository menuRepository;
    private final CalendarService calendarService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<DashboardWidgetResponse> getAdminWidgets(Long menuId) {
        return widgetRepository.findByMenuId(menuId).stream()
                .map(w -> toResponse(w, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardWidgetResponse> getActiveWidgets(Long menuId, Long memberId) {
        List<DashboardWidget> widgets = widgetRepository.findActiveByMenuId(menuId);
        if (widgets.isEmpty()) {
            return menuId == null ? getDefaultWidgets() : List.of();
        }
        return widgets.stream().map(w -> toResponse(w, memberId)).toList();
    }

    /** 데이터 없이 위젯 메타데이터만 반환 (빠른 레이아웃 로드용) */
    @Transactional(readOnly = true)
    public List<DashboardWidgetResponse> getActiveWidgetLayouts(Long menuId) {
        List<DashboardWidget> widgets = widgetRepository.findActiveByMenuId(menuId);
        if (widgets.isEmpty()) {
            if (menuId != null) return List.of();
            // 홈 대시보드 기본 위젯도 레이아웃만
            return boardRepository.findByIsActiveTrueOrderByIdAsc().stream()
                    .limit(4)
                    .map(board -> DashboardWidget.builder()
                            .id(-board.getId())
                            .widgetType("RECENT_POSTS")
                            .title(board.getName())
                            .targetBoardId(board.getId())
                            .postCount(5)
                            .sortOrder(board.getId().intValue())
                            .isActive(true)
                            .build())
                    .map(w -> toLayoutResponse(w, w.getTargetBoardId() != null
                            ? boardRepository.findById(w.getTargetBoardId()).map(b -> b.getName()).orElse(null)
                            : null))
                    .toList();
        }
        // boardId → name 일괄 조회 (N+1 방지)
        Map<Long, String> boardNameMap = boardRepository.findByIsActiveTrueOrderByIdAsc().stream()
                .collect(Collectors.toMap(Board::getId, Board::getName));
        return widgets.stream()
                .map(w -> toLayoutResponse(w, w.getTargetBoardId() != null
                        ? boardNameMap.get(w.getTargetBoardId()) : null))
                .toList();
    }

    /** 대시보드 목록 (홈 + 메뉴 대시보드) */
    @Transactional(readOnly = true)
    public List<DashboardInfoResponse> getDashboardList() {
        // 모든 메뉴 맵 (경로 빌딩용)
        Map<Long, Menu> menuMap = menuRepository.findAllByOrderBySortOrderAsc().stream()
                .collect(Collectors.toMap(Menu::getId, m -> m));

        List<DashboardInfoResponse> result = new java.util.ArrayList<>();

        // 홈 대시보드
        int homeCount = widgetRepository.findByMenuId(null).size();
        result.add(new DashboardInfoResponse(null, "홈 대시보드", "홈 대시보드", homeCount, true));

        // 메뉴 연결 대시보드
        List<Menu> dashboardMenus = menuRepository.findByPageType("DASHBOARD");
        for (Menu menu : dashboardMenus) {
            int count = widgetRepository.findByMenuId(menu.getId()).size();
            String path = buildMenuPath(menu, menuMap);
            result.add(new DashboardInfoResponse(menu.getId(), path, menu.getName(), count, false));
        }
        return result;
    }

    private String buildMenuPath(Menu menu, Map<Long, Menu> menuMap) {
        java.util.Deque<String> parts = new java.util.ArrayDeque<>();
        Menu cur = menu;
        while (cur != null) {
            parts.addFirst(cur.getName());
            cur = cur.getParentId() != null ? menuMap.get(cur.getParentId()) : null;
        }
        return String.join(" > ", parts);
    }

    /** 단일 위젯의 실제 데이터 로드 */
    @Transactional(readOnly = true)
    public DashboardWidgetResponse getWidgetData(Long id, int weekOffset, Long memberId) {
        if (id < 0) {
            return widgetRepository.findById(-id)
                    .map(w -> toResponseWithOffset(w, weekOffset, memberId))
                    .orElseThrow(() -> BusinessException.notFound("위젯을 찾을 수 없습니다."));
        }
        DashboardWidget widget = widgetRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("위젯을 찾을 수 없습니다."));
        return toResponseWithOffset(widget, weekOffset, memberId);
    }

    private DashboardWidgetResponse toResponseWithOffset(DashboardWidget widget, int weekOffset, Long memberId) {
        if (weekOffset == 0) return toResponse(widget, memberId);
        // weekOffset != 0 이면 캘린더만 특별 처리, 나머지는 기존과 동일
        if ("CALENDAR_WEEKLY".equals(widget.getWidgetType())) {
            String boardName = widget.getTargetBoardId() != null
                    ? boardRepository.findById(widget.getTargetBoardId()).map(b -> b.getName()).orElse(null)
                    : null;
            List<DashboardCalendarEventResponse> events = buildCalendarEvents(widget, weekOffset, memberId);
            return new DashboardWidgetResponse(widget, boardName, List.of(), null, events);
        }
        return toResponse(widget, memberId);
    }

    @Transactional
    public DashboardWidgetResponse create(DashboardWidgetRequest request, Long memberId) {
        int nextOrder = widgetRepository.findMaxSortOrderByMenuId(request.getMenuId()) + 1;
        DashboardWidget widget = widgetRepository.save(DashboardWidget.builder()
                .widgetType(request.getWidgetType())
                .title(request.getTitle())
                .description(request.getDescription())
                .menuId(request.getMenuId())
                .targetBoardId(request.getTargetBoardId())
                .postCount(normalizePostCount(request.getPostCount()))
                .sortOrder(nextOrder)
                .isActive(request.getIsActive() == null || request.getIsActive())
                .extraConfig(request.getExtraConfig())
                .updatedBy(memberId)
                .build());
        return toResponse(widget, null);
    }

    @Transactional
    public DashboardWidgetResponse update(Long id, DashboardWidgetRequest request, Long memberId) {
        DashboardWidget widget = widgetRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("대시보드 위젯을 찾을 수 없습니다."));
        widget.update(
                request.getWidgetType(),
                request.getTitle(),
                request.getDescription(),
                request.getTargetBoardId(),
                normalizePostCount(request.getPostCount()),
                request.getSortOrder() != null ? request.getSortOrder() : 0,
                request.getIsActive() == null || request.getIsActive(),
                request.getExtraConfig(),
                memberId
        );
        return toResponse(widget, null);
    }

    @Transactional
    public void delete(Long id) {
        DashboardWidget widget = widgetRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("대시보드 위젯을 찾을 수 없습니다."));
        widgetRepository.delete(widget);
    }

    @Transactional
    public void updateSortOrder(List<Long> widgetIds, Long memberId) {
        for (int i = 0; i < widgetIds.size(); i++) {
            DashboardWidget widget = widgetRepository.findById(widgetIds.get(i))
                    .orElseThrow(() -> BusinessException.notFound("위젯을 찾을 수 없습니다."));
            widget.updateSortOrder(i, memberId);
        }
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        VisitStats today = visitStatsRepository.findByStatDate(LocalDate.now()).orElse(null);
        return new DashboardStatsResponse(
                memberRepository.count(),
                boardRepository.count(),
                inquiryRepository.countByStatus("RECEIVED"),
                today != null ? today.getTotalVisits() : 0,
                today != null ? today.getUniqueVisits() : 0,
                today != null ? today.getLoginVisits() : 0
        );
    }

    // ===== 내부 변환 =====

    /** 데이터 없는 레이아웃 전용 응답 */
    private DashboardWidgetResponse toLayoutResponse(DashboardWidget widget, String boardName) {
        return new DashboardWidgetResponse(widget, boardName, List.of(), null, List.of());
    }

    private DashboardWidgetResponse toResponse(DashboardWidget widget, Long memberId) {
        String boardName = null;
        if (widget.getTargetBoardId() != null) {
            boardName = boardRepository.findById(widget.getTargetBoardId())
                    .map(Board::getName)
                    .orElse(null);
        }

        String type = widget.getWidgetType();

        // 최신 글 (텍스트 목록)
        if ("RECENT_POSTS".equals(type)) {
            List<DashboardPostResponse> posts = buildPostsResponse(widget, false);
            return new DashboardWidgetResponse(widget, boardName, posts, null);
        }

        // 갤러리 최신 글 (썸네일 그리드)
        if ("RECENT_GALLERY".equals(type)) {
            List<DashboardPostResponse> posts = buildPostsResponse(widget, true);
            return new DashboardWidgetResponse(widget, boardName, posts, null);
        }

        // 캘린더 위클리
        if ("CALENDAR_WEEKLY".equals(type)) {
            List<DashboardCalendarEventResponse> events = buildCalendarEvents(widget, 0, memberId);
            return new DashboardWidgetResponse(widget, boardName, List.of(), null, events);
        }

        // 회원 현황 / 접속 통계
        if ("MEMBER_STATS".equals(type) || "VISIT_STATS".equals(type)) {
            DashboardStatsResponse stats = getStats();
            return new DashboardWidgetResponse(widget, boardName, List.of(), stats);
        }

        // IMAGE_SLIDER, LINK_LIST, QUICK_LINKS, IMAGE_GRID, CUSTOM: extraConfig만 사용
        return new DashboardWidgetResponse(widget, boardName, List.of(), null);
    }

    private List<DashboardPostResponse> buildPostsResponse(DashboardWidget widget, boolean withThumbs) {
        List<Post> postList = getRecentPosts(widget);
        if (postList.isEmpty()) return List.of();

        boolean isAll = widget.getTargetBoardId() == null;
        Map<Long, String> boardNameMap = isAll
                ? boardRepository.findByIsActiveTrueOrderByIdAsc().stream()
                        .collect(Collectors.toMap(Board::getId, Board::getName))
                : Map.of();

        Map<Long, String> thumbMap = new HashMap<>();
        if (withThumbs) {
            List<Long> postIds = postList.stream().map(Post::getId).toList();
            // 1순위: 첨부 이미지 파일 썸네일
            List<UploadFile> images = uploadFileRepository.findImageFilesByPostIds(postIds);
            for (UploadFile f : images) {
                thumbMap.putIfAbsent(f.getTargetId(), buildFileUrl(f));
            }
            // 2순위: 본문 인라인 이미지 src 추출 (갤러리/웹진 TipTap 이미지)
            for (Post p : postList) {
                if (!thumbMap.containsKey(p.getId())) {
                    String inlineUrl = extractFirstImageFromContent(p.getContent());
                    if (inlineUrl != null) thumbMap.put(p.getId(), inlineUrl);
                }
            }
        }

        return postList.stream()
                .map(p -> new DashboardPostResponse(
                        p,
                        isAll ? boardNameMap.get(p.getBoardId()) : null,
                        withThumbs ? thumbMap.get(p.getId()) : null
                ))
                .toList();
    }

    private String extractFirstImageFromContent(String content) {
        if (content == null || content.isBlank()) return null;
        int idx = content.indexOf("<img ");
        if (idx < 0) return null;
        int srcIdx = content.indexOf("src=\"", idx);
        if (srcIdx < 0) return null;
        int start = srcIdx + 5;
        int end = content.indexOf("\"", start);
        if (end < 0) return null;
        return toInlineThumbnailUrl(content.substring(start, end));
    }

    /** 인라인 이미지 URL을 대시보드용 소형 썸네일 URL로 변환 */
    private String toInlineThumbnailUrl(String url) {
        if (url == null) return null;
        if (url.contains("/api/files/inline/")) {
            return url.replace("/api/files/inline/", "/api/files/inline-thumb/");
        }
        return url;
    }

    private String buildFileUrl(UploadFile f) {
        return f.getThumbnailPath() != null
                ? "/api/files/" + f.getId() + "/small-thumb"
                : "/api/files/" + f.getId() + "/view";
    }

    private List<DashboardCalendarEventResponse> buildCalendarEvents(DashboardWidget widget, int weekOffset, Long memberId) {
        Long calendarId = parseCalendarId(widget.getExtraConfig());

        LocalDateTime weekStart = LocalDate.now().with(DayOfWeek.MONDAY).plusWeeks(weekOffset).atStartOfDay();
        LocalDateTime weekEnd   = weekStart.plusDays(7).minusNanos(1);

        // 열람 권한이 있는 캘린더 ID 목록
        List<Long> readableIds = calendarService.getReadableCalendarIds(memberId);
        if (readableIds.isEmpty()) return List.of();

        List<CalendarEntity> calendarList;
        if (calendarId != null) {
            // 위젯에 특정 캘린더가 지정된 경우: 열람 권한이 있을 때만 표시
            if (!readableIds.contains(calendarId)) return List.of();
            calendarList = calendarRepository.findById(calendarId)
                    .map(List::of).orElse(List.of());
        } else {
            // 전체 캘린더: 읽기 권한 있는 것만
            calendarList = calendarRepository.findByIsActiveTrueOrderByIdAsc().stream()
                    .filter(c -> readableIds.contains(c.getId()))
                    .collect(Collectors.toList());
        }
        if (calendarList.isEmpty()) return List.of();

        Map<Long, CalendarEntity> calMap = calendarList.stream()
                .collect(Collectors.toMap(CalendarEntity::getId, c -> c));
        List<Long> calIds = calendarList.stream().map(CalendarEntity::getId).toList();

        return calendarService.getEvents(weekStart, weekEnd, memberId, calIds)
                .stream()
                .map(dto -> new DashboardCalendarEventResponse(dto, calMap.get(dto.getCalendarId())))
                .toList();
    }

    private Long parseCalendarId(String extraConfig) {
        if (extraConfig == null || extraConfig.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(extraConfig);
            JsonNode cal = node.get("calendarId");
            if (cal != null && !cal.isNull()) return cal.asLong();
        } catch (Exception e) {
            log.debug("extraConfig 파싱 실패: {}", e.getMessage());
        }
        return null;
    }

    private List<Post> getRecentPosts(DashboardWidget widget) {
        int count = normalizePostCount(widget.getPostCount());
        if (widget.getTargetBoardId() != null) {
            return postRepository.findByBoardId(widget.getTargetBoardId(), PageRequest.of(0, count)).getContent();
        }
        List<Long> boardIds = boardRepository.findByIsActiveTrueOrderByIdAsc().stream()
                .map(Board::getId)
                .toList();
        if (boardIds.isEmpty()) return List.of();
        return postRepository.findLatestByBoardIds(boardIds, PageRequest.of(0, count));
    }

    private List<DashboardWidgetResponse> getDefaultWidgets() {
        DashboardStatsResponse stats = getStats();
        List<Board> boards = boardRepository.findByIsActiveTrueOrderByIdAsc();
        List<DashboardWidgetResponse> widgets = boards.stream()
                .limit(4)
                .map(board -> DashboardWidget.builder()
                        .id(-board.getId())
                        .widgetType("RECENT_POSTS")
                        .title(board.getName())
                        .targetBoardId(board.getId())
                        .postCount(5)
                        .sortOrder(board.getId().intValue())
                        .isActive(true)
                        .build())
                .map(w -> toResponse(w, null))
                .toList();
        if (!widgets.isEmpty()) return widgets;

        DashboardWidget statsWidget = DashboardWidget.builder()
                .id(-1L)
                .widgetType("MEMBER_STATS")
                .title("사이트 현황")
                .postCount(5)
                .sortOrder(0)
                .isActive(true)
                .build();
        return List.of(new DashboardWidgetResponse(statsWidget, null, List.of(), stats));
    }

    private int normalizePostCount(Integer postCount) {
        if (postCount == null || postCount < 1) return 5;
        return Math.min(postCount, 20);
    }
}
