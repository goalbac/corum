<template>
  <NodeViewWrapper class="resizable-image-wrapper" :style="wrapperStyle">
    <div class="resizable-image-inner" :class="{ selected }">
      <img
        :src="node.attrs.src"
        :alt="node.attrs.alt || ''"
        :title="node.attrs.title || ''"
        :style="{ width: node.attrs.width || undefined, height: node.attrs.height || undefined }"
        draggable="false"
      />

      <template v-if="editor.isEditable && selected">
        <div class="img-toolbar" @mousedown.prevent>
          <button type="button" :class="{ active: node.attrs.align === 'left' }" title="왼쪽 정렬 (오른쪽에 글 배치)" @click="setAlign('left')">
            <i class="ti ti-align-left"></i>
          </button>
          <button type="button" :class="{ active: !node.attrs.align }" title="가운데 정렬" @click="setAlign(null)">
            <i class="ti ti-align-center"></i>
          </button>
          <button type="button" :class="{ active: node.attrs.align === 'right' }" title="오른쪽 정렬 (왼쪽에 글 배치)" @click="setAlign('right')">
            <i class="ti ti-align-right"></i>
          </button>
          <span class="img-toolbar-sep"></span>
          <button type="button" :class="{ active: node.attrs.align === 'inline' }" title="텍스트 줄 안 (글자처럼 배치)" @click="setAlign('inline')">
            <i class="ti ti-letter-case"></i>
          </button>
        </div>
        <div class="resize-handle resize-handle-corner" title="비율 유지 리사이즈" @mousedown.stop.prevent="startResize"></div>
        <div class="resize-handle resize-handle-right" title="가로만 조절" @mousedown.stop.prevent="startResizeWidth"></div>
        <div class="resize-handle resize-handle-bottom" title="세로만 조절" @mousedown.stop.prevent="startResizeHeight"></div>
      </template>
    </div>
  </NodeViewWrapper>
</template>

<script setup>
import { computed } from 'vue'
import { NodeViewWrapper, nodeViewProps } from '@tiptap/vue-3'

const props = defineProps(nodeViewProps)

// 실제 저장되는 HTML(imageStyle.js의 buildImageStyle)과 개념적으로 동일하게 맞추되,
// 에디터에서는 <img>가 아닌 wrapper div가 뜨는 노드뷰 구조라 float 시 폭을 콘텐츠에
// 맞추기 위한 처리(block+float은 자동 shrink-to-fit, 가운데는 table로 shrink-wrap)가 필요하다.
const wrapperStyle = computed(() => {
  const align = props.node.attrs.align
  if (align === 'left') return { float: 'left', display: 'block', margin: '4px 16px 8px 0' }
  if (align === 'right') return { float: 'right', display: 'block', margin: '4px 0 8px 16px' }
  if (align === 'inline') return { display: 'inline-block', verticalAlign: 'bottom', margin: '0 4px' }
  return { display: 'table', margin: '0.6em auto' }
})

function setAlign(align) {
  const isCurrentlyInline = props.node.type.name === 'imageInline'
  const willBeInline = align === 'inline'

  // 좌/가운데/우 정렬끼리는 같은 block 노드이므로 속성만 바꾸면 되지만,
  // '텍스트 줄 안'은 별도의 inline 그룹 노드 타입이라 노드 자체를 바꿔치기해야 한다.
  if (isCurrentlyInline === willBeInline) {
    props.updateAttributes({ align })
    return
  }

  const pos = props.getPos()
  if (typeof pos !== 'number') return
  const targetType = willBeInline
    ? props.editor.schema.nodes.imageInline
    : props.editor.schema.nodes.image
  if (!targetType) return

  const attrs = { ...props.node.attrs, align }
  props.editor.chain().focus().command(({ tr }) => {
    tr.replaceWith(pos, pos + props.node.nodeSize, targetType.create(attrs))
    return true
  }).run()
}

// 코너 핸들 — 비율 유지 리사이즈. 이전에 가로/세로 개별 조절로 잠겨있던 height를
// 초기화해서(null) 다시 원본 비율(height:auto)을 따르도록 되돌린다.
function startResize(e) {
  const wrapper = e.currentTarget.closest('.resizable-image-inner')
  const img = wrapper?.querySelector('img')
  if (!img) return

  const startX = e.clientX
  const startWidth = img.offsetWidth
  const dir = props.node.attrs.align === 'right' ? -1 : 1
  props.updateAttributes({ height: null })

  function onMove(ev) {
    const delta = (ev.clientX - startX) * dir
    const newWidth = Math.max(60, Math.round(startWidth + delta))
    props.updateAttributes({ width: `${newWidth}px` })
  }
  function onUp() {
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
  }
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}

// 오른쪽 핸들 — 가로만 조절. 세로가 잠겨있지 않으면 현재 높이를 고정시켜
// 가로만 바뀌어도 세로가 같이 늘어나지 않게 한다.
function startResizeWidth(e) {
  const wrapper = e.currentTarget.closest('.resizable-image-inner')
  const img = wrapper?.querySelector('img')
  if (!img) return

  const startX = e.clientX
  const startWidth = img.offsetWidth
  const dir = props.node.attrs.align === 'right' ? -1 : 1
  if (!props.node.attrs.height) {
    props.updateAttributes({ height: `${img.offsetHeight}px` })
  }

  function onMove(ev) {
    const delta = (ev.clientX - startX) * dir
    const newWidth = Math.max(60, Math.round(startWidth + delta))
    props.updateAttributes({ width: `${newWidth}px` })
  }
  function onUp() {
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
  }
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}

// 아래쪽 핸들 — 세로만 조절. 가로가 잠겨있지 않으면 현재 너비를 고정시킨다.
function startResizeHeight(e) {
  const wrapper = e.currentTarget.closest('.resizable-image-inner')
  const img = wrapper?.querySelector('img')
  if (!img) return

  const startY = e.clientY
  const startHeight = img.offsetHeight
  if (!props.node.attrs.width) {
    props.updateAttributes({ width: `${img.offsetWidth}px` })
  }

  function onMove(ev) {
    const delta = ev.clientY - startY
    const newHeight = Math.max(40, Math.round(startHeight + delta))
    props.updateAttributes({ height: `${newHeight}px` })
  }
  function onUp() {
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseup', onUp)
  }
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseup', onUp)
}
</script>

<style scoped>
.resizable-image-wrapper {
  max-width: 100%;
}

.resizable-image-inner {
  position: relative;
  display: inline-block;
  max-width: 100%;
  line-height: 0;
}

.resizable-image-inner img {
  display: block;
  max-width: 100%;
  height: auto;
  border-radius: var(--radius-xs);
}

.resizable-image-inner.selected img {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

.img-toolbar {
  position: absolute;
  top: -36px;
  left: 0;
  display: flex;
  gap: 2px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 2px;
  box-shadow: var(--shadow-sm);
  z-index: 5;
}

.img-toolbar button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: var(--t2);
  cursor: pointer;
  padding: 0;
}
.img-toolbar button:hover { background: var(--surface2); color: var(--t1); }
.img-toolbar button.active { background: var(--accent-bg); color: var(--accent); }

.img-toolbar-sep {
  width: 1px;
  height: 18px;
  background: var(--border);
  margin: 0 2px;
  align-self: center;
}

.resize-handle {
  position: absolute;
  width: 14px;
  height: 14px;
  background: var(--accent);
  border: 2px solid var(--surface);
  border-radius: 50%;
  z-index: 5;
}

.resize-handle-corner {
  right: -6px;
  bottom: -6px;
  cursor: nwse-resize;
}

.resize-handle-right {
  right: -6px;
  top: 50%;
  transform: translateY(-50%);
  cursor: ew-resize;
}

.resize-handle-bottom {
  bottom: -6px;
  left: 50%;
  transform: translateX(-50%);
  cursor: ns-resize;
}
</style>
