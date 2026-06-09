package com.corum.backend.service.dashboard;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.board.Board;
import com.corum.backend.domain.board.BoardRepository;
import com.corum.backend.domain.dashboard.DashboardWidget;
import com.corum.backend.domain.dashboard.DashboardWidgetRepository;
import com.corum.backend.domain.dashboard.VisitStats;
import com.corum.backend.domain.dashboard.VisitStatsRepository;
import com.corum.backend.domain.inquiry.InquiryRepository;
import com.corum.backend.domain.member.MemberRepository;
import com.corum.backend.domain.post.Post;
import com.corum.backend.domain.post.PostRepository;
import com.corum.backend.dto.dashboard.DashboardPostResponse;
import com.corum.backend.dto.dashboard.DashboardStatsResponse;
import com.corum.backend.dto.dashboard.DashboardWidgetRequest;
import com.corum.backend.dto.dashboard.DashboardWidgetResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardWidgetService {

    private final DashboardWidgetRepository widgetRepository;
    private final BoardRepository boardRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final InquiryRepository inquiryRepository;
    private final VisitStatsRepository visitStatsRepository;

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
        DashboardWidget widget = widgetRepository.save(DashboardWidget.builder()
                .widgetType(request.getWidgetType())
                .title(request.getTitle())
                .targetBoardId(request.getTargetBoardId())
                .postCount(normalizePostCount(request.getPostCount()))
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
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
                    .orElseThrow(() -> BusinessException.notFound("??쒕낫???꾩젽??李얠쓣 ???놁뒿?덈떎."));
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

    private DashboardWidgetResponse toResponse(DashboardWidget widget) {
        String boardName = null;
        if (widget.getTargetBoardId() != null) {
            boardName = boardRepository.findById(widget.getTargetBoardId())
                    .map(Board::getName)
                    .orElse(null);
        }

        List<DashboardPostResponse> posts = List.of();
        DashboardStatsResponse stats = null;
        if ("RECENT_POSTS".equals(widget.getWidgetType())) {
            posts = getRecentPosts(widget).stream().map(DashboardPostResponse::new).toList();
        }
        if ("MEMBER_STATS".equals(widget.getWidgetType()) || "VISIT_STATS".equals(widget.getWidgetType())) {
            stats = getStats();
        }
        return new DashboardWidgetResponse(widget, boardName, posts, stats);
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
