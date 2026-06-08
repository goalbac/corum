package com.corum.backend.controller.admin;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.member.AdminMemberMemo;
import com.corum.backend.domain.member.AdminMemberMemoRepository;
import com.corum.backend.domain.member.Member;
import com.corum.backend.domain.member.MemberRepository;
import com.corum.backend.security.CustomUserDetails;
import com.corum.backend.service.auth.TokenSessionService;
import com.corum.backend.service.log.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberRepository memberRepository;
    private final AdminMemberMemoRepository adminMemberMemoRepository;
    private final TokenSessionService tokenSessionService;
    private final OperationLogService operationLogService;

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
            return pdf.getBytes(StandardCharsets.ISO_8859_1);
        }

        private static String escape(String value) {
            return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
        }
    }
}
