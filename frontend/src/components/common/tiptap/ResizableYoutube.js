import { Youtube } from '@tiptap/extension-youtube'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import YoutubeVideoNode from './YoutubeVideoNode.vue'

// 공식 Youtube 확장은 renderHTML에서 width/height를 노드 속성이 아닌
// 확장 옵션(this.options.width/height) 값으로 고정 출력해 개별 영상 크기 조절이
// 반영되지 않는다. 래퍼 div의 style width로 크기를 제어하는 별도 속성을 추가하고,
// 드래그로 리사이즈 가능한 NodeView를 붙인다.
export const ResizableYoutube = Youtube.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      wrapperWidth: {
        default: null,
        // parseHTML의 element는 확장 자체의 parseHTML 규칙(div[data-youtube-video] iframe)에
        // 따라 iframe이 전달되므로, 부모(래퍼 div)의 style에서 읽어야 한다.
        parseHTML: element => element.parentElement?.style?.width || null,
        renderHTML: () => ({}),
      },
    }
  },

  renderHTML(props) {
    const output = this.parent?.(props)
    const wrapperWidth = props.node.attrs.wrapperWidth
    if (!output || !wrapperWidth) return output
    const [tag, divAttrs, iframeSpec] = output
    return [tag, { ...divAttrs, style: `width: ${wrapperWidth}` }, iframeSpec]
  },

  addNodeView() {
    return VueNodeViewRenderer(YoutubeVideoNode)
  },
})
