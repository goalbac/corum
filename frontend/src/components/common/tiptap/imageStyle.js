// 이미지 노드의 width/align 속성으로부터 style 문자열을 만든다.
// 에디터 내부(NodeView)와 저장되는 HTML(스키마 renderHTML) 양쪽에서 동일하게 사용해
// 편집 화면과 실제 게시글 화면의 모양이 일치하도록 한다.
export function buildImageStyle({ width, align }) {
  const parts = ['max-width: 100%']
  if (width) parts.push(`width: ${width}`)

  if (align === 'left') {
    parts.push('float: left', 'margin: 4px 16px 8px 0')
  } else if (align === 'right') {
    parts.push('float: right', 'margin: 4px 0 8px 16px')
  } else {
    parts.push('display: block', 'margin: 0.6em auto')
  }

  return parts.join('; ')
}
