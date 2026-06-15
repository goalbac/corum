package com.corum.backend.controller.admin;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.domain.board.Board;
import com.corum.backend.domain.board.BoardRepository;
import com.corum.backend.domain.post.Post;
import com.corum.backend.domain.post.PostRepository;
import com.corum.backend.dto.post.AdminPostBatchRequest;
import com.corum.backend.dto.post.AdminPostListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {

    private final PostRepository postRepository;
    private final BoardRepository boardRepository;

    // ===== 게시물 목록 (전체 or 게시판별) =====
    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> getPosts(
            @RequestParam(required = false) Long boardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Post> postPage = postRepository.findAdminPosts(boardId, PageRequest.of(page, size));

        // 사용된 boardId들을 배치 조회
        List<Long> boardIds = postPage.getContent().stream()
                .map(Post::getBoardId).distinct().collect(Collectors.toList());
        Map<Long, String> boardNameMap = boardRepository.findAllById(boardIds).stream()
                .collect(Collectors.toMap(Board::getId, Board::getName));

        List<AdminPostListResponse> items = postPage.getContent().stream()
                .map(p -> AdminPostListResponse.of(p, boardNameMap.getOrDefault(p.getBoardId(), "")))
                .collect(Collectors.toList());

        return ApiResponse.ok(Map.of(
                "items", items,
                "totalElements", postPage.getTotalElements(),
                "totalPages", postPage.getTotalPages(),
                "page", page,
                "size", size
        ));
    }

    // ===== 게시판 목록 (필터 드롭다운용) =====
    @GetMapping("/boards")
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> getBoards() {
        return ApiResponse.ok(
                boardRepository.findAll().stream()
                        .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
                        .sorted((a, b) -> a.getName().compareTo(b.getName()))
                        .map(b -> Map.<String, Object>of("id", b.getId(), "name", b.getName()))
                        .collect(Collectors.toList())
        );
    }

    // ===== 배치 수정 (writerName / viewCount / likeCount / createdAt / 게시판 이동) =====
    @PutMapping("/batch")
    @Transactional
    public ApiResponse<Void> batchUpdate(@RequestBody AdminPostBatchRequest req) {
        if (req.getIds() == null || req.getIds().isEmpty()) return ApiResponse.ok(null);

        // 게시판 이동
        if (req.getTargetBoardId() != null) {
            postRepository.moveToBoardBatch(req.getTargetBoardId(), req.getIds());
        }

        // 개별 필드 수정
        boolean hasFieldUpdate = req.getWriterName() != null
                || req.getViewCount() != null
                || req.getLikeCount() != null;
        boolean hasDateUpdate = req.getCreatedAt() != null;

        if (hasFieldUpdate || hasDateUpdate) {
            List<Post> posts = postRepository.findAllById(req.getIds());
            for (Post post : posts) {
                if (hasFieldUpdate) {
                    post.adminUpdate(
                            post.getTitle(),
                            post.getContent(),
                            req.getWriterName() != null ? req.getWriterName() : post.getWriterName(),
                            post.getIsNotice(),
                            post.getIsHidden(),
                            req.getViewCount() != null ? req.getViewCount() : post.getViewCount(),
                            req.getLikeCount() != null ? req.getLikeCount() : post.getLikeCount(),
                            null
                    );
                }
                if (hasDateUpdate) {
                    postRepository.updateCreatedAtById(post.getId(), req.getCreatedAt());
                }
            }
        }
        return ApiResponse.ok(null);
    }

    // ===== 배치 삭제 =====
    @DeleteMapping("/batch")
    @Transactional
    public ApiResponse<Void> batchDelete(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids != null && !ids.isEmpty()) {
            postRepository.deleteAllById(ids);
        }
        return ApiResponse.ok(null);
    }
}
