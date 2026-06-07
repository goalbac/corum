<template>
  <div class="login-wrap">
    <div class="login-card">
      <div class="login-logo">
        <span class="logo-text">Corum</span>
        <p class="logo-sub">사단법인 통합 관리 시스템</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        @submit.prevent="handleLogin"
        class="login-form"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="아이디"
            size="large"
            prefix-icon="User"
            autocomplete="username"
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

        <el-button
          type="primary"
          size="large"
          :loading="loading"
          style="width: 100%; margin-top: 8px;"
          @click="handleLogin"
        >
          로그인
        </el-button>
      </el-form>

      <div class="login-footer">
        <router-link to="/register">회원가입</router-link>
        <span class="divider">·</span>
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
const route  = useRoute()
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
      const redirect = route.query.redirect || '/'
      router.push(redirect)
    } catch (e) {
      ElMessage.error(e.response?.data?.message || '로그인에 실패했습니다.')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-base);
}

.login-card {
  width: 400px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 40px 36px;
  box-shadow: var(--shadow-md);
}

.login-logo {
  text-align: center;
  margin-bottom: 32px;
}

.logo-text {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-primary);
  letter-spacing: -1px;
}

.logo-sub {
  font-size: 13px;
  color: var(--color-text-muted);
  margin-top: 6px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.login-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.login-footer a {
  color: var(--color-text-secondary);
  transition: var(--transition);
}

.login-footer a:hover {
  color: var(--color-primary);
}

.divider {
  color: var(--color-border);
}
</style>
