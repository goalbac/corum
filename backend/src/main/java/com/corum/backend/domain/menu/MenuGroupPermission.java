package com.corum.backend.domain.menu;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "menu_group_permissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class MenuGroupPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;
}
