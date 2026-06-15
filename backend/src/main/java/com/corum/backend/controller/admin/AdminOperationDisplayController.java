package com.corum.backend.controller.admin;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.dto.operation.BannerRequest;
import com.corum.backend.dto.operation.BannerResponse;
import com.corum.backend.dto.operation.PopupRequest;
import com.corum.backend.dto.operation.PopupResponse;
import com.corum.backend.security.CustomUserDetails;
import com.corum.backend.service.file.FileStorageService;
import com.corum.backend.service.operation.OperationDisplayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/display")
public class AdminOperationDisplayController {

    private final OperationDisplayService operationDisplayService;
    private final FileStorageService fileStorageService;

    @PostMapping("/popups/image")
    public ApiResponse<Map<String, String>> uploadPopupImage(
            @RequestParam("file") MultipartFile file) {
        String url = fileStorageService.uploadPopupImage(file);
        return ApiResponse.ok(Map.of("url", url));
    }

    @GetMapping("/popups")
    public ApiResponse<List<PopupResponse>> getPopups() {
        return ApiResponse.ok(operationDisplayService.getPopups());
    }

    @PostMapping("/popups")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PopupResponse> createPopup(
            @Valid @RequestBody PopupRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long createdBy = userDetails != null ? userDetails.getMemberId() : null;
        return ApiResponse.ok(operationDisplayService.createPopup(request, createdBy));
    }

    @PutMapping("/popups/{id}")
    public ApiResponse<PopupResponse> updatePopup(@PathVariable Long id, @Valid @RequestBody PopupRequest request) {
        return ApiResponse.ok(operationDisplayService.updatePopup(id, request));
    }

    @DeleteMapping("/popups/{id}")
    public ApiResponse<Void> deletePopup(@PathVariable Long id) {
        operationDisplayService.deletePopup(id);
        return ApiResponse.ok("팝업이 삭제되었습니다.");
    }

    @GetMapping("/banners")
    public ApiResponse<List<BannerResponse>> getBanners() {
        return ApiResponse.ok(operationDisplayService.getBanners());
    }

    @PostMapping("/banners")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BannerResponse> createBanner(
            @Valid @RequestBody BannerRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long createdBy = userDetails != null ? userDetails.getMemberId() : null;
        return ApiResponse.ok(operationDisplayService.createBanner(request, createdBy));
    }

    @PutMapping("/banners/{id}")
    public ApiResponse<BannerResponse> updateBanner(@PathVariable Long id, @Valid @RequestBody BannerRequest request) {
        return ApiResponse.ok(operationDisplayService.updateBanner(id, request));
    }

    @DeleteMapping("/banners/{id}")
    public ApiResponse<Void> deleteBanner(@PathVariable Long id) {
        operationDisplayService.deleteBanner(id);
        return ApiResponse.ok("배너가 삭제되었습니다.");
    }
}
