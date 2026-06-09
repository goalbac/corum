package com.corum.backend.domain.message;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // 내가 보낸 메시지 전체 (공지 제외) - 대화 목록 구성용
    List<Message> findBySenderIdAndIsNoticeFalseOrderByCreatedAtDesc(Long senderId);
}
