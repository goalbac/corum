package com.corum.backend.dto.dashboard;

import lombok.Getter;

@Getter
public class DashboardStatsResponse {

    private final long memberCount;
    private final long boardCount;
    private final long pendingInquiryCount;
    private final int todayVisits;
    private final int todayUniqueVisits;
    private final int todayLoginVisits;

    public DashboardStatsResponse(long memberCount, long boardCount, long pendingInquiryCount,
                                  int todayVisits, int todayUniqueVisits, int todayLoginVisits) {
        this.memberCount = memberCount;
        this.boardCount = boardCount;
        this.pendingInquiryCount = pendingInquiryCount;
        this.todayVisits = todayVisits;
        this.todayUniqueVisits = todayUniqueVisits;
        this.todayLoginVisits = todayLoginVisits;
    }
}
