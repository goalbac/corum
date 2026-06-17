package com.corum.backend.dto.stats;

import lombok.Getter;

import java.util.List;

@Getter
public class DailyStatsResponse {
    private final List<DailyStatRow> rows;
    private final DailyStatRow week;   // 최근 7일 합계
    private final DailyStatRow month;  // 이번달 합계

    public DailyStatsResponse(List<DailyStatRow> rows, DailyStatRow week, DailyStatRow month) {
        this.rows = rows;
        this.week = week;
        this.month = month;
    }
}
