package com.corum.backend.dto.menu;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MenuReorderItem {
    private Long id;
    private Long parentId;
    private int sortOrder;
}
