package com.corum.backend.domain.menu;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "menu_calendar_targets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class MenuCalendarTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    @Column(name = "calendar_id", nullable = false)
    private Long calendarId;
}
