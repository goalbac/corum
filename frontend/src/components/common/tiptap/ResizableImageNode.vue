<template>
  <NodeViewWrapper class="resizable-image-wrapper" :style="wrapperStyle">
    <div class="resizable-image-inner" :class="{ selected }">
      <img
        :src="node.attrs.src"
        :alt="node.attrs.alt || ''"
        :title="node.attrs.title || ''"
        :style="{ width: node.attrs.width || undefined }"
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
        </div>
        <div class="resize-handle" @mousedown.stop.prevent="startResize"></div>
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
  return { display: 'table', margin: '0.6em auto' }
})

function setAlign(align) {
  props.updateAttributes({ align })
}

function startResize(e) {
  const wrapper = e.currentTarget.closest('.resizable-image-inner')
  const img = wrapper?.querySelector('img')
  if (!img) return

  const startX = e.clientX
  const startWidth = img.offsetWidth
  const dir = props.node.attrs.align === 'right' ? -1 : 1

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

.resize-handle {
  position: absolute;
  right: -6px;
  bottom: -6px;
  width: 14px;
  height: 14px;
  background: var(--accent);
  border: 2px solid var(--surface);
  border-radius: 50%;
  cursor: nwse-resize;
  z-index: 5;
}
</style>
