package com.corum.backend.controller.inquiry;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.dto.inquiry.InquiryCreateRequest;
import com.corum.backend.dto.inquiry.InquiryReplyRequest;
import com.corum.backend.dto.inquiry.InquiryResponse;
import com.corum.backend.security.CustomUserDetails;
import com.corum.backend.service.inquiry.InquiryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    // 문의 접수 (비로그인 가능)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InquiryResponse> create(
            @Valid @RequestBody InquiryCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest) {
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        return ApiResponse.ok(inquiryService.create(request, memberId, httpRequest));
    }

    // 문의 목록 (관리자)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ApiResponse<Page<InquiryResponse>> getList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String inquiryType,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(inquiryService.getList(status, inquiryType, PageRequest.of(page, size)));
    }

    // 문의 상세 (관리자)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ApiResponse<InquiryResponse> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(inquiryService.getDetail(id));
    }

    // 상태 변경 (관리자)
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ApiResponse<InquiryResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ApiResponse.ok(inquiryService.updateStatus(id, body.get("status")));
    }

    // 답변 등록/수정 (관리자)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/reply")
    public ApiResponse<InquiryResponse> reply(
            @PathVariable Long id,
            @Valid @RequestBody InquiryReplyRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(inquiryService.reply(id, request.getContent(), userDetails.getMemberId()));
    }

    // 메모 추가 (관리자)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/memos")
    public ApiResponse<Void> addMemo(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        inquiryService.addMemo(id, body.get("memo"), userDetails.getMemberId());
        return ApiResponse.ok("메모가 추가되었습니다.");
    }

    // 메모 삭제 (관리자)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/memos/{memoId}")
    public ApiResponse<Void> deleteMemo(@PathVariable Long memoId) {
        inquiryService.deleteMemo(memoId);
        return ApiResponse.ok("메모가 삭제되었습니다.");
    }

    // 문의/제보 삭제 (관리자)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        inquiryService.delete(id);
        return ApiResponse.ok("삭제되었습니다.");
    }

    // 내 제보/문의 목록 (로그인 사용자)
    @GetMapping("/my")
    public ApiResponse<List<InquiryResponse>> getMyList(
            @RequestParam(required = false) String inquiryType,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(inquiryService.getMyList(userDetails.getMemberId(), inquiryType));
    }
}
