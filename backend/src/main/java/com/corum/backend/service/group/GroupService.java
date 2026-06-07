package com.corum.backend.service.group;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.group.Group;
import com.corum.backend.domain.group.GroupRepository;
import com.corum.backend.domain.group.MemberGroup;
import com.corum.backend.domain.group.MemberGroupRepository;
import com.corum.backend.domain.member.MemberRepository;
import com.corum.backend.dto.group.GroupCreateRequest;
import com.corum.backend.dto.group.GroupResponse;
import com.corum.backend.dto.group.GroupUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final MemberGroupRepository memberGroupRepository;
    private final MemberRepository memberRepository;

    // ===== 그룹 트리 조회 =====
    @Transactional(readOnly = true)
    public List<GroupResponse> getGroupTree() {
        List<Group> all = groupRepository.findAllByOrderBySortOrderAsc();

        // GroupResponse 변환
        Map<Long, GroupResponse> map = all.stream()
                .collect(Collectors.toMap(Group::getId, GroupResponse::new));

        // 트리 구조 구성
        List<GroupResponse> roots = all.stream()
                .filter(g -> g.getParentId() == null)
                .map(g -> map.get(g.getId()))
                .collect(Collectors.toList());

        all.stream()
                .filter(g -> g.getParentId() != null)
                .forEach(g -> {
                    GroupResponse parent = map.get(g.getParentId());
                    if (parent != null) {
                        parent.addChild(map.get(g.getId()));
                    }
                });

        return roots;
    }

    // ===== 그룹 단건 조회 =====
    @Transactional(readOnly = true)
    public GroupResponse getGroup(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("그룹을 찾을 수 없습니다."));
        return new GroupResponse(group);
    }

    // ===== 그룹 생성 =====
    @Transactional
    public GroupResponse createGroup(GroupCreateRequest request, Long createdBy) {
        // 상위 그룹 존재 확인
        Group parent = groupRepository.findById(request.getParentId())
                .orElseThrow(() -> BusinessException.notFound("상위 그룹을 찾을 수 없습니다."));

        Group group = Group.builder()
                .parentId(parent.getId())
                .name(request.getName())
                .description(request.getDescription())
                .type(parent.getType())   // 상위 그룹 타입 상속
                .sortOrder(request.getSortOrder())
                .isSystem(false)
                .build();

        return new GroupResponse(groupRepository.save(group));
    }

    // ===== 그룹 수정 =====
    @Transactional
    public GroupResponse updateGroup(Long id, GroupUpdateRequest request) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("그룹을 찾을 수 없습니다."));

        group.update(request.getName(), request.getDescription(), request.getSortOrder());
        return new GroupResponse(group);
    }

    // ===== 그룹 삭제 =====
    @Transactional
    public void deleteGroup(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("그룹을 찾을 수 없습니다."));

        if (group.getIsSystem()) {
            throw new BusinessException("시스템 그룹은 삭제할 수 없습니다.");
        }

        // 하위 그룹 존재 확인
        List<Group> children = groupRepository.findByParentIdOrderBySortOrderAsc(id);
        if (!children.isEmpty()) {
            throw new BusinessException("하위 그룹이 있는 경우 삭제할 수 없습니다.");
        }

        // 해당 그룹에 속한 회원 존재 확인
        List<?> members = memberGroupRepository.findByGroupId(id);
        if (!members.isEmpty()) {
            throw new BusinessException("그룹에 속한 회원이 있는 경우 삭제할 수 없습니다.");
        }

        groupRepository.delete(group);
    }

    // ===== 회원에게 그룹 부여 =====
    @Transactional
    public void assignGroup(Long memberId, Long groupId, Long assignedBy) {
        // 회원 존재 확인
        if (!memberRepository.existsById(memberId)) {
            throw BusinessException.notFound("회원을 찾을 수 없습니다.");
        }

        // 그룹 존재 확인
        if (!groupRepository.existsById(groupId)) {
            throw BusinessException.notFound("그룹을 찾을 수 없습니다.");
        }

        // 이미 부여된 그룹인지 확인
        if (memberGroupRepository.existsByMemberIdAndGroupId(memberId, groupId)) {
            throw new BusinessException("이미 부여된 그룹입니다.");
        }

        memberGroupRepository.save(MemberGroup.builder()
                .memberId(memberId)
                .groupId(groupId)
                .assignedBy(assignedBy)
                .build());
    }

    // ===== 회원에서 그룹 회수 =====
    @Transactional
    public void revokeGroup(Long memberId, Long groupId) {
        if (!memberGroupRepository.existsByMemberIdAndGroupId(memberId, groupId)) {
            throw BusinessException.notFound("부여된 그룹이 아닙니다.");
        }
        memberGroupRepository.deleteByMemberIdAndGroupId(memberId, groupId);
    }

    // ===== 회원의 그룹 목록 조회 =====
    @Transactional(readOnly = true)
    public List<GroupResponse> getMemberGroups(Long memberId) {
        List<Long> groupIds = memberGroupRepository.findGroupIdsByMemberId(memberId);
        return groupRepository.findAllById(groupIds).stream()
                .map(GroupResponse::new)
                .collect(Collectors.toList());
    }

    // ===== 회원이 관리자 그룹인지 확인 =====
    @Transactional(readOnly = true)
    public boolean isAdminMember(Long memberId) {
        return memberGroupRepository.existsAdminGroupByMemberId(memberId);
    }
}
