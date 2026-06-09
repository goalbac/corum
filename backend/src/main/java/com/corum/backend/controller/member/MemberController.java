package com.corum.backend.controller.member;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.group.Group;
import com.corum.backend.domain.group.GroupRepository;
import com.corum.backend.domain.group.MemberGroupRepository;
import com.corum.backend.domain.member.Member;
import com.corum.backend.domain.member.MemberRepository;
import com.corum.backend.dto.member.MemberPublicResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberRepository memberRepository;
    private final MemberGroupRepository memberGroupRepository;
    private final GroupRepository groupRepository;

    // 회원 검색 (이름 or 아이디, 쪽지 수신자 선택용)
    @GetMapping("/search")
    public ApiResponse<List<MemberPublicResponse>> search(
            @RequestParam(defaultValue = "") String q) {
        if (q.isBlank()) return ApiResponse.ok(List.of());
        List<Member> members = memberRepository.searchActive(q, PageRequest.of(0, 20));
        List<MemberPublicResponse> result = members.stream()
                .map(m -> new MemberPublicResponse(m, List.of()))
                .collect(Collectors.toList());
        return ApiResponse.ok(result);
    }

    // 회원 공개 프로필
    @GetMapping("/{id}/public")
    public ApiResponse<MemberPublicResponse> getPublicProfile(@PathVariable Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("회원을 찾을 수 없습니다."));

        List<Long> groupIds = memberGroupRepository.findGroupIdsByMemberId(id);
        List<Group> groups = groupIds.isEmpty() ? List.of() : groupRepository.findAllById(groupIds);

        return ApiResponse.ok(new MemberPublicResponse(member, groups));
    }
}
