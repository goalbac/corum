package com.corum.backend.service.inquiry;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.group.MemberGroupRepository;
import com.corum.backend.domain.inquiry.Inquiry;
import com.corum.backend.domain.inquiry.InquiryMemo;
import com.corum.backend.domain.inquiry.InquiryMemoRepository;
import com.corum.backend.domain.inquiry.InquiryRepository;
import com.corum.backend.dto.inquiry.InquiryCreateRequest;
import com.corum.backend.dto.inquiry.InquiryResponse;
import com.corum.backend.service.mail.MailService;
import com.corum.backend.service.notification.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final InquiryMemoRepository inquiryMemoRepository;
    private final MailService mailService;
    private final MemberGroupRepository memberGroupRepository;
    private final NotificationService notificationService;

    // ===== 문의 접수 (비로그인 가능) =====
    @Transactional
    public InquiryResponse create(InquiryCreateRequest request, Long memberId,
                                   HttpServletRequest httpRequest) {
        String ip = httpRequest.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = httpRequest.getRemoteAddr();

        Inquiry inquiry = Inquiry.builder()
                .memberId(memberId)
                .writerName(request.getWriterName())
                .title(request.getTitle())
                .content(request.getContent())
                .contactPhone(request.getContactPhone())
                .contactEmail(request.getContactEmail())
                .clientIp(ip)
                .build();

        Inquiry saved = inquiryRepository.save(inquiry);
        try {
            mailService.sendToAdmin("[Corum] 새 문의가 접수되었습니다",
                    "<p><strong>" + saved.getTitle() + "</strong></p><p>" + saved.getContent() + "</p>",
                    "INQUIRY_NOTIFY");
        } catch (Exception ignored) { }
        // 관리자 그룹 회원들에게 알림
        try {
            List<Long> adminIds = memberGroupRepository.findAdminMemberIds();
            notificationService.createForMembers(
                    adminIds, "INQUIRY",
                    "새 문의: " + truncate(saved.getTitle(), 30),
                    (saved.getWriterName() != null ? saved.getWriterName() : "익명") + "님의 문의가 접수되었습니다.",
                    "/admin/inquiries"
            );
        } catch (Exception ignored) { }
        return new InquiryResponse(saved, List.of());
    }

    // ===== 문의 목록 (관리자) =====
    @Transactional(readOnly = true)
    public Page<InquiryResponse> getList(String status, Pageable pageable) {
        Page<Inquiry> page = status != null
                ? inquiryRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : inquiryRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<InquiryResponse> content = page.getContent().stream()
                .map(i -> new InquiryResponse(i,
                        inquiryMemoRepository.findByInquiryIdOrderByCreatedAtAsc(i.getId())))
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    // ===== 문의 상세 (관리자) =====
    @Transactional(readOnly = true)
    public InquiryResponse getDetail(Long id) {
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("문의를 찾을 수 없습니다."));
        List<InquiryMemo> memos = inquiryMemoRepository.findByInquiryIdOrderByCreatedAtAsc(id);
        return new InquiryResponse(inquiry, memos);
    }

    // ===== 상태 변경 (관리자) =====
    @Transactional
    public InquiryResponse updateStatus(Long id, String status) {
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("문의를 찾을 수 없습니다."));
        inquiry.updateStatus(status);
        List<InquiryMemo> memos = inquiryMemoRepository.findByInquiryIdOrderByCreatedAtAsc(id);
        return new InquiryResponse(inquiry, memos);
    }

    // ===== 메모 추가 (관리자) =====
    @Transactional
    public void addMemo(Long inquiryId, String memo, Long createdBy) {
        if (!inquiryRepository.existsById(inquiryId)) {
            throw BusinessException.notFound("문의를 찾을 수 없습니다.");
        }
        inquiryMemoRepository.save(InquiryMemo.builder()
                .inquiryId(inquiryId)
                .createdBy(createdBy)
                .memo(memo)
                .build());
    }

    // ===== 메모 삭제 (관리자) =====
    @Transactional
    public void deleteMemo(Long memoId) {
        inquiryMemoRepository.deleteById(memoId);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
