package com.corum.backend.dto.group;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class GroupCreateRequest {

    // 상위 그룹 ID (필수 — UI에서 최상위 그룹은 시스템 고정이므로 항상 하위 그룹만 생성)
    private Long parentId;

    @NotBlank(message = "그룹명을 입력해주세요.")
    private String name;

    private String description;

    private Integer sortOrder = 0;
}
