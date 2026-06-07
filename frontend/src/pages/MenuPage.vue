<template>
  <BoardListPage v-if="activeMenu?.pageType === 'BOARD'" />
  <CalendarPage v-else-if="activeMenu?.pageType === 'CALENDAR'" />
  <DashboardPage v-else-if="activeMenu?.pageType === 'DASHBOARD'" />
  <div v-else class="menu-page">
    <h1>{{ activeMenu?.name || '메뉴' }}</h1>
    <p>이 메뉴의 콘텐츠가 아직 준비되지 않았습니다.</p>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useMenuStore } from '@/stores/menu'
import BoardListPage from '@/pages/board/BoardListPage.vue'
import CalendarPage from '@/pages/calendar/CalendarPage.vue'
import DashboardPage from '@/pages/DashboardPage.vue'

const route = useRoute()
const menuStore = useMenuStore()
const activeMenu = computed(() => menuStore.findMenuById(route.params.menuId))
</script>

<style scoped>
.menu-page {
  min-height: 360px;
  padding: 36px;
  border: 0.5px solid var(--border2);
  border-radius: var(--radius-sm);
  background: var(--surface);
  color: var(--t1);
  box-shadow: var(--shadow);
}

.menu-page h1 {
  font-size: 24px;
  font-weight: 800;
  margin-bottom: 10px;
}

.menu-page p {
  font-size: 16px;
  color: var(--t2);
}
</style>
