package com.corum.backend.service.mail;

import com.corum.backend.common.BusinessException;
import com.corum.backend.domain.log.SmtpSendLog;
import com.corum.backend.domain.log.SmtpSendLogRepository;
import com.corum.backend.domain.setting.SiteSetting;
import com.corum.backend.domain.setting.SiteSettingRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Service
@RequiredArgsConstructor
public class MailService {

    private final SiteSettingRepository siteSettingRepository;
    private final SmtpSendLogRepository smtpSendLogRepository;

    public void send(Long memberId, String toEmail, String subject, String html, String sendType) {
        try {
            SiteSetting setting = siteSettingRepository.findTopByOrderByIdAsc()
                    .orElseThrow(() -> BusinessException.notFound("사이트 설정을 찾을 수 없습니다."));
            if (setting.getSmtpHost() == null || setting.getSmtpHost().isBlank()) {
                throw new BusinessException("SMTP Host가 설정되지 않았습니다.");
            }
            JavaMailSenderImpl sender = createSender(setting);
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(setting.getSmtpUsername());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            sender.send(message);
            saveLog(memberId, toEmail, subject, sendType, true, null);
        } catch (Exception e) {
            saveLog(memberId, toEmail, subject, sendType, false, e.getMessage());
            throw new BusinessException("메일 발송에 실패했습니다: " + e.getMessage());
        }
    }

    @Async
    public void sendAsync(Long memberId, String toEmail, String subject, String html, String sendType) {
        try {
            send(memberId, toEmail, subject, html, sendType);
        } catch (Exception ignored) { }
    }

    public void sendToAdmin(String subject, String html, String sendType) {
        SiteSetting setting = siteSettingRepository.findTopByOrderByIdAsc()
                .orElseThrow(() -> BusinessException.notFound("사이트 설정을 찾을 수 없습니다."));
        if (setting.getSmtpUsername() == null || setting.getSmtpUsername().isBlank()) {
            saveLog(null, "", subject, sendType, false, "SMTP Username이 설정되지 않았습니다.");
            return;
        }
        send(null, setting.getSmtpUsername(), subject, html, sendType);
    }

    private JavaMailSenderImpl createSender(SiteSetting setting) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(setting.getSmtpHost());
        sender.setPort(setting.getSmtpPort());
        sender.setUsername(setting.getSmtpUsername());
        sender.setPassword(setting.getSmtpPasswordEnc());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        int port = setting.getSmtpPort() != null ? setting.getSmtpPort() : 587;
        boolean useTls = Boolean.TRUE.equals(setting.getSmtpUseTls());
        // 포트 465 = SSL(SMTPS), 포트 587/25 = STARTTLS
        boolean useSsl = (port == 465);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        if (useSsl) {
            // SSL-wrapped (포트 465) — STARTTLS 비활성화, SSL 활성화
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.trust", setting.getSmtpHost());
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.smtp.starttls.required", "false");
        } else if (useTls) {
            // STARTTLS (포트 587)
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.enable", "false");
        } else {
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.smtp.ssl.enable", "false");
        }
        return sender;
    }

    private void saveLog(Long memberId, String toEmail, String subject, String sendType, boolean success, String errorMessage) {
        smtpSendLogRepository.save(SmtpSendLog.builder()
                .memberId(memberId)
                .toEmail(toEmail)
                .subject(subject)
                .sendType(sendType)
                .success(success)
                .errorMessage(errorMessage)
                .build());
    }
}
