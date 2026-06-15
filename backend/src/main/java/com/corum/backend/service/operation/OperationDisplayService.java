package com.corum.backend.service.operation;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.member.MemberRepository;
import com.corum.backend.domain.operation.Banner;
import com.corum.backend.domain.operation.BannerRepository;
import com.corum.backend.domain.operation.Popup;
import com.corum.backend.domain.operation.PopupRepository;
import com.corum.backend.domain.operation.PopupTargetPage;
import com.corum.backend.domain.operation.PopupTargetPageRepository;
import com.corum.backend.dto.operation.BannerRequest;
import com.corum.backend.dto.operation.BannerResponse;
import com.corum.backend.dto.operation.PopupRequest;
import com.corum.backend.dto.operation.PopupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OperationDisplayService {

    private final PopupRepository popupRepository;
    private final PopupTargetPageRepository popupTargetPageRepository;
    private final BannerRepository bannerRepository;
    private final MemberRepository memberRepository;

    // ===== 공개 API — 현재 활성 팝업 (기간 유효 + isActive) =====
    @Transactional(readOnly = true)
    public List<PopupResponse> getActivePopups(Long menuId) {
        LocalDateTime now = LocalDateTime.now();
        return popupRepository.findAllByOrderByPriorityAscCreatedAtDesc().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .filter(p -> p.getStartAt() == null || !p.getStartAt().isAfter(now))
                .filter(p -> p.getEndAt() == null || !p.getEndAt().isBefore(now))
                .filter(p -> {
                    List<PopupTargetPage> targets = popupTargetPageRepository.findByPopupId(p.getId());
                    String targetType = targets.stream().findFirst().map(PopupTargetPage::getTargetType).orElse("ALL");
                    if ("ALL".equals(targetType)) return true;
                    if (menuId == null) return false;
                    return targets.stream().anyMatch(t -> menuId.equals(t.getTargetMenuId()));
                })
                .map(p -> new PopupResponse(p, popupTargetPageRepository.findByPopupId(p.getId()), null))
                .toList();
    }

    // ===== 공개 API — 현재 활성 배너 =====
    @Transactional(readOnly = true)
    public List<BannerResponse> getActiveBanners() {
        LocalDateTime now = LocalDateTime.now();
        return bannerRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
                .filter(b -> b.getStartAt() == null || !b.getStartAt().isAfter(now))
                .filter(b -> b.getEndAt() == null || !b.getEndAt().isBefore(now))
                .map(b -> new BannerResponse(b, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PopupResponse> getPopups() {
        List<Popup> popups = popupRepository.findAllByOrderByCreatedAtDesc();
        Map<Long, String> nameMap = resolveMemberNames(
                popups.stream().map(Popup::getCreatedBy).collect(Collectors.toSet()));
        return popups.stream()
                .map(p -> new PopupResponse(p, popupTargetPageRepository.findByPopupId(p.getId()),
                        nameMap.get(p.getCreatedBy())))
                .toList();
    }

    @Transactional
    public PopupResponse createPopup(PopupRequest request, Long createdBy) {
        Popup popup = popupRepository.save(Popup.builder()
                .title(request.getTitle())
                .contentType(request.getContentType())
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .linkUrl(request.getLinkUrl())
                .linkNewWindow(request.getLinkNewWindow())
                .position(request.getPosition())
                .priority(request.getPriority())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .isActive(request.getIsActive())
                .createdBy(createdBy)
                .build());
        savePopupTargets(popup.getId(), request);
        String name = popup.getCreatedBy() != null
                ? memberRepository.findById(popup.getCreatedBy()).map(m -> m.getName()).orElse(null) : null;
        return new PopupResponse(popup, popupTargetPageRepository.findByPopupId(popup.getId()), name);
    }

    @Transactional
    public PopupResponse updatePopup(Long id, PopupRequest request) {
        Popup popup = popupRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("팝업을 찾을 수 없습니다."));
        popup.update(
                request.getTitle(), request.getContentType(), request.getContent(), request.getImageUrl(),
                request.getLinkUrl(), request.getLinkNewWindow(), request.getPosition(), request.getPriority(),
                request.getStartAt(), request.getEndAt(), request.getIsActive()
        );
        popupTargetPageRepository.deleteByPopupId(id);
        savePopupTargets(id, request);
        String name = popup.getCreatedBy() != null
                ? memberRepository.findById(popup.getCreatedBy()).map(m -> m.getName()).orElse(null) : null;
        return new PopupResponse(popup, popupTargetPageRepository.findByPopupId(id), name);
    }

    @Transactional
    public void deletePopup(Long id) {
        Popup popup = popupRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("팝업을 찾을 수 없습니다."));
        popupTargetPageRepository.deleteByPopupId(id);
        popupRepository.delete(popup);
    }

    @Transactional(readOnly = true)
    public List<BannerResponse> getBanners() {
        List<Banner> banners = bannerRepository.findAllByOrderByCreatedAtDesc();
        Map<Long, String> nameMap = resolveMemberNames(
                banners.stream().map(Banner::getCreatedBy).collect(Collectors.toSet()));
        return banners.stream()
                .map(b -> new BannerResponse(b, nameMap.get(b.getCreatedBy())))
                .toList();
    }

    @Transactional
    public BannerResponse createBanner(BannerRequest request, Long createdBy) {
        Banner banner = bannerRepository.save(Banner.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .linkUrl(request.getLinkUrl())
                .linkNewWindow(request.getLinkNewWindow())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .isActive(request.getIsActive())
                .createdBy(createdBy)
                .build());
        String name = createdBy != null
                ? memberRepository.findById(createdBy).map(m -> m.getName()).orElse(null) : null;
        return new BannerResponse(banner, name);
    }

    @Transactional
    public BannerResponse updateBanner(Long id, BannerRequest request) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("배너를 찾을 수 없습니다."));
        banner.update(
                request.getTitle(), request.getContent(), request.getLinkUrl(), request.getLinkNewWindow(),
                request.getStartAt(), request.getEndAt(), request.getIsActive()
        );
        String name = banner.getCreatedBy() != null
                ? memberRepository.findById(banner.getCreatedBy()).map(m -> m.getName()).orElse(null) : null;
        return new BannerResponse(banner, name);
    }

    @Transactional
    public void deleteBanner(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("배너를 찾을 수 없습니다."));
        bannerRepository.delete(banner);
    }

    private Map<Long, String> resolveMemberNames(Set<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) return Map.of();
        return memberIds.stream()
                .filter(id -> id != null)
                .collect(Collectors.toMap(
                        id -> id,
                        id -> memberRepository.findById(id).map(m -> m.getName()).orElse("알 수 없음")
                ));
    }

    private void savePopupTargets(Long popupId, PopupRequest request) {
        if ("MENU".equals(request.getTargetType()) && request.getTargetMenuIds() != null && !request.getTargetMenuIds().isEmpty()) {
            request.getTargetMenuIds().forEach(menuId -> popupTargetPageRepository.save(PopupTargetPage.builder()
                    .popupId(popupId)
                    .targetType("MENU")
                    .targetMenuId(menuId)
                    .build()));
            return;
        }
        popupTargetPageRepository.save(PopupTargetPage.builder()
                .popupId(popupId)
                .targetType("ALL")
                .build());
    }
}
