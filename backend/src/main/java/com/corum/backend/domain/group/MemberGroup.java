package com.corum.backend.domain.group;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_groups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class MemberGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "assigned_by")
    private Long assignedBy;

    @Column(name = "assigned_at", nullable = false)
    @Builder.Default
    private LocalDateTime assignedAt = LocalDateTime.now();
}
