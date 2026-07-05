package com.corum.backend.service.comment;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.board.BoardGroupPermissionRepository;
import com.corum.backend.domain.comment.Comment;
import com.corum.backend.domain.comment.CommentReaction;
import com.corum.backend.domain.comment.CommentReactionRepository;
import com.corum.backend.domain.comment.CommentRepository;
import com.corum.backend.domain.group.Group;
import com.corum.backend.domain.group.GroupRepository;
import com.corum.backend.domain.group.MemberGroup;
import com.corum.backend.domain.group.MemberGroupRepository;
import com.corum.backend.domain.member.MemberRepository;
import com.corum.backend.domain.post.Post;
import com.corum.backend.domain.post.PostRepository;
import com.corum.backend.dto.comment.CommentCreateRequest;
import com.corum.backend.dto.comment.CommentResponse;
import com.corum.backend.dto.post.ReactionResult;
import com.corum.backend.service.board.BoardService;
import com.corum.backend.service.notification.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final MemberGroupRepository memberGroupRepository;
    private final GroupRepository groupRepository;
    private final BoardGroupPermissionRepository boardGroupPermissionRepository;
    private final BoardService boardService;
    private final NotificationService notificationService;

    private static final int MAX_DEPTH = 2; // 0-based (0,1,2 = 3뎁스)
    private static final List<String> EMOJI_ORDER =
            List.of("HEART", "THUMBS_UP", "SMILE", "LAUGH_CRY", "SAD", "ANGRY");

    // ===== 댓글 목록 (트리 구조) =====
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long postId, Long memberId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> BusinessException.notFound("게시글을 찾을 수 없습니다."));
        if (!boardService.hasPermission(post.getBoardId(), memberId, "READ")) {
            throw BusinessException.forbidden("댓글을 조회할 권한이 없습니다.");
        }

        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);

        // memberId → profileImageUrl 배치 조회
        Set<Long> memberIds = comments.stream()
                .filter(c -> c.getMemberId() != null)
                .map(Comment::getMemberId)
                .collect(Collectors.toSet());
        Map<Long, String> profileImageMap = memberRepository.findAllById(memberIds).stream()
                .filter(m -> m.getProfileImageUrl() != null)
                .collect(Collectors.toMap(m -> m.getId(), m -> m.getProfileImageUrl()));

        // memberId → 대표 그룹명 배치 조회 (정렬 순서 가장 낮은 leaf 그룹)
        List<MemberGroup> memberGroups = memberGroupRepository.findByMemberIdIn(new ArrayList<>(memberIds));
        Set<Long> groupIds = memberGroups.stream().map(MemberGroup::getGroupId).collect(Collectors.toSet());
        Map<Long, Group> groupMap = groupRepository.findAllById(groupIds).stream()
                .collect(Collectors.toMap(Group::getId, g -> g));
        Map<Long, String> groupNameMap = new HashMap<>();
        for (MemberGroup mg : memberGroups) {
            Group g = groupMap.get(mg.getGroupId());
            if (g == null || g.getParentId() == null) continue; // 최상위 그룹 제외
            groupNameMap.merge(mg.getMemberId(), g.getName(), (a, b) -> a);
        }

        // 댓글 리액션 배치 조회
        List<Long> commentIds = comments.stream().map(Comment::getId).toList();
        List<CommentReaction> allReactions = commentIds.isEmpty()
                ? List.of()
                : commentReactionRepository.findByCommentIdIn(commentIds);

        // commentId → (emojiType → count)
        Map<Long, Map<String, Integer>> reactionCountMap = new HashMap<>();
        // commentId → 내가 누른 타입 목록
        Map<Long, Set<String>> myReactionMap = new HashMap<>();
        for (CommentReaction r : allReactions) {
            reactionCountMap.computeIfAbsent(r.getCommentId(), k -> new LinkedHashMap<>())
                    .merge(r.getEmojiType(), 1, Integer::sum);
            if (memberId != null && r.getMemberId().equals(memberId)) {
                myReactionMap.computeIfAbsent(r.getCommentId(), k -> new HashSet<>())
                        .add(r.getEmojiType());
            }
        }

        boolean canSeeReal = boardService.canUseAliasWriter(post.getBoardId(), memberId);
        Map<Long, String> realNameMap = new HashMap<>();
        if (canSeeReal) {
            memberRepository.findAllById(memberIds).forEach(m -> realNameMap.put(m.getId(), m.getName()));
        }

        Map<Long, CommentResponse> map = comments.stream()
                .collect(Collectors.toMap(Comment::getId,
                        c -> {
                            String realName = c.getMemberId() != null ? realNameMap.get(c.getMemberId()) : null;
                            String actualWriterName = (realName != null && !realName.equals(c.getWriterName()))
                                    ? realName : null;
                            return new CommentResponse(c,
                                    profileImageMap.get(c.getMemberId()),
                                    groupNameMap.get(c.getMemberId()),
                                    actualWriterName);
                        }));

        // 리액션 정보 주입
        map.values().forEach(cr -> {
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (String type : EMOJI_ORDER) counts.put(type, 0);
            Map<String, Integer> actual = reactionCountMap.getOrDefault(cr.getId(), Map.of());
            actual.forEach(counts::put);
            cr.setReactions(counts, myReactionMap.getOrDefault(cr.getId(), Set.of()));
        });

        List<CommentResponse> roots = new ArrayList<>();
        comments.forEach(c -> {
            if (c.getParentId() == null) {
                roots.add(map.get(c.getId()));
            } else {
                CommentResponse parent = map.get(c.getParentId());
                if (parent != null) {
                    parent.addChild(map.get(c.getId()));
                }
            }
        });

        return roots;
    }

    // ===== 댓글 리액션 토글 =====
    @Transactional
    public ReactionResult toggleCommentReaction(Long commentId, Long memberId, String emojiType) {
        if (!commentRepository.existsById(commentId)) {
            throw BusinessException.notFound("댓글을 찾을 수 없습니다.");
        }
        if (commentReactionRepository.existsByCommentIdAndMemberIdAndEmojiType(commentId, memberId, emojiType)) {
            commentReactionRepository.deleteByCommentIdAndMemberIdAndEmojiType(commentId, memberId, emojiType);
        } else {
            commentReactionRepository.save(CommentReaction.builder()
                    .commentId(commentId).memberId(memberId).emojiType(emojiType).build());
        }
        List<CommentReaction> allReactions = commentReactionRepository.findByCommentId(commentId);
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String type : EMOJI_ORDER) counts.put(type, 0);
        allReactions.forEach(r -> counts.merge(r.getEmojiType(), 1, Integer::sum));
        Set<String> myReactions = allReactions.stream()
                .filter(r -> r.getMemberId().equals(memberId))
                .map(CommentReaction::getEmojiType)
                .collect(Collectors.toCollection(HashSet::new));
        return new ReactionResult(counts, myReactions);
    }

    // ===== 댓글 작성 =====
    @Transactional
    public CommentResponse createComment(Long postId, CommentCreateRequest request,
                                         Long memberId, HttpServletRequest httpRequest) {
        Post targetPost = postRepository.findById(postId)
                .orElseThrow(() -> BusinessException.notFound("게시글을 찾을 수 없습니다."));
        if (!boardService.hasPermission(targetPost.getBoardId(), memberId, "COMMENT")) {
            throw BusinessException.forbidden("댓글을 작성할 권한이 없습니다.");
        }

        int depth = 0;

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> BusinessException.notFound("상위 댓글을 찾을 수 없습니다."));

            if (parent.getIsDeleted()) {
                throw new BusinessException("삭제된 댓글에는 답글을 달 수 없습니다.");
            }

            depth = parent.getDepth() + 1;
            if (depth > MAX_DEPTH) {
                throw new BusinessException("댓글은 최대 3단계까지만 작성할 수 있습니다.");
            }
        }

        String realName = memberRepository.findById(memberId)
                .map(m -> m.getName())
                .orElse("알 수 없음");
        final String writerName = (request.getAliasName() != null && !request.getAliasName().isBlank()
                && boardService.isValidAliasName(targetPost.getBoardId(), memberId, request.getAliasName()))
                ? request.getAliasName().trim()
                : realName;

        String ip = httpRequest.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = httpRequest.getRemoteAddr();

        Comment comment = Comment.builder()
                .postId(postId)
                .parentId(request.getParentId())
                .memberId(memberId)
                .writerName(writerName)
                .content(request.getContent())
                .depth(depth)
                .clientIp(ip)
                .build();

        Comment saved = commentRepository.save(comment);

        String link = "/board/" + targetPost.getBoardId() + "/posts/" + postId;
        // 내 글에 댓글이 달리면 알림 (본인 댓글 제외)
        if (targetPost.getMemberId() != null && !targetPost.getMemberId().equals(memberId)) {
            notificationService.create(
                    targetPost.getMemberId(),
                    "COMMENT_ON_MY_POST",
                    "'" + truncate(targetPost.getTitle(), 30) + "'에 새 댓글",
                    writerName + ": " + truncate(request.getContent(), 50),
                    link
            );
        }
        // 내 댓글에 답글이 달리면 알림
        if (request.getParentId() != null) {
            commentRepository.findById(request.getParentId()).ifPresent(parent -> {
                if (parent.getMemberId() != null
                        && !parent.getMemberId().equals(memberId)
                        && !parent.getMemberId().equals(targetPost.getMemberId())) { // 중복 방지
                    notificationService.create(
                            parent.getMemberId(),
                            "REPLY_ON_MY_COMMENT",
                            "내 댓글에 답글이 달렸습니다",
                            writerName + ": " + truncate(request.getContent(), 50),
                            link
                    );
                }
            });
        }

        return new CommentResponse(saved);
    }

    // ===== 댓글 수정 =====
    @Transactional
    public CommentResponse updateComment(Long commentId, String content, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> BusinessException.notFound("댓글을 찾을 수 없습니다."));

        boolean isOwner = comment.getMemberId() != null && comment.getMemberId().equals(memberId);
        if (!isOwner) {
            Post post = postRepository.findById(comment.getPostId()).orElse(null);
            boolean hasBoardManage = post != null && !memberGroupRepository.findGroupIdsByMemberId(memberId).isEmpty()
                    && boardGroupPermissionRepository.existsManagePermission(post.getBoardId(),
                            memberGroupRepository.findGroupIdsByMemberId(memberId));
            if (!hasBoardManage) throw BusinessException.forbidden("수정 권한이 없습니다.");
        }

        comment.update(content);
        return new CommentResponse(comment);
    }

    // ===== 댓글 삭제 =====
    @Transactional
    public void deleteComment(Long commentId, Long memberId, boolean isAdmin) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> BusinessException.notFound("댓글을 찾을 수 없습니다."));

        boolean hasBoardManage = false;
        if (!isAdmin) {
            Post post = postRepository.findById(comment.getPostId()).orElse(null);
            if (post != null) {
                List<Long> groupIds = memberGroupRepository.findGroupIdsByMemberId(memberId);
                hasBoardManage = !groupIds.isEmpty()
                        && boardGroupPermissionRepository.existsManagePermission(post.getBoardId(), groupIds);
            }
        }

        if (!isAdmin && !hasBoardManage && !comment.getMemberId().equals(memberId)) {
            throw BusinessException.forbidden("삭제 권한이 없습니다.");
        }

        // 대댓글이 있으면 소프트 삭제, 없으면 하드 삭제
        if (commentRepository.existsByParentIdAndIsDeletedFalse(commentId)) {
            comment.delete();
        } else {
            commentRepository.delete(comment);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
