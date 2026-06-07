package com.corum.backend.dto.group;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class MemberGroupAssignRequest {

    @NotNull(message = "회원 ID를 입력해주세요.")
    private Long memberId;

    @NotNull(message = "그룹 ID를 입력해주세요.")
    private Long groupId;
}
