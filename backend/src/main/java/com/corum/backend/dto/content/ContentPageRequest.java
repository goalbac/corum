package com.corum.backend.dto.content;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContentPageRequest {

    @NotBlank(message = "제목을 입력해주세요.")
    private String title;

    private String content;
}
