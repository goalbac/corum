<template>
  <div class="terms-page">
    <div class="terms-container">
      <!-- type 필터가 있으면 단일 약관 제목, 없으면 전체 -->
      <div class="terms-header">
        <h1>{{ pageTitle }}</h1>
        <div v-if="!filterType" class="terms-tabs">
          <button
            class="terms-tab"
            :class="{ active: !activeFilter }"
            @click="activeFilter = null"
          >전체</button>
          <button
            class="terms-tab"
            :class="{ active: activeFilter === 'SERVICE' }"
            @click="activeFilter = 'SERVICE'"
          >이용약관</button>
          <button
            class="terms-tab"
            :class="{ active: activeFilter === 'PRIVACY' }"
            @click="activeFilter = 'PRIVACY'"
          >개인정보처리방침</button>
        </div>
      </div>

      <div v-loading="loading" class="terms-list">
        <section v-for="term in filteredTerms" :key="term.id" class="terms-section">
          <h2>{{ labelOf(term.type) }} <span class="version-badge">v{{ term.version }}</span></h2>
          <div class="terms-content" v-html="sanitizeHtml(term.content) || '약관 내용이 등록되지 않았습니다.'" />
        </section>
        <el-empty v-if="!loading && !filteredTerms.length" description="등록된 활성 약관이 없습니다." />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api/axios'
import { sanitizeHtml } from '@/utils/sanitize'

const route = useRoute()
const terms = ref([])
const loading = ref(false)

// URL query ?type=SERVICE 또는 ?type=PRIVACY
const filterType = computed(() => route.query.type || null)
const activeFilter = ref(null)

watch(filterType, v => { activeFilter.value = v }, { immediate: true })

const filteredTerms = computed(() => {
  const f = activeFilter.value
  if (!f) return terms.value
  return terms.value.filter(t => t.type === f)
})

const pageTitle = computed(() => {
  if (filterType.value === 'PRIVACY') return '개인정보처리방침'
  if (filterType.value === 'SERVICE') return '이용약관'
  return '약관'
})

async function fetchTerms() {
  loading.value = true
  try {
    const res = await api.get('/terms/active')
    terms.value = res.data.data || []
  } finally {
    loading.value = false
  }
}

function labelOf(type) {
  return type === 'PRIVACY' ? '개인정보처리방침' : '이용약관'
}

onMounted(fetchTerms)
</script>

<style scoped>
.terms-page {
  min-height: 60vh;
  background: var(--bg);
  padding: 40px 20px;
}

.terms-container {
  width: min(920px, 100%);
  margin: 0 auto;
}

.terms-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}

h1 {
  font-size: 26px;
  font-weight: 800;
  color: var(--t1);
  margin: 0;
}

.terms-tabs {
  display: flex;
  gap: 4px;
}

.terms-tab {
  padding: 6px 14px;
  border: 0.5px solid var(--border);
  border-radius: var(--radius-xs);
  background: var(--surface);
  color: var(--t2);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
}

.terms-tab:hover { background: var(--surface2); color: var(--t1); }
.terms-tab.active { background: var(--accent); color: #fff; border-color: var(--accent); }

.terms-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 120px;
}

.terms-section {
  background: var(--surface);
  border: 0.5px solid var(--border);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

h2 {
  font-size: 16px;
  font-weight: 700;
  color: var(--t1);
  padding: 14px 18px;
  border-bottom: 0.5px solid var(--border2);
  background: var(--surface2);
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
}

.version-badge {
  font-size: 11px;
  font-weight: 700;
  color: var(--t3);
  background: var(--surface);
  border: 0.5px solid var(--border);
  border-radius: 4px;
  padding: 1px 6px;
}

.terms-content {
  padding: 20px 18px;
  color: var(--t2);
  line-height: 1.8;
  font-size: 14px;
}

@media (max-width: 640px) {
  .terms-page { padding: 24px 14px; }
  h1 { font-size: 22px; }
  .terms-header { flex-direction: column; align-items: flex-start; }
}
</style>
