package com.corum.backend.service.menu;

import com.corum.backend.domain.menu.MemberFavoriteMenu;
import com.corum.backend.domain.menu.MemberFavoriteMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberFavoriteMenuService {

    private final MemberFavoriteMenuRepository favoriteMenuRepository;

    @Transactional(readOnly = true)
    public List<Long> getFavoriteMenuIds(Long memberId) {
        return favoriteMenuRepository.findMenuIdsByMemberId(memberId);
    }

    @Transactional
    public void addFavorite(Long memberId, Long menuId) {
        if (favoriteMenuRepository.existsByMemberIdAndMenuId(memberId, menuId)) return;
        favoriteMenuRepository.save(MemberFavoriteMenu.builder()
                .memberId(memberId)
                .menuId(menuId)
                .build());
    }

    @Transactional
    public void removeFavorite(Long memberId, Long menuId) {
        favoriteMenuRepository.deleteByMemberIdAndMenuId(memberId, menuId);
    }
}
