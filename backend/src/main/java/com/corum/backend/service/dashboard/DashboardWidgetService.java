package com.corum.backend.service.dashboard;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.board.Board;
import com.corum.backend.domain.board.BoardRepository;
import com.corum.backend.domain.calendar.CalendarEntity;
import com.corum.backend.domain.calendar.CalendarEventRepository;
import com.corum.backend.domain.calendar.CalendarRepository;
import com.corum.backend.domain.dashboard.DashboardWidget;
import com.corum.backend.domain.dashboard.DashboardWidgetRepository;
import com.corum.backend.domain.dashboard.VisitStats;
import com.corum.backend.domain.dashboard.VisitStatsRepository;
import com.corum.backend.domain.file.UploadFile;
import com.corum.backend.domain.file.UploadFileRepository;
import com.corum.backend.domain.inquiry.InquiryRepository;
import com.corum.backend.domain.member.MemberRepository;
import com.corum.backend.domain.post.Post;
import com.corum.backend.domain.post.PostRepository;
import com.corum.backend.dto.dashboard.DashboardCalendarEventResponse;
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
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<DashboardWidgetResponse> getAdminWidgets() {
        return widgetRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardWidgetResponse> getActiveWidgets() {
        List<DashboardWidget> widgets = widgetRepository.findByIsActiveTrueOrderBySortOrderAscIdAsc();
        if (widgets.isEmpty()) {
            return getDefaultWidgets();
        }
        return widgets.stream().map(this::toResponse).toList();
    }

    @Transactional
    public DashboardWidgetResponse create(DashboardWidgetRequest request, Long memberId) {
        int nextOrder = widgetRepository.findMaxSortOrder() + 1;
        DashboardWidget widget = widgetRepository.save(DashboardWidget.builder()
                .widgetType(request.getWidgetType())
                .title(request.getTitle())
                .targetBoardId(request.getTargetBoardId())
                .postCount(normalizePostCount(request.getPostCount()))
                .sortOrder(nextOrder)
                .isActive(request.getIsActive() == null || request.getIsActive())
                .extraConfig(request.getExtraConfig())
                .updatedBy(memberId)
                .build());
        return toResponse(widget);
    }

    @Transactional
    public DashboardWidgetResponse update(Long id, DashboardWidgetRequest request, Long memberId) {
        DashboardWidget widget = widgetRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("대시보드 위젯을 찾을 수 없습니다."));
        widget.update(
                request.getWidgetType(),
                request.getTitle(),
                request.getTargetBoardId(),
                normalizePostCount(request.getPostCount()),
                request.getSortOrder() != null ? request.getSortOrder() : 0,
                request.getIsActive() == null || request.getIsActive(),
                request.getExtraConfig(),
                memberId
        );
        return toResponse(widget);
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

    private DashboardWidgetResponse toResponse(DashboardWidget widget) {
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
            List<DashboardCalendarEventResponse> events = buildCalendarEvents(widget);
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
        return content.substring(start, end);
    }

    private String buildFileUrl(UploadFile f) {
        return f.getThumbnailPath() != null
                ? "/api/files/" + f.getId() + "/thumbnail"
                : "/api/files/" + f.getId() + "/view";
    }

    private List<DashboardCalendarEventResponse> buildCalendarEvents(DashboardWidget widget) {
        Long calendarId = parseCalendarId(widget.getExtraConfig());

        LocalDateTime weekStart = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime weekEnd   = weekStart.plusDays(7).minusNanos(1);

        List<CalendarEntity> calendarList;
        if (calendarId != null) {
            calendarList = calendarRepository.findById(calendarId)
                    .map(List::of).orElse(List.of());
        } else {
            calendarList = calendarRepository.findByIsActiveTrueOrderByIdAsc();
        }
        if (calendarList.isEmpty()) return List.of();

        Map<Long, CalendarEntity> calMap = calendarList.stream()
                .collect(Collectors.toMap(CalendarEntity::getId, c -> c));
        List<Long> calIds = calendarList.stream().map(CalendarEntity::getId).toList();

        return calendarEventRepository.findByPeriod(calIds, weekStart, weekEnd)
                .stream()
                .map(e -> new DashboardCalendarEventResponse(e, calMap.get(e.getCalendarId())))
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
                .map(this::toResponse)
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
