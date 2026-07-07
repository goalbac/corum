<template>
  <NodeViewWrapper class="resizable-video-wrapper" :style="wrapperStyle">
    <div class="resizable-video-inner" :class="{ selected }">
      <video :src="node.attrs.src" controls preload="metadata"></video>
      <div v-if="editor.isEditable && selected" class="resize-handle" @mousedown.stop.prevent="startResize"></div>
    </div>
  </NodeViewWrapper>
</template>

<script setup>
import { computed } from 'vue'
import { NodeViewWrapper, nodeViewProps } from '@tiptap/vue-3'

const props = defineProps(nodeViewProps)

const wrapperStyle = computed(() => ({
  width: props.node.attrs.width || '100%',
}))

function startResize(e) {
  const wrapper = e.currentTarget.closest('.resizable-video-inner')
  const startX = e.clientX
  const startWidth = wrapper.offsetWidth

  function onMove(ev) {
    const delta = ev.clientX - startX
    const newWidth = Math.max(160, Math.round(startWidth + delta))
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
.resizable-video-wrapper {
  display: block;
  max-width: 100%;
  margin: 0.6em auto;
}

.resizable-video-inner {
  position: relative;
}

.resizable-video-inner video {
  display: block;
  width: 100%;
  border-radius: var(--radius-xs);
  background: #000;
}

.resizable-video-inner.selected video {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}

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
