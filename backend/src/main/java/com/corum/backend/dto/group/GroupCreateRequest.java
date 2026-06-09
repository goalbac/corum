package com.corum.backend.dto.group;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class GroupCreateRequest {

    // null 허용 — null이면 최상위 그룹 생성 (type 필수)
    private Long parentId;

    @NotBlank(message = "그룹명을 입력해주세요.")
    private String name;

    private String description;

    private Integer sortOrder = 0;

    // 최상위 그룹 생성 시 사용 (ADMIN / NORMAL)
    private String type;
}
