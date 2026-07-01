package com.corum.backend.service.menu;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.board.Board;
import com.corum.backend.domain.board.BoardGroupPermission;
import com.corum.backend.domain.board.BoardGroupPermissionRepository;
import com.corum.backend.domain.board.BoardRepository;
import com.corum.backend.domain.calendar.CalendarEntity;
import com.corum.backend.domain.calendar.CalendarRepository;
import com.corum.backend.domain.content.ContentPage;
import com.corum.backend.domain.content.ContentPageRepository;
import com.corum.backend.domain.menu.Menu;
import com.corum.backend.domain.menu.MenuCalendarTarget;
import com.corum.backend.domain.menu.MenuCalendarTargetRepository;
import com.corum.backend.domain.menu.MenuGroupPermission;
import com.corum.backend.domain.menu.MenuGroupPermissionRepository;
import com.corum.backend.domain.menu.MenuRepository;
import com.corum.backend.domain.post.PostRepository;
import com.corum.backend.dto.menu.MenuCreateRequest;
import com.corum.backend.dto.menu.MenuResponse;
import com.corum.backend.dto.menu.MenuUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final MenuGroupPermissionRepository menuGroupPermissionRepository;
    private final MenuCalendarTargetRepository menuCalendarTargetRepository;
    private final BoardRepository boardRepository;
    private final BoardGroupPermissionRepository boardGroupPermissionRepository;
    private final ContentPageRepository contentPageRepository;
    private final PostRepository postRepository;
    private final CalendarRepository calendarRepository;

    // ===== 전체 메뉴 트리 (관리자용 — 숨김 포함) =====
    @Transactional(readOnly = true)
    public List<MenuResponse> getFullMenuTree() {
        List<Menu> all = menuRepository.findAllByOrderBySortOrderAsc();
        return buildTree(all);
    }

    // ===== 사용자용 메뉴 트리 (권한 필터링) =====
    @Transactional(readOnly = true)
    public List<MenuResponse> getMenuTreeForMember(List<Long> memberGroupIds, boolean isLoggedIn, boolean isAdmin) {
        List<Menu> all = menuRepository.findByIsActiveTrueOrderBySortOrderAsc();

        // 관리자 그룹은 모든 메뉴 접근 허용
        if (isAdmin) return buildTree(all);

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
        List<Long> calIds = menuCalendarTargetRepository.findCalendarIdsByMenuId(id);
        return new MenuResponse(menu, groupIds, calIds);
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

        // PAGE 타입 메뉴 생성 시 관련 리소스 자동 생성
        if ("PAGE".equals(request.getMenuType())) {
            if ("BOARD".equals(request.getPageType()) && request.getTargetId() == null) {
                autoCreateBoard(saved, request.getAllowedGroupIds());
            } else if ("CALENDAR".equals(request.getPageType()) && request.getTargetId() == null
                    && (request.getTargetCalendarIds() == null || request.getTargetCalendarIds().isEmpty())) {
                autoCreateCalendar(saved);
            } else if ("CONTENT".equals(request.getPageType())) {
                autoCreateContentPage(saved);
            }
        }

        // 다중 캘린더 연결 저장
        saveCalendarTargets(saved.getId(), request.getTargetCalendarIds());

        List<Long> groupIds = request.getAllowedGroupIds() != null
                ? request.getAllowedGroupIds() : List.of();
        List<Long> calIds = menuCalendarTargetRepository.findCalendarIdsByMenuId(saved.getId());
        return new MenuResponse(saved, groupIds, calIds);
    }

    private void autoCreateBoard(Menu menu, List<Long> allowedGroupIds) {
        Board board = boardRepository.save(Board.builder()
                .menuId(menu.getId())
                .name(menu.getName())
                .build());

        // 메뉴 허용 그룹에 게시판 기본 권한(읽기/댓글/다운로드) 부여
        if (allowedGroupIds != null && !allowedGroupIds.isEmpty()) {
            List<BoardGroupPermission> perms = allowedGroupIds.stream()
                    .map(gid -> BoardGroupPermission.builder()
                            .boardId(board.getId())
                            .groupId(gid)
                            .canRead(true)
                            .canWrite(true)
                            .canComment(true)
                            .canDownload(true)
                            .build())
                    .collect(Collectors.toList());
            boardGroupPermissionRepository.saveAll(perms);
        }

        // 메뉴의 targetId를 생성된 게시판 ID로 설정
        menu.update(menu.getName(), menu.getDescription(), menu.getMenuType(),
                menu.getPageType(), board.getId(), menu.getUrl(), menu.getUrlAuto(),
                menu.getNewWindow(), menu.getSortOrder(), menu.getIsHidden(),
                menu.getHideIfNoPermission(), menu.getAccessType(), menu.getIsActive(), menu.getShowHoliday());
    }

    private void autoCreateContentPage(Menu menu) {
        contentPageRepository.findByMenuId(menu.getId()).orElseGet(() ->
                contentPageRepository.save(ContentPage.builder()
                        .menuId(menu.getId())
                        .title(menu.getName())
                        .content("")
                        .build())
        );
    }

    private void autoCreateCalendar(Menu menu) {
        CalendarEntity calendar = calendarRepository.save(CalendarEntity.builder()
                .name(menu.getName())
                .color("#4f6ef7")
                .description(menu.getDescription())
                .isActive(true)
                .build());
        menu.update(menu.getName(), menu.getDescription(), menu.getMenuType(),
                menu.getPageType(), calendar.getId(), menu.getUrl(), menu.getUrlAuto(),
                menu.getNewWindow(), menu.getSortOrder(), menu.getIsHidden(),
                menu.getHideIfNoPermission(), menu.getAccessType(), menu.getIsActive(), menu.getShowHoliday());
    }

    // ===== 메뉴 수정 =====
    @Transactional
    public MenuResponse updateMenu(Long id, MenuUpdateRequest request) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("메뉴를 찾을 수 없습니다."));

        Long newParentId = request.getParentId();
        if (newParentId != null && newParentId.equals(id)) {
            throw new BusinessException("메뉴를 자기 자신의 하위로 지정할 수 없습니다.");
        }

        List<Menu> all = menuRepository.findAllByOrderBySortOrderAsc();
        if (newParentId != null && isDescendant(all, id, newParentId)) {
            throw new BusinessException("하위 메뉴를 상위 메뉴로 지정할 수 없습니다.");
        }

        boolean parentChanged = !Objects.equals(menu.getParentId(), newParentId);
        int sortOrder = request.getSortOrder();
        if (parentChanged) {
            Long finalNewParentId = newParentId;
            sortOrder = (int) all.stream()
                    .filter(m -> Objects.equals(m.getParentId(), finalNewParentId))
                    .count();
        }

        menu.update(
                request.getName(), request.getDescription(), menu.getMenuType(),
                menu.getPageType(), request.getTargetId(), request.getUrl(),
                request.getUrlAuto(), request.getNewWindow(), sortOrder,
                request.getIsHidden(), request.getHideIfNoPermission(),
                request.getAccessType(), request.getIsActive(), request.getShowHoliday()
        );
        menu.move(newParentId, sortOrder);

        // 그룹 권한 재설정
        menuGroupPermissionRepository.deleteByMenuId(id);
        saveGroupPermissions(id, request.getAllowedGroupIds());

        // 다중 캘린더 연결 재설정
        menuCalendarTargetRepository.deleteByMenuId(id);
        saveCalendarTargets(id, request.getTargetCalendarIds());

        List<Long> groupIds = request.getAllowedGroupIds() != null
                ? request.getAllowedGroupIds() : List.of();
        List<Long> calIds = menuCalendarTargetRepository.findCalendarIdsByMenuId(id);
        return new MenuResponse(menu, groupIds, calIds);
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
        menuCalendarTargetRepository.deleteByMenuId(id);
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
                    menu.getAccessType(), menu.getIsActive(), menu.getShowHoliday());
        }
    }

    // ===== 메뉴 트리 재정렬 (부모 변경 포함) =====
    @Transactional
    public void reorder(List<com.corum.backend.dto.menu.MenuReorderItem> items) {
        for (com.corum.backend.dto.menu.MenuReorderItem item : items) {
            Menu menu = menuRepository.findById(item.getId())
                    .orElseThrow(() -> BusinessException.notFound("메뉴를 찾을 수 없습니다."));
            menu.move(item.getParentId(), item.getSortOrder());
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

    // targetId(신규 상위 메뉴 후보)가 ancestorId(수정 중인 메뉴)의 하위 트리에 속하는지 확인 (순환 참조 방지)
    private boolean isDescendant(List<Menu> all, Long ancestorId, Long targetId) {
        Map<Long, Menu> byId = all.stream().collect(Collectors.toMap(Menu::getId, m -> m));
        Long current = targetId;
        while (current != null) {
            if (current.equals(ancestorId)) return true;
            Menu m = byId.get(current);
            current = (m != null) ? m.getParentId() : null;
        }
        return false;
    }

    private List<MenuResponse> buildTree(List<Menu> menus) {
        // 메뉴별 그룹 권한 일괄 조회
        List<Long> menuIds = menus.stream().map(Menu::getId).collect(Collectors.toList());
        Map<Long, List<Long>> groupPermMap = new HashMap<>();
        Map<Long, List<Long>> calTargetMap = new HashMap<>();

        if (!menuIds.isEmpty()) {
            menuGroupPermissionRepository.findByMenuIdIn(menuIds)
                    .forEach(p -> groupPermMap
                            .computeIfAbsent(p.getMenuId(), k -> new ArrayList<>())
                            .add(p.getGroupId()));
            menuCalendarTargetRepository.findByMenuIdIn(menuIds)
                    .forEach(t -> calTargetMap
                            .computeIfAbsent(t.getMenuId(), k -> new ArrayList<>())
                            .add(t.getCalendarId()));
        }

        Map<Long, Boolean> newPostMap = getNewPostMap(menus);

        Map<Long, MenuResponse> map = menus.stream()
                .collect(Collectors.toMap(
                        Menu::getId,
                        m -> new MenuResponse(
                                m,
                                groupPermMap.getOrDefault(m.getId(), List.of()),
                                calTargetMap.getOrDefault(m.getId(), List.of()),
                                newPostMap.getOrDefault(m.getId(), false)
                        )
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

    private Map<Long, Boolean> getNewPostMap(List<Menu> menus) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(3);
        return menus.stream()
                .filter(this::isBoardPage)
                .filter(menu -> menu.getTargetId() != null)
                .collect(Collectors.toMap(
                        Menu::getId,
                        menu -> postRepository.existsByBoardIdAndIsHiddenFalseAndCreatedAtAfter(menu.getTargetId(), threshold)
                ));
    }

    private boolean isBoardPage(Menu menu) {
        return "PAGE".equals(menu.getMenuType()) && "BOARD".equals(menu.getPageType());
    }

    private void saveCalendarTargets(Long menuId, List<Long> calendarIds) {
        if (calendarIds == null || calendarIds.isEmpty()) return;
        List<MenuCalendarTarget> targets = calendarIds.stream()
                .map(calId -> MenuCalendarTarget.builder()
                        .menuId(menuId)
                        .calendarId(calId)
                        .build())
                .collect(Collectors.toList());
        menuCalendarTargetRepository.saveAll(targets);
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
