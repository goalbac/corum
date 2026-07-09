package com.corum.backend.controller.admin;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.board.BoardGroupPermission;
import com.corum.backend.domain.board.BoardGroupPermissionRepository;
import com.corum.backend.domain.board.BoardRepository;
import com.corum.backend.domain.group.Group;
import com.corum.backend.domain.group.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;

/**
 * 게시판별 그룹 권한 관리 API
 * GET  /api/admin/boards/{boardId}/permissions → 현재 권한 목록
 * PUT  /api/admin/boards/{boardId}/permissions → 권한 일괄 저장
 */
@RestController
@RequestMapping("/api/admin/boards/{boardId}/permissions")
@RequiredArgsConstructor
public class AdminBoardPermissionController {

    private final BoardRepository boardRepository;
    private final BoardGroupPermissionRepository boardGroupPermissionRepository;
    private final GroupRepository groupRepository;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getPermissions(@PathVariable Long boardId) {
        if (!boardRepository.existsById(boardId)) throw BusinessException.notFound("게시판을 찾을 수 없습니다.");

        // 전체 그룹 트리 (운영 먼저, 각 타입 내 sortOrder 순)
        List<Group> allGroups = groupRepository.findAllByOrderBySortOrderAsc();
        Map<Long, Group> groupMap = allGroups.stream().collect(Collectors.toMap(Group::getId, g -> g));

        // 현재 설정된 권한
        Map<Long, BoardGroupPermission> permMap = boardGroupPermissionRepository.findByBoardId(boardId)
                .stream().collect(Collectors.toMap(BoardGroupPermission::getGroupId, p -> p));

        // 하위 그룹만 추출 (최고관리자 시스템 그룹 제외) → ADMIN 먼저(운영), NORMAL 다음(일반), 각 타입 내 sortOrder 순
        List<Group> subGroups = allGroups.stream()
                .filter(g -> g.getParentId() != null && !Boolean.TRUE.equals(g.getIsSystem()))
                .sorted(Comparator
                        .comparing((Group g) -> "ADMIN".equals(g.getType()) ? 0 : 1)
                        .thenComparingInt(Group::getSortOrder))
                .collect(Collectors.toList());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Group g : subGroups) {

            Group parent = groupMap.get(g.getParentId());
            String label = (parent != null ? parent.getName() + " - " : "") + g.getName();

            BoardGroupPermission perm = permMap.get(g.getId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("groupId",     g.getId());
            row.put("groupName",   g.getName());
            row.put("parentName",  parent != null ? parent.getName() : "");
            row.put("label",       label);
            row.put("groupType",   g.getType());
            row.put("canRead",     perm != null ? perm.getCanRead()     : false);
            row.put("canWrite",    perm != null ? perm.getCanWrite()    : false);
            row.put("canComment",  perm != null ? perm.getCanComment()  : false);
            row.put("canDownload", perm != null ? perm.getCanDownload() : false);
            row.put("canManage",   perm != null ? perm.getCanManage()   : false);
            result.add(row);
        }
        return ApiResponse.ok(result);
    }

    @PutMapping
    @Transactional
    public ApiResponse<Void> savePermissions(
            @PathVariable Long boardId,
            @RequestBody List<Map<String, Object>> body) {

        if (!boardRepository.existsById(boardId)) throw BusinessException.notFound("게시판을 찾을 수 없습니다.");

        // 기존 권한 전부 삭제 — 같은 트랜잭션에서 바로 재삽입하면 Hibernate가 삽입을
        // 삭제보다 먼저 플러시해서 (board_id, group_id) 유니크 제약에 걸리므로 즉시 flush
        boardGroupPermissionRepository.deleteByBoardId(boardId);
        boardGroupPermissionRepository.flush();

        // 새 권한 저장 (체크된 것만)
        for (Map<String, Object> row : body) {
            Long groupId = toLong(row.get("groupId"));
            if (groupId == null) continue;

            boolean canRead     = toBool(row.get("canRead"));
            boolean canWrite    = toBool(row.get("canWrite"));
            boolean canComment  = toBool(row.get("canComment"));
            boolean canDownload = toBool(row.get("canDownload"));
            boolean canManage   = toBool(row.get("canManage"));

            // 아무 권한도 없으면 행 저장 안 함
            if (!canRead && !canWrite && !canComment && !canDownload && !canManage) continue;

            boardGroupPermissionRepository.save(BoardGroupPermission.builder()
                    .boardId(boardId)
                    .groupId(groupId)
                    .canRead(canRead)
                    .canWrite(canWrite)
                    .canComment(canComment)
                    .canDownload(canDownload)
                    .canManage(canManage)
                    .build());
        }
        return ApiResponse.ok("권한이 저장되었습니다.");
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
