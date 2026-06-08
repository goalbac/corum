package com.corum.backend.service.post;

import com.corum.backend.common.BusinessException;
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
import com.corum.backend.service.file.FileStorageService;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final MemberRepository memberRepository;
    private final FileStorageService fileStorageService;

    // ===== 게시글 목록 =====
    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> getPosts(Long boardId, String searchType,
                                               String keyword, Pageable pageable) {
        Page<Post> posts;
        if (keyword != null && !keyword.isBlank()) {
            posts = postRepository.search(boardId, searchType, keyword, pageable);
        } else {
            posts = postRepository.findByBoardId(boardId, pageable);
        }

        long total = posts.getTotalElements();
        int offset = pageable.getPageNumber() * pageable.getPageSize();
        List<Post> postList = posts.getContent();

        List<PostSummaryResponse> content = new java.util.ArrayList<>();
        for (int i = 0; i < postList.size(); i++) {
            Post p = postList.get(i);
            List<FileResponse> files = fileStorageService.getFiles("POST", p.getId());
            String contentThumb = extractFirstImageFromContent(p.getContent());
            String thumbnailUrl = contentThumb != null ? contentThumb
                    : files.stream()
                            .filter(f -> f.getMimeType() != null && f.getMimeType().startsWith("image/"))
                            .findFirst()
                            .map(f -> "/api/files/" + f.getId() + "/view")
                            .orElse(null);
            long rowNum = total - offset - i;
            content.add(new PostSummaryResponse(p, 0, !files.isEmpty(), thumbnailUrl, rowNum));
        }

        return new PageImpl<>(content, pageable, posts.getTotalElements());
    }

    // ===== 게시글 상세 =====
    @Transactional
    public PostResponse getPost(Long postId, Long memberId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> BusinessException.notFound("게시글을 찾을 수 없습니다."));

        post.increaseViewCount();

        List<FileResponse> files = fileStorageService.getFiles("POST", postId);
        boolean liked = memberId != null && postLikeRepository
                .existsByPostIdAndMemberId(postId, memberId);

        String writerProfileImageUrl = post.getMemberId() != null
                ? memberRepository.findById(post.getMemberId())
                        .map(m -> m.getProfileImageUrl()).orElse(null)
                : null;

        return new PostResponse(post, files, liked, 0, writerProfileImageUrl);
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
                .clientIp(getClientIp(httpRequest))
                .build();

        Post saved = postRepository.save(post);

        List<FileResponse> fileResponses = List.of();
        if (files != null && !files.isEmpty()) {
            fileResponses = fileStorageService.uploadFiles("POST", saved.getId(), files, memberId);
        }

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

        if (!isAdmin && !post.getMemberId().equals(memberId)) {
            throw BusinessException.forbidden("수정 권한이 없습니다.");
        }

        post.update(request.getTitle(), request.getContent(),
                request.getIsNotice(), post.getIsHidden(), memberId);

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

        if (!isAdmin && !post.getMemberId().equals(memberId)) {
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

    private String extractFirstImageFromContent(String content) {
        if (content == null || content.isBlank()) return null;
        int idx = content.indexOf("<img ");
        if (idx < 0) return null;
        int srcIdx = content.indexOf("src=\"", idx);
        if (srcIdx < 0) return null;
        int start = srcIdx + 5;
        int end = content.indexOf("\"", start);
        if (end < 0) return null;
        return content.substring(start, end);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        return (ip == null || ip.isBlank()) ? request.getRemoteAddr() : ip;
    }
}
