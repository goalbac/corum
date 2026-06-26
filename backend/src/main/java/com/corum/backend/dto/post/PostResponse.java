package com.corum.backend.dto.post;

import com.corum.backend.domain.post.Post;
import com.corum.backend.dto.file.FileResponse;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
public class PostResponse {

    private final Long id;
    private final Long boardId;
    private final Long memberId;
    private final String title;
    private final String content;
    private final String writerName;
    private final String writerProfileImageUrl;
    private final Boolean isNotice;
    private final Integer viewCount;
    private final Integer likeCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final List<FileResponse> files;
    private final int commentCount;
    private final Long categoryId;
    private final String categoryName;

    /** 리액션 집계: emoji_type → 개수 */
    private final Map<String, Integer> reactions;

    /** 현재 사용자가 누른 리액션 타입 목록 */
    private final Set<String> myReactions;

    public PostResponse(Post post, List<FileResponse> files, int commentCount,
                        String writerProfileImageUrl,
                        Map<String, Integer> reactions, Set<String> myReactions) {
        this(post, files, commentCount, writerProfileImageUrl, null, reactions, myReactions);
    }

    public PostResponse(Post post, List<FileResponse> files, int commentCount,
                        String writerProfileImageUrl, String categoryName,
                        Map<String, Integer> reactions, Set<String> myReactions) {
        this.id = post.getId();
        this.boardId = post.getBoardId();
        this.memberId = post.getMemberId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.writerName = post.getWriterName();
        this.writerProfileImageUrl = writerProfileImageUrl;
        this.isNotice = post.getIsNotice();
        this.viewCount = post.getViewCount();
        this.likeCount = post.getLikeCount();
        this.createdAt = post.getCreatedAt();
        this.updatedAt = post.getUpdatedAt();
        this.files = files;
        this.commentCount = commentCount;
        this.categoryId = post.getCategoryId();
        this.categoryName = categoryName;
        this.reactions = reactions != null ? reactions : Map.of();
        this.myReactions = myReactions != null ? myReactions : Set.of();
    }
}
