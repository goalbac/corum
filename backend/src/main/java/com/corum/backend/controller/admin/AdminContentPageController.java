package com.corum.backend.controller.admin;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.dto.content.ContentPageHistoryResponse;
import com.corum.backend.dto.content.ContentPageRequest;
import com.corum.backend.dto.content.ContentPageResponse;
import com.corum.backend.security.CustomUserDetails;
import com.corum.backend.service.content.ContentPageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/content-pages")
public class AdminContentPageController {

    private final ContentPageService contentPageService;

    @GetMapping
    public ApiResponse<List<ContentPageResponse>> getPages() {
        return ApiResponse.ok(contentPageService.getPages());
    }

    @GetMapping("/menus/{menuId}")
    public ApiResponse<ContentPageResponse> getByMenu(@PathVariable Long menuId) {
        return ApiResponse.ok(contentPageService.getByMenuId(menuId));
    }

    @PutMapping("/menus/{menuId}")
    public ApiResponse<ContentPageResponse> saveByMenu(
            @PathVariable Long menuId,
            @Valid @RequestBody ContentPageRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        return ApiResponse.ok("안내 페이지가 저장되었습니다.", contentPageService.saveByMenuId(menuId, request, memberId));
    }

    @GetMapping("/{contentPageId}/histories")
    public ApiResponse<List<ContentPageHistoryResponse>> getHistories(@PathVariable Long contentPageId) {
        return ApiResponse.ok(contentPageService.getHistories(contentPageId));
    }

    @PostMapping("/{contentPageId}/histories/{historyId}/restore")
    public ApiResponse<ContentPageResponse> restore(
            @PathVariable Long contentPageId,
            @PathVariable Long historyId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        return ApiResponse.ok("선택한 이력으로 복원되었습니다.", contentPageService.restore(contentPageId, historyId, memberId));
    }
}
