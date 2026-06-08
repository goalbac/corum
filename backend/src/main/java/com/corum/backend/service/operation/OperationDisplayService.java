package com.corum.backend.service.operation;

import com.corum.backend.common.BusinessException;
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

@Service
@RequiredArgsConstructor
public class OperationDisplayService {

    private final PopupRepository popupRepository;
    private final PopupTargetPageRepository popupTargetPageRepository;
    private final BannerRepository bannerRepository;

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
                .map(p -> new PopupResponse(p, popupTargetPageRepository.findByPopupId(p.getId())))
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
                .map(BannerResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PopupResponse> getPopups() {
        return popupRepository.findAllByOrderByPriorityAscCreatedAtDesc().stream()
                .map(popup -> new PopupResponse(popup, popupTargetPageRepository.findByPopupId(popup.getId())))
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
        return new PopupResponse(popup, popupTargetPageRepository.findByPopupId(popup.getId()));
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
        return new PopupResponse(popup, popupTargetPageRepository.findByPopupId(id));
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
        return bannerRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(BannerResponse::new)
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
        return new BannerResponse(banner);
    }

    @Transactional
    public BannerResponse updateBanner(Long id, BannerRequest request) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("배너를 찾을 수 없습니다."));
        banner.update(
                request.getTitle(), request.getContent(), request.getLinkUrl(), request.getLinkNewWindow(),
                request.getStartAt(), request.getEndAt(), request.getIsActive()
        );
        return new BannerResponse(banner);
    }

    @Transactional
    public void deleteBanner(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("배너를 찾을 수 없습니다."));
        bannerRepository.delete(banner);
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
