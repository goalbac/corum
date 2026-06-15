package com.corum.backend.dto.post;

import com.corum.backend.domain.post.Post;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminPostListResponse {
    private Long id;
    private Long boardId;
    private String boardName;
    private String title;
    private String writerName;
    private Integer viewCount;
    private Integer likeCount;
    private Boolean isNotice;
    private Boolean isHidden;
    private LocalDateTime createdAt;

    public static AdminPostListResponse of(Post post, String boardName) {
        return AdminPostListResponse.builder()
                .id(post.getId())
                .boardId(post.getBoardId())
                .boardName(boardName)
                .title(post.getTitle())
                .writerName(post.getWriterName())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .isNotice(post.getIsNotice())
                .isHidden(post.getIsHidden())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
