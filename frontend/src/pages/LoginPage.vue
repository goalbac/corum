<template>
  <div class="auth-page">
    <div class="auth-container">
      <div class="auth-brand">
        <img v-if="logoUrl" :src="logoUrl" :alt="siteName" class="auth-brand-img" />
        <div v-else class="auth-brand-mark">{{ brandLetter }}</div>
      </div>

      <div class="auth-card">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="auth-form" @submit.prevent="handleLogin">
          <el-form-item label="아이디" prop="username">
            <el-input
              v-model="form.username"
              placeholder="아이디를 입력하세요"
              size="large"
              autocomplete="username"
              autocapitalize="none"
              autocorrect="off"
              spellcheck="false"
            />
          </el-form-item>
          <el-form-item label="비밀번호" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="비밀번호를 입력하세요"
              size="large"
              show-password
              autocomplete="current-password"
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <div class="auth-row-end">
            <button type="button" class="link-btn" @click="showReset = true">비밀번호 찾기</button>
          </div>

          <el-button type="primary" size="large" :loading="loading" class="auth-submit" @click="handleLogin">
            로그인
          </el-button>
        </el-form>

        <div class="auth-divider"><span></span><span>또는</span><span></span></div>
        <router-link to="/register" class="auth-secondary-btn">회원가입</router-link>
      </div>

      <div class="auth-caption">가입 후 이메일 인증을 완료하면 서비스를 이용할 수 있습니다</div>
    </div>

    <el-dialog v-model="showReset" title="비밀번호 찾기" :width="dialogWidth">
      <el-form label-position="top">
        <el-form-item label="가입 이메일">
          <el-input v-model="resetEmail" placeholder="example@email.com" size="large" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReset = false">취소</el-button>
        <el-button type="primary" :loading="resetLoading" @click="requestReset">재설정 메일 발송</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useSiteStore } from '@/stores/site'
import { ElMessage } from 'element-plus'
import api from '@/api/axios'

const siteStore = useSiteStore()
const siteName = computed(() => siteStore.siteName || '코럼')
const logoUrl = computed(() => siteStore.logoUrl)
const brandLetter = computed(() => siteName.value.charAt(0))

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)
const resetLoading = ref(false)
const showReset = ref(false)
const resetEmail = ref('')
const form = ref({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '아이디를 입력해주세요.' }],
  password: [{ required: true, message: '비밀번호를 입력해주세요.' }],
}

const dialogWidth = computed(() => {
  if (typeof window !== 'undefined' && window.innerWidth <= 480) return 'calc(100vw - 40px)'
  return '420px'
})

async function handleLogin() {
  await formRef.value?.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const member = await authStore.login(form.value.username, form.value.password)
      if (member?.requiresTermsAgreement) {
        router.push('/terms-agreement')
        return
      }
      const redirect = route.query.redirect
      const dest = redirect && !redirect.startsWith('/login') ? redirect : '/'
      router.push(dest)
    } catch(e) {
      ElMessage.error(e.response?.data?.message || '로그인에 실패했습니다.')
    } finally {
      loading.value = false
    }
  })
}

async function requestReset() {
  if (!resetEmail.value) {
    ElMessage.warning('이메일을 입력해주세요.')
    return
  }
  resetLoading.value = true
  try {
    await api.post('/auth/request-password-reset', { email: resetEmail.value })
    ElMessage.success('비밀번호 재설정 메일을 발송했습니다.')
    showReset.value = false
  } finally {
    resetLoading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  min-height: 100dvh; /* iOS Safari 주소창 포함 오버플로우 방지 */
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg);
  padding: 40px 20px;
}

.auth-container {
  width: 100%;
  max-width: 400px;
}

/* ===== 브랜드 로고 ===== */
.auth-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 30px;
}

.auth-brand-img {
  height: 48px;
  width: auto;
  max-width: 100%;
  object-fit: contain;
}

.auth-brand-mark {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: var(--accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 24px;
  letter-spacing: -0.04em;
  box-shadow: var(--shadow-sm);
  flex-shrink: 0;
}

/* ===== 카드 ===== */
.auth-card {
  background: var(--surface);
  border: 0.5px solid var(--border);
  border-radius: 18px;
  padding: 32px 30px;
  box-shadow: var(--shadow);
}

.auth-form :deep(.el-form-item) { margin-bottom: 16px; }
.auth-form :deep(.el-form-item__label) {
  font-size: 13px;
  font-weight: 700;
  color: var(--t2);
  padding-bottom: 7px;
  line-height: 1.3;
}

.auth-row-end {
  display: flex;
  justify-content: flex-end;
  margin: -6px 0 16px;
}

.auth-submit {
  width: 100%;
  border-radius: 10px;
  font-weight: 700;
  height: 46px;
}

.auth-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 22px 0;
}
.auth-divider span:first-child,
.auth-divider span:last-child {
  flex: 1;
  height: 1px;
  background: var(--border);
}
.auth-divider span:nth-child(2) {
  font-size: 12px;
  color: var(--t3);
}

.auth-secondary-btn {
  display: block;
  width: 100%;
  text-align: center;
  border: 0.5px solid var(--border2);
  background: var(--surface);
  color: var(--t1);
  font-weight: 600;
  font-size: 14px;
  padding: 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: var(--transition);
  box-sizing: border-box;
}
.auth-secondary-btn:hover { background: var(--surface2); }

.auth-caption {
  text-align: center;
  font-size: 12px;
  color: var(--t3);
  margin-top: 22px;
}

.link-btn {
  border: none;
  background: transparent;
  color: var(--t3);
  font-size: 13px;
  cursor: pointer;
  padding: 0;
  transition: color 0.15s;
}
.link-btn:hover { color: var(--accent); }

/* ===== 모바일: 입력창 줌 방지 (iOS font-size < 16px 시 자동 확대) ===== */
@media (max-width: 768px) {
  :deep(.el-input__inner) { font-size: 16px !important; }
  :deep(.el-input__wrapper) { font-size: 16px !important; }
  .auth-card { padding: 26px 22px; }
}
</style>
