<template>
  <BoardWritePage v-if="isBoardWrite" />
  <BoardDetailPage v-else-if="isBoardDetail" />
  <BoardListPage v-else-if="activeMenu?.pageType === 'BOARD'" />
  <CalendarPage v-else-if="activeMenu?.pageType === 'CALENDAR'" />
  <DashboardPage v-else-if="activeMenu?.pageType === 'DASHBOARD'" />
  <ContentPage v-else-if="activeMenu?.pageType === 'CONTENT'" />
  <div v-else-if="activeMenu" class="menu-page">
    <h1>{{ activeMenu.name }}</h1>
    <p>이 메뉴의 콘텐츠가 아직 준비되지 않았습니다.</p>
  </div>
  <NotFoundPage v-else-if="menuStore.loaded" />
</template>

<script setup>
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMenuStore } from '@/stores/menu'
import BoardListPage from '@/pages/board/BoardListPage.vue'
import BoardDetailPage from '@/pages/board/BoardDetailPage.vue'
import BoardWritePage from '@/pages/board/BoardWritePage.vue'
import CalendarPage from '@/pages/calendar/CalendarPage.vue'
import ContentPage from '@/pages/content/ContentPage.vue'
import DashboardPage from '@/pages/DashboardPage.vue'
import NotFoundPage from '@/pages/NotFoundPage.vue'

const route = useRoute()
const router = useRouter()
const menuStore = useMenuStore()

// '/menu/:menuId'(숫자 ID)는 항상 목록/조회 모드. ':customSlug+'(직접 지정 URL/자동
// 넘버링)는 마지막 경로 조각이 글번호/write/edit인지에 따라 조회·작성·수정 모드까지 해석된다.
const resolved = computed(() => menuStore.parseCustomRoute(route.params))
const activeMenu = computed(() => resolved.value.menu)
const isBoardDetail = computed(() => activeMenu.value?.pageType === 'BOARD' && !!resolved.value.postId && !resolved.value.isEdit)
const isBoardWrite = computed(() => activeMenu.value?.pageType === 'BOARD' && (resolved.value.isWrite || resolved.value.isEdit))

// GROUP 타입 메뉴에 직접 진입 시 첫 번째 탐색 가능 하위 메뉴로 리다이렉트
watch(activeMenu, menu => {
  if (!menu || menu.menuType !== 'GROUP') return
  const first = menuStore.firstNavigableMenu(menu)
  if (first && first.id !== menu.id) {
    router.replace(menuStore.resolveMenuPath(first))
  }
}, { immediate: true })
</script>

<style scoped>
.menu-page {
  min-height: 360px;
  padding: 36px;
  color: var(--t1);
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
