package com.corum.backend.dto.post;

import lombok.Getter;

import java.util.Map;
import java.util.Set;

@Getter
public class ReactionResult {

    private final Map<String, Integer> reactions;
    private final Set<String> myReactions;
    private final int totalCount;

    public ReactionResult(Map<String, Integer> reactions, Set<String> myReactions) {
        this.reactions = reactions;
        this.myReactions = myReactions;
        this.totalCount = reactions.values().stream().mapToInt(Integer::intValue).sum();
    }
}
