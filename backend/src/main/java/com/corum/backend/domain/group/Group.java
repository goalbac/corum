package com.corum.backend.domain.group;

import com.corum.backend.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "groups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Group extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    // ADMIN: 운영 그룹 계열, NORMAL: 일반 그룹 계열
    @Column(name = "type", nullable = false, length = 20)
    @Builder.Default
    private String type = "NORMAL";

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    // 시스템 고정 그룹 (삭제 불가)
    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    // ===== 비즈니스 메서드 =====

    public void update(String name, String description, Integer sortOrder) {
        this.name = name;
        this.description = description;
        this.sortOrder = sortOrder;
    }

    public boolean isAdminType() {
        return "ADMIN".equals(this.type);
    }
}
