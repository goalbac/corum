<template>
  <div class="login-wrap">
    <div class="login-card">
      <!-- 그라디언트 헤더 (LNB 스타일과 통일) -->
      <div class="login-hero">
        <div class="hero-overlay"></div>
        <span class="hero-logo">Corum</span>
        <p class="hero-sub">사단법인 통합 관리 시스템</p>
      </div>

      <div class="login-body">
        <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="아이디"
              size="large"
              prefix-icon="User"
              autocomplete="username"
              autocapitalize="none"
              autocorrect="off"
              spellcheck="false"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="비밀번호"
              size="large"
              prefix-icon="Lock"
              show-password
              autocomplete="current-password"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="login-submit" @click="handleLogin">
            로그인
          </el-button>
        </el-form>

        <div class="login-footer">
          <router-link to="/register">회원가입</router-link>
          <span class="dot">·</span>
          <button type="button" class="link-btn" @click="showReset = true">비밀번호 찾기</button>
        </div>
      </div>
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
import { ElMessage } from 'element-plus'
import api from '@/api/axios'

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
      router.push(route.query.redirect || '/')
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
/* ===== 전체 배경 ===== */
.login-wrap {
  min-height: 100vh;
  min-height: 100dvh; /* iOS Safari 주소창 포함 오버플로우 방지 */
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg);
  padding: 24px 16px;
}

/* ===== 카드 ===== */
.login-card {
  width: 100%;
  max-width: 400px;
  background: var(--surface);
  border: 0.5px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  overflow: hidden;
}

/* ===== 그라디언트 헤더 (LNB 스타일 통일) ===== */
.login-hero {
  position: relative;
  padding: 36px 32px 30px;
  background: linear-gradient(135deg, #4f6ef7 0%, #7c4ff7 50%, #e05fc4 100%);
  text-align: center;
  overflow: hidden;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 80% 20%, rgba(255,255,255,0.18) 0%, transparent 50%),
    radial-gradient(circle at 10% 80%, rgba(255,255,255,0.10) 0%, transparent 40%);
  pointer-events: none;
}

.hero-logo {
  position: relative;
  z-index: 1;
  display: block;
  font-size: 32px;
  font-weight: 900;
  color: #fff;
  letter-spacing: -0.5px;
}

.hero-sub {
  position: relative;
  z-index: 1;
  font-size: 13px;
  color: rgba(255,255,255,0.78);
  margin-top: 6px;
  font-weight: 500;
}

/* ===== 폼 영역 ===== */
.login-body {
  padding: 28px 28px 32px;
}

.login-submit {
  width: 100%;
  margin-top: 4px;
  border-radius: 10px;
  font-weight: 700;
  height: 46px;
}

/* ===== 하단 링크 ===== */
.login-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
  font-size: 14px;
  color: var(--t3);
}

.login-footer a,
.link-btn {
  color: var(--t2);
  transition: color 0.15s;
  border: none;
  background: transparent;
  font-size: 14px;
  cursor: pointer;
  padding: 0;
}

.login-footer a:hover,
.link-btn:hover { color: var(--accent); }
.dot { color: var(--border); }

/* ===== 모바일: 입력창 줌 방지 (iOS font-size < 16px 시 자동 확대) ===== */
@media (max-width: 768px) {
  :deep(.el-input__inner) {
    font-size: 16px !important;
  }
  :deep(.el-input__wrapper) {
    font-size: 16px !important;
  }

  .login-hero {
    padding: 28px 24px 24px;
  }

  .hero-logo { font-size: 28px; }

  .login-body { padding: 22px 20px 28px; }
}

@media (max-width: 400px) {
  .login-card {
    border-radius: var(--radius-sm);
  }
}
</style>
