package com.corum.backend.domain.post;

import com.corum.backend.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "writer_name", length = 100)
    private String writerName;

    @Column(name = "is_notice", nullable = false)
    @Builder.Default
    private Boolean isNotice = false;

    @Column(name = "is_hidden", nullable = false)
    @Builder.Default
    private Boolean isHidden = false;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @Column(name = "client_ip", length = 50)
    private String clientIp;

    @Column(name = "updated_by")
    private Long updatedBy;

    // ===== 비즈니스 메서드 =====

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }

    public void update(String title, String content, Boolean isNotice, Boolean isHidden,
                       Long updatedBy) {
        this.title = title;
        this.content = content;
        this.isNotice = isNotice;
        this.isHidden = isHidden;
        this.updatedBy = updatedBy;
    }

    // 관리자 전체 필드 수정
    public void adminUpdate(String title, String content, String writerName,
                            Boolean isNotice, Boolean isHidden,
                            Integer viewCount, Integer likeCount, Long updatedBy) {
        this.title = title;
        this.content = content;
        this.writerName = writerName;
        this.isNotice = isNotice;
        this.isHidden = isHidden;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.updatedBy = updatedBy;
    }
}
