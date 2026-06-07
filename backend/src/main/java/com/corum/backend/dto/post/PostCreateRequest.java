package com.corum.backend.dto.post;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PostCreateRequest {

    @NotBlank(message = "제목을 입력해주세요.")
    private String title;

    private String content;

    private Boolean isNotice = false;
}
