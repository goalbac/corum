import Blockquote from '@tiptap/extension-blockquote'

// 인용구 스타일(기본/박스/강조)을 data-variant 속성으로 저장한다.
// 실제 스타일은 RichEditor.vue의 .ProseMirror blockquote[data-variant=...] CSS에서 처리한다.
export const StyledBlockquote = Blockquote.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      variant: {
        default: 'default',
        parseHTML: element => element.getAttribute('data-variant') || 'default',
        renderHTML: attributes => ({ 'data-variant': attributes.variant || 'default' }),
      },
    }
  },
})
