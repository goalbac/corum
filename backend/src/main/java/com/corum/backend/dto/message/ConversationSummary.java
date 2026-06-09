package com.corum.backend.dto.message;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ConversationSummary {
    private final Long partnerId;
    private final String partnerName;
    private final String partnerProfileImageUrl;
    private final String lastContent;
    private final LocalDateTime lastAt;
    private final long unreadCount;
    private final boolean lastIsMine;
}
