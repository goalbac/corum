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
          <el-input v-model="form.password" type="password" placeholder="비밀번호" size="large" prefix-icon="Lock" show-password autocomplete="current-password" @keyup.enter="handleLogin" />
        </el-form-item>
        <el-button type="primary" size="large" :loading="loading" style="width:100%;margin-top:4px;border-radius:10px;font-weight:500" @click="handleLogin">
          로그인
        </el-button>
      </el-form>
      <div class="login-footer">
        <router-link to="/register">회원가입</router-link>
        <span class="dot">·</span>
        <router-link to="/forgot-password">비밀번호 찾기</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)
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
    } finally { loading.value = false }
  })
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
.logo-text { font-size: 26px; font-weight: 700; color: var(--accent); letter-spacing: -1px; }
.logo-sub { font-size: 13px; color: var(--t3); margin-top: 6px; }
.login-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
  font-size: 13px;
  color: var(--t3);
}
.login-footer a { color: var(--t2); transition: color 0.15s; }
.login-footer a:hover { color: var(--accent); }
.dot { color: var(--border); }

@media (max-width: 480px) {
  .login-card { padding: 32px 24px; }
}
</style>
