package com.corum.backend.dto.comment;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class AdminCommentBatchRequest {
    private List<Long> ids;
    private LocalDateTime createdAt;
}
