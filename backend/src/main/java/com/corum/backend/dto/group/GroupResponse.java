package com.corum.backend.dto.group;

import com.corum.backend.domain.group.Group;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class GroupResponse {

    private final Long id;
    private final Long parentId;
    private final String name;
    private final String description;
    private final String type;
    private final Integer sortOrder;
    private final Boolean isSystem;
    private final List<GroupResponse> children;

    public GroupResponse(Group group) {
        this.id = group.getId();
        this.parentId = group.getParentId();
        this.name = group.getName();
        this.description = group.getDescription();
        this.type = group.getType();
        this.sortOrder = group.getSortOrder();
        this.isSystem = group.getIsSystem();
        this.children = new ArrayList<>();
    }

    public void addChild(GroupResponse child) {
        this.children.add(child);
    }
}
