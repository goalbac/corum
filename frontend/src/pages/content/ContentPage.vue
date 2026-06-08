<template>
  <article class="content-page" v-loading="loading">
    <template v-if="page">
      <h1>{{ page.title }}</h1>
      <div class="content-body" v-html="page.content || ''" />
    </template>
    <div v-else-if="!loading" class="empty-content">
      <h1>{{ activeMenu?.name || '안내 페이지' }}</h1>
      <p>등록된 안내 페이지가 없습니다.</p>
    </div>
  </article>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useMenuStore } from '@/stores/menu'
import api from '@/api/axios'

const route = useRoute()
const menuStore = useMenuStore()
const activeMenu = computed(() => menuStore.findMenuById(route.params.menuId))
const page = ref(null)
const loading = ref(false)

async function fetchPage() {
  if (!route.params.menuId) return
  loading.value = true
  try {
    const res = await api.get(`/content-pages/menus/${route.params.menuId}`)
    page.value = res.data.data
  } catch {
    page.value = null
  } finally {
    loading.value = false
  }
}

watch(() => route.params.menuId, fetchPage)

onMounted(async () => {
  await menuStore.fetchMenus()
  fetchPage()
})
</script>

<style scoped>
.content-page {
  min-height: 360px;
  padding: 0 0 40px;
  color: var(--t1);
}

.content-page h1,
.empty-content h1 {
  font-size: 30px;
  font-weight: 800;
  line-height: 1.35;
  margin-bottom: 24px;
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

.empty-content {
  min-height: 320px;
  padding: 36px;
  border: 0.5px solid var(--border2);
  border-radius: var(--radius-sm);
  background: var(--surface);
  box-shadow: var(--shadow);
}

.empty-content p {
  font-size: 16px;
  color: var(--t2);
}

@media (max-width: 768px) {
  .content-page h1,
  .empty-content h1 { font-size: 24px; }
  .content-body { font-size: 16px; }
}
</style>
