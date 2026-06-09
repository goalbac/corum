package com.corum.backend.dto.message;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

@Getter
public class MessageSendRequest {

    @NotEmpty(message = "수신자를 선택해주세요.")
    private List<Long> recipientIds;

    // 채팅 모드에서는 제목 없이 내용만 전송; null/blank 허용
    private String title;

    private String content;

    private Boolean isNotice = false;
}
