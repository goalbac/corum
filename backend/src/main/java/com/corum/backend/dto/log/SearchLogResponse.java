package com.corum.backend.dto.log;

import com.corum.backend.domain.log.SearchLog;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SearchLogResponse {
    private final Long id;
    private final Long memberId;
    private final String keyword;
    private final String searchType;
    private final Integer resultCount;
    private final String ipAddress;
    private final LocalDateTime createdAt;

    public SearchLogResponse(SearchLog log) {
        this.id = log.getId();
        this.memberId = log.getMemberId();
        this.keyword = log.getKeyword();
        this.searchType = log.getSearchType();
        this.resultCount = log.getResultCount();
        this.ipAddress = log.getIpAddress();
        this.createdAt = log.getCreatedAt();
    }
}
