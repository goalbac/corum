<template>
  <div class="layout-wrap">
    <AppHeader />
    <AppSidebar />

    <main
      class="main-content"
      :class="{ 'with-sidebar': menuStore.sideMenus.length > 0 }"
    >
      <div v-if="pageTitle" class="page-header">
        <div class="page-header-inner">
          <h1 class="page-title">{{ pageTitle }}</h1>
          <p v-if="pageDesc" class="page-desc">{{ pageDesc }}</p>
        </div>
      </div>
      <div class="page-body">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/common/AppHeader.vue'
import AppSidebar from '@/components/common/AppSidebar.vue'
import { useMenuStore } from '@/stores/menu'
import { useAuthStore } from '@/stores/auth'

const menuStore = useMenuStore()
const authStore = useAuthStore()
const route = useRoute()

const pageTitle = computed(() => route.meta?.title || '')
const pageDesc  = computed(() => route.meta?.description || '')

onMounted(async () => {
  await menuStore.fetchMenus()
  if (authStore.isLoggedIn && !authStore.member) {
    await authStore.fetchMe()
  }
})
</script>

<style scoped>
.layout-wrap { min-height: 100vh; }

.main-content {
  margin-top: var(--header-height);
  margin-left: 0;
  transition: margin-left 0.2s ease;
  min-height: calc(100vh - var(--header-height));
}

.main-content.with-sidebar {
  margin-left: var(--sidebar-width);
}

.page-header {
  background: #fff;
  border-bottom: 1px solid var(--color-border);
  padding: 20px 32px;
}

.page-header-inner {
  max-width: var(--content-max-width);
  margin: 0 auto;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.page-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.page-body {
  padding: 24px 32px;
  max-width: calc(var(--content-max-width) + 64px);
  margin: 0 auto;
}
</style>
