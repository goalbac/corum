package com.corum.backend.domain.comment;

import com.corum.backend.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "writer_name", length = 100)
    private String writerName;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "depth", nullable = false)
    @Builder.Default
    private Integer depth = 0;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "client_ip", length = 50)
    private String clientIp;

    // ===== 비즈니스 메서드 =====

    public void update(String content) {
        this.content = content;
    }

    public void delete() {
        this.isDeleted = true;
        this.content = "삭제된 댓글입니다.";
    }
}
