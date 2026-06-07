package com.corum.backend.dto.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class GroupCreateRequest {

    @NotNull(message = "상위 그룹을 선택해주세요.")
    private Long parentId;

    @NotBlank(message = "그룹명을 입력해주세요.")
    private String name;

    private String description;

    private Integer sortOrder = 0;
}
