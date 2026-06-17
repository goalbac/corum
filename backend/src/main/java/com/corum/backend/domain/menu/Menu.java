package com.corum.backend.domain.menu;

import com.corum.backend.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "menus")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Menu extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    // PAGE, LINK, GROUP(폴더)
    @Column(name = "menu_type", nullable = false, length = 20)
    @Builder.Default
    private String menuType = "PAGE";

    // BOARD, CALENDAR, CONTENT, DASHBOARD
    @Column(name = "page_type", length = 20)
    private String pageType;

    // 연결된 게시판/페이지 ID
    @Column(name = "target_id")
    private Long targetId;

    // 직접 지정 URL
    @Column(name = "url", length = 500)
    private String url;

    // true: 자동 넘버링(/id), false: url 필드 사용
    @Column(name = "url_auto", nullable = false)
    @Builder.Default
    private Boolean urlAuto = true;

    // 링크 새창 여부
    @Column(name = "new_window", nullable = false)
    @Builder.Default
    private Boolean newWindow = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    // 메뉴 완전 숨김
    @Column(name = "is_hidden", nullable = false)
    @Builder.Default
    private Boolean isHidden = false;

    // 권한 없을 때 숨김 (true: 숨김, false: 잠금 표시)
    @Column(name = "hide_if_no_permission", nullable = false)
    @Builder.Default
    private Boolean hideIfNoPermission = true;

    // ALL, LOGIN, GROUP
    @Column(name = "access_type", nullable = false, length = 20)
    @Builder.Default
    private String accessType = "ALL";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // 캘린더 메뉴: 대한민국의 휴일 표시 여부
    @Column(name = "show_holiday", nullable = false)
    @Builder.Default
    private Boolean showHoliday = true;

    // ===== 비즈니스 메서드 =====

    public void update(String name, String description, String menuType,
                       String pageType, Long targetId, String url, Boolean urlAuto,
                       Boolean newWindow, Integer sortOrder, Boolean isHidden,
                       Boolean hideIfNoPermission, String accessType, Boolean isActive,
                       Boolean showHoliday) {
        this.name = name;
        this.description = description;
        this.menuType = menuType;
        this.pageType = pageType;
        this.targetId = targetId;
        this.url = url;
        this.urlAuto = urlAuto;
        this.newWindow = newWindow;
        this.sortOrder = sortOrder;
        this.isHidden = isHidden;
        this.hideIfNoPermission = hideIfNoPermission;
        this.accessType = accessType;
        this.isActive = isActive;
        this.showHoliday = showHoliday != null ? showHoliday : true;
    }

    // 실제 접근 URL 반환
    public String resolveUrl() {
        if ("GROUP".equals(menuType)) return null;
        if ("LINK".equals(menuType)) return url;
        if (urlAuto) return "/" + id;
        return url;
    }

    public void move(Long newParentId, int newSortOrder) {
        this.parentId = newParentId;
        this.sortOrder = newSortOrder;
    }

    public boolean isGroupType() { return "GROUP".equals(menuType); }
    public boolean isLinkType()  { return "LINK".equals(menuType); }
    public boolean isPageType()  { return "PAGE".equals(menuType); }
    public boolean isPublic()    { return "ALL".equals(accessType); }
    public boolean isLoginOnly() { return "LOGIN".equals(accessType); }
    public boolean isGroupRestricted() { return "GROUP".equals(accessType); }
}
