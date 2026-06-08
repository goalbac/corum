package com.corum.backend.domain.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 게시판 글 목록 (공지 우선, 최신순)
    @Query("""
        SELECT p FROM Post p
        WHERE p.boardId = :boardId AND p.isHidden = false
        ORDER BY p.isNotice DESC, p.createdAt DESC
        """)
    Page<Post> findByBoardId(Long boardId, Pageable pageable);

    // 검색
    @Query("""
        SELECT p FROM Post p
        WHERE p.boardId = :boardId AND p.isHidden = false
        AND (
            (:searchType = 'title'   AND p.title LIKE %:keyword%) OR
            (:searchType = 'content' AND p.content LIKE %:keyword%) OR
            (:searchType = 'writer'  AND p.writerName LIKE %:keyword%) OR
            (:searchType = 'all'     AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%))
        )
        ORDER BY p.isNotice DESC, p.createdAt DESC
        """)
    Page<Post> search(Long boardId, String searchType, String keyword, Pageable pageable);

    // 공지글 목록
    List<Post> findByBoardIdAndIsNoticeTrueOrderByCreatedAtDesc(Long boardId);

    // 대시보드용 최신글
    List<Post> findTop5ByBoardIdAndIsHiddenFalseOrderByCreatedAtDesc(Long boardId);

    // 비공개 제외 게시글 수 (rowNum 계산용)
    long countByBoardIdAndIsHiddenFalse(Long boardId);

    // 이전 글 (현재보다 id가 작은 것 중 최대)
    @Query("SELECT p FROM Post p WHERE p.boardId = :boardId AND p.isHidden = false AND p.id < :postId ORDER BY p.id DESC")
    List<Post> findPrevPost(@Param("boardId") Long boardId, @Param("postId") Long postId, Pageable pageable);

    // 다음 글 (현재보다 id가 큰 것 중 최소)
    @Query("SELECT p FROM Post p WHERE p.boardId = :boardId AND p.isHidden = false AND p.id > :postId ORDER BY p.id ASC")
    List<Post> findNextPost(@Param("boardId") Long boardId, @Param("postId") Long postId, Pageable pageable);

    // 최근 새 글 여부
    boolean existsByBoardIdAndIsHiddenFalseAndCreatedAtAfter(Long boardId, LocalDateTime createdAt);

    // 관리자 created_at / like_count 직접 수정 (updatable=false 우회)
    @Modifying
    @Query(value = "UPDATE posts SET created_at = :createdAt, like_count = :likeCount WHERE id = :id", nativeQuery = true)
    void updateAdminFields(@Param("id") Long id, @Param("createdAt") LocalDateTime createdAt, @Param("likeCount") Integer likeCount);

    // 전체 게시판 최신글
    @Query("""
        SELECT p FROM Post p
        WHERE p.boardId IN :boardIds AND p.isHidden = false
        ORDER BY p.createdAt DESC
        """)
    List<Post> findLatestByBoardIds(List<Long> boardIds, Pageable pageable);
}
