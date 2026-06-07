<template>
  <div class="tree-node">
    <div class="tree-item" :style="{ paddingLeft: (16 + (depth * 20)) + 'px' }">
      <i class="ti ti-grip-vertical drag-handle" title="드래그하여 순서 변경"></i>
      <span class="tree-type">{{ typeIcon(menu.menuType) }}</span>
      <span class="tree-name">{{ menu.name }}</span>
      <span class="tree-url">{{ menu.url || '' }}</span>
      <span v-if="!menu.isActive" class="tree-tag inactive">비활성</span>
      <span v-if="menu.isHidden" class="tree-tag hidden">숨김</span>
      <div class="tree-actions">
        <button class="tree-btn" @click="$emit('add-child', menu)" title="하위 메뉴 추가">
          <i class="ti ti-plus"></i>
        </button>
        <button class="tree-btn" @click="$emit('edit', menu)" title="수정">
          <i class="ti ti-edit"></i>
        </button>
        <button class="tree-btn danger" @click="$emit('delete', menu.id)" title="삭제">
          <i class="ti ti-trash"></i>
        </button>
      </div>
    </div>
    <!-- 하위 메뉴 -->
    <div v-if="menu.children?.length" class="tree-children">
      <MenuTreeItem
        v-for="child in menu.children"
        :key="child.id"
        :menu="child"
        :depth="depth + 1"
        @edit="$emit('edit', $event)"
        @add-child="$emit('add-child', $event)"
        @delete="$emit('delete', $event)"
      />
    </div>
  </div>
</template>

<script setup>
defineProps({ menu: Object, depth: { type: Number, default: 0 } })
defineEmits(['edit', 'add-child', 'delete'])

function typeIcon(t) {
  return { PAGE: '📄', LINK: '🔗', GROUP: '📁' }[t] || '📄'
}
</script>

<style scoped>
.tree-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 10px;
  padding-right: 16px;
  padding-bottom: 10px;
  border-bottom: 0.5px solid var(--border2);
  transition: var(--transition);
}
.tree-item:hover { background: var(--surface2); }
.drag-handle {
  font-size: 15px;
  color: var(--t3);
  cursor: grab;
  flex-shrink: 0;
  transition: color 0.15s;
}
.drag-handle:active { cursor: grabbing; }
.tree-item:hover .drag-handle { color: var(--t2); }
.tree-type { font-size: 14px; flex-shrink: 0; }
.tree-name { font-size: 13.5px; color: var(--t1); font-weight: 500; flex-shrink: 0; }
.tree-url { font-size: 12px; color: var(--t3); flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tree-tag { font-size: 10px; padding: 1px 6px; border-radius: 4px; flex-shrink: 0; }
.tree-tag.inactive { background: var(--surface2); color: var(--t3); border: 0.5px solid var(--border); }
.tree-tag.hidden { background: var(--new-bg); color: var(--new); }
.tree-actions { display: flex; gap: 4px; flex-shrink: 0; }
.tree-btn {
  background: none;
  border: 0.5px solid var(--border);
  border-radius: var(--radius-xs);
  padding: 4px 7px;
  color: var(--t2);
  cursor: pointer;
  font-size: 13px;
  transition: var(--transition);
}
.tree-btn:hover { background: var(--surface2); color: var(--t1); }
.tree-btn.danger:hover { background: var(--new-bg); color: var(--new); border-color: var(--new); }
.tree-children { background: var(--surface2); }
/* 드래그 중 스타일 */
.sortable-ghost { opacity: 0.4; background: var(--accent-bg) !important; }
.sortable-drag { background: var(--surface) !important; box-shadow: var(--shadow) !important; }
</style>