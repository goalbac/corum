<template>
  <div class="terms-wrap">
    <div class="terms-panel">
      <h1>약관 동의</h1>
      <p class="desc">서비스 이용을 위해 현재 활성 약관에 동의해주세요.</p>

      <div v-loading="loading" class="terms-list">
        <section v-for="term in requiredTerms" :key="term.id" class="terms-item">
          <div class="terms-head">
            <strong>{{ labelOf(term.type) }} v{{ term.version }}</strong>
            <el-checkbox v-model="checked[term.id]">동의</el-checkbox>
          </div>
          <div class="terms-content" v-html="sanitizeHtml(term.content) || '약관 내용이 등록되지 않았습니다.'" />
        </section>
        <el-empty v-if="!loading && !requiredTerms.length" description="동의가 필요한 약관이 없습니다." />
      </div>

      <div class="actions">
        <el-button @click="logout">로그아웃</el-button>
        <el-button type="primary" :disabled="!canSubmit" :loading="saving" @click="submit">
          동의하고 계속하기
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/api/axios'
import { useAuthStore } from '@/stores/auth'
import { sanitizeHtml } from '@/utils/sanitize'

const router = useRouter()
const authStore = useAuthStore()
const requiredTerms = ref([])
const checked = ref({})
const loading = ref(false)
const saving = ref(false)

const canSubmit = computed(() => requiredTerms.value.length > 0 && requiredTerms.value.every(term => checked.value[term.id]))

async function fetchRequiredTerms() {
  loading.value = true
  try {
    const res = await api.get('/terms/required')
    requiredTerms.value = res.data.data?.requiredTerms || []
    if (!requiredTerms.value.length) {
      await authStore.fetchMe()
      router.replace('/')
    }
  } finally {
    loading.value = false
  }
}

async function submit() {
  saving.value = true
  try {
    await api.post('/terms/agreements', { termsIds: requiredTerms.value.map(term => term.id) })
    ElMessage.success('약관 동의가 완료되었습니다.')
    await authStore.fetchMe()
    router.replace('/')
  } finally {
    saving.value = false
  }
}

async function logout() {
  await authStore.logout()
  router.replace('/login')
}

function labelOf(type) {
  return type === 'PRIVACY' ? '개인정보 처리방침' : '이용약관'
}

onMounted(fetchRequiredTerms)
</script>

<style scoped>
.terms-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg);
  padding: 24px;
}
.terms-panel {
  width: min(860px, 100%);
  background: var(--surface);
  border: 0.5px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 28px;
  box-shadow: var(--shadow);
}
h1 { font-size: 24px; font-weight: 800; color: var(--t1); margin: 0; }
.desc { margin: 6px 0 18px; color: var(--t3); }
.terms-list { display: flex; flex-direction: column; gap: 14px; min-height: 160px; }
.terms-item {
  border: 0.5px solid var(--border2);
  border-radius: var(--radius-sm);
  background: var(--surface2);
  overflow: hidden;
}
.terms-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 12px 14px;
  border-bottom: 0.5px solid var(--border2);
  color: var(--t1);
}
.terms-content {
  max-height: 220px;
  overflow: auto;
  padding: 14px;
  color: var(--t2);
  line-height: 1.7;
  background: var(--surface);
}
.actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 18px; }
</style>
