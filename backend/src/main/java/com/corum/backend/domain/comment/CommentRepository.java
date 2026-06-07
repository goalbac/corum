package com.corum.backend.domain.comment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    int countByPostIdAndIsDeletedFalse(Long postId);

    // 대댓글 존재 여부 (삭제 시 소프트 삭제 결정용)
    boolean existsByParentIdAndIsDeletedFalse(Long parentId);
}
