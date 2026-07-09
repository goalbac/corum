package com.corum.backend.service.auth;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.comment.CommentRepository;
import com.corum.backend.domain.group.MemberGroupRepository;
import com.corum.backend.domain.member.Member;
import com.corum.backend.domain.member.MemberLoginLog;
import com.corum.backend.domain.member.MemberLoginLogRepository;
import com.corum.backend.domain.member.MemberRepository;
import com.corum.backend.domain.post.PostRepository;
import com.corum.backend.domain.setting.SiteSetting;
import com.corum.backend.domain.setting.SiteSettingRepository;
import com.corum.backend.dto.auth.LoginRequest;
import com.corum.backend.dto.auth.LoginResponse;
import com.corum.backend.dto.auth.MemberResponse;
import com.corum.backend.dto.auth.PasswordResetConfirmRequest;
import com.corum.backend.dto.auth.PasswordResetRequest;
import com.corum.backend.dto.auth.RegisterRequest;
import com.corum.backend.dto.auth.UpdatePasswordRequest;
import com.corum.backend.dto.auth.UpdateProfileRequest;
import com.corum.backend.dto.auth.WithdrawRequest;
import com.corum.backend.dto.group.GroupResponse;
import com.corum.backend.security.JwtProvider;
import com.corum.backend.service.file.FileStorageService;
import com.corum.backend.service.group.GroupService;
import com.corum.backend.service.log.OperationLogService;
import com.corum.backend.service.mail.MailService;
import com.corum.backend.service.mail.MailTemplateService;
import com.corum.backend.service.notification.NotificationService;
import com.corum.backend.service.terms.TermsService;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
    private final MailService mailService;
    private final MailTemplateService mailTemplateService;
    private final OperationLogService operationLogService;
    private final TermsService termsService;
    private final SiteSettingRepository siteSettingRepository;
    private final TokenSessionService tokenSessionService;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Value("${jwt.login-fail-limit:5}")
    private int loginFailLimit;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional
    public void register(RegisterRequest request, HttpServletRequest httpRequest) {
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
                .phone(request.getPhone())
                .gender(request.getGender())
                .birthDate(request.getBirthDate())
                .newsletterYn(request.getNewsletterYn() != null ? request.getNewsletterYn() : false)
                .isActive(false)
                .build();

        Member saved = memberRepository.save(member);
        operationLogService.audit(saved.getId(), "CREATE", "members", saved.getId(), null, saved.getUsername(), httpRequest);
        notificationService.initPrefsForNewMember(saved.getId());
        sendVerificationEmail(saved);
        log.info("New member registered: {}", request.getUsername());
    }

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        Member member = memberRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    saveLoginLog(null, "LOGIN_FAIL", ip, userAgent, false);
                    return BusinessException.unauthorized("아이디 또는 비밀번호가 올바르지 않습니다.");
                });

        if (member.getIsLocked()) {
            throw BusinessException.unauthorized("계정이 잠겨 있습니다. 관리자에게 문의해주세요.");
        }

        if (!passwordEncoder.matches(request.getPassword(), member.getPasswordHash())) {
            member.increaseLoginFailCount();
            saveLoginLog(member.getId(), "LOGIN_FAIL", ip, userAgent, false);
            if (member.getLoginFailCount() >= getLoginFailLimit()) {
                member.lock(LocalDateTime.now());
                tokenSessionService.invalidateMember(member.getId());
                throw BusinessException.unauthorized("로그인 실패 횟수를 초과하여 계정이 잠겼습니다.");
            }
            throw BusinessException.unauthorized("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        if (!member.getIsActive()) {
            if (member.getWithdrawnAt() != null) {
                throw BusinessException.unauthorized("탈퇴한 계정입니다.");
            }
            throw BusinessException.unauthorized("이메일 인증이 필요합니다.");
        }

        member.resetLoginFailCount();
        saveLoginLog(member.getId(), "LOGIN", ip, userAgent, true);
        operationLogService.audit(member.getId(), "LOGIN", "members", member.getId(), null, member.getUsername(), httpRequest);

        SiteSetting setting = siteSettingRepository.findTopByOrderByIdAsc().orElse(null);
        long expiryMs = getSessionTimeoutMin(setting) * 60_000L;
        boolean allowConcurrentLogin = setting == null || Boolean.TRUE.equals(setting.getAllowConcurrentLogin());
        String token = jwtProvider.createAccessToken(member.getId(), member.getUsername(), expiryMs);
        tokenSessionService.registerLogin(member.getId(), token, allowConcurrentLogin);

        List<GroupResponse> groups = groupService.getMemberGroups(member.getId());
        return new LoginResponse(token, member, groups, termsService.getRequiredTerms(member.getId()));
    }

    @Transactional
    public void verifyEmail(String token, HttpServletRequest httpRequest) {
        validatePurpose(token, "EMAIL_VERIFY");
        Long memberId = jwtProvider.getMemberId(token);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));
        member.activate();
        tokenSessionService.invalidateToken(token);
        operationLogService.audit(memberId, "UPDATE", "members", memberId, "inactive", "active", httpRequest);
    }

    // 인증 메일을 놓쳤거나 24시간 만료로 링크가 죽었을 때 재발송. 존재하지 않는 이메일이거나
    // 이미 인증된 계정이어도 동일하게 조용히 반환해 계정 존재 여부가 노출되지 않도록 한다.
    @Transactional
    public void resendVerificationEmail(PasswordResetRequest request) {
        memberRepository.findByEmail(request.getEmail())
                .filter(member -> !Boolean.TRUE.equals(member.getIsActive()))
                .ifPresent(this::sendVerificationEmail);
    }

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        memberRepository.findByEmail(request.getEmail()).ifPresent(member -> {
            String token = jwtProvider.createPurposeToken(member.getId(), member.getUsername(), "PASSWORD_RESET", 1000L * 60 * 30);
            String link = frontendUrl + "/reset-password?token=" + token;
            String html = mailTemplateService.render(MailTemplateService.ICON_LOCK, MailTemplateService.TINT_ORANGE, MailTemplateService.STROKE_ORANGE,
                    "비밀번호 재설정", "아래 버튼을 눌러 새 비밀번호를 설정해주세요.<br>(30분간 유효)", null,
                    "비밀번호 재설정하기", link);
            mailService.send(member.getId(), member.getEmail(), "[Corum] 비밀번호 재설정", html, "PASSWORD_RESET");
        });
    }

    // 관리자가 회원 비밀번호를 초기화 — 임시 비밀번호를 평문으로 발급/발송하지 않고
    // 기존 비밀번호는 즉시 무효화한 뒤 본인만 여는 재설정 링크를 이메일로 보낸다.
    @Transactional
    public void adminResetPassword(Long memberId, Long adminId, HttpServletRequest httpRequest) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));
        member.updatePassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        member.unlock();
        memberRepository.save(member);
        tokenSessionService.invalidateMember(memberId);

        String token = jwtProvider.createPurposeToken(member.getId(), member.getUsername(), "PASSWORD_RESET", 1000L * 60 * 30);
        String link = frontendUrl + "/reset-password?token=" + token;
        String memberName = member.getName() != null ? member.getName() : member.getUsername();
        String html = mailTemplateService.render(MailTemplateService.ICON_LOCK, MailTemplateService.TINT_ORANGE, MailTemplateService.STROKE_ORANGE,
                "비밀번호가 초기화되었습니다", memberName + "님의 비밀번호가 관리자에 의해 초기화되었습니다.<br>아래 버튼에서 새 비밀번호를 설정해주세요. (30분간 유효)", null,
                "비밀번호 재설정하기", link);
        mailService.sendAsync(member.getId(), member.getEmail(), "[Corum] 비밀번호 재설정 안내", html, "PASSWORD_RESET");
        operationLogService.audit(adminId, "UPDATE", "members", memberId, null, "password_reset_by_admin", httpRequest);
    }

    @Transactional
    public void resetPassword(PasswordResetConfirmRequest request, HttpServletRequest httpRequest) {
        validatePurpose(request.getToken(), "PASSWORD_RESET");
        Long memberId = jwtProvider.getMemberId(request.getToken());
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));
        member.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        member.unlock();
        member.clearMustChangePassword();
        tokenSessionService.invalidateMember(memberId);
        tokenSessionService.invalidateToken(request.getToken());
        operationLogService.audit(memberId, "UPDATE", "members", memberId, "password_reset_requested", "password_reset_done", httpRequest);
    }

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
        List<Long> groupIds = memberGroupRepository.findGroupIdsByMemberId(memberId);
        return new MemberResponse(member, isAdmin, termsService.getRequiredTerms(memberId), groupIds,
                postRepository.countByMemberId(memberId), commentRepository.countByMemberIdAndIsDeletedFalse(memberId));
    }

    @Transactional
    public void updatePassword(Long memberId, UpdatePasswordRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));
        if (!passwordEncoder.matches(request.getCurrentPassword(), member.getPasswordHash())) {
            throw new BusinessException("현재 비밀번호가 올바르지 않습니다.");
        }
        member.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        member.clearMustChangePassword();
        tokenSessionService.invalidateMember(memberId);
    }

    @Transactional
    public void withdraw(Long memberId, WithdrawRequest request, HttpServletRequest httpRequest) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));
        if (!passwordEncoder.matches(request.getPassword(), member.getPasswordHash())) {
            throw new BusinessException("비밀번호가 올바르지 않습니다.");
        }
        member.withdraw(getClientIp(httpRequest));
        tokenSessionService.invalidateMember(memberId);
        operationLogService.audit(memberId, "DELETE", "members", memberId, null, "withdrawn", httpRequest);
    }

    @Transactional
    public void logout(Long memberId, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        tokenSessionService.invalidateToken(resolveToken(httpRequest));
        saveLoginLog(memberId, "LOGOUT", ip, userAgent, true);
        operationLogService.audit(memberId, "LOGOUT", "members", memberId, null, null, httpRequest);
    }

    @Transactional(readOnly = true)
    public MemberResponse getMe(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));
        boolean isAdmin = memberGroupRepository.existsAdminGroupByMemberId(memberId);
        List<Long> groupIds = memberGroupRepository.findGroupIdsByMemberId(memberId);
        return new MemberResponse(member, isAdmin, termsService.getRequiredTerms(memberId), groupIds,
                postRepository.countByMemberId(memberId), commentRepository.countByMemberIdAndIsDeletedFalse(memberId));
    }

    @Transactional
    public MemberResponse uploadProfileImage(Long memberId, MultipartFile file) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));
        String imageUrl = fileStorageService.uploadProfileImage(memberId, file);
        member.updateProfile(member.getName(), member.getPhone(), member.getAddress(),
                member.getBirthDate(), member.getHomePhone(), member.getOccupation(),
                member.getWorkPhone(), member.getNewsletterYn(), imageUrl);
        boolean isAdmin = memberGroupRepository.existsAdminGroupByMemberId(memberId);
        List<Long> groupIds = memberGroupRepository.findGroupIdsByMemberId(memberId);
        return new MemberResponse(member, isAdmin, termsService.getRequiredTerms(memberId), groupIds,
                postRepository.countByMemberId(memberId), commentRepository.countByMemberIdAndIsDeletedFalse(memberId));
    }

    private void sendVerificationEmail(Member member) {
        String token = jwtProvider.createPurposeToken(member.getId(), member.getUsername(), "EMAIL_VERIFY", 1000L * 60 * 60 * 24);
        String link = frontendUrl + "/verify-email?token=" + token;
        String html = mailTemplateService.render(MailTemplateService.ICON_CHECK, MailTemplateService.TINT_GREEN, MailTemplateService.STROKE_GREEN,
                "이메일 인증", "아래 버튼을 눌러 이메일 인증을 완료해주세요.", null,
                "이메일 인증하기", link);
        // 비동기 + 예외 무시 — 메일 발송 실패가 register()의 회원 저장 트랜잭션까지
        // 롤백시키지 않도록 한다 (SMTP 장애 시에도 계정은 정상 생성되어야 함)
        mailService.sendAsync(member.getId(), member.getEmail(), "[Corum] 이메일 인증", html, "EMAIL_VERIFY");
    }

    private void validatePurpose(String token, String purpose) {
        if (!jwtProvider.validate(token) || !purpose.equals(jwtProvider.getPurpose(token))) {
            throw BusinessException.unauthorized("유효하지 않은 토큰입니다.");
        }
        if (tokenSessionService.isInvalidated(token)) {
            throw BusinessException.unauthorized("이미 사용되었거나 만료된 링크입니다.");
        }
    }

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
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private int getLoginFailLimit() {
        return siteSettingRepository.findTopByOrderByIdAsc()
                .map(SiteSetting::getLoginFailLimit)
                .filter(limit -> limit > 0)
                .orElse(loginFailLimit);
    }

    private int getSessionTimeoutMin(SiteSetting setting) {
        if (setting == null || setting.getSessionTimeoutMin() == null || setting.getSessionTimeoutMin() <= 0) {
            return 30;
        }
        return setting.getSessionTimeoutMin();
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
