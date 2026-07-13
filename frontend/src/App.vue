<template>
  <div :data-theme="themeStore.isDark ? 'dark' : undefined">
    <!-- 점검 모드: 로그인/관리자 경로는 항상 허용 -->
    <MaintenancePage
      v-if="maintenance.active && !isExemptPath"
      :message="maintenance.message"
      :until="maintenance.until"
    />
    <template v-else>
      <AppProgress />
      <AppPopup v-if="!route.path.startsWith('/login') && !route.path.startsWith('/register')" />
      <router-view />
    </template>
  </div>
</template>

<script setup>
import { reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'
import MaintenancePage from '@/pages/MaintenancePage.vue'
import AppPopup from '@/components/common/AppPopup.vue'
import AppProgress from '@/components/common/AppProgress.vue'
import api from '@/api/axios'

const themeStore = useThemeStore()
const authStore = useAuthStore()
const route = useRoute()
const maintenance = reactive({ active: false, message: null, until: null })

// 점검 모드에서도 접근 가능한 경로 (로그인, 어드민 패널)
const isExemptPath = computed(() =>
  route.path.startsWith('/login') ||
  route.path.startsWith('/admin') ||
  authStore.member?.isAdmin
)

onMounted(async () => {
  // 토큰이 있으면 먼저 사용자 정보를 가져와서 관리자 여부 확인
  if (authStore.token && !authStore.member) {
    try { await authStore.fetchMe() } catch { /* ignore */ }
  }
  try {
    const res = await api.get('/site/public')
    const info = res.data.data
    if (info.maintenanceMode) {
      maintenance.active = true
      maintenance.message = info.maintenanceMessage
      maintenance.until = info.maintenanceUntil
    }
  } catch { /* 점검 모드 확인 실패 시 정상 표시 */ }
})
</script>

<style>
div[data-theme] { min-height: 100vh; background: var(--bg); transition: background 0.25s; }
</style>
