// 이미지 노드의 width/align 속성으로부터 style 문자열을 만든다.
// 에디터 내부(NodeView)와 저장되는 HTML(스키마 renderHTML) 양쪽에서 동일하게 사용해
// 편집 화면과 실제 게시글 화면의 모양이 일치하도록 한다.
export function buildImageStyle({ width, height, align }) {
  const parts = ['max-width: 100%']
  if (width) parts.push(`width: ${width}`)
  // 가로/세로 개별 조절 시에만 값이 채워진다 — 비율 유지 모드(코너 핸들)에서는 비워둬서
  // height:auto로 원본 비율을 그대로 따르게 한다.
  if (height) parts.push(`height: ${height}`)

  if (align === 'left') {
    parts.push('float: left', 'margin: 4px 16px 8px 0')
  } else if (align === 'right') {
    parts.push('float: right', 'margin: 4px 0 8px 16px')
  } else if (align === 'inline') {
    // 워드의 '텍스트 줄 안'과 동일 — 이미지를 글자처럼 텍스트 흐름 안에 배치한다
    parts.push('display: inline-block', 'vertical-align: bottom', 'margin: 0 4px')
  } else {
    parts.push('display: block', 'margin: 0.6em auto')
  }

  return parts.join('; ')
}
