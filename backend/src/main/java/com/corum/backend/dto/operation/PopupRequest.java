package com.corum.backend.dto.operation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PopupRequest {
    @NotBlank(message = "팝업 제목을 입력해주세요.")
    private String title;
    @NotBlank(message = "콘텐츠 유형을 선택해주세요.")
    private String contentType;
    private String content;
    private String imageUrl;
    private String linkUrl;
    @NotNull(message = "새 창 여부를 선택해주세요.")
    private Boolean linkNewWindow = false;
    @NotBlank(message = "위치를 선택해주세요.")
    private String position;
    private Integer priority = 0;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    @NotNull(message = "활성 여부를 선택해주세요.")
    private Boolean isActive = true;
    @NotBlank(message = "노출 대상을 선택해주세요.")
    private String targetType = "ALL";
    private List<Long> targetMenuIds = new ArrayList<>();
}
