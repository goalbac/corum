package com.corum.backend.controller.auth;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.dto.auth.LoginRequest;
import com.corum.backend.dto.auth.LoginResponse;
import com.corum.backend.dto.auth.MemberResponse;
import com.corum.backend.dto.auth.PasswordResetConfirmRequest;
import com.corum.backend.dto.auth.PasswordResetRequest;
import com.corum.backend.dto.auth.RegisterRequest;
import com.corum.backend.dto.auth.UpdatePasswordRequest;
import com.corum.backend.dto.auth.UpdateProfileRequest;
import com.corum.backend.dto.auth.WithdrawRequest;
import com.corum.backend.security.CustomUserDetails;
import com.corum.backend.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        authService.register(request, httpRequest);
        return ApiResponse.ok("회원가입이 완료되었습니다. 이메일 인증을 진행해주세요.");
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.login(request, httpRequest));
    }

    @GetMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@RequestParam String token, HttpServletRequest httpRequest) {
        authService.verifyEmail(token, httpRequest);
        return ApiResponse.ok("이메일 인증이 완료되었습니다.");
    }

    @PostMapping("/request-password-reset")
    public ApiResponse<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request);
        return ApiResponse.ok("비밀번호 재설정 안내 메일을 발송했습니다.");
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody PasswordResetConfirmRequest request,
                                           HttpServletRequest httpRequest) {
        authService.resetPassword(request, httpRequest);
        return ApiResponse.ok("비밀번호가 재설정되었습니다.");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal CustomUserDetails userDetails,
                                    HttpServletRequest httpRequest) {
        authService.logout(userDetails.getMemberId(), httpRequest);
        return ApiResponse.ok("로그아웃되었습니다.");
    }

    @GetMapping("/me")
    public ApiResponse<MemberResponse> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(authService.getMe(userDetails.getMemberId()));
    }

    @PutMapping("/me")
    public ApiResponse<MemberResponse> updateProfile(
            @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(authService.updateProfile(userDetails.getMemberId(), request));
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> updatePassword(
            @Valid @RequestBody UpdatePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        authService.updatePassword(userDetails.getMemberId(), request);
        return ApiResponse.ok("비밀번호가 변경되었습니다.");
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(
            @Valid @RequestBody WithdrawRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest) {
        authService.withdraw(userDetails.getMemberId(), request, httpRequest);
        return ApiResponse.ok("탈퇴 처리되었습니다.");
    }
}
