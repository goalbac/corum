<template>
  <div class="auth-page">
    <div class="auth-container terms-container">
      <div class="terms-icon">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z" />
          <polyline points="14 3 14 9 20 9" />
          <path d="m9 15 2 2 4-4" />
        </svg>
      </div>
      <h1 class="terms-title">약관 동의</h1>
      <p class="terms-desc">서비스 이용을 위해 아래 약관에 동의해 주세요.<br />최초 로그인 시 한 번만 진행합니다.</p>

      <div v-loading="loading" class="auth-card">
        <div class="agree-all-row" :class="{ checked: allChecked }" @click="toggleAll">
          <span class="check-box" :class="{ checked: allChecked }">
            <svg v-if="allChecked" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12" /></svg>
          </span>
          <span class="agree-all-label">전체 약관에 동의합니다</span>
        </div>

        <div class="terms-items">
          <div v-for="term in requiredTerms" :key="term.id" class="terms-item">
            <div class="terms-item-head" @click="toggleOne(term.id)">
              <span class="check-box small" :class="{ checked: checked[term.id] }">
                <svg v-if="checked[term.id]" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12" /></svg>
              </span>
              <span class="terms-item-label">{{ labelOf(term.type) }} v{{ term.version }} <span class="required">(필수)</span></span>
            </div>
            <div class="terms-item-content" v-html="sanitizeHtml(term.content) || '약관 내용이 등록되지 않았습니다.'" />
          </div>
          <el-empty v-if="!loading && !requiredTerms.length" description="동의가 필요한 약관이 없습니다." />
        </div>

        <el-button type="primary" size="large" :disabled="!canSubmit" :loading="saving" class="auth-submit" @click="submit">
          동의하고 계속하기
        </el-button>
        <button type="button" class="logout-link" @click="logout">로그아웃</button>
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
const allChecked = computed(() => canSubmit.value)

function toggleAll() {
  const next = !allChecked.value
  requiredTerms.value.forEach(term => { checked.value[term.id] = next })
}

function toggleOne(id) {
  checked.value[id] = !checked.value[id]
}

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
.auth-page {
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg);
  padding: 40px 20px;
}

.terms-container { width: 100%; max-width: 520px; }

.terms-icon {
  width: 48px;
  height: 48px;
  border-radius: 13px;
  background: var(--accent-bg);
  color: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 14px;
}

.terms-title {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.03em;
  color: var(--t1);
  text-align: center;
}

.terms-desc {
  margin: 0 0 26px;
  font-size: 13.5px;
  color: var(--t3);
  text-align: center;
  line-height: 1.6;
}

.auth-card {
  background: var(--surface);
  border: 0.5px solid var(--border);
  border-radius: 18px;
  padding: 24px;
  box-shadow: var(--shadow);
}

.agree-all-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  background: var(--surface2);
  cursor: pointer;
  margin-bottom: 16px;
}
.agree-all-label { font-size: 15px; font-weight: 700; color: var(--t1); }

.check-box {
  width: 24px;
  height: 24px;
  border-radius: 7px;
  border: 2px solid var(--border2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: var(--transition);
}
.check-box.small { width: 22px; height: 22px; border-radius: 6px; }
.check-box.checked { background: var(--accent); border-color: var(--accent); }

.terms-items { display: flex; flex-direction: column; gap: 8px; }

.terms-item {
  border: 0.5px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
}

.terms-item-head {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 13px 15px;
  cursor: pointer;
}
.terms-item-label { flex: 1; font-size: 14px; font-weight: 600; color: var(--t1); }
.required { color: var(--danger); font-weight: 700; }

.terms-item-content {
  max-height: 96px;
  overflow-y: auto;
  padding: 12px 15px;
  margin: 0 8px 12px;
  background: var(--bg);
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--t3);
}

.auth-submit {
  width: 100%;
  border-radius: 11px;
  font-weight: 700;
  height: 48px;
  margin-top: 18px;
}

.logout-link {
  display: block;
  width: 100%;
  text-align: center;
  border: none;
  background: transparent;
  color: var(--t3);
  font-size: 13px;
  cursor: pointer;
  padding: 14px 0 0;
  transition: color 0.15s;
}
.logout-link:hover { color: var(--t1); }
</style>
