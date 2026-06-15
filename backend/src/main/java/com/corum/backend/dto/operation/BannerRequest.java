package com.corum.backend.dto.operation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BannerRequest {
    @NotBlank(message = "배너 제목을 입력해주세요.")
    private String title;
    private String content;
    private String linkUrl;
    @NotNull(message = "새 창 여부를 선택해주세요.")
    private Boolean linkNewWindow = false;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    @NotNull(message = "활성 여부를 선택해주세요.")
    private Boolean isActive = true;
    private String bgColor;
    private String textAlign;
}
