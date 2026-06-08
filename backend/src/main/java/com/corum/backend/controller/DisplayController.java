package com.corum.backend.controller;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.dto.operation.BannerResponse;
import com.corum.backend.dto.operation.PopupResponse;
import com.corum.backend.service.operation.OperationDisplayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/display")
public class DisplayController {

    private final OperationDisplayService operationDisplayService;

    @GetMapping("/popups/active")
    public ApiResponse<List<PopupResponse>> getActivePopups(
            @RequestParam(required = false) Long menuId) {
        return ApiResponse.ok(operationDisplayService.getActivePopups(menuId));
    }

    @GetMapping("/banners/active")
    public ApiResponse<List<BannerResponse>> getActiveBanners() {
        return ApiResponse.ok(operationDisplayService.getActiveBanners());
    }
}
