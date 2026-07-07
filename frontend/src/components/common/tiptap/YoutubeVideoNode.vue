<template>
  <NodeViewWrapper class="resizable-youtube-wrapper" :style="wrapperStyle">
    <div class="resizable-youtube-inner" :class="{ selected }">
      <div class="youtube-aspect">
        <iframe
          :src="embedSrc"
          frameborder="0"
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
          allowfullscreen
        ></iframe>
      </div>
      <div v-if="editor.isEditable && selected" class="resize-handle" @mousedown.stop.prevent="startResize"></div>
    </div>
  </NodeViewWrapper>
</template>

<script setup>
import { computed } from 'vue'
import { NodeViewWrapper, nodeViewProps } from '@tiptap/vue-3'
import { getEmbedUrlFromYoutubeUrl } from '@tiptap/extension-youtube'

const props = defineProps(nodeViewProps)

const embedSrc = computed(() => {
  return getEmbedUrlFromYoutubeUrl({ url: props.node.attrs.src, allowFullscreen: true, rel: 1 }) || props.node.attrs.src
})

const wrapperStyle = computed(() => ({
  width: props.node.attrs.wrapperWidth || '100%',
}))

function startResize(e) {
  const wrapper = e.currentTarget.closest('.resizable-youtube-inner')
  const startX = e.clientX
  const startWidth = wrapper.offsetWidth

  function onMove(ev) {
    const delta = ev.clientX - startX
    const newWidth = Math.max(160, Math.round(startWidth + delta))
    props.updateAttributes({ wrapperWidth: `${newWidth}px` })
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
.resizable-youtube-wrapper {
  display: block;
  max-width: 100%;
  margin: 0.6em auto;
}

.resizable-youtube-inner {
  position: relative;
}

.youtube-aspect {
  position: relative;
  width: 100%;
  padding-top: 56.25%; /* 16:9 */
  border-radius: var(--radius-xs);
  overflow: hidden;
  background: #000;
}

.youtube-aspect iframe {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  border: none;
  /* 선택 전에는 클릭이 노드 선택(테두리/리사이즈 핸들 노출)으로만 이어지도록,
     iframe 자체의 클릭(영상 재생 등)은 선택된 후에만 가능하게 한다 */
  pointer-events: none;
}

.resizable-youtube-inner.selected .youtube-aspect iframe {
  pointer-events: auto;
}

.resizable-youtube-inner.selected .youtube-aspect {
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
