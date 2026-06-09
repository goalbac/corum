package com.corum.backend.domain.admin;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admin_menu_group_permissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class AdminMenuGroupPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_menu_id", nullable = false)
    private Long adminMenuId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "can_view", nullable = false)
    @Builder.Default
    private Boolean canView = true;

    @Column(name = "can_edit", nullable = false)
    @Builder.Default
    private Boolean canEdit = false;

    public void update(Boolean canView, Boolean canEdit) {
        this.canView = canView;
        this.canEdit = canEdit;
    }
}
