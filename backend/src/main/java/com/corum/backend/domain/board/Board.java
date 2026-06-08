package com.corum.backend.domain.board;

import com.corum.backend.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "boards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Board extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "menu_id")
    private Long menuId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    // POST, GALLERY, WEBZINE, DOCUMENT
    @Column(name = "board_type", nullable = false, length = 20)
    @Builder.Default
    private String boardType = "POST";

    @Column(name = "use_comment", nullable = false)
    @Builder.Default
    private Boolean useComment = true;

    @Column(name = "use_like", nullable = false)
    @Builder.Default
    private Boolean useLike = true;

    @Column(name = "use_anonymous", nullable = false)
    @Builder.Default
    private Boolean useAnonymous = false;

    @Column(name = "use_notice", nullable = false)
    @Builder.Default
    private Boolean useNotice = true;

    @Column(name = "notice_count_limit", nullable = false)
    @Builder.Default
    private Integer noticeCountLimit = 5;

    // null이면 site_settings 전역 설정 사용
    @Column(name = "file_max_size_mb")
    private Integer fileMaxSizeMb;

    @Column(name = "file_allowed_extensions", length = 500)
    private String fileAllowedExtensions;

    @Column(name = "file_max_count", nullable = false)
    @Builder.Default
    private Integer fileMaxCount = 5;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // ===== 비즈니스 메서드 =====

    public void update(String name, String boardType, Boolean useComment, Boolean useLike,
                       Boolean useAnonymous, Boolean useNotice, Integer noticeCountLimit,
                       Integer fileMaxSizeMb, String fileAllowedExtensions,
                       Integer fileMaxCount, Boolean isActive) {
        this.name = name;
        this.boardType = boardType;
        this.useComment = useComment;
        this.useLike = useLike;
        this.useAnonymous = useAnonymous;
        this.useNotice = useNotice;
        this.noticeCountLimit = noticeCountLimit;
        this.fileMaxSizeMb = fileMaxSizeMb;
        this.fileAllowedExtensions = fileAllowedExtensions;
        this.fileMaxCount = fileMaxCount;
        this.isActive = isActive;
    }
}
