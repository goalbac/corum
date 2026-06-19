package com.corum.backend.domain.inquiry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    Page<Inquiry> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Inquiry> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<Inquiry> findByInquiryTypeOrderByCreatedAtDesc(String inquiryType, Pageable pageable);

    Page<Inquiry> findByStatusAndInquiryTypeOrderByCreatedAtDesc(String status, String inquiryType, Pageable pageable);

    java.util.List<Inquiry> findByMemberIdAndInquiryTypeOrderByCreatedAtDesc(Long memberId, String inquiryType);

    java.util.List<Inquiry> findByMemberIdAndInquiryTypeInOrderByCreatedAtDesc(Long memberId, java.util.List<String> types);

    long countByStatus(String status);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
