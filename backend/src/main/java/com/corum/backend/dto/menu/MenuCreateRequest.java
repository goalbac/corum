package com.corum.backend.dto.menu;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.util.List;

@Getter
public class MenuCreateRequest {

    private Long parentId;

    @NotBlank(message = "메뉴명을 입력해주세요.")
    private String name;

    private String description;

    // PAGE, LINK, GROUP
    @NotBlank(message = "메뉴 유형을 선택해주세요.")
    private String menuType;

    // BOARD, CALENDAR, CONTENT, DASHBOARD
    private String pageType;

    private Long targetId;

    private String url;

    private Boolean urlAuto = true;

    private Boolean newWindow = false;

    private Integer sortOrder = 0;

    private Boolean isHidden = false;

    private Boolean hideIfNoPermission = true;

    // ALL, LOGIN, GROUP
    private String accessType = "ALL";

    private Boolean isActive = true;

    // accessType=GROUP 일 때 접근 허용 그룹 ID 목록
    private List<Long> allowedGroupIds;
}
