package com.corum.backend.domain.board;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "board_group_permissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class BoardGroupPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "can_read", nullable = false)
    @Builder.Default
    private Boolean canRead = true;

    @Column(name = "can_write", nullable = false)
    @Builder.Default
    private Boolean canWrite = false;

    @Column(name = "can_comment", nullable = false)
    @Builder.Default
    private Boolean canComment = false;

    @Column(name = "can_download", nullable = false)
    @Builder.Default
    private Boolean canDownload = true;
}
