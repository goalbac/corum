package com.corum.backend.controller.admin;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.domain.admin.AdminMenu;
import com.corum.backend.domain.admin.AdminMenuGroupPermission;
import com.corum.backend.domain.admin.AdminMenuGroupPermissionRepository;
import com.corum.backend.domain.admin.AdminMenuRepository;
import com.corum.backend.domain.group.Group;
import com.corum.backend.domain.group.GroupRepository;
import com.corum.backend.domain.group.MemberGroupRepository;
import com.corum.backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 관리자 메뉴 권한 API
 * GET  /api/admin/admin-menus             → 전체 메뉴 + 그룹별 권한 (권한 설정 페이지용)
 * PUT  /api/admin/admin-menus/permissions → 그룹 권한 일괄 저장
 * GET  /api/admin/sidebar                 → 현재 사용자가 접근 가능한 사이드바 메뉴
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminMenuPermissionController {

    private final AdminMenuRepository adminMenuRepository;
    private final AdminMenuGroupPermissionRepository adminMenuGroupPermissionRepository;
    private final GroupRepository groupRepository;
    private final MemberGroupRepository memberGroupRepository;

    /** 권한 설정 페이지: 메뉴 트리 + 그룹별 권한 매트릭스 */
    @GetMapping("/admin-menus")
    public ApiResponse<Map<String, Object>> getAdminMenuPermissions() {
        List<AdminMenu> menus   = adminMenuRepository.findAllByOrderBySortOrderAsc();
        List<Group>     groups  = getAdminSubGroups();
        List<AdminMenuGroupPermission> allPerms = adminMenuGroupPermissionRepository.findAll();

        // permMap: menuId → groupId → perm
        Map<Long, Map<Long, AdminMenuGroupPermission>> permMap = new HashMap<>();
        for (AdminMenuGroupPermission p : allPerms) {
            permMap.computeIfAbsent(p.getAdminMenuId(), k -> new HashMap<>()).put(p.getGroupId(), p);
        }

        // 메뉴 트리 구성
        Map<Long, Map<String, Object>> menuNodes = new LinkedHashMap<>();
        List<Map<String, Object>> roots = new ArrayList<>();

        for (AdminMenu m : menus) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", m.getId());
            node.put("parentId", m.getParentId());
            node.put("name", m.getName());
            node.put("url", m.getUrl());
            node.put("icon", m.getIcon());
            node.put("children", new ArrayList<>());

            // 그룹별 권한
            Map<Long, Map<String, Object>> groupPerms = new LinkedHashMap<>();
            for (Group g : groups) {
                AdminMenuGroupPermission p = permMap.getOrDefault(m.getId(), Map.of()).get(g.getId());
                Map<String, Object> gp = new LinkedHashMap<>();
                gp.put("canView", p != null ? p.getCanView() : false);
                gp.put("canEdit", p != null ? p.getCanEdit() : false);
                groupPerms.put(g.getId(), gp);
            }
            node.put("groupPerms", groupPerms);
            menuNodes.put(m.getId(), node);
        }

        for (AdminMenu m : menus) {
            if (m.getParentId() == null) {
                roots.add(menuNodes.get(m.getId()));
            } else {
                Map<String, Object> parent = menuNodes.get(m.getParentId());
                if (parent != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                    children.add(menuNodes.get(m.getId()));
                }
            }
        }

        // 그룹 목록 (뷰에서 컬럼 헤더로 사용)
        List<Map<String, Object>> groupList = groups.stream().map(g -> {
            Group parent = groupRepository.findById(g.getParentId() != null ? g.getParentId() : -1L).orElse(null);
            Map<String, Object> gm = new LinkedHashMap<>();
            gm.put("id", g.getId());
            gm.put("name", g.getName());
            gm.put("parentName", parent != null ? parent.getName() : "");
            gm.put("label", (parent != null ? parent.getName() + " - " : "") + g.getName());
            return gm;
        }).collect(Collectors.toList());

        return ApiResponse.ok(Map.of("menus", roots, "groups", groupList));
    }

    /** 권한 일괄 저장 */
    @PutMapping("/admin-menus/permissions")
    @Transactional
    public ApiResponse<Void> savePermissions(@RequestBody List<Map<String, Object>> body) {
        for (Map<String, Object> row : body) {
            Long menuId  = toLong(row.get("menuId"));
            Long groupId = toLong(row.get("groupId"));
            if (menuId == null || groupId == null) continue;

            boolean canView = toBool(row.get("canView"));
            boolean canEdit = toBool(row.get("canEdit"));

            adminMenuGroupPermissionRepository
                    .findByAdminMenuIdAndGroupId(menuId, groupId)
                    .ifPresentOrElse(
                            p -> p.update(canView, canEdit),
                            () -> adminMenuGroupPermissionRepository.save(
                                    AdminMenuGroupPermission.builder()
                                            .adminMenuId(menuId)
                                            .groupId(groupId)
                                            .canView(canView)
                                            .canEdit(canEdit)
                                            .build())
                    );
        }
        return ApiResponse.ok("저장되었습니다.");
    }

    /**
     * 현재 사용자가 볼 수 있는 사이드바 메뉴 반환.
     * 최고관리자(SUPER_ADMIN 또는 시스템그룹)는 전체 반환.
     */
    @GetMapping("/sidebar")
    public ApiResponse<List<Map<String, Object>>> getSidebar(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<Long> groupIds = memberGroupRepository.findGroupIdsByMemberId(userDetails.getMemberId());
        boolean isSuperAdmin = groupIds.stream().anyMatch(gid -> {
            return groupRepository.findById(gid)
                    .map(g -> Boolean.TRUE.equals(g.getIsSystem()) && "ADMIN".equals(g.getType()) && g.getParentId() != null)
                    .orElse(false);
        });

        List<AdminMenu> allMenus = adminMenuRepository.findAllByOrderBySortOrderAsc();
        Set<Long> viewableIds;

        if (isSuperAdmin) {
            viewableIds = allMenus.stream().map(AdminMenu::getId).collect(Collectors.toSet());
        } else {
            viewableIds = new HashSet<>(adminMenuGroupPermissionRepository.findViewableMenuIds(groupIds));
            // 부모 메뉴도 포함 (자식이 보이면 부모도 보여야 함)
            Map<Long, AdminMenu> menuMap = allMenus.stream().collect(Collectors.toMap(AdminMenu::getId, m -> m));
            Set<Long> toAdd = new HashSet<>();
            for (Long id : viewableIds) {
                AdminMenu m = menuMap.get(id);
                if (m != null && m.getParentId() != null) toAdd.add(m.getParentId());
            }
            viewableIds.addAll(toAdd);
        }

        // 트리 구성
        Map<Long, Map<String, Object>> nodes = new LinkedHashMap<>();
        List<Map<String, Object>> roots = new ArrayList<>();

        for (AdminMenu m : allMenus) {
            if (!viewableIds.contains(m.getId())) continue;
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id",       m.getId());
            node.put("parentId", m.getParentId());
            node.put("name",     m.getName());
            node.put("url",      m.getUrl());
            node.put("icon",     m.getIcon());
            node.put("children", new ArrayList<>());
            nodes.put(m.getId(), node);
        }

        for (AdminMenu m : allMenus) {
            if (!nodes.containsKey(m.getId())) continue;
            if (m.getParentId() == null) {
                roots.add(nodes.get(m.getId()));
            } else if (nodes.containsKey(m.getParentId())) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) nodes.get(m.getParentId()).get("children");
                children.add(nodes.get(m.getId()));
            }
        }
        return ApiResponse.ok(roots);
    }

    /** ADMIN 타입 하위 그룹 중 최고관리자(is_system=true) 제외 */
    private List<Group> getAdminSubGroups() {
        return groupRepository.findAllByOrderBySortOrderAsc().stream()
                .filter(g -> "ADMIN".equals(g.getType())
                        && g.getParentId() != null
                        && !Boolean.TRUE.equals(g.getIsSystem()))
                .collect(Collectors.toList());
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    private boolean toBool(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        return Boolean.parseBoolean(v.toString());
    }
}
