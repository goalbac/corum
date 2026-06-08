package com.corum.backend.service.terms;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.terms.MemberTermsAgreement;
import com.corum.backend.domain.terms.MemberTermsAgreementRepository;
import com.corum.backend.domain.terms.Terms;
import com.corum.backend.domain.terms.TermsRepository;
import com.corum.backend.dto.terms.TermsAgreementStatusResponse;
import com.corum.backend.dto.terms.TermsRequest;
import com.corum.backend.dto.terms.TermsResponse;
import com.corum.backend.service.log.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TermsService {

    private final TermsRepository termsRepository;
    private final MemberTermsAgreementRepository agreementRepository;
    private final OperationLogService operationLogService;

    @Transactional(readOnly = true)
    public List<TermsResponse> getAll() {
        return termsRepository.findAllByOrderByTypeAscVersionDesc()
                .stream()
                .map(TermsResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TermsResponse> getActiveTerms() {
        return termsRepository.findByIsActiveTrueOrderByTypeAsc()
                .stream()
                .map(TermsResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public TermsAgreementStatusResponse getAgreementStatus(Long memberId) {
        return new TermsAgreementStatusResponse(getRequiredTerms(memberId));
    }

    @Transactional(readOnly = true)
    public List<TermsResponse> getRequiredTerms(Long memberId) {
        List<Terms> activeTerms = termsRepository.findByIsActiveTrueOrderByTypeAsc();
        if (activeTerms.isEmpty()) {
            return List.of();
        }

        List<Long> activeIds = activeTerms.stream().map(Terms::getId).toList();
        Set<Long> agreedIds = new HashSet<>(agreementRepository.findByMemberIdAndTermsIdIn(memberId, activeIds)
                .stream()
                .map(MemberTermsAgreement::getTermsId)
                .toList());

        return activeTerms.stream()
                .filter(terms -> !agreedIds.contains(terms.getId()))
                .map(TermsResponse::new)
                .toList();
    }

    @Transactional
    public TermsResponse createVersion(TermsRequest request, Long adminId, HttpServletRequest httpRequest) {
        int nextVersion = termsRepository.findTopByTypeOrderByVersionDesc(request.getType())
                .map(Terms::getVersion)
                .orElse(0) + 1;

        if (Boolean.TRUE.equals(request.getIsActive())) {
            termsRepository.findByTypeAndIsActiveTrueOrderByVersionDesc(request.getType())
                    .forEach(Terms::deactivate);
        }

        Terms terms = termsRepository.save(Terms.builder()
                .type(request.getType())
                .version(nextVersion)
                .content(request.getContent())
                .isActive(Boolean.TRUE.equals(request.getIsActive()))
                .requireReagree(Boolean.TRUE.equals(request.getRequireReagree()))
                .build());
        operationLogService.audit(adminId, "CREATE", "terms", terms.getId(), null,
                terms.getType() + " v" + terms.getVersion(), httpRequest);
        return new TermsResponse(terms);
    }

    @Transactional
    public TermsResponse update(Long id, TermsRequest request, Long adminId, HttpServletRequest httpRequest) {
        Terms terms = termsRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("약관을 찾을 수 없습니다."));
        if (Boolean.TRUE.equals(request.getIsActive())) {
            termsRepository.findByTypeAndIsActiveTrueOrderByVersionDesc(terms.getType()).stream()
                    .filter(active -> !active.getId().equals(id))
                    .forEach(Terms::deactivate);
        }
        terms.update(request.getContent(), Boolean.TRUE.equals(request.getIsActive()),
                Boolean.TRUE.equals(request.getRequireReagree()));
        operationLogService.audit(adminId, "UPDATE", "terms", id, null,
                terms.getType() + " v" + terms.getVersion(), httpRequest);
        return new TermsResponse(terms);
    }

    @Transactional
    public TermsResponse activate(Long id, Long adminId, HttpServletRequest httpRequest) {
        Terms terms = termsRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("약관을 찾을 수 없습니다."));
        termsRepository.findByTypeAndIsActiveTrueOrderByVersionDesc(terms.getType()).stream()
                .filter(active -> !active.getId().equals(id))
                .forEach(Terms::deactivate);
        terms.update(terms.getContent(), true, terms.getRequireReagree());
        operationLogService.audit(adminId, "UPDATE", "terms", id, null, "activate", httpRequest);
        return new TermsResponse(terms);
    }

    @Transactional
    public void agree(Long memberId, List<Long> termsIds, HttpServletRequest httpRequest) {
        List<Terms> activeTerms = termsRepository.findByIsActiveTrueOrderByTypeAsc();
        Set<Long> requestedIds = new HashSet<>(termsIds);
        List<Long> missingIds = activeTerms.stream()
                .map(Terms::getId)
                .filter(id -> !requestedIds.contains(id))
                .toList();
        if (!missingIds.isEmpty()) {
            throw new BusinessException("현재 활성 약관에 모두 동의해야 합니다.");
        }

        String ip = operationLogService.getClientIp(httpRequest);
        for (Terms terms : activeTerms) {
            if (!agreementRepository.existsByMemberIdAndTermsId(memberId, terms.getId())) {
                agreementRepository.save(MemberTermsAgreement.builder()
                        .memberId(memberId)
                        .termsId(terms.getId())
                        .agreedIp(ip)
                        .build());
            }
        }
        operationLogService.audit(memberId, "AGREE", "terms", null, null, requestedIds.toString(), httpRequest);
    }
}
