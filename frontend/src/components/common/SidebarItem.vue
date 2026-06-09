<template>
  <div class="sidebar-item-wrap">
    <!-- 메뉴 아이템 -->
    <div
      class="sidebar-item"
      :class="{ active: isActive, 'has-children': hasChildren }"
      :style="{ paddingLeft: `${16 + depth * 14}px` }"
      @click="handleClick"
    >
      <span class="item-label">{{ menu.name }}</span>
      <span v-if="menu.hasNew" class="new-badge">N</span>
      <el-icon v-if="hasChildren" class="expand-icon" :class="{ expanded: isOpen }">
        <ArrowRight />
      </el-icon>
    </div>

    <!-- 하위 메뉴 (재귀) -->
    <transition name="slide">
      <div v-if="hasChildren && isOpen" class="sub-items">
        <SidebarItem
          v-for="child in menu.children"
          :key="child.id"
          :menu="child"
          :depth="depth + 1"
        />
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'

const props = defineProps({
  menu: { type: Object, required: true },
  depth: { type: Number, default: 0 }
})

const route = useRoute()
const router = useRouter()
const isOpen = ref(true)

const hasChildren = computed(() => props.menu.children?.length > 0)

const isActive = computed(() => {
  if (!props.menu.url) return false
  return route.path === props.menu.url || route.path.startsWith(props.menu.url + '/')
})

function handleClick() {
  if (hasChildren.value) {
    isOpen.value = !isOpen.value
    return
  }
  if (props.menu.menuType === 'LINK' && props.menu.url) {
    if (props.menu.newWindow) window.open(props.menu.url, '_blank')
    else router.push(props.menu.url)
    return
  }
  if (props.menu.url) {
    router.push(props.menu.url)
  }
}
</script>

<style scoped>
.sidebar-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  cursor: pointer;
  font-size: 13.5px;
  color: var(--color-text-secondary);
  transition: var(--transition);
  border-radius: 0;
  position: relative;
}

.sidebar-item:hover {
  color: var(--color-text-primary);
  background: var(--color-bg-hover);
}

.sidebar-item.active {
  color: var(--color-primary);
  background: var(--color-primary-bg);
  font-weight: 600;
}

.sidebar-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 3px;
  background: var(--color-primary);
  border-radius: 0 2px 2px 0;
}

.item-label {
  flex: 1;
}

.new-badge {
  flex-shrink: 0;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.02em;
  color: #fff;
  background: var(--color-danger);
  border-radius: 4px;
  padding: 1px 4px;
  line-height: 1.4;
}

.expand-icon {
  font-size: 12px;
  transition: transform 0.2s ease;
  color: var(--color-text-muted);
}

.expand-icon.expanded {
  transform: rotate(90deg);
}

.slide-enter-active,
.slide-leave-active {
  transition: all 0.2s ease;
  overflow: hidden;
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  max-height: 0;
}

.slide-enter-to,
.slide-leave-from {
  opacity: 1;
  max-height: 500px;
}
</style>
