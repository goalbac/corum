package com.corum.backend.controller.admin;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.group.GroupRepository;
import com.corum.backend.domain.group.MemberGroup;
import com.corum.backend.domain.group.MemberGroupRepository;
import com.corum.backend.domain.member.AdminMemberMemo;
import com.corum.backend.domain.member.AdminMemberMemoRepository;
import com.corum.backend.domain.member.Member;
import com.corum.backend.domain.member.MemberRepository;
import com.corum.backend.dto.group.GroupResponse;
import com.corum.backend.dto.member.AdminMemberCreateRequest;
import com.corum.backend.security.CustomUserDetails;
import com.corum.backend.service.auth.TokenSessionService;
import com.corum.backend.service.log.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberRepository memberRepository;
    private final AdminMemberMemoRepository adminMemberMemoRepository;
    private final MemberGroupRepository memberGroupRepository;
    private final GroupRepository groupRepository;
    private final TokenSessionService tokenSessionService;
    private final OperationLogService operationLogService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ApiResponse<Page<Member>> getMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Member> result;
        if (keyword != null && !keyword.isBlank()) {
            result = memberRepository.findByNameContainingOrEmailContaining(keyword, keyword, pageRequest);
        } else {
            result = memberRepository.findAll(pageRequest);
        }
        return ApiResponse.ok(result);
    }

    @PostMapping
    @Transactional
    public ApiResponse<Map<String, Object>> createMember(@Valid @RequestBody AdminMemberCreateRequest createRequest,
                                                         @AuthenticationPrincipal CustomUserDetails userDetails,
                                                         HttpServletRequest httpRequest) {
        String username = createRequest.getUsername().trim();
        String email = createRequest.getEmail().trim();
        String name = createRequest.getName().trim();

        if (memberRepository.existsByUsername(username)) {
            throw new BusinessException("이미 사용 중인 아이디입니다.");
        }
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException("이미 사용 중인 이메일입니다.");
        }

        Member member = Member.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(createRequest.getPassword()))
                .name(name)
                .gender(blankToNull(createRequest.getGender()))
                .phone(blankToNull(createRequest.getPhone()))
                .address(blankToNull(createRequest.getAddress()))
                .birthDate(createRequest.getBirthDate())
                .homePhone(blankToNull(createRequest.getHomePhone()))
                .occupation(blankToNull(createRequest.getOccupation()))
                .workPhone(blankToNull(createRequest.getWorkPhone()))
                .newsletterYn(Boolean.TRUE.equals(createRequest.getNewsletterYn()))
                .isActive(createRequest.getIsActive() == null || Boolean.TRUE.equals(createRequest.getIsActive()))
                .build();

        Member saved = memberRepository.save(member);
        assignGroups(saved.getId(), createRequest.getGroupIds(), userDetails.getMemberId());
        operationLogService.audit(userDetails.getMemberId(), "CREATE", "members", saved.getId(), null, saved.getUsername(), httpRequest);

        return ApiResponse.ok("회원이 추가되었습니다.", toDetailResponse(saved));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getMember(@PathVariable Long id) {
        return ApiResponse.ok(toDetailResponse(findMember(id)));
    }

    @PutMapping("/{id}/unlock")
    @Transactional
    public ApiResponse<Void> unlockMember(@PathVariable Long id,
                                          @AuthenticationPrincipal CustomUserDetails userDetails,
                                          HttpServletRequest request) {
        Member member = findMember(id);
        member.unlock();
        memberRepository.save(member);
        operationLogService.audit(userDetails.getMemberId(), "UPDATE", "members", id, "locked", "unlocked", request);
        return ApiResponse.ok("계정 잠금을 해제했습니다.");
    }

    @PatchMapping("/{id}/lock")
    public ApiResponse<Void> toggleLock(@PathVariable Long id,
                                        @RequestBody Map<String, Boolean> body,
                                        @AuthenticationPrincipal CustomUserDetails userDetails,
                                        HttpServletRequest request) {
        Member member = findMember(id);
        boolean locked = Boolean.TRUE.equals(body.get("locked"));
        if (locked) {
            member.lock(LocalDateTime.now());
            tokenSessionService.invalidateMember(id);
        } else {
            member.unlock();
        }
        memberRepository.save(member);
        operationLogService.audit(userDetails.getMemberId(), "UPDATE", "members", id, null,
                locked ? "locked" : "unlocked", request);
        return ApiResponse.ok("처리되었습니다.");
    }

    @PostMapping("/{id}/force-logout")
    public ApiResponse<Void> forceLogout(@PathVariable Long id,
                                         @AuthenticationPrincipal CustomUserDetails userDetails,
                                         HttpServletRequest request) {
        findMember(id);
        tokenSessionService.invalidateMember(id);
        operationLogService.audit(userDetails.getMemberId(), "FORCE_LOGOUT", "members", id, null, null, request);
        return ApiResponse.ok("강제 로그아웃 처리되었습니다.");
    }

    @GetMapping("/{id}/memo")
    public ApiResponse<AdminMemberMemo> getMemo(@PathVariable Long id) {
        return ApiResponse.ok(adminMemberMemoRepository.findByMemberId(id).orElse(null));
    }

    @PutMapping("/{id}/memo")
    public ApiResponse<Void> saveMemo(@PathVariable Long id,
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

    @PostMapping("/{id}/memos")
    public ApiResponse<AdminMemberMemo> addMemo(@PathVariable Long id,
                                                @RequestBody Map<String, String> body,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        findMember(id);
        AdminMemberMemo memo = adminMemberMemoRepository.findByMemberId(id)
                .orElse(AdminMemberMemo.builder()
                        .memberId(id)
                        .createdBy(userDetails.getMemberId())
                        .build());
        memo.updateMemo(body.get("memo"), userDetails.getMemberId());
        return ApiResponse.ok(adminMemberMemoRepository.save(memo));
    }

    @PostMapping("/{id}/groups")
    public ApiResponse<Void> addGroup(@PathVariable Long id,
                                      @RequestBody Map<String, Long> body,
                                      @AuthenticationPrincipal CustomUserDetails userDetails,
                                      HttpServletRequest request) {
        findMember(id);
        Long groupId = body.get("groupId");
        if (groupId == null) {
            throw new BusinessException("그룹을 선택해주세요.");
        }
        if (!groupRepository.existsById(groupId)) {
            throw BusinessException.notFound("그룹을 찾을 수 없습니다.");
        }
        if (!memberGroupRepository.existsByMemberIdAndGroupId(id, groupId)) {
            memberGroupRepository.save(MemberGroup.builder()
                    .memberId(id)
                    .groupId(groupId)
                    .assignedBy(userDetails.getMemberId())
                    .build());
            operationLogService.audit(userDetails.getMemberId(), "UPDATE", "member_groups", id, null, String.valueOf(groupId), request);
        }
        return ApiResponse.ok("그룹을 추가했습니다.");
    }

    @DeleteMapping("/{id}/groups/{groupId}")
    public ApiResponse<Void> removeGroup(@PathVariable Long id,
                                         @PathVariable Long groupId,
                                         @AuthenticationPrincipal CustomUserDetails userDetails,
                                         HttpServletRequest request) {
        findMember(id);
        if (!memberGroupRepository.existsByMemberIdAndGroupId(id, groupId)) {
            throw BusinessException.notFound("부여된 그룹이 아닙니다.");
        }
        memberGroupRepository.deleteByMemberIdAndGroupId(id, groupId);
        operationLogService.audit(userDetails.getMemberId(), "UPDATE", "member_groups", id, String.valueOf(groupId), null, request);
        return ApiResponse.ok("그룹을 제거했습니다.");
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel() {
        List<Member> members = memberRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("ID,아이디,이름,이메일,연락처,활성,잠금,가입일,탈퇴일\n");
        for (Member member : members) {
            csv.append(member.getId()).append(',')
                    .append(csv(member.getUsername())).append(',')
                    .append(csv(member.getName())).append(',')
                    .append(csv(member.getEmail())).append(',')
                    .append(csv(member.getPhone())).append(',')
                    .append(Boolean.TRUE.equals(member.getIsActive()) ? "활성" : "비활성").append(',')
                    .append(Boolean.TRUE.equals(member.getIsLocked()) ? "잠금" : "").append(',')
                    .append(csv(String.valueOf(member.getJoinedAt()))).append(',')
                    .append(csv(member.getWithdrawnAt() == null ? "" : String.valueOf(member.getWithdrawnAt())))
                    .append('\n');
        }
        return download(csv.toString().getBytes(StandardCharsets.UTF_8), "members.csv", "text/csv;charset=UTF-8");
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf() {
        List<Member> members = memberRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        StringBuilder text = new StringBuilder();
        text.append("Corum Member Report\n");
        text.append("Total: ").append(members.size()).append("\n\n");
        members.stream().limit(60).forEach(member -> text
                .append(member.getId()).append(" / ")
                .append(safeAscii(member.getUsername())).append(" / ")
                .append(safeAscii(member.getEmail())).append(" / ")
                .append(Boolean.TRUE.equals(member.getIsActive()) ? "active" : "inactive")
                .append("\n"));
        return download(SimplePdf.create(text.toString()), "members.pdf", MediaType.APPLICATION_PDF_VALUE);
    }

    private Member findMember(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("회원을 찾을 수 없습니다."));
    }

    private void assignGroups(Long memberId, List<Long> groupIds, Long assignedBy) {
        if (groupIds == null || groupIds.isEmpty()) {
            return;
        }
        for (Long groupId : groupIds.stream().distinct().toList()) {
            if (groupId == null) {
                continue;
            }
            if (!groupRepository.existsById(groupId)) {
                throw BusinessException.notFound("그룹을 찾을 수 없습니다.");
            }
            memberGroupRepository.save(MemberGroup.builder()
                    .memberId(memberId)
                    .groupId(groupId)
                    .assignedBy(assignedBy)
                    .build());
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Map<String, Object> toDetailResponse(Member member) {
        List<Long> groupIds = memberGroupRepository.findGroupIdsByMemberId(member.getId());
        List<GroupResponse> groups = groupRepository.findAllById(groupIds).stream()
                .map(GroupResponse::new)
                .toList();
        List<AdminMemberMemo> memos = adminMemberMemoRepository.findByMemberId(member.getId())
                .map(List::of)
                .orElseGet(List::of);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", member.getId());
        response.put("username", member.getUsername());
        response.put("email", member.getEmail());
        response.put("name", member.getName());
        response.put("gender", member.getGender());
        response.put("phone", member.getPhone());
        response.put("address", member.getAddress());
        response.put("birthDate", member.getBirthDate());
        response.put("homePhone", member.getHomePhone());
        response.put("occupation", member.getOccupation());
        response.put("workPhone", member.getWorkPhone());
        response.put("newsletterYn", member.getNewsletterYn());
        response.put("profileImageUrl", member.getProfileImageUrl());
        response.put("isActive", member.getIsActive());
        response.put("isLocked", member.getIsLocked());
        response.put("loginFailCount", member.getLoginFailCount());
        response.put("lockedAt", member.getLockedAt());
        response.put("joinedAt", member.getJoinedAt());
        response.put("withdrawnAt", member.getWithdrawnAt());
        response.put("createdAt", member.getCreatedAt());
        response.put("updatedAt", member.getUpdatedAt());
        response.put("groups", groups);
        response.put("memos", memos);
        return response;
    }

    private ResponseEntity<byte[]> download(byte[] bytes, String filename, String contentType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(contentType))
                .body(bytes);
    }

    private String csv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String safeAscii(String value) {
        if (value == null) return "";
        return value.replaceAll("[^\\x20-\\x7E]", "?");
    }

    private static class SimplePdf {
        static byte[] create(String text) {
            String[] lines = text.split("\\n");
            StringBuilder content = new StringBuilder("BT /F1 11 Tf 50 780 Td ");
            int count = 0;
            for (String line : lines) {
                if (count++ > 0) content.append("0 -16 Td ");
                content.append('(').append(escape(line)).append(") Tj ");
                if (count >= 45) break;
            }
            content.append("ET");
            String[] objects = {
                    "<< /Type /Catalog /Pages 2 0 R >>",
                    "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                    "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
                    "<< /Length " + content.toString().getBytes(StandardCharsets.ISO_8859_1).length + " >> stream\n" + content + "\nendstream"
            };
            StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
            int[] offsets = new int[objects.length + 1];
            for (int i = 0; i < objects.length; i++) {
                offsets[i + 1] = pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length;
                pdf.append(i + 1).append(" 0 obj ").append(objects[i]).append(" endobj\n");
            }
            int xrefOffset = pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length;
            pdf.append("xref\n0 ").append(objects.length + 1).append("\n");
            pdf.append("0000000000 65535 f \n");
            for (int i = 1; i <= objects.length; i++) {
                pdf.append(String.format("%010d 00000 n \n", offsets[i]));
            }
            pdf.append("trailer << /Size ").append(objects.length + 1).append(" /Root 1 0 R >>\n");
            pdf.append("startxref\n").append(xrefOffset).append("\n%%EOF");
            return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
        }

        private static String escape(String value) {
            return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
        }
    }
}
