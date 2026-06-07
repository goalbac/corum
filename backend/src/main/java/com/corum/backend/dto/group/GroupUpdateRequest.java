package com.corum.backend.dto.group;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class GroupUpdateRequest {

    @NotBlank(message = "그룹명을 입력해주세요.")
    private String name;

    private String description;

    private Integer sortOrder = 0;
}
