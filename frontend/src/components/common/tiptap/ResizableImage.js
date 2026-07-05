import Image from '@tiptap/extension-image'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import { buildImageStyle } from './imageStyle'
import ResizableImageNode from './ResizableImageNode.vue'

// 기본 Image 확장에 width(리사이즈)/align(정렬) 속성과
// 드래그 리사이즈가 가능한 NodeView를 추가한다.
export const ResizableImage = Image.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      width: {
        default: null,
        parseHTML: element => element.style.width || null,
        // 실제 style 렌더링은 align 속성에서 width까지 합쳐 한 번에 처리한다
        renderHTML: () => ({}),
      },
      align: {
        default: null,
        parseHTML: element => {
          if (element.style.float === 'left') return 'left'
          if (element.style.float === 'right') return 'right'
          return null
        },
        renderHTML: attributes => ({
          style: buildImageStyle(attributes),
        }),
      },
    }
  },

  addNodeView() {
    return VueNodeViewRenderer(ResizableImageNode)
  },
})
