package com.corum.backend.domain.terms;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MemberTermsAgreementRepository extends JpaRepository<MemberTermsAgreement, Long> {
    boolean existsByMemberIdAndTermsId(Long memberId, Long termsId);
    List<MemberTermsAgreement> findByMemberIdAndTermsIdIn(Long memberId, Collection<Long> termsIds);
}
