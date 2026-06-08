<template>
  <div class="rich-editor" :class="{ disabled }">
    <!-- 툴바 -->
    <div class="editor-toolbar">
      <!-- 텍스트 스타일 -->
      <div class="toolbar-group">
        <button type="button" title="굵게 (Ctrl+B)" :class="{ active: editor?.isActive('bold') }"
          @click="editor?.chain().focus().toggleBold().run()">
          <i class="ti ti-bold"></i>
        </button>
        <button type="button" title="기울임 (Ctrl+I)" :class="{ active: editor?.isActive('italic') }"
          @click="editor?.chain().focus().toggleItalic().run()">
          <i class="ti ti-italic"></i>
        </button>
        <button type="button" title="밑줄 (Ctrl+U)" :class="{ active: editor?.isActive('underline') }"
          @click="editor?.chain().focus().toggleUnderline().run()">
          <i class="ti ti-underline"></i>
        </button>
        <button type="button" title="취소선" :class="{ active: editor?.isActive('strike') }"
          @click="editor?.chain().focus().toggleStrike().run()">
          <i class="ti ti-strikethrough"></i>
        </button>
      </div>
      <div class="toolbar-sep"></div>
      <!-- 제목 -->
      <div class="toolbar-group">
        <button type="button" :class="{ active: editor?.isActive('heading', { level: 1 }) }"
          @click="editor?.chain().focus().toggleHeading({ level: 1 }).run()" title="제목 1">
          H1
        </button>
        <button type="button" :class="{ active: editor?.isActive('heading', { level: 2 }) }"
          @click="editor?.chain().focus().toggleHeading({ level: 2 }).run()" title="제목 2">
          H2
        </button>
        <button type="button" :class="{ active: editor?.isActive('heading', { level: 3 }) }"
          @click="editor?.chain().focus().toggleHeading({ level: 3 }).run()" title="제목 3">
          H3
        </button>
      </div>
      <div class="toolbar-sep"></div>
      <!-- 정렬 -->
      <div class="toolbar-group">
        <button type="button" title="왼쪽 정렬" :class="{ active: editor?.isActive({ textAlign: 'left' }) }"
          @click="editor?.chain().focus().setTextAlign('left').run()">
          <i class="ti ti-align-left"></i>
        </button>
        <button type="button" title="가운데 정렬" :class="{ active: editor?.isActive({ textAlign: 'center' }) }"
          @click="editor?.chain().focus().setTextAlign('center').run()">
          <i class="ti ti-align-center"></i>
        </button>
        <button type="button" title="오른쪽 정렬" :class="{ active: editor?.isActive({ textAlign: 'right' }) }"
          @click="editor?.chain().focus().setTextAlign('right').run()">
          <i class="ti ti-align-right"></i>
        </button>
      </div>
      <div class="toolbar-sep"></div>
      <!-- 목록/인용/코드 -->
      <div class="toolbar-group">
        <button type="button" title="글머리 기호" :class="{ active: editor?.isActive('bulletList') }"
          @click="editor?.chain().focus().toggleBulletList().run()">
          <i class="ti ti-list"></i>
        </button>
        <button type="button" title="번호 목록" :class="{ active: editor?.isActive('orderedList') }"
          @click="editor?.chain().focus().toggleOrderedList().run()">
          <i class="ti ti-list-numbers"></i>
        </button>
        <button type="button" title="인용구" :class="{ active: editor?.isActive('blockquote') }"
          @click="editor?.chain().focus().toggleBlockquote().run()">
          <i class="ti ti-quote"></i>
        </button>
        <button type="button" title="코드 블록" :class="{ active: editor?.isActive('codeBlock') }"
          @click="editor?.chain().focus().toggleCodeBlock().run()">
          <i class="ti ti-code"></i>
        </button>
        <button type="button" title="가로선" @click="editor?.chain().focus().setHorizontalRule().run()">
          <i class="ti ti-separator"></i>
        </button>
      </div>
      <div class="toolbar-sep"></div>
      <!-- 링크/이미지 -->
      <div class="toolbar-group">
        <button type="button" title="링크" :class="{ active: editor?.isActive('link') }"
          @click="insertLink">
          <i class="ti ti-link"></i>
        </button>
        <button type="button" title="링크 제거" @click="editor?.chain().focus().unsetLink().run()">
          <i class="ti ti-link-off"></i>
        </button>
        <button type="button" title="이미지 URL 삽입" @click="insertImageUrl">
          <i class="ti ti-photo"></i>
        </button>
      </div>
      <!-- 글자색 -->
      <div class="toolbar-group">
        <label class="color-btn" title="글자색">
          <i class="ti ti-palette"></i>
          <input type="color" class="color-input" @input="setColor" />
        </label>
        <button type="button" title="색상 초기화" @click="editor?.chain().focus().unsetColor().run()">
          <i class="ti ti-color-swatch-off"></i>
        </button>
      </div>
      <div class="toolbar-sep"></div>
      <!-- undo/redo -->
      <div class="toolbar-group">
        <button type="button" title="실행 취소" @click="editor?.chain().focus().undo().run()">
          <i class="ti ti-arrow-back-up"></i>
        </button>
        <button type="button" title="다시 실행" @click="editor?.chain().focus().redo().run()">
          <i class="ti ti-arrow-forward-up"></i>
        </button>
      </div>
    </div>

    <!-- 에디터 본문 -->
    <EditorContent class="editor-body" :editor="editor" />
  </div>
</template>

<script setup>
import { onBeforeUnmount, watch } from 'vue'
import { Editor, EditorContent } from '@tiptap/vue-3'
import { StarterKit } from '@tiptap/starter-kit'
import { Link } from '@tiptap/extension-link'
import { Image } from '@tiptap/extension-image'
import { TextAlign } from '@tiptap/extension-text-align'
import { Color } from '@tiptap/extension-color'
import { TextStyle } from '@tiptap/extension-text-style'
import { Underline } from '@tiptap/extension-underline'
import { Placeholder } from '@tiptap/extension-placeholder'
import { ElMessageBox } from 'element-plus'

const props = defineProps({
  modelValue: { type: String, default: '' },
  placeholder: { type: String, default: '내용을 입력하세요.' },
  disabled: { type: Boolean, default: false },
  minHeight: { type: String, default: '360px' },
})
const emit = defineEmits(['update:modelValue'])

const editor = new Editor({
  extensions: [
    StarterKit,
    Underline,
    TextStyle,
    Color,
    TextAlign.configure({ types: ['heading', 'paragraph'] }),
    Link.configure({ openOnClick: false, autolink: true }),
    Image.configure({ inline: false }),
    Placeholder.configure({ placeholder: props.placeholder }),
  ],
  content: props.modelValue || '',
  editorProps: {
    attributes: { class: 'tiptap-body' }
  },
  onUpdate: ({ editor }) => {
    emit('update:modelValue', editor.getHTML())
  },
  editable: !props.disabled,
})

watch(() => props.modelValue, val => {
  if (editor.getHTML() !== val) {
    editor.commands.setContent(val || '', false)
  }
})

watch(() => props.disabled, val => {
  editor.setEditable(!val)
})

async function insertLink() {
  const prev = editor.getAttributes('link').href || ''
  try {
    const { value } = await ElMessageBox.prompt('링크 URL을 입력하세요.', '링크 삽입', {
      inputValue: prev,
      inputPlaceholder: 'https://example.com',
      confirmButtonText: '확인',
      cancelButtonText: '취소',
    })
    if (value) {
      editor.chain().focus().setLink({ href: value, target: '_blank' }).run()
    }
  } catch {}
}

async function insertImageUrl() {
  try {
    const { value } = await ElMessageBox.prompt('이미지 URL을 입력하세요.', '이미지 삽입', {
      inputPlaceholder: 'https://example.com/image.jpg',
      confirmButtonText: '확인',
      cancelButtonText: '취소',
    })
    if (value) {
      editor.chain().focus().setImage({ src: value }).run()
    }
  } catch {}
}

function setColor(e) {
  editor.chain().focus().setColor(e.target.value).run()
}

onBeforeUnmount(() => { editor.destroy() })
</script>

<style scoped>
.rich-editor {
  border: 0.5px solid var(--border2);
  border-radius: var(--radius-xs);
  background: var(--surface);
  overflow: hidden;
  transition: var(--transition);
}
.rich-editor:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 0 2px var(--accent-bg);
}
.rich-editor.disabled { opacity: 0.5; pointer-events: none; }

/* ===== 툴바 ===== */
.editor-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 2px;
  padding: 6px 8px;
  border-bottom: 0.5px solid var(--border);
  background: var(--surface2);
}
.toolbar-group { display: flex; align-items: center; gap: 1px; }
.toolbar-sep {
  width: 1px;
  height: 18px;
  background: var(--border);
  margin: 0 4px;
}
.editor-toolbar button,
.color-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: var(--t2);
  cursor: pointer;
  font-size: 14px;
  font-family: inherit;
  font-weight: 600;
  transition: var(--transition);
  padding: 0;
  line-height: 1;
}
.editor-toolbar button:hover,
.color-btn:hover { background: var(--surface); color: var(--t1); }
.editor-toolbar button.active { background: var(--accent-bg); color: var(--accent); }

.color-btn { cursor: pointer; position: relative; }
.color-input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
  pointer-events: none;
}
.color-btn:hover .color-input { pointer-events: auto; }
.color-btn i { pointer-events: none; }

/* ===== 에디터 본문 ===== */
.editor-body {
  padding: 16px 20px;
  min-height: v-bind(minHeight);
  cursor: text;
}
.editor-body :deep(.ProseMirror) {
  outline: none;
  color: var(--t1);
  font-size: 15px;
  line-height: 1.8;
  min-height: v-bind(minHeight);
}
.editor-body :deep(.ProseMirror p) { margin: 0 0 0.6em; }
.editor-body :deep(.ProseMirror h1) { font-size: 1.8em; font-weight: 800; margin: 1em 0 0.4em; }
.editor-body :deep(.ProseMirror h2) { font-size: 1.4em; font-weight: 700; margin: 0.9em 0 0.4em; }
.editor-body :deep(.ProseMirror h3) { font-size: 1.15em; font-weight: 700; margin: 0.8em 0 0.3em; }
.editor-body :deep(.ProseMirror ul) { padding-left: 1.5em; margin: 0.4em 0; }
.editor-body :deep(.ProseMirror ol) { padding-left: 1.5em; margin: 0.4em 0; }
.editor-body :deep(.ProseMirror li) { margin: 0.2em 0; }
.editor-body :deep(.ProseMirror blockquote) {
  border-left: 3px solid var(--accent);
  padding-left: 16px;
  color: var(--t2);
  margin: 0.8em 0;
}
.editor-body :deep(.ProseMirror code) {
  background: var(--surface2);
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 0.9em;
  font-family: 'Fira Code', monospace;
  color: var(--accent);
}
.editor-body :deep(.ProseMirror pre) {
  background: var(--surface2);
  border-radius: var(--radius-xs);
  padding: 16px;
  overflow-x: auto;
  margin: 0.8em 0;
}
.editor-body :deep(.ProseMirror pre code) {
  background: none;
  padding: 0;
  color: var(--t1);
  font-size: 14px;
}
.editor-body :deep(.ProseMirror a) {
  color: var(--accent);
  text-decoration: underline;
  cursor: pointer;
}
.editor-body :deep(.ProseMirror img) {
  max-width: 100%;
  border-radius: var(--radius-xs);
  margin: 0.6em 0;
}
.editor-body :deep(.ProseMirror hr) {
  border: none;
  border-top: 1px solid var(--border);
  margin: 1em 0;
}
.editor-body :deep(.ProseMirror .is-editor-empty:first-child::before) {
  content: attr(data-placeholder);
  color: var(--t4);
  pointer-events: none;
  float: left;
  height: 0;
}
</style>
