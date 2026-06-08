<template>
  <div class="login-wrap">
    <div class="login-card">
      <div class="login-logo">
        <span class="logo-text">Corum</span>
        <p class="logo-sub">사단법인 통합 관리 시스템</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="아이디" size="large" prefix-icon="User" autocomplete="username" />
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

    <el-dialog v-model="showReset" title="비밀번호 찾기" width="420px">
      <el-form label-position="top">
        <el-form-item label="가입 이메일">
          <el-input v-model="resetEmail" placeholder="example@email.com" />
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
import { ref } from 'vue'
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

async function handleLogin() {
  await formRef.value?.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await authStore.login(form.value.username, form.value.password)
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
.login-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg);
  padding: 20px;
}
.login-card {
  width: 100%;
  max-width: 400px;
  background: var(--surface);
  border: 0.5px solid var(--border);
  border-radius: var(--radius);
  padding: 40px 36px;
  box-shadow: var(--shadow);
}
.login-logo { text-align: center; margin-bottom: 32px; }
.logo-text { font-size: 28px; font-weight: 800; color: var(--accent); }
.logo-sub { font-size: 14px; color: var(--t3); margin-top: 6px; }
.login-submit {
  width: 100%;
  margin-top: 4px;
  border-radius: 10px;
  font-weight: 700;
}
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
}
.login-footer a:hover,
.link-btn:hover { color: var(--accent); }
.dot { color: var(--border); }

@media (max-width: 480px) {
  .login-card { padding: 32px 24px; }
}
</style>
