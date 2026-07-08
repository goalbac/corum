package com.corum.backend.domain.notification;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "board_notification_subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class BoardNotificationSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(name = "notify_new_post", nullable = false)
    @Builder.Default
    private Boolean notifyNewPost = true;

    @Column(name = "notify_new_comment", nullable = false)
    @Builder.Default
    private Boolean notifyNewComment = false;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public void update(Boolean notifyNewPost, Boolean notifyNewComment) {
        this.notifyNewPost = notifyNewPost;
        this.notifyNewComment = notifyNewComment;
    }
}
