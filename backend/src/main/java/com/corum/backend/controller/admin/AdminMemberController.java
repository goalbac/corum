package com.corum.backend.controller.admin;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.member.Member;
import com.corum.backend.domain.member.MemberRepository;
import com.corum.backend.domain.member.AdminMemberMemo;
import com.corum.backend.domain.member.AdminMemberMemoRepository;
import com.corum.backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberRepository memberRepository;
    private final AdminMemberMemoRepository adminMemberMemoRepository;

    // 회원 목록
    @GetMapping
    public ApiResponse<Page<Member>> getMembers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String keyword) {

        Page<Member> result;
        if (keyword != null && !keyword.isBlank()) {
            result = memberRepository.findByNameContainingOrEmailContaining(
                    keyword, keyword, PageRequest.of(page, size));
        } else {
            result = memberRepository.findAll(PageRequest.of(page, size));
        }
        return ApiResponse.ok(result);
    }

    // 계정 잠금/해제
    @PatchMapping("/{id}/lock")
    public ApiResponse<Void> toggleLock(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("회원을 찾을 수 없습니다."));
        if (Boolean.TRUE.equals(body.get("locked"))) {
            member.lock(LocalDateTime.now());
        } else {
            member.unlock();
        }
        memberRepository.save(member);
        return ApiResponse.ok("처리되었습니다.");
    }

    // 관리자 메모 조회
    @GetMapping("/{id}/memo")
    public ApiResponse<AdminMemberMemo> getMemo(@PathVariable Long id) {
        return ApiResponse.ok(
            adminMemberMemoRepository.findByMemberId(id).orElse(null)
        );
    }

    // 관리자 메모 저장
    @PutMapping("/{id}/memo")
    public ApiResponse<Void> saveMemo(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        AdminMemberMemo memo = adminMemberMemoRepository.findByMemberId(id)
                .orElse(AdminMemberMemo.builder()
                        .memberId(id)
                        .createdBy(userDetails.getMemberId())
                        .build());
        memo.updateMemo(body.get("memo"), userDetails.getMemberId());
        adminMemberMemoRepository.save(memo);
        return ApiResponse.ok("메모가 저장되었습니다.");
    }
}
