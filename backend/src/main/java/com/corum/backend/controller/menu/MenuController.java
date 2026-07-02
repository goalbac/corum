package com.corum.backend.controller.menu;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.domain.group.MemberGroupRepository;
import com.corum.backend.dto.menu.MenuCreateRequest;
import com.corum.backend.dto.menu.MenuResponse;
import com.corum.backend.dto.menu.MenuUpdateRequest;
import com.corum.backend.security.CustomUserDetails;
import com.corum.backend.service.menu.MenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;
    private final MemberGroupRepository memberGroupRepository;

    // 사용자용 메뉴 트리 (권한 필터링)
    @GetMapping
    public ApiResponse<List<MenuResponse>> getMenuTree(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        boolean isLoggedIn = userDetails != null;
        List<Long> groupIds = isLoggedIn
                ? memberGroupRepository.findGroupIdsByMemberId(userDetails.getMemberId())
                : List.of();
        boolean isAdmin = isLoggedIn && memberGroupRepository.existsAdminGroupByMemberId(userDetails.getMemberId());

        return ApiResponse.ok(menuService.getMenuTreeForMember(groupIds, isLoggedIn, isAdmin));
    }

    // 관리자용 전체 메뉴 트리 (숨김 포함)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public ApiResponse<List<MenuResponse>> getFullMenuTree() {
        return ApiResponse.ok(menuService.getFullMenuTree());
    }

    // 메뉴 단건 조회
    @GetMapping("/{id}")
    public ApiResponse<MenuResponse> getMenu(@PathVariable Long id) {
        return ApiResponse.ok(menuService.getMenu(id));
    }

    // 메뉴 생성
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MenuResponse> createMenu(@Valid @RequestBody MenuCreateRequest request) {
        return ApiResponse.ok(menuService.createMenu(request));
    }

    // 메뉴 수정
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<MenuResponse> updateMenu(
            @PathVariable Long id,
            @Valid @RequestBody MenuUpdateRequest request) {
        return ApiResponse.ok(menuService.updateMenu(id, request));
    }

    // 메뉴 삭제
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMenu(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return ApiResponse.ok("메뉴가 삭제되었습니다.");
    }

    // 메뉴 순서 일괄 변경
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/sort")
    public ApiResponse<Void> updateSortOrder(@RequestBody List<Long> menuIds) {
        menuService.updateSortOrder(menuIds);
        return ApiResponse.ok("순서가 변경되었습니다.");
    }

    // 메뉴 트리 재정렬 (부모 변경 포함)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/reorder")
    public ApiResponse<Void> reorder(
            @RequestBody List<com.corum.backend.dto.menu.MenuReorderItem> items) {
        menuService.reorder(items);
        return ApiResponse.ok("순서가 변경되었습니다.");
    }
}
