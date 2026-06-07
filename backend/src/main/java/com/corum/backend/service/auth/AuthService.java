package com.corum.backend.service.auth;

import com.corum.backend.domain.group.MemberGroupRepository;
import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.member.Member;
import com.corum.backend.domain.member.MemberLoginLog;
import com.corum.backend.domain.member.MemberLoginLogRepository;
import com.corum.backend.domain.member.MemberRepository;
import com.corum.backend.dto.auth.LoginRequest;
import com.corum.backend.dto.auth.LoginResponse;
import com.corum.backend.dto.auth.MemberResponse;
import com.corum.backend.dto.auth.RegisterRequest;
import com.corum.backend.dto.auth.UpdatePasswordRequest;
import com.corum.backend.dto.auth.UpdateProfileRequest;
import com.corum.backend.dto.auth.WithdrawRequest;
import com.corum.backend.dto.group.GroupResponse;
import com.corum.backend.security.JwtProvider;
import com.corum.backend.service.group.GroupService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final MemberLoginLogRepository loginLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final GroupService groupService;
    private final MemberGroupRepository memberGroupRepository;

    @Value("${jwt.login-fail-limit:5}")
    private int loginFailLimit;

    // ===== 회원가입 =====
    @Transactional
    public void register(RegisterRequest request) {
        // 중복 확인
        if (memberRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("이미 사용 중인 아이디입니다.");
        }
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("이미 사용 중인 이메일입니다.");
        }

        Member member = Member.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .isActive(true)   // TODO: 이메일 인증 구현 후 false로 변경
                .build();

        memberRepository.save(member);
        log.info("New member registered: {}", request.getUsername());
    }

    // ===== 로그인 =====
    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        Member member = memberRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    saveLoginLog(null, "LOGIN_FAIL", ip, userAgent, false);
                    return BusinessException.unauthorized("아이디 또는 비밀번호가 올바르지 않습니다.");
                });

        // 계정 잠금 확인
        if (member.getIsLocked()) {
            throw BusinessException.unauthorized("계정이 잠겨 있습니다. 관리자에게 문의해주세요.");
        }

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), member.getPasswordHash())) {
            member.increaseLoginFailCount();

            // 실패 횟수 초과 시 계정 잠금
            if (member.getLoginFailCount() >= loginFailLimit) {
                member.lock(LocalDateTime.now());
                saveLoginLog(member.getId(), "LOGIN_FAIL", ip, userAgent, false);
                throw BusinessException.unauthorized("로그인 실패 횟수를 초과하여 계정이 잠겼습니다.");
            }

            saveLoginLog(member.getId(), "LOGIN_FAIL", ip, userAgent, false);
            throw BusinessException.unauthorized("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        // 활성화 확인
        if (!member.getIsActive()) {
            throw BusinessException.unauthorized("이메일 인증이 필요합니다.");
        }

        // 로그인 성공
        member.resetLoginFailCount();
        saveLoginLog(member.getId(), "LOGIN", ip, userAgent, true);

        String token = jwtProvider.createAccessToken(member.getId(), member.getUsername());
        List<GroupResponse> groups = groupService.getMemberGroups(member.getId());
        return new LoginResponse(token, member, groups);
    }

    // ===== 프로필 수정 =====
    @Transactional
    public MemberResponse updateProfile(Long memberId, UpdateProfileRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));
        member.updateProfile(
                request.getName(), request.getPhone(), request.getAddress(),
                request.getBirthDate(), request.getHomePhone(),
                request.getOccupation(), request.getWorkPhone(),
                request.getNewsletterYn(), member.getProfileImageUrl()
        );
        boolean isAdmin = memberGroupRepository.existsAdminGroupByMemberId(memberId);
        return new MemberResponse(member, isAdmin);
    }

    // ===== 비밀번호 변경 =====
    @Transactional
    public void updatePassword(Long memberId, UpdatePasswordRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));
        if (!passwordEncoder.matches(request.getCurrentPassword(), member.getPasswordHash())) {
            throw new BusinessException("현재 비밀번호가 올바르지 않습니다.");
        }
        member.updatePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    // ===== 회원 탈퇴 =====
    @Transactional
    public void withdraw(Long memberId, WithdrawRequest request, HttpServletRequest httpRequest) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));
        if (!passwordEncoder.matches(request.getPassword(), member.getPasswordHash())) {
            throw new BusinessException("비밀번호가 올바르지 않습니다.");
        }
        member.withdraw(getClientIp(httpRequest));
    }

    // ===== 로그아웃 로그 =====
    @Transactional
    public void logout(Long memberId, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        saveLoginLog(memberId, "LOGOUT", ip, userAgent, true);
    }

    // ===== 내 정보 조회 =====
    @Transactional(readOnly = true)
    public MemberResponse getMe(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));
        boolean isAdmin = memberGroupRepository.existsAdminGroupByMemberId(memberId);
        return new MemberResponse(member, isAdmin);
    }

    // ===== 내부 메서드 =====
    private void saveLoginLog(Long memberId, String action, String ip, String userAgent, boolean success) {
        loginLogRepository.save(MemberLoginLog.builder()
                .memberId(memberId)
                .action(action)
                .ipAddress(ip)
                .userAgent(userAgent)
                .success(success)
                .build());
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
