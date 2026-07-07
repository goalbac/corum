import { Node, mergeAttributes } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import VideoNode from './VideoNode.vue'

// 자체 업로드 동영상(mp4/webm/ogg)을 <video controls> 태그로 삽입하는 노드
export const Video = Node.create({
  name: 'video',
  group: 'block',
  atom: true,
  draggable: true,

  addAttributes() {
    return {
      src: { default: null },
      width: {
        default: null,
        parseHTML: element => element.style.width || null,
        renderHTML: attributes => (attributes.width ? { style: `width: ${attributes.width}` } : {}),
      },
    }
  },

  parseHTML() {
    return [{ tag: 'video' }]
  },

  renderHTML({ HTMLAttributes }) {
    return ['video', mergeAttributes(HTMLAttributes, { controls: 'true', preload: 'metadata' })]
  },

  addNodeView() {
    return VueNodeViewRenderer(VideoNode)
  },

  addCommands() {
    return {
      setVideo: (options) => ({ commands }) => commands.insertContent({ type: this.name, attrs: options }),
    }
  },
})
