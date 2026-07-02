import DOMPurify from 'dompurify'

// TipTap 위지윅 콘텐츠를 v-html로 렌더링하기 전 정제한다.
// 백엔드에서도 저장 시점에 sanitize하지만, 과거 저장된 데이터나
// 다른 경로로 들어온 데이터에 대한 방어선을 하나 더 둔다.
export function sanitizeHtml(html) {
  if (!html) return html
  return DOMPurify.sanitize(html)
}
