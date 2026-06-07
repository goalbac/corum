package com.corum.backend.dto.comment;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class CommentCreateRequest {

    private Long parentId;  // null이면 최상위 댓글

    @NotBlank(message = "댓글 내용을 입력해주세요.")
    private String content;
}
