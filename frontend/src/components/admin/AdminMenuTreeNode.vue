<template>
  <div class="sortable-node" :data-id="menu.id">
    <div
      class="tree-item"
      :class="{ 'is-group': menu.menuType === 'GROUP' }"
      :style="{ paddingLeft: (16 + depth * 24) + 'px' }"
    >
      <i class="ti ti-grip-vertical drag-handle" title="드래그하여 순서 변경"></i>
      <span class="tree-type">{{ typeIcon(menu.menuType) }}</span>
      <span class="tree-name">{{ menu.name }}</span>
      <span class="tree-url">{{ menu.url || '' }}</span>
      <span v-if="!menu.isActive" class="tree-tag inactive">비활성</span>
      <span v-if="menu.isHidden" class="tree-tag hidden">숨김</span>
      <div class="tree-actions">
        <button v-if="resourceLabel" class="tree-btn resource" @click="actions.onOpenResource(menu)">
          {{ resourceLabel }}
        </button>
        <button class="tree-btn" @click="actions.onCreate(menu)" title="하위 추가"><i class="ti ti-plus"></i></button>
        <button class="tree-btn" @click="actions.onEdit(menu)" title="수정"><i class="ti ti-edit"></i></button>
        <span class="tree-actions-divider"></span>
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
  background: var(--surface);
  border-bottom: 0.5px solid var(--border2);
  transition: var(--transition);
}
.tree-item:hover { background: var(--surface2); }

.tree-item.is-group .tree-name { font-weight: 700; }

/* 하위 메뉴가 없어도 드래그앤드롭 대상 영역은 항상 존재해야 하지만, 배경을
   바로 위 tree-item과 맞춰서 흰 틈처럼 보이지 않고 자연스럽게 이어지게 한다 */
.child-sortable {
  min-height: 4px;
  margin-left: 20px;
  border-left: 3px solid transparent;
  background: var(--surface);
}
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

.tree-actions { display: flex; align-items: center; gap: 2px; flex-shrink: 0; }

.tree-actions-divider {
  width: 1px;
  height: 14px;
  background: var(--border2);
  margin: 0 4px;
}

.tree-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  background: none;
  border: none;
  border-radius: var(--radius-xs);
  padding: 0;
  color: var(--t3);
  cursor: pointer;
  font-size: 13px;
  transition: var(--transition);
}
.tree-btn:hover { background: var(--surface2); color: var(--t1); }
.tree-btn.resource {
  width: auto;
  padding: 0 10px;
  color: var(--accent);
  background: var(--accent-bg);
  font-weight: 700;
  font-size: 12px;
}
.tree-btn.resource:hover { color: #fff; background: var(--accent); }
.tree-btn.danger:hover { background: var(--new-bg); color: var(--new); }
</style>
