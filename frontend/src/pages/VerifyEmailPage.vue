<template>
  <div class="auth-result">
    <div class="result-card">
      <i :class="['ti', success ? 'ti-circle-check' : 'ti-alert-circle']"></i>
      <h1>{{ success ? '이메일 인증 완료' : '이메일 인증 실패' }}</h1>
      <p>{{ message }}</p>
      <el-button type="primary" @click="$router.push('/login')">로그인으로 이동</el-button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api/axios'

const route = useRoute()
const success = ref(false)
const message = ref('이메일 인증을 처리하고 있습니다.')

onMounted(async () => {
  try {
    await api.get('/auth/verify-email', { params: { token: route.query.token } })
    success.value = true
    message.value = '이메일 인증이 완료되었습니다. 이제 로그인할 수 있습니다.'
  } catch (e) {
    success.value = false
    message.value = e.response?.data?.message || '유효하지 않거나 만료된 인증 링크입니다.'
  }
})
</script>

<style scoped>
.auth-result { min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 24px; background: var(--bg); }
.result-card { width: min(420px, 100%); padding: 34px; border: 0.5px solid var(--border2); border-radius: var(--radius-sm); background: var(--surface); box-shadow: var(--shadow); text-align: center; }
.result-card i { font-size: 48px; color: var(--accent-t); }
.result-card h1 { margin-top: 14px; font-size: 24px; font-weight: 800; color: var(--t1); }
.result-card p { margin: 10px 0 20px; font-size: 15px; color: var(--t2); }
</style>
