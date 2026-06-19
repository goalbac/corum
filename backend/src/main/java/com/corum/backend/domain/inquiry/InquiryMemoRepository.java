package com.corum.backend.domain.inquiry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InquiryMemoRepository extends JpaRepository<InquiryMemo, Long> {

    List<InquiryMemo> findByInquiryIdOrderByCreatedAtAsc(Long inquiryId);

    void deleteByInquiryId(Long inquiryId);
}
