<template>
  <div class="sortable-node" :data-id="menu.id">
    <div
      class="tree-item"
      :class="depth === 0 ? 'root-item' : (depth % 2 === 1 ? 'child-item' : 'sub-item')"
      :style="{ paddingLeft: (16 + depth * 24) + 'px' }"
    >
      <i class="ti ti-grip-vertical drag-handle" title="드래그하여 순서 변경"></i>
      <span class="tree-type">{{ typeIcon(menu.menuType) }}</span>
      <span class="tree-name">{{ menu.name }}</span>
      <span class="tree-url">{{ menu.url || '' }}</span>
      <span v-if="!menu.isActive" class="tree-tag inactive">비활성</span>
      <span v-if="menu.isHidden" class="tree-tag hidden">숨김</span>
      <div class="tree-actions">
        <button class="tree-btn" @click="actions.onCreate(menu)" title="하위 추가"><i class="ti ti-plus"></i></button>
        <button v-if="resourceLabel" class="tree-btn resource" @click="actions.onOpenResource(menu)">
          {{ resourceLabel }}
        </button>
        <button class="tree-btn" @click="actions.onEdit(menu)" title="수정"><i class="ti ti-edit"></i></button>
        <button class="tree-btn danger" @click="actions.onDelete(menu.id)" title="삭제"><i class="ti ti-trash"></i></button>
      </div>
    </div>

    <!-- 하위 메뉴 드래그 영역 (하위 메뉴가 없어도 드롭 대상이 되도록 항상 렌더링, 깊이 제한 없이 재귀) -->
    <div
      :ref="el => actions.onSetChildRef(el, menu.id)"
      :data-parent-id="menu.id"
      class="child-sortable"
      :class="{ 'has-children': menu.children?.length }"
    >
      <AdminMenuTreeNode
        v-for="child in menu.children"
        :key="child.id"
        :menu="child"
        :depth="depth + 1"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, inject } from 'vue'

defineOptions({ name: 'AdminMenuTreeNode' })

const props = defineProps({
  menu: { type: Object, required: true },
  depth: { type: Number, default: 0 },
})

const actions = inject('menuTreeActions')

const resourceLabel = computed(() => actions.resourceButtonLabel(props.menu))

function typeIcon(t) {
  return { PAGE: '📄', LINK: '🔗', GROUP: '📁' }[t] || '📄'
}
</script>

<style scoped>
.tree-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-bottom: 0.5px solid var(--border2);
  transition: var(--transition);
}
.tree-item:hover { background: var(--surface2); }

.root-item { background: var(--surface); }
.child-item { background: var(--surface2); }
.sub-item { background: var(--bg); }

.child-sortable { min-height: 6px; margin-left: 20px; border-left: 3px solid transparent; }
.child-sortable.has-children { border-left-color: var(--accent); }

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
.tree-btn.resource {
  color: var(--accent);
  border-color: rgba(37,99,235,0.25);
  background: var(--accent-bg);
  font-weight: 700;
}
.tree-btn.resource:hover { color: #fff; background: var(--accent); border-color: var(--accent); }
.tree-btn.danger:hover { background: var(--new-bg); color: var(--new); border-color: var(--new); }
</style>
