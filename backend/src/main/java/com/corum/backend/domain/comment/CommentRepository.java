package com.corum.backend.domain.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    int countByPostIdAndIsDeletedFalse(Long postId);

    // 대댓글 존재 여부 (삭제 시 소프트 삭제 결정용)
    boolean existsByParentIdAndIsDeletedFalse(Long parentId);

    // 관리자: 최근 댓글 전체 (페이지)
    Page<Comment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 관리자: created_at 수정
    @Modifying
    @Query(value = "UPDATE comments SET created_at = :createdAt WHERE id = :id", nativeQuery = true)
    void updateCreatedAtById(@Param("id") Long id, @Param("createdAt") LocalDateTime createdAt);

    long countByCreatedAtBetweenAndIsDeletedFalse(LocalDateTime from, LocalDateTime to);
}
