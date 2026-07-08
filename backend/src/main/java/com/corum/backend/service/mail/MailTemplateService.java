package com.corum.backend.service.mail;

import com.corum.backend.domain.setting.SiteSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 알림 메일 공통 템플릿(브랜드 헤더바 + 아이콘 카드형).
 * 각 발신 로직은 아이콘/제목/본문/버튼 등 내용만 채우고, 로고·사이트명·주소 같은
 * 사이트 설정 기반 요소는 이 서비스가 매번 최신 site_settings 값으로 채워 넣는다.
 */
@Service
@RequiredArgsConstructor
public class MailTemplateService {

    private final SiteSettingRepository siteSettingRepository;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public static final String ICON_CHECK = "<path d=\"M12 3l8 4v5c0 5-3.5 8-8 9-4.5-1-8-4-8-9V7l8-4z\"></path><path d=\"M9 12l2 2 4-4\"></path>";
    public static final String ICON_LOCK = "<rect x=\"5\" y=\"11\" width=\"14\" height=\"9\" rx=\"2\"></rect><path d=\"M8 11V7a4 4 0 0 1 8 0v4\"></path>";
    public static final String ICON_MAIL = "<rect x=\"2\" y=\"4\" width=\"20\" height=\"16\" rx=\"2.5\"></rect><path d=\"m2.5 6.5 9.5 6 9.5-6\"></path>";
    public static final String ICON_COMMENT = "<path d=\"M21 11.5a8.4 8.4 0 0 1-9 8 9 9 0 0 1-4-1l-4 1 1-3.5A8.4 8.4 0 1 1 21 11.5z\"></path>";
    public static final String ICON_MEGAPHONE = "<path d=\"M3 11l18-5v12L3 14v-3z\"></path><path d=\"M11.6 16.8a3 3 0 0 1-5.8-1.6\"></path>";

    public static final String TINT_GREEN = "#e3f5ec";
    public static final String STROKE_GREEN = "#1f9d6b";
    public static final String TINT_BLUE = "#e9f0fe";
    public static final String STROKE_BLUE = "#2f5fd6";
    public static final String TINT_ORANGE = "rgba(196,127,28,0.12)";
    public static final String STROKE_ORANGE = "#c47f1c";

    /**
     * @param iconPath   위 ICON_* 상수 중 하나(아이콘 없이 쓰려면 null)
     * @param iconBg     아이콘 원 배경색(TINT_* 상수)
     * @param iconStroke 아이콘 선 색(STROKE_* 상수)
     * @param title      본문 중앙 굵은 제목
     * @param message    제목 아래 회색 설명 문구(HTML 가능, null이면 생략)
     * @param cardHtml   회색 박스 카드(인용/미리보기 등, null이면 생략)
     * @param ctaText    버튼 문구(null이면 버튼 생략)
     * @param ctaUrl     버튼 링크
     */
    public String render(String iconPath, String iconBg, String iconStroke,
                          String title, String message, String cardHtml,
                          String ctaText, String ctaUrl) {
        var setting = siteSettingRepository.findTopByOrderByIdAsc().orElse(null);
        String siteName = (setting != null && setting.getSiteName() != null && !setting.getSiteName().isBlank())
                ? setting.getSiteName() : "Corum";
        String address = (setting != null && setting.getContactAddress() != null) ? setting.getContactAddress() : "";
        String logoUrl = (setting != null) ? setting.getLogoUrl() : null;

        String logoHtml = (logoUrl != null && !logoUrl.isBlank())
                ? "<img src=\"" + frontendUrl + logoUrl + "\" alt=\"" + siteName + "\" width=\"32\" height=\"32\" style=\"width:32px;height:32px;border-radius:9px;object-fit:cover;display:block;\" />"
                : "<div style=\"width:32px;height:32px;border-radius:9px;background:rgba(255,255,255,0.16);display:flex;align-items:center;justify-content:center;color:#fff;font-weight:800;font-size:17px;\">"
                        + siteName.substring(0, 1) + "</div>";

        String iconHtml = (iconPath != null)
                ? "<div style=\"display:flex;justify-content:center;margin-bottom:18px;\">"
                        + "<span style=\"width:52px;height:52px;border-radius:14px;background:" + iconBg + ";display:flex;align-items:center;justify-content:center;\">"
                        + "<svg width=\"26\" height=\"26\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"" + iconStroke + "\" stroke-width=\"1.9\" stroke-linecap=\"round\" stroke-linejoin=\"round\">" + iconPath + "</svg>"
                        + "</span></div>"
                : "";

        String messageHtml = (message != null)
                ? "<div style=\"text-align:center;font-size:14px;color:#566072;line-height:1.6;margin-bottom:24px;\">" + message + "</div>"
                : "";

        String cardBlock = (cardHtml != null)
                ? "<div style=\"background:#f5f7fa;border:1px solid #e4e8ee;border-radius:12px;padding:18px 20px;margin-bottom:14px;\">" + cardHtml + "</div>"
                : "";

        String ctaBlock = (ctaText != null && ctaUrl != null)
                ? "<div style=\"text-align:center;margin-top:24px;\"><a href=\"" + ctaUrl + "\" style=\"display:inline-block;background:#2f5fd6;color:#fff;font-size:15px;font-weight:700;padding:14px 40px;border-radius:11px;text-decoration:none;\">" + ctaText + "</a></div>"
                : "";

        return "<div style=\"font-family:'Pretendard','Malgun Gothic',-apple-system,sans-serif;padding:40px 16px;background:#e7ebf1;\">"
                + "<div style=\"max-width:600px;margin:0 auto;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 8px 30px rgba(20,30,55,0.10);\">"
                + "<div style=\"background:#2f5fd6;padding:20px 32px;display:flex;align-items:center;gap:11px;\">"
                + logoHtml
                + "<div style=\"font-size:16px;font-weight:800;color:#fff;letter-spacing:-0.02em;\">" + siteName + "</div>"
                + "</div>"
                + "<div style=\"padding:34px 32px 30px;\">"
                + iconHtml
                + "<div style=\"text-align:center;font-size:20px;font-weight:800;letter-spacing:-0.02em;margin-bottom:7px;\">" + title + "</div>"
                + messageHtml
                + cardBlock
                + ctaBlock
                + "</div>"
                + "<div style=\"border-top:1px solid #eef1f6;padding:22px 32px;background:#fbfcfe;text-align:center;\">"
                + "<div style=\"font-size:12px;color:#929aab;line-height:1.7;\">본 메일은 알림 설정에 따라 발송되었습니다.</div>"
                + "<div style=\"font-size:11px;color:#b6bdc9;margin-top:12px;\">" + siteName + (address.isBlank() ? "" : " · " + address) + "</div>"
                + "</div></div></div>";
    }
}
