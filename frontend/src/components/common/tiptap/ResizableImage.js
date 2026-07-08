import Image from '@tiptap/extension-image'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import { buildImageStyle } from './imageStyle'
import ResizableImageNode from './ResizableImageNode.vue'

// width(리사이즈)/align(정렬) 속성은 두 이미지 노드(블록/인라인)가 동일하게 사용한다.
function imageAttributes() {
  return {
    width: {
      default: null,
      parseHTML: element => element.style.width || null,
      // 실제 style 렌더링은 align 속성에서 width까지 합쳐 한 번에 처리한다
      renderHTML: () => ({}),
    },
    height: {
      default: null,
      parseHTML: element => element.style.height || null,
      // width와 마찬가지로 실제 style 렌더링은 align 속성에서 처리한다
      renderHTML: () => ({}),
    },
    align: {
      default: null,
      parseHTML: element => {
        if (element.style.float === 'left') return 'left'
        if (element.style.float === 'right') return 'right'
        if (element.style.display === 'inline-block') return 'inline'
        return null
      },
      renderHTML: attributes => ({
        style: buildImageStyle(attributes),
      }),
    },
  }
}

// 기본 Image 확장(block)에 width/align 속성과 드래그 리사이즈 NodeView를 추가한다.
export const ResizableImage = Image.extend({
  addAttributes() {
    return { ...this.parent?.(), ...imageAttributes() }
  },

  addNodeView() {
    return VueNodeViewRenderer(ResizableImageNode)
  },
})

// '텍스트 줄 안' 배치용 별도 인라인 노드. ProseMirror는 노드의 inline/group을
// 인스턴스별 속성으로 바꿀 수 없어(스키마에 고정), 워드의 '텍스트 줄 안'처럼
// 텍스트와 같은 줄에 흐르게 하려면 별도의 인라인 그룹 노드 타입이 필요하다.
// (정렬 전환 시 ResizableImageNode.vue에서 이 노드와 서로 타입을 바꿔치기한다)
export const ResizableImageInline = Image.extend({
  name: 'imageInline',
  inline: true,
  group: 'inline',

  addAttributes() {
    return { ...this.parent?.(), ...imageAttributes() }
  },

  parseHTML() {
    return [{ tag: 'img[style*="inline-block"]', priority: 100 }]
  },

  addNodeView() {
    return VueNodeViewRenderer(ResizableImageNode)
  },

  addCommands() {
    return {
      setInlineImage: (options) => ({ commands }) => commands.insertContent({ type: this.name, attrs: options }),
    }
  },
})
