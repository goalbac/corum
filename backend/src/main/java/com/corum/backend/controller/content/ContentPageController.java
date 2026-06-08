package com.corum.backend.controller.content;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.dto.content.ContentPageResponse;
import com.corum.backend.service.content.ContentPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/content-pages")
public class ContentPageController {

    private final ContentPageService contentPageService;

    @GetMapping("/menus/{menuId}")
    public ApiResponse<ContentPageResponse> getByMenu(@PathVariable Long menuId) {
        return ApiResponse.ok(contentPageService.getByMenuId(menuId));
    }
}
