package com.corum.backend.domain.calendar;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "calendars")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class CalendarEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "color", length = 20)
    private String color;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "calendar_type", nullable = false, length = 20)
    @Builder.Default
    private String calendarType = "GENERAL";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public void update(String name, String color, String description, String calendarType) {
        this.name = name;
        this.color = color;
        this.description = description;
        if (calendarType != null) this.calendarType = calendarType;
    }

    public void updateActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
