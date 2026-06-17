package com.corum.backend.dto.stats;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class DailyStatRow {
    private final LocalDate date;
    private final int visitors;       // unique IP 방문자
    private final int pageViews;      // 전체 페이지뷰
    private final long newMembers;
    private final long newPosts;
    private final long newComments;
    private final long newInquiries;

    public DailyStatRow(LocalDate date, int visitors, int pageViews,
                        long newMembers, long newPosts, long newComments, long newInquiries) {
        this.date = date;
        this.visitors = visitors;
        this.pageViews = pageViews;
        this.newMembers = newMembers;
        this.newPosts = newPosts;
        this.newComments = newComments;
        this.newInquiries = newInquiries;
    }
}
