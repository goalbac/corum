package com.corum.backend.dto.auth;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class UpdateProfileRequest {
    private String name;
    private String phone;
    private String gender;
    private LocalDate birthDate;
    private String homePhone;
    private String address;
    private String occupation;
    private String workPhone;
    private Boolean newsletterYn;
}
