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
@Table(name = "banners")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Banner extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "link_new_window", nullable = false)
    private Boolean linkNewWindow;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "bg_color", length = 30)
    private String bgColor;

    @Column(name = "text_align", length = 10)
    private String textAlign;

    @Column(name = "created_by")
    private Long createdBy;

    public void update(String title, String content, String linkUrl, Boolean linkNewWindow,
                       LocalDateTime startAt, LocalDateTime endAt, Boolean isActive, String bgColor, String textAlign) {
        this.title = title;
        this.content = content;
        this.linkUrl = linkUrl;
        this.linkNewWindow = linkNewWindow;
        this.startAt = startAt;
        this.endAt = endAt;
        this.isActive = isActive;
        this.bgColor = bgColor;
        this.textAlign = textAlign;
    }
}
