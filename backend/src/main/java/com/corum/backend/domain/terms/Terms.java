package com.corum.backend.domain.terms;

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
@Table(name = "terms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Terms {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false)
    private Integer version;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "require_reagree", nullable = false)
    private Boolean requireReagree;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public void deactivate() {
        this.isActive = false;
    }

    public void update(String content, Boolean isActive, Boolean requireReagree) {
        this.content = content;
        this.isActive = isActive;
        this.requireReagree = requireReagree;
    }
}
