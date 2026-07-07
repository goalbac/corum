import DOMPurify from 'dompurify'

// 유튜브 임베드만 허용 (백엔드 HtmlSanitizer와 동일한 제한) - 그 외 iframe은 임의 사이트를
// 심을 수 있어 클릭재킹/피싱 위험이 있으므로 여기서도 한 번 더 도메인을 검증한다.
const YOUTUBE_EMBED_SRC = /^https:\/\/(www\.)?youtube(-nocookie)?\.com\/embed\/[A-Za-z0-9_-]+(\?.*)?$/

DOMPurify.addHook('uponSanitizeElement', (node, data) => {
  if (data.tagName === 'iframe' && !YOUTUBE_EMBED_SRC.test(node.getAttribute?.('src') || '')) {
    node.remove()
  }
})

// TipTap 위지윅 콘텐츠를 v-html로 렌더링하기 전 정제한다.
// 백엔드에서도 저장 시점에 sanitize하지만, 과거 저장된 데이터나
// 다른 경로로 들어온 데이터에 대한 방어선을 하나 더 둔다.
export function sanitizeHtml(html) {
  if (!html) return html
  return DOMPurify.sanitize(html, {
    ADD_TAGS: ['iframe', 'mark'],
    ADD_ATTR: ['allowfullscreen', 'frameborder', 'allow', 'data-type', 'data-checked', 'data-youtube-video', 'data-variant'],
  })
}
