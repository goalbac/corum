package com.corum.backend.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

@Getter
public class MessageSendRequest {

    @NotEmpty(message = "수신자를 선택해주세요.")
    private List<Long> recipientIds;

    @NotBlank(message = "제목을 입력해주세요.")
    private String title;

    private String content;

    private Boolean isNotice = false;
}
