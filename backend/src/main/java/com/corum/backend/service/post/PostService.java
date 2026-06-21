package com.corum.backend.service.post;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.board.Board;
import com.corum.backend.domain.board.BoardCategory;
import com.corum.backend.domain.board.BoardCategoryRepository;
import com.corum.backend.domain.board.BoardGroupPermissionRepository;
import com.corum.backend.domain.board.BoardRepository;
import com.corum.backend.domain.group.MemberGroupRepository;
import com.corum.backend.domain.member.MemberRepository;
import com.corum.backend.domain.post.Post;
import com.corum.backend.domain.post.PostLike;
import com.corum.backend.domain.post.PostLikeRepository;
import com.corum.backend.domain.post.PostRepository;
import com.corum.backend.dto.file.FileResponse;
import com.corum.backend.dto.post.AdjacentPostsResponse;
import com.corum.backend.dto.post.PostCreateRequest;
import com.corum.backend.dto.post.PostResponse;
import com.corum.backend.dto.post.PostSummaryResponse;
import com.corum.backend.domain.comment.CommentRepository;
import com.corum.backend.service.file.FileStorageService;
import com.corum.backend.service.notification.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final MemberRepository memberRepository;
    private final FileStorageService fileStorageService;
    private final CommentRepository commentRepository;
    private final MemberGroupRepository memberGroupRepository;
    private final BoardGroupPermissionRepository boardGroupPermissionRepository;
    private final BoardCategoryRepository boardCategoryRepository;
    private final BoardRepository boardRepository;
    private final NotificationService notificationService;

    // 24시간 내 동일 게시글 중복 조회 방지 (key: "postId:u{memberId}" or "postId:i{ip}")
    private static final long VIEW_THROTTLE_MS = 24 * 60 * 60 * 1000L;
    private final ConcurrentHashMap<String, Long> viewedCache = new ConcurrentHashMap<>();

    private boolean shouldIncrementView(Long postId, Long memberId, String ip) {
        String key = postId + ":" + (memberId != null ? "u" + memberId : "i" + ip);
        long now = System.currentTimeMillis();
        Long last = viewedCache.get(key);
        if (last != null && (now - last) < VIEW_THROTTLE_MS) return false;
        viewedCache.put(key, now);
        // 캐시가 너무 커지지 않도록 50만 건 초과 시 오래된 항목 정리
        if (viewedCache.size() > 500_000) {
            viewedCache.entrySet().removeIf(e -> (now - e.getValue()) >= VIEW_THROTTLE_MS);
        }
        return true;
    }

    // ===== 게시글 목록 =====
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> getPosts(Long boardId, String searchType,
                                              String keyword, Long categoryId, Pageable pageable) {
        Board board = boardRepository.findById(boardId).orElse(null);
        boolean isDocument = board != null && "DOCUMENT".equals(board.getBoardType());
        Page<Post> posts;
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (categoryId != null) {
            posts = hasKeyword
                    ? postRepository.searchByCategory(boardId, categoryId, searchType, keyword, pageable)
                    : postRepository.findByBoardIdAndCategoryId(boardId, categoryId, pageable);
        } else if (hasKeyword) {
            posts = postRepository.search(boardId, searchType, keyword, pageable);
        } else {
            posts = postRepository.findByBoardId(boardId, pageable);
        }

        // 카테고리 이름 맵
        java.util.Map<Long, String> categoryNameMap = boardCategoryRepository
                .findByBoardIdOrderBySortOrderAsc(boardId).stream()
                .collect(java.util.stream.Collectors.toMap(BoardCategory::getId, BoardCategory::getName));

        long total = posts.getTotalElements();
        int offset = pageable.getPageNumber() * pageable.getPageSize();
        List<Post> postList = posts.getContent();

        // 작성자 프로필 이미지 일괄 조회
        java.util.Set<Long> memberIds = postList.stream()
                .map(Post::getMemberId).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        java.util.Map<Long, String> profileImageMap = memberRepository.findAllById(memberIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.corum.backend.domain.member.Member::getId,
                        m -> m.getProfileImageUrl() != null ? m.getProfileImageUrl() : ""));

        List<PostSummaryResponse> content = new java.util.ArrayList<>();
        for (int i = 0; i < postList.size(); i++) {
            Post p = postList.get(i);
            List<FileResponse> files = fileStorageService.getFiles("POST", p.getId());
            List<FileResponse> imageFiles = files.stream()
                    .filter(f -> f.getMimeType() != null && f.getMimeType().startsWith("image/"))
                    .toList();

            // 목록용 이미지: 첨부파일은 300px small-thumb, 인라인은 /view → /thumbnail 변환
            List<String> imageUrls = imageFiles.stream()
                    .map(f -> "/api/files/" + f.getId() + "/small-thumb")
                    .toList();

            String thumbnailUrl;
            if (!imageUrls.isEmpty()) {
                thumbnailUrl = imageUrls.get(0);
            } else {
                thumbnailUrl = toThumbnailUrl(extractFirstImageFromContent(p.getContent()));
                if (thumbnailUrl != null) {
                    imageUrls = extractAllImagesFromContent(p.getContent()).stream()
                            .map(this::toThumbnailUrl)
                            .toList();
                }
            }

            long rowNum = total - offset - i;
            int commentCount = commentRepository.countByPostIdAndIsDeletedFalse(p.getId());
            String catName = p.getCategoryId() != null ? categoryNameMap.get(p.getCategoryId()) : null;
            String profileImageUrl = p.getMemberId() != null ? profileImageMap.getOrDefault(p.getMemberId(), "") : "";

            if (isDocument) {
                // 자료실: 이미지 외 첫 번째 파일을 대표 파일로
                FileResponse primary = files.isEmpty() ? null : files.get(0);
                content.add(new PostSummaryResponse(p, commentCount, !files.isEmpty(),
                        null, List.of(), rowNum, catName, profileImageUrl,
                        primary != null ? primary.getOriginalName() : null,
                        primary != null ? primary.getFileSize() : null,
                        primary != null ? primary.getDownloadCount() : null,
                        files.size()));
            } else {
                content.add(new PostSummaryResponse(p, commentCount, !files.isEmpty(), thumbnailUrl, imageUrls, rowNum, catName, profileImageUrl));
            }
        }

        return new PageImpl<>(content, pageable, posts.getTotalElements());
    }

    // ===== 게시글 상세 =====
    @Transactional
    public PostResponse getPost(Long postId, Long memberId, String clientIp) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> BusinessException.notFound("게시글을 찾을 수 없습니다."));

        if (shouldIncrementView(postId, memberId, clientIp)) {
            post.increaseViewCount();
        }

        List<FileResponse> files = fileStorageService.getFiles("POST", postId);
        boolean liked = memberId != null && postLikeRepository
                .existsByPostIdAndMemberId(postId, memberId);

        String writerProfileImageUrl = post.getMemberId() != null
                ? memberRepository.findById(post.getMemberId())
                        .map(m -> m.getProfileImageUrl()).orElse(null)
                : null;

        String categoryName = post.getCategoryId() != null
                ? boardCategoryRepository.findById(post.getCategoryId()).map(BoardCategory::getName).orElse(null)
                : null;
        return new PostResponse(post, files, liked, 0, writerProfileImageUrl, categoryName);
    }

    // ===== 게시글 작성 =====
    @Transactional
    public PostResponse createPost(Long boardId, PostCreateRequest request,
                                   Long memberId, List<MultipartFile> files,
                                   HttpServletRequest httpRequest) {
        String writerName = memberRepository.findById(memberId)
                .map(m -> m.getName())
                .orElse("알 수 없음");

        Post post = Post.builder()
                .boardId(boardId)
                .memberId(memberId)
                .title(request.getTitle())
                .content(request.getContent())
                .writerName(writerName)
                .isNotice(request.getIsNotice())
                .categoryId(request.getCategoryId())
                .clientIp(getClientIp(httpRequest))
                .build();

        Post saved = postRepository.save(post);

        List<FileResponse> fileResponses = List.of();
        if (files != null && !files.isEmpty()) {
            fileResponses = fileStorageService.uploadFiles("POST", saved.getId(), files, memberId);
        }

        // 해당 게시판 manage 권한자에게 새 게시글 알림 (작성자 제외)
        try {
            List<Long> managerIds = findBoardManagerMemberIds(boardId).stream()
                    .filter(id -> !id.equals(memberId))
                    .collect(Collectors.toList());
            if (!managerIds.isEmpty()) {
                notificationService.createForMembers(
                        managerIds, "NEW_POST_ON_MANAGED_BOARD",
                        "새 게시글: " + truncate(request.getTitle(), 30),
                        writerName + "님이 게시글을 작성했습니다.",
                        "/board/" + boardId + "/posts/" + saved.getId()
                );
            }
        } catch (Exception ignored) { }

        String savedProfileImageUrl = memberRepository.findById(memberId)
                .map(m -> m.getProfileImageUrl()).orElse(null);
        return new PostResponse(saved, fileResponses, false, 0, savedProfileImageUrl);
    }

    // ===== 이전/다음 글 =====
    @Transactional(readOnly = true)
    public AdjacentPostsResponse getAdjacentPosts(Long boardId, Long postId) {
        Pageable one = PageRequest.of(0, 1);
        Post prev = postRepository.findPrevPost(boardId, postId, one).stream().findFirst().orElse(null);
        Post next = postRepository.findNextPost(boardId, postId, one).stream().findFirst().orElse(null);
        return new AdjacentPostsResponse(prev, next);
    }

    // ===== 게시글 수정 =====
    @Transactional
    public PostResponse updatePost(Long postId, PostCreateRequest request, Long memberId, boolean isAdmin) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> BusinessException.notFound("게시글을 찾을 수 없습니다."));

        boolean hasManage = !isAdmin && hasManagePermission(post.getBoardId(), memberId);
        if (!isAdmin && !hasManage && !post.getMemberId().equals(memberId)) {
            throw BusinessException.forbidden("수정 권한이 없습니다.");
        }

        post.update(request.getTitle(), request.getContent(),
                request.getIsNotice(), post.getIsHidden(), request.getCategoryId(), memberId);

        if (isAdmin && (request.getCreatedAt() != null || request.getLikeCount() != null)) {
            java.time.LocalDateTime createdAt = request.getCreatedAt() != null ? request.getCreatedAt() : post.getCreatedAt();
            int likeCount = request.getLikeCount() != null ? request.getLikeCount() : post.getLikeCount();
            postRepository.updateAdminFields(postId, createdAt, likeCount);
        }

        List<FileResponse> files = fileStorageService.getFiles("POST", postId);
        boolean liked = postLikeRepository.existsByPostIdAndMemberId(postId, memberId);
        String updaterProfileImageUrl = memberRepository.findById(memberId)
                .map(m -> m.getProfileImageUrl()).orElse(null);
        return new PostResponse(post, files, liked, 0, updaterProfileImageUrl);
    }

    // ===== 게시글 삭제 =====
    @Transactional
    public void deletePost(Long postId, Long memberId, boolean isAdmin) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> BusinessException.notFound("게시글을 찾을 수 없습니다."));

        boolean hasManage = !isAdmin && hasManagePermission(post.getBoardId(), memberId);
        if (!isAdmin && !hasManage && !post.getMemberId().equals(memberId)) {
            throw BusinessException.forbidden("삭제 권한이 없습니다.");
        }

        fileStorageService.deleteFilesByTarget("POST", postId);
        postRepository.delete(post);
    }

    // ===== 좋아요 토글 =====
    @Transactional
    public boolean toggleLike(Long postId, Long memberId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> BusinessException.notFound("게시글을 찾을 수 없습니다."));

        if (postLikeRepository.existsByPostIdAndMemberId(postId, memberId)) {
            postLikeRepository.deleteByPostIdAndMemberId(postId, memberId);
            post.decreaseLikeCount();
            return false;
        } else {
            postLikeRepository.save(PostLike.builder()
                    .postId(postId).memberId(memberId).build());
            post.increaseLikeCount();
            return true;
        }
    }

    // ===== 대시보드용 최신글 =====
    @Transactional(readOnly = true)
    public List<PostSummaryResponse> getLatestPosts(Long boardId) {
        return postRepository.findTop5ByBoardIdAndIsHiddenFalseOrderByCreatedAtDesc(boardId)
                .stream()
                .map(p -> new PostSummaryResponse(p, 0, false))
                .collect(Collectors.toList());
    }

    // 인라인 이미지 URL을 썸네일 URL로 변환 (/view → /thumbnail, 그 외는 그대로)
    private String toThumbnailUrl(String url) {
        if (url == null) return null;
        return url.endsWith("/view") ? url.substring(0, url.length() - 5) + "/thumbnail" : url;
    }

    private String extractFirstImageFromContent(String content) {
        List<String> all = extractAllImagesFromContent(content);
        return all.isEmpty() ? null : all.get(0);
    }

    private List<String> extractAllImagesFromContent(String content) {
        if (content == null || content.isBlank()) return List.of();
        List<String> result = new java.util.ArrayList<>();
        int pos = 0;
        while (true) {
            int idx = content.indexOf("<img ", pos);
            if (idx < 0) break;
            int srcIdx = content.indexOf("src=\"", idx);
            if (srcIdx < 0) { pos = idx + 5; continue; }
            int start = srcIdx + 5;
            int end = content.indexOf("\"", start);
            if (end < 0) break;
            result.add(content.substring(start, end));
            pos = end + 1;
        }
        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip == null || ip.isBlank()) ? request.getRemoteAddr() : ip;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    public boolean hasManagePermission(Long boardId, Long memberId) {
        List<Long> groupIds = memberGroupRepository.findGroupIdsByMemberId(memberId);
        if (groupIds.isEmpty()) return false;
        return boardGroupPermissionRepository.existsManagePermission(boardId, groupIds);
    }

    /** 게시판 manage 권한을 가진 회원 ID 목록 */
    private List<Long> findBoardManagerMemberIds(Long boardId) {
        List<Long> manageGroupIds = boardGroupPermissionRepository.findByBoardId(boardId).stream()
                .filter(p -> Boolean.TRUE.equals(p.getCanManage()))
                .map(p -> p.getGroupId())
                .collect(Collectors.toList());
        if (manageGroupIds.isEmpty()) return List.of();
        return memberGroupRepository.findByGroupIdIn(manageGroupIds).stream()
                .map(mg -> mg.getMemberId())
                .distinct()
                .collect(Collectors.toList());
    }
}
