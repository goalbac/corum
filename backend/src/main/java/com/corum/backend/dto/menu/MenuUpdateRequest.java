package com.corum.backend.dto.menu;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.util.List;

@Getter
public class MenuUpdateRequest {

    @NotBlank(message = "메뉴명을 입력해주세요.")
    private String name;

    private String description;

    private String menuType;

    private String pageType;

    private Long targetId;

    // CALENDAR 타입: 다중 캘린더 연결
    private List<Long> targetCalendarIds;

    private String url;

    private Boolean urlAuto = true;

    private Boolean newWindow = false;

    private Integer sortOrder = 0;

    private Boolean isHidden = false;

    private Boolean hideIfNoPermission = true;

    private String accessType = "ALL";

    private Boolean isActive = true;

    private Boolean showHoliday = true;

    private List<Long> allowedGroupIds;
}
