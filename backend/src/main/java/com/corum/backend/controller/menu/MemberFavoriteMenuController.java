package com.corum.backend.controller.menu;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.security.CustomUserDetails;
import com.corum.backend.service.menu.MemberFavoriteMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorite-menus")
@RequiredArgsConstructor
public class MemberFavoriteMenuController {

    private final MemberFavoriteMenuService favoriteMenuService;

    @GetMapping
    public ApiResponse<List<Long>> getFavorites(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(favoriteMenuService.getFavoriteMenuIds(userDetails.getMemberId()));
    }

    @PostMapping("/{menuId}")
    public ApiResponse<Void> addFavorite(
            @PathVariable Long menuId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        favoriteMenuService.addFavorite(userDetails.getMemberId(), menuId);
        return ApiResponse.ok("즐겨찾기에 추가되었습니다.");
    }

    @DeleteMapping("/{menuId}")
    public ApiResponse<Void> removeFavorite(
            @PathVariable Long menuId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        favoriteMenuService.removeFavorite(userDetails.getMemberId(), menuId);
        return ApiResponse.ok("즐겨찾기가 해제되었습니다.");
    }
}
