package com.corum.backend.service.menu;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.menu.Menu;
import com.corum.backend.domain.menu.MenuGroupPermission;
import com.corum.backend.domain.menu.MenuGroupPermissionRepository;
import com.corum.backend.domain.menu.MenuRepository;
import com.corum.backend.dto.menu.MenuCreateRequest;
import com.corum.backend.dto.menu.MenuResponse;
import com.corum.backend.dto.menu.MenuUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final MenuGroupPermissionRepository menuGroupPermissionRepository;

    // ===== 전체 메뉴 트리 (관리자용 — 숨김 포함) =====
    @Transactional(readOnly = true)
    public List<MenuResponse> getFullMenuTree() {
        List<Menu> all = menuRepository.findAllByOrderBySortOrderAsc();
        return buildTree(all);
    }

    // ===== 사용자용 메뉴 트리 (권한 필터링) =====
    @Transactional(readOnly = true)
    public List<MenuResponse> getMenuTreeForMember(List<Long> memberGroupIds, boolean isLoggedIn) {
        List<Menu> all = menuRepository.findByIsActiveTrueOrderBySortOrderAsc();

        // 권한 필터링
        List<Menu> filtered = all.stream()
                .filter(menu -> canAccess(menu, memberGroupIds, isLoggedIn))
                .collect(Collectors.toList());

        return buildTree(filtered);
    }

    // ===== 메뉴 단건 조회 =====
    @Transactional(readOnly = true)
    public MenuResponse getMenu(Long id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("메뉴를 찾을 수 없습니다."));
        List<Long> groupIds = getMenuGroupIds(id);
        return new MenuResponse(menu, groupIds);
    }

    // ===== 메뉴 생성 =====
    @Transactional
    public MenuResponse createMenu(MenuCreateRequest request) {
        Menu menu = Menu.builder()
                .parentId(request.getParentId())
                .name(request.getName())
                .description(request.getDescription())
                .menuType(request.getMenuType())
                .pageType(request.getPageType())
                .targetId(request.getTargetId())
                .url(request.getUrl())
                .urlAuto(request.getUrlAuto())
                .newWindow(request.getNewWindow())
                .sortOrder(request.getSortOrder())
                .isHidden(request.getIsHidden())
                .hideIfNoPermission(request.getHideIfNoPermission())
                .accessType(request.getAccessType())
                .isActive(request.getIsActive())
                .build();

        Menu saved = menuRepository.save(menu);

        // 그룹 권한 저장
        saveGroupPermissions(saved.getId(), request.getAllowedGroupIds());

        return new MenuResponse(saved, request.getAllowedGroupIds() != null
                ? request.getAllowedGroupIds() : List.of());
    }

    // ===== 메뉴 수정 =====
    @Transactional
    public MenuResponse updateMenu(Long id, MenuUpdateRequest request) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("메뉴를 찾을 수 없습니다."));

        menu.update(
                request.getName(), request.getDescription(), request.getMenuType(),
                request.getPageType(), request.getTargetId(), request.getUrl(),
                request.getUrlAuto(), request.getNewWindow(), request.getSortOrder(),
                request.getIsHidden(), request.getHideIfNoPermission(),
                request.getAccessType(), request.getIsActive()
        );

        // 그룹 권한 재설정
        menuGroupPermissionRepository.deleteByMenuId(id);
        saveGroupPermissions(id, request.getAllowedGroupIds());

        List<Long> groupIds = request.getAllowedGroupIds() != null
                ? request.getAllowedGroupIds() : List.of();
        return new MenuResponse(menu, groupIds);
    }

    // ===== 메뉴 삭제 =====
    @Transactional
    public void deleteMenu(Long id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("메뉴를 찾을 수 없습니다."));

        // 하위 메뉴 존재 확인
        List<Menu> children = menuRepository
                .findByParentIdAndIsActiveTrueOrderBySortOrderAsc(id);
        if (!children.isEmpty()) {
            throw new BusinessException("하위 메뉴가 있는 경우 삭제할 수 없습니다.");
        }

        menuGroupPermissionRepository.deleteByMenuId(id);
        menuRepository.delete(menu);
    }

    // ===== 메뉴 순서 일괄 변경 =====
    @Transactional
    public void updateSortOrder(List<Long> menuIds) {
        for (int i = 0; i < menuIds.size(); i++) {
            Long menuId = menuIds.get(i);
            Menu menu = menuRepository.findById(menuId)
                    .orElseThrow(() -> BusinessException.notFound("메뉴를 찾을 수 없습니다."));
            menu.update(menu.getName(), menu.getDescription(), menu.getMenuType(),
                    menu.getPageType(), menu.getTargetId(), menu.getUrl(),
                    menu.getUrlAuto(), menu.getNewWindow(), i,
                    menu.getIsHidden(), menu.getHideIfNoPermission(),
                    menu.getAccessType(), menu.getIsActive());
        }
    }

    // ===== 접근 권한 체크 =====
    public boolean canAccess(Menu menu, List<Long> memberGroupIds, boolean isLoggedIn) {
        if (menu.isPublic()) return true;
        if (menu.isLoginOnly()) return isLoggedIn;
        if (menu.isGroupRestricted()) {
            if (!isLoggedIn || memberGroupIds.isEmpty()) return false;
            return menuGroupPermissionRepository
                    .existsByMenuIdAndGroupIdIn(menu.getId(), memberGroupIds);
        }
        return false;
    }

    // ===== 내부 메서드 =====
    private List<MenuResponse> buildTree(List<Menu> menus) {
        // 메뉴별 그룹 권한 일괄 조회
        List<Long> menuIds = menus.stream().map(Menu::getId).collect(Collectors.toList());
        Map<Long, List<Long>> groupPermMap = new HashMap<>();

        if (!menuIds.isEmpty()) {
            menuGroupPermissionRepository.findByMenuIdIn(menuIds)
                    .forEach(p -> groupPermMap
                            .computeIfAbsent(p.getMenuId(), k -> new ArrayList<>())
                            .add(p.getGroupId()));
        }

        Map<Long, MenuResponse> map = menus.stream()
                .collect(Collectors.toMap(
                        Menu::getId,
                        m -> new MenuResponse(m, groupPermMap.getOrDefault(m.getId(), List.of()))
                ));

        List<MenuResponse> roots = menus.stream()
                .filter(m -> m.getParentId() == null)
                .map(m -> map.get(m.getId()))
                .collect(Collectors.toList());

        menus.stream()
                .filter(m -> m.getParentId() != null)
                .forEach(m -> {
                    MenuResponse parent = map.get(m.getParentId());
                    if (parent != null) {
                        parent.addChild(map.get(m.getId()));
                    }
                });

        return roots;
    }

    private void saveGroupPermissions(Long menuId, List<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) return;
        List<MenuGroupPermission> permissions = groupIds.stream()
                .map(gid -> MenuGroupPermission.builder()
                        .menuId(menuId)
                        .groupId(gid)
                        .build())
                .collect(Collectors.toList());
        menuGroupPermissionRepository.saveAll(permissions);
    }

    private List<Long> getMenuGroupIds(Long menuId) {
        return menuGroupPermissionRepository.findByMenuId(menuId)
                .stream()
                .map(MenuGroupPermission::getGroupId)
                .collect(Collectors.toList());
    }
}
