package com.corum.backend.domain.operation;

import com.corum.backend.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "popups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Popup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "link_new_window", nullable = false)
    private Boolean linkNewWindow;

    @Column(nullable = false, length = 20)
    private String position;

    @Column(nullable = false)
    private Integer priority;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_by")
    private Long createdBy;

    public void update(String title, String contentType, String content, String imageUrl,
                       String linkUrl, Boolean linkNewWindow, String position,
                       Integer priority, LocalDateTime startAt, LocalDateTime endAt,
                       Boolean isActive) {
        this.title = title;
        this.contentType = contentType;
        this.content = content;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.linkNewWindow = linkNewWindow;
        this.position = position;
        this.priority = priority;
        this.startAt = startAt;
        this.endAt = endAt;
        this.isActive = isActive;
    }
}
