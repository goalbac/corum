package com.corum.backend.common;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * TipTap 위지윅 에디터로 작성된 HTML을 저장하기 전에 정제한다.
 * TextAlign/Color 확장이 인라인 style을, 첨부 이미지가 상대경로 src를 생성하므로
 * style 전역 허용 + preserveRelativeLinks(true)가 필요하다.
 */
public final class HtmlSanitizer {

    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("hr", "s")
            .addAttributes(":all", "style", "class")
            .addAttributes("a", "target", "rel")
            .addAttributes("img", "width", "height")
            .addProtocols("img", "src", "http", "https")
            .preserveRelativeLinks(true);

    private HtmlSanitizer() {
    }

    public static String sanitize(String html) {
        if (html == null || html.isBlank()) return html;
        return Jsoup.clean(html, SAFELIST);
    }
}
