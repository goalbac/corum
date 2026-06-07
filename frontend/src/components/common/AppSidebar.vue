<template>
  <aside class="app-sidebar" :class="{ visible: menuStore.sideMenus.length > 0 }">
    <div class="sidebar-header" v-if="activeMenu">
      <h3 class="sidebar-title">{{ activeMenu.name }}</h3>
      <p v-if="activeMenu.description" class="sidebar-desc">{{ activeMenu.description }}</p>
    </div>

    <nav class="sidebar-nav">
      <SidebarItem
        v-for="menu in menuStore.sideMenus"
        :key="menu.id"
        :menu="menu"
        :depth="0"
      />
    </nav>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { useMenuStore } from '@/stores/menu'
import SidebarItem from './SidebarItem.vue'

const menuStore = useMenuStore()

const activeMenu = computed(() =>
  menuStore.menus.find(m => m.id === menuStore.activeTopMenuId)
)
</script>

<style scoped>
.app-sidebar {
  position: fixed;
  top: var(--header-height);
  left: 0;
  bottom: 0;
  width: 0;
  background: #fff;
  border-right: 1px solid var(--color-border);
  overflow: hidden;
  transition: width 0.2s ease;
  z-index: 90;
}

.app-sidebar.visible {
  width: var(--sidebar-width);
  overflow-y: auto;
}

.sidebar-header {
  padding: 20px 16px 12px;
  border-bottom: 1px solid var(--color-border);
}

.sidebar-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.sidebar-desc {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 4px;
  line-height: 1.4;
}

.sidebar-nav {
  padding: 8px 0;
}
</style>
