<template>
  <div class="reset-page">
    <div class="reset-card">
      <h1>비밀번호 재설정</h1>
      <el-form label-position="top">
        <el-form-item label="새 비밀번호">
          <el-input v-model="newPassword" type="password" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" style="width:100%" @click="submit">
          비밀번호 재설정
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/api/axios'

const route = useRoute()
const router = useRouter()
const newPassword = ref('')
const loading = ref(false)

function getUsernameFromToken(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    return payload.sub || payload.username || ''
  } catch {
    return ''
  }
}

async function submit() {
  if (newPassword.value.length < 8) {
    ElMessage.warning('비밀번호는 8자 이상이어야 합니다.')
    return
  }
  loading.value = true
  try {
    const token = route.query.token
    const username = getUsernameFromToken(token)
    const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(username + newPassword.value))
    const hashedPassword = Array.from(new Uint8Array(buf)).map(b => b.toString(16).padStart(2, '0')).join('')
    await api.post('/auth/reset-password', {
      token,
      newPassword: hashedPassword
    })
    ElMessage.success('비밀번호가 재설정되었습니다.')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.reset-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 24px; background: var(--bg); }
.reset-card { width: min(420px, 100%); padding: 30px; border: 0.5px solid var(--border2); border-radius: var(--radius-sm); background: var(--surface); box-shadow: var(--shadow); }
.reset-card h1 { font-size: 24px; font-weight: 800; color: var(--t1); margin-bottom: 18px; }
</style>
