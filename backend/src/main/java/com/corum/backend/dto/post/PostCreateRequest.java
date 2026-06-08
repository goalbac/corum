package com.corum.backend.dto.post;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PostCreateRequest {

    @NotBlank(message = "제목을 입력해주세요.")
    private String title;

    private String content;

    private Boolean isNotice = false;

    // 관리자 전용 편집 필드
    private LocalDateTime createdAt;
    private Integer likeCount;
}
