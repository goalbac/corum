package com.corum.backend.controller.auth;

import com.corum.backend.common.ApiResponse;
import com.corum.backend.domain.member.Member;
import com.corum.backend.dto.auth.LoginRequest;
import com.corum.backend.dto.auth.LoginResponse;
import com.corum.backend.dto.auth.MemberResponse;
import com.corum.backend.dto.auth.RegisterRequest;
import com.corum.backend.security.CustomUserDetails;
import com.corum.backend.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 회원가입
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.ok("회원가입이 완료되었습니다.");
    }

    // 로그인
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletRequest httpRequest) {
        LoginResponse response = authService.login(request, httpRequest);
        return ApiResponse.ok(response);
    }

    // 로그아웃
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal CustomUserDetails userDetails,
                                    HttpServletRequest httpRequest) {
        authService.logout(userDetails.getMemberId(), httpRequest);
        return ApiResponse.ok("로그아웃되었습니다.");
    }

    // 내 정보 조회
    @GetMapping("/me")
    public ApiResponse<MemberResponse> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = authService.getMe(userDetails.getMemberId());
        return ApiResponse.ok(new MemberResponse(member));
    }
}
