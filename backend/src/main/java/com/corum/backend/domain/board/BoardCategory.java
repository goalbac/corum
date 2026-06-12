package com.corum.backend.domain.board;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "board_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class BoardCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    // 해당 카테고리의 게시글 수 (조회 시 채움, DB 미저장)
    @Transient
    private Long postCount;

    public void setPostCount(Long postCount) { this.postCount = postCount; }

    public void update(String name, Integer sortOrder) {
        this.name = name;
        this.sortOrder = sortOrder;
    }
}
