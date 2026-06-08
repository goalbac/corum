<template>
  <div :data-theme="themeStore.isDark ? 'dark' : undefined">
    <MaintenancePage
      v-if="maintenance.active"
      :message="maintenance.message"
      :until="maintenance.until"
    />
    <template v-else>
      <AppPopup />
      <router-view />
    </template>
  </div>
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'
import MaintenancePage from '@/pages/MaintenancePage.vue'
import AppPopup from '@/components/common/AppPopup.vue'
import api from '@/api/axios'

const themeStore = useThemeStore()
const authStore = useAuthStore()
const maintenance = reactive({ active: false, message: null, until: null })

onMounted(async () => {
  try {
    const res = await api.get('/site/public')
    const info = res.data.data
    // 관리자는 점검 모드 우회
    if (info.maintenanceMode && !authStore.member?.admin) {
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
