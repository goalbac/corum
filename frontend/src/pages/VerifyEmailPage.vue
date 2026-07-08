<template>
  <div class="auth-result">
    <div class="result-card">
      <i :class="['ti', success ? 'ti-circle-check' : 'ti-alert-circle']"></i>
      <h1>{{ success ? '이메일 인증 완료' : '이메일 인증 실패' }}</h1>
      <p>{{ message }}</p>

      <template v-if="!success">
        <div class="resend-box">
          <el-input v-model="resendEmail" placeholder="가입 시 사용한 이메일" />
          <el-button :loading="resending" @click="handleResend">인증 메일 재발송</el-button>
        </div>
        <p v-if="resendDone" class="resend-done">계정이 존재하고 미인증 상태라면 인증 메일을 재발송했습니다.</p>
      </template>

      <el-button type="primary" @click="$router.push('/login')">로그인으로 이동</el-button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/api/axios'

const route = useRoute()
const success = ref(false)
const message = ref('이메일 인증을 처리하고 있습니다.')

const resendEmail = ref('')
const resending = ref(false)
const resendDone = ref(false)

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

async function handleResend() {
  if (!resendEmail.value) {
    ElMessage.warning('이메일을 입력해주세요.')
    return
  }
  resending.value = true
  try {
    await api.post('/auth/resend-verification-email', { email: resendEmail.value })
    resendDone.value = true
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '재발송에 실패했습니다.')
  } finally {
    resending.value = false
  }
}
</script>

<style scoped>
.auth-result { min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 24px; background: var(--bg); }
.result-card { width: min(420px, 100%); padding: 34px; border: 0.5px solid var(--border2); border-radius: var(--radius-sm); background: var(--surface); box-shadow: var(--shadow); text-align: center; }
.result-card i { font-size: 48px; color: var(--accent-t); }
.result-card h1 { margin-top: 14px; font-size: 24px; font-weight: 800; color: var(--t1); }
.result-card p { margin: 10px 0 20px; font-size: 15px; color: var(--t2); }
.resend-box { display: flex; gap: 8px; margin-bottom: 14px; }
.resend-done { font-size: 13px; color: var(--accent-t); margin-top: -6px; }
</style>
