package com.corum.backend.service.stats;

import com.corum.backend.domain.comment.CommentRepository;
import com.corum.backend.domain.dashboard.VisitStats;
import com.corum.backend.domain.dashboard.VisitStatsRepository;
import com.corum.backend.domain.inquiry.InquiryRepository;
import com.corum.backend.domain.log.VisitLogRepository;
import com.corum.backend.domain.member.MemberRepository;
import com.corum.backend.domain.post.PostRepository;
import com.corum.backend.dto.stats.DailyStatRow;
import com.corum.backend.dto.stats.DailyStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatService {

    private final VisitStatsRepository visitStatsRepository;
    private final VisitLogRepository visitLogRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final InquiryRepository inquiryRepository;

    @Transactional(readOnly = true)
    public DailyStatsResponse getDailyStats(int days) {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(days - 1);

        // 방문 통계 맵 (날짜 → VisitStats)
        Map<LocalDate, VisitStats> visitMap = visitStatsRepository
                .findByStatDateBetweenOrderByStatDateAsc(from, today).stream()
                .collect(Collectors.toMap(VisitStats::getStatDate, v -> v));

        // 날짜별 행 생성
        List<DailyStatRow> rows = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            rows.add(buildRow(date, visitMap));
        }

        // 최근 7일 합계
        LocalDate week7 = today.minusDays(6);
        DailyStatRow weekRow = buildSummary(week7, today, visitMap, "최근 7일");

        // 이번달 합계
        LocalDate monthStart = today.withDayOfMonth(1);
        DailyStatRow monthRow = buildSummary(monthStart, today, visitMap, "이번달");

        return new DailyStatsResponse(rows, weekRow, monthRow);
    }

    private DailyStatRow buildRow(LocalDate date, Map<LocalDate, VisitStats> visitMap) {
        VisitStats vs = visitMap.get(date);
        int visitors = vs != null ? vs.getUniqueVisits() : 0;
        int pageViews = vs != null ? vs.getTotalVisits() : 0;

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        return new DailyStatRow(
                date, visitors, pageViews,
                memberRepository.countByJoinedAtBetween(start, end),
                postRepository.countByCreatedAtBetween(start, end),
                commentRepository.countByCreatedAtBetweenAndIsDeletedFalse(start, end),
                inquiryRepository.countByCreatedAtBetween(start, end)
        );
    }

    private DailyStatRow buildSummary(LocalDate from, LocalDate to,
                                      Map<LocalDate, VisitStats> visitMap, String label) {
        // 방문자(uniqueVisits)는 날짜별 값을 그대로 더하면 같은 사람이 기간 내 여러 날
        // 방문했을 때 그만큼 중복 집계된다. 기간 전체 기준 distinct IP 수를 따로 구한다.
        int visitors = (int) visitLogRepository.countDistinctIpByVisitDateBetween(from, to);
        int pageViews = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            VisitStats vs = visitMap.get(d);
            if (vs != null) { pageViews += vs.getTotalVisits(); }
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        return new DailyStatRow(
                from, visitors, pageViews,
                memberRepository.countByJoinedAtBetween(start, end),
                postRepository.countByCreatedAtBetween(start, end),
                commentRepository.countByCreatedAtBetweenAndIsDeletedFalse(start, end),
                inquiryRepository.countByCreatedAtBetween(start, end)
        );
    }
}
