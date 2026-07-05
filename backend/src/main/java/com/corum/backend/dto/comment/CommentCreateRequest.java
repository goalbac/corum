package com.corum.backend.dto.comment;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class CommentCreateRequest {

    private Long parentId;  // null이면 최상위 댓글

    @NotBlank(message = "댓글 내용을 입력해주세요.")
    private String content;

    // 대리 작성: 게시판에 등록된 이름 중 하나 (권한 없으면 서버에서 무시됨)
    private String aliasName;
}
