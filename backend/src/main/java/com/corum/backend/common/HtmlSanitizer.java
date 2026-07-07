package com.corum.backend.common;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;

import java.util.regex.Pattern;

/**
 * TipTap 위지윅 에디터로 작성된 HTML을 저장하기 전에 정제한다.
 * TextAlign/Color/FontSize/Highlight 확장이 인라인 style을, 첨부 이미지가 상대경로 src를
 * 생성하므로 style 전역 허용 + preserveRelativeLinks(true)가 필요하다.
 * 표/체크리스트는 style 허용만으로 충분하지만, 유튜브 임베드(iframe)는 임의 사이트를
 * 심을 수 있는 위험이 있어 src를 유튜브 임베드 URL로만 제한하는 후처리 검증을 거친다.
 */
public final class HtmlSanitizer {

    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("hr", "s", "mark", "label", "input", "iframe", "video", "source")
            .addAttributes(":all", "style", "class")
            .addAttributes("a", "target", "rel")
            .addAttributes("img", "width", "height")
            .addAttributes("ul", "data-type")
            .addAttributes("li", "data-checked")
            .addAttributes("input", "type", "checked")
            .addAttributes("iframe", "src", "width", "height", "frameborder", "allow", "allowfullscreen")
            // TipTap Youtube 확장의 parseHTML은 div[data-youtube-video] iframe 형태만 인식하므로
            // 이 속성이 사라지면 재편집 시 유튜브 임베드를 인식하지 못해 통째로 사라진다.
            .addAttributes("div", "data-youtube-video")
            .addAttributes("blockquote", "data-variant")
            .addAttributes("video", "src", "controls", "width", "height", "preload")
            .addAttributes("source", "src", "type")
            .addProtocols("img", "src", "http", "https")
            .addProtocols("iframe", "src", "https")
            .addProtocols("video", "src", "http", "https")
            .addProtocols("source", "src", "http", "https")
            .preserveRelativeLinks(true);

    // 유튜브 임베드 URL만 허용 (그 외 iframe은 전부 제거) - 임의 사이트 삽입/클릭재킹 방지
    private static final Pattern YOUTUBE_EMBED_SRC =
            Pattern.compile("^https://(www\\.)?youtube(-nocookie)?\\.com/embed/[A-Za-z0-9_-]+(\\?.*)?$");

    private HtmlSanitizer() {
    }

    // baseUri 없이 Jsoup.clean(html, safelist)를 쓰면 상대경로 URL의 프로토콜을
    // 판단할 기준이 없어 preserveRelativeLinks(true)여도 href/src가 통째로 drop된다.
    // 임의의 http(s) baseUri를 지정해 상대경로가 허용 프로토콜로 해석되게 한다.
    private static final String BASE_URI = "http://localhost/";

    public static String sanitize(String html) {
        if (html == null || html.isBlank()) return html;
        String cleaned = Jsoup.clean(html, BASE_URI, SAFELIST);
        return stripInvalidIframes(cleaned);
    }

    // Safelist는 태그/속성/프로토콜만 검증할 뿐 도메인은 볼 수 없으므로,
    // src가 유튜브 임베드 URL 형식이 아닌 iframe은 정제 후 별도로 제거한다.
    private static String stripInvalidIframes(String html) {
        if (!html.contains("<iframe")) return html;
        Document doc = Jsoup.parseBodyFragment(html, BASE_URI);
        for (Element iframe : doc.select("iframe")) {
            if (!YOUTUBE_EMBED_SRC.matcher(iframe.attr("src")).matches()) {
                iframe.remove();
            }
        }
        return doc.body().html();
    }
}
