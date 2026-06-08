<template>
  <div class="terms-page">
    <div class="terms-container">
      <h1>약관</h1>
      <div v-loading="loading" class="terms-list">
        <section v-for="term in terms" :key="term.id" class="terms-section">
          <h2>{{ labelOf(term.type) }} v{{ term.version }}</h2>
          <div class="terms-content" v-html="term.content || '약관 내용이 등록되지 않았습니다.'" />
        </section>
        <el-empty v-if="!loading && !terms.length" description="등록된 활성 약관이 없습니다." />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import api from '@/api/axios'

const terms = ref([])
const loading = ref(false)

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
  return type === 'PRIVACY' ? '개인정보 처리방침' : '이용약관'
}

onMounted(fetchTerms)
</script>

<style scoped>
.terms-page { min-height: 100vh; background: var(--bg); padding: 40px 20px; }
.terms-container { width: min(920px, 100%); margin: 0 auto; }
h1 { font-size: 28px; font-weight: 800; color: var(--t1); margin-bottom: 18px; }
.terms-list { display: flex; flex-direction: column; gap: 16px; min-height: 180px; }
.terms-section {
  background: var(--surface);
  border: 0.5px solid var(--border);
  border-radius: var(--radius-sm);
  overflow: hidden;
}
h2 {
  font-size: 17px;
  font-weight: 800;
  color: var(--t1);
  padding: 14px 16px;
  border-bottom: 0.5px solid var(--border2);
  background: var(--surface2);
}
.terms-content { padding: 16px; color: var(--t2); line-height: 1.75; }
</style>
