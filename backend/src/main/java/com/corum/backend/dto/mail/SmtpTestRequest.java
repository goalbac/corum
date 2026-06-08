package com.corum.backend.dto.mail;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmtpTestRequest {

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @NotBlank(message = "받는 이메일을 입력해주세요.")
    private String toEmail;
}
