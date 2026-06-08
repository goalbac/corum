package com.corum.backend.service.content;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.content.ContentPage;
import com.corum.backend.domain.content.ContentPageHistory;
import com.corum.backend.domain.content.ContentPageHistoryRepository;
import com.corum.backend.domain.content.ContentPageRepository;
import com.corum.backend.domain.member.MemberRepository;
import com.corum.backend.dto.content.ContentPageHistoryResponse;
import com.corum.backend.dto.content.ContentPageRequest;
import com.corum.backend.dto.content.ContentPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentPageService {

    private final ContentPageRepository contentPageRepository;
    private final ContentPageHistoryRepository historyRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<ContentPageResponse> getPages() {
        return contentPageRepository.findAll().stream()
                .map(ContentPageResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContentPageResponse getByMenuId(Long menuId) {
        ContentPage page = contentPageRepository.findByMenuId(menuId)
                .orElseThrow(() -> BusinessException.notFound("등록된 안내 페이지가 없습니다."));
        return new ContentPageResponse(page);
    }

    @Transactional
    public ContentPageResponse saveByMenuId(Long menuId, ContentPageRequest request, Long memberId) {
        ContentPage page = contentPageRepository.findByMenuId(menuId).orElse(null);
        if (page == null) {
            page = contentPageRepository.save(ContentPage.builder()
                    .menuId(menuId)
                    .title(request.getTitle())
                    .content(request.getContent())
                    .createdBy(memberId)
                    .updatedBy(memberId)
                    .build());
            saveHistory(page.getId(), page.getContent(), memberId);
            return new ContentPageResponse(page);
        }

        saveHistory(page.getId(), page.getContent(), memberId);
        page.update(request.getTitle(), request.getContent(), memberId);
        return new ContentPageResponse(page);
    }

    @Transactional(readOnly = true)
    public List<ContentPageHistoryResponse> getHistories(Long contentPageId) {
        List<ContentPageHistory> histories =
                historyRepository.findByContentPageIdOrderByCreatedAtDesc(contentPageId);

        // 수정자 ID → 이름 일괄 조회
        List<Long> memberIds = histories.stream()
                .map(ContentPageHistory::getCreatedBy)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> nameMap = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(m -> m.getId(), m -> m.getName()));

        return histories.stream()
                .map(h -> new ContentPageHistoryResponse(h, nameMap.getOrDefault(h.getCreatedBy(), "알 수 없음")))
                .toList();
    }

    @Transactional
    public ContentPageResponse restore(Long contentPageId, Long historyId, Long memberId) {
        ContentPage page = contentPageRepository.findById(contentPageId)
                .orElseThrow(() -> BusinessException.notFound("안내 페이지를 찾을 수 없습니다."));
        ContentPageHistory history = historyRepository.findById(historyId)
                .filter(h -> h.getContentPageId().equals(contentPageId))
                .orElseThrow(() -> BusinessException.notFound("복원할 이력을 찾을 수 없습니다."));

        saveHistory(page.getId(), page.getContent(), memberId);
        page.update(page.getTitle(), history.getContent(), memberId);
        return new ContentPageResponse(page);
    }

    private void saveHistory(Long contentPageId, String content, Long memberId) {
        historyRepository.save(ContentPageHistory.builder()
                .contentPageId(contentPageId)
                .content(content)
                .createdBy(memberId)
                .build());
    }
}
