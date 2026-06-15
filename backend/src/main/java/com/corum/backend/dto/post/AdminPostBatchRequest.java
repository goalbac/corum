package com.corum.backend.dto.post;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class AdminPostBatchRequest {
    private List<Long> ids;
    private String writerName;
    private Integer viewCount;
    private Integer likeCount;
    private LocalDateTime createdAt;
    private Long targetBoardId;
}
