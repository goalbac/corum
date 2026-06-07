package com.corum.backend.domain.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    // 전체 게시판 최신글
    @Query("""
        SELECT p FROM Post p
        WHERE p.boardId IN :boardIds AND p.isHidden = false
        ORDER BY p.createdAt DESC
        """)
    List<Post> findLatestByBoardIds(List<Long> boardIds, Pageable pageable);
}
