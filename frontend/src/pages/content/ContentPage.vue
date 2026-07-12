<template>
  <div class="content-page" v-loading="loading">
    <template v-if="page">
      <div class="content-card">
        <h1>{{ page.title }}</h1>
        <div class="content-body" v-html="sanitizeHtml(page.content) || ''" />
      </div>
    </template>
    <div v-else-if="!loading" class="content-card empty-content">
      <h1>{{ activeMenu?.name || '안내 페이지' }}</h1>
      <p>등록된 안내 페이지가 없습니다.</p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useMenuStore } from '@/stores/menu'
import { useLoadingStore } from '@/stores/loading'
import api from '@/api/axios'
import { sanitizeHtml } from '@/utils/sanitize'

const route = useRoute()
const menuStore = useMenuStore()
const loadingStore = useLoadingStore()
const activeMenu = computed(() => menuStore.findMenuByRouteParams(route.params))
const page = ref(null)
const loading = ref(false)

async function fetchPage() {
  if (!activeMenu.value) return
  loading.value = true
  loadingStore.start()
  try {
    const res = await api.get(`/content-pages/menus/${activeMenu.value.id}`)
    page.value = res.data.data
  } catch {
    page.value = null
  } finally {
    loading.value = false
    loadingStore.finish()
  }
}

watch(activeMenu, fetchPage)

onMounted(async () => {
  await menuStore.fetchMenus()
  fetchPage()
})
</script>

<style scoped>
.content-page {
  max-width: 864px;
  margin: 0 auto;
  color: var(--t1);
}

.content-card {
  min-height: 360px;
}

.content-card h1,
.empty-content h1 {
  display: none;
}

.content-body {
  font-size: 17px;
  line-height: 1.9;
  color: var(--t1);
}

.content-body :deep(p) { margin: 0 0 14px; }
.content-body :deep(h1),
.content-body :deep(h2),
.content-body :deep(h3) {
  color: var(--t1);
  line-height: 1.35;
  margin: 28px 0 12px;
}

.content-body :deep(ul),
.content-body :deep(ol) {
  padding-left: 24px;
  margin: 12px 0;
}

.content-body :deep(blockquote) {
  border-left: 3px solid var(--accent);
  padding-left: 16px;
  color: var(--t2);
  margin: 0.8em 0;
}
.content-body :deep(blockquote p:last-child) { margin-bottom: 0; }
.content-body :deep(blockquote[data-variant="box"]) {
  border-left: none;
  background: var(--surface2);
  border-radius: var(--radius-xs);
  padding: 14px 18px;
}
.content-body :deep(blockquote[data-variant="accent"]) {
  border-left: none;
  background: var(--accent-bg);
  border-radius: var(--radius-xs);
  padding: 16px 20px 16px 44px;
  position: relative;
  color: var(--t1);
  font-style: italic;
  font-size: 1.05em;
}
.content-body :deep(blockquote[data-variant="accent"])::before {
  content: '\201C';
  position: absolute;
  left: 14px;
  top: 6px;
  font-size: 2.2em;
  font-weight: 800;
  font-style: normal;
  color: var(--accent);
  line-height: 1;
  opacity: 0.5;
}
.content-body :deep(code) {
  background: var(--surface2);
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 0.9em;
  color: var(--accent);
}
.content-body :deep(pre) {
  background: var(--surface2);
  border-radius: var(--radius-xs);
  padding: 16px;
  overflow-x: auto;
  margin: 0.8em 0;
}
.content-body :deep(pre code) { background: none; padding: 0; color: var(--t1); }
.content-body :deep(hr) { border: none; border-top: 1px solid var(--border); margin: 1em 0; }
.content-body :deep(a) {
  color: var(--accent);
  text-decoration: underline;
}

.content-body :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: var(--radius-xs);
}

.content-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
}

.content-body :deep(th),
.content-body :deep(td) {
  border: 1px solid var(--border);
  padding: 8px 10px;
}

.content-body :deep(iframe) { max-width: 100%; border-radius: var(--radius-xs); border: none; }
/* 유튜브 래퍼(div[data-youtube-video])의 리사이즈 폭(style width)이 실제로 반영되도록
   16:9 비율 박스 안에 iframe을 꽉 채운다. iframe 자체의 width/height 속성은 무시된다.
   padding-top 트릭은 % 값이 자신이 아닌 containing block 폭 기준으로 계산되어
   리사이즈된 폭보다 부모가 넓을 때 비율이 깨지므로 aspect-ratio를 사용한다. */
.content-body :deep([data-youtube-video]) {
  max-width: 100%;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  border-radius: var(--radius-xs);
}
.content-body :deep([data-youtube-video] iframe) {
  width: 100%;
  height: 100%;
  border-radius: var(--radius-xs);
  border: none;
}
/* 자체 업로드 동영상 - 크기 조절을 안 했으면 원본 해상도로 화면을 넘어가지 않도록 제한 */
.content-body :deep(video) {
  display: block;
  max-width: 100%;
  height: auto;
  border-radius: var(--radius-xs);
}
.content-body :deep(ul[data-type="taskList"]) { list-style: none; padding-left: 0.2em; }
.content-body :deep(ul[data-type="taskList"] li) { display: flex; align-items: flex-start; gap: 6px; }
.content-body :deep(ul[data-type="taskList"] li > label) { margin-top: 0.3em; }
.content-body :deep(ul[data-type="taskList"] li > div) { flex: 1; }
.content-body :deep(ul[data-type="taskList"] li[data-checked="true"] > div) { color: var(--t3); text-decoration: line-through; }

.empty-content p {
  font-size: 16px;
  color: var(--t2);
}

@media (max-width: 768px) {
  .content-body { font-size: 16px; }
}
</style>
