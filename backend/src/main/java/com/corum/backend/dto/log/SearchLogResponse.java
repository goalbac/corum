package com.corum.backend.dto.log;

import com.corum.backend.domain.log.SearchLog;
import com.corum.backend.domain.member.Member;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
public class SearchLogResponse {
    private final Long id;
    private final Long memberId;
    private final String memberUsername;
    private final String memberName;
    private final String keyword;
    private final String searchType;
    private final Integer resultCount;
    private final String ipAddress;
    private final LocalDateTime createdAt;

    public SearchLogResponse(SearchLog log) {
        this(log, Map.of());
    }

    public SearchLogResponse(SearchLog log, Map<Long, Member> members) {
        this.id = log.getId();
        this.memberId = log.getMemberId();
        Member member = members.get(log.getMemberId());
        this.memberUsername = member != null ? member.getUsername() : null;
        this.memberName = member != null ? member.getName() : null;
        this.keyword = log.getKeyword();
        this.searchType = log.getSearchType();
        this.resultCount = log.getResultCount();
        this.ipAddress = log.getIpAddress();
        this.createdAt = log.getCreatedAt();
    }
}
