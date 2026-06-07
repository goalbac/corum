package com.corum.backend.controller.group;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.dto.group.GroupCreateRequest;
import com.corum.backend.dto.group.GroupResponse;
import com.corum.backend.dto.group.GroupUpdateRequest;
import com.corum.backend.dto.group.MemberGroupAssignRequest;
import com.corum.backend.security.CustomUserDetails;
import com.corum.backend.service.group.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    // ===== 그룹 CRUD =====

    // 그룹 트리 전체 조회
    @GetMapping("/api/groups")
    public ApiResponse<List<GroupResponse>> getGroupTree() {
        return ApiResponse.ok(groupService.getGroupTree());
    }

    // 그룹 단건 조회
    @GetMapping("/api/groups/{id}")
    public ApiResponse<GroupResponse> getGroup(@PathVariable Long id) {
        return ApiResponse.ok(groupService.getGroup(id));
    }

    // 그룹 생성 (SUPER_ADMIN)
    @PostMapping("/api/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GroupResponse> createGroup(
            @Valid @RequestBody GroupCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(groupService.createGroup(request, userDetails.getMemberId()));
    }

    // 그룹 수정 (SUPER_ADMIN)
    @PutMapping("/api/groups/{id}")
    public ApiResponse<GroupResponse> updateGroup(
            @PathVariable Long id,
            @Valid @RequestBody GroupUpdateRequest request) {
        return ApiResponse.ok(groupService.updateGroup(id, request));
    }

    // 그룹 삭제 (SUPER_ADMIN)
    @DeleteMapping("/api/groups/{id}")
    public ApiResponse<Void> deleteGroup(@PathVariable Long id) {
        groupService.deleteGroup(id);
        return ApiResponse.ok("그룹이 삭제되었습니다.");
    }

    // ===== 회원 그룹 부여/회수 (/api/member-groups 로 분리) =====

    // 회원에게 그룹 부여
    @PostMapping("/api/member-groups")
    public ApiResponse<Void> assignGroup(
            @Valid @RequestBody MemberGroupAssignRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        groupService.assignGroup(request.getMemberId(), request.getGroupId(),
                userDetails.getMemberId());
        return ApiResponse.ok("그룹이 부여되었습니다.");
    }

    // 회원에서 그룹 회수
    @DeleteMapping("/api/member-groups")
    public ApiResponse<Void> revokeGroup(
            @Valid @RequestBody MemberGroupAssignRequest request) {
        groupService.revokeGroup(request.getMemberId(), request.getGroupId());
        return ApiResponse.ok("그룹이 회수되었습니다.");
    }

    // 특정 회원의 그룹 목록
    @GetMapping("/api/member-groups/members/{memberId}")
    public ApiResponse<List<GroupResponse>> getMemberGroups(@PathVariable Long memberId) {
        return ApiResponse.ok(groupService.getMemberGroups(memberId));
    }
}
