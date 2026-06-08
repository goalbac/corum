<template>
  <div class="admin-content-pages">
    <AdminPageHeader title="안내 페이지 관리" desc="CONTENT 메뉴에 표시될 안내 페이지를 편집합니다.">
      <el-button type="primary" size="small" :loading="saving" @click="savePage">저장</el-button>
    </AdminPageHeader>

    <div class="content-grid">
      <section class="editor-panel">
        <el-form label-position="top">
          <el-form-item label="대상 메뉴">
            <el-select v-model="selectedMenuId" placeholder="CONTENT 메뉴 선택" style="width:100%" @change="loadPage">
              <el-option
                v-for="menu in contentMenus"
                :key="menu.id"
                :label="menu.label"
                :value="menu.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="제목">
            <el-input v-model="form.title" placeholder="페이지 제목" />
          </el-form-item>
        </el-form>

        <div class="editor-shell" :class="{ disabled: !selectedMenuId }">
          <div class="editor-toolbar">
            <button type="button" :class="{ active: editor?.isActive('bold') }" @click="editor?.chain().focus().toggleBold().run()">
              <i class="ti ti-bold"></i>
            </button>
            <button type="button" :class="{ active: editor?.isActive('italic') }" @click="editor?.chain().focus().toggleItalic().run()">
              <i class="ti ti-italic"></i>
            </button>
            <button type="button" :class="{ active: editor?.isActive('heading', { level: 2 }) }" @click="editor?.chain().focus().toggleHeading({ level: 2 }).run()">
              H2
            </button>
            <button type="button" :class="{ active: editor?.isActive('bulletList') }" @click="editor?.chain().focus().toggleBulletList().run()">
              <i class="ti ti-list"></i>
            </button>
            <button type="button" :class="{ active: editor?.isActive('orderedList') }" @click="editor?.chain().focus().toggleOrderedList().run()">
              <i class="ti ti-list-numbers"></i>
            </button>
            <button type="button" @click="editor?.chain().focus().undo().run()">
              <i class="ti ti-arrow-back-up"></i>
            </button>
            <button type="button" @click="editor?.chain().focus().redo().run()">
              <i class="ti ti-arrow-forward-up"></i>
            </button>
          </div>
          <EditorContent class="editor-content" :editor="editor" />
        </div>
      </section>

      <aside class="history-panel">
        <div class="panel-title">버전 이력</div>
        <div v-if="!currentPage?.id" class="history-empty">저장 후 이력이 표시됩니다.</div>
        <div v-else-if="histories.length === 0" class="history-empty">저장된 이력이 없습니다.</div>
        <div v-else class="history-list">
          <div v-for="history in histories" :key="history.id" class="history-item">
            <div>
              <strong>{{ formatDate(history.createdAt) }}</strong>
              <p>{{ previewText(history.content) }}</p>
            </div>
            <el-button size="small" @click="restoreHistory(history)">복원</el-button>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Editor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import { useMenuStore } from '@/stores/menu'
import api from '@/api/axios'

const menuStore = useMenuStore()
const selectedMenuId = ref(null)
const currentPage = ref(null)
const histories = ref([])
const saving = ref(false)
const form = ref({ title: '', content: '' })

const editor = new Editor({
  extensions: [StarterKit],
  content: '',
  editorProps: {
    attributes: {
      class: 'tiptap-body'
    }
  },
  onUpdate: ({ editor }) => {
    form.value.content = editor.getHTML()
  }
})

const contentMenus = computed(() => {
  return menuStore.flatMenus
    .filter(menu => menu.menuType === 'PAGE' && menu.pageType === 'CONTENT')
    .map(menu => ({
      id: menu.id,
      label: `${menu.top?.name && menu.top.id !== menu.id ? `${menu.top.name} > ` : ''}${menu.name}`
    }))
})

async function loadPage() {
  if (!selectedMenuId.value) return
  try {
    const res = await api.get(`/admin/content-pages/menus/${selectedMenuId.value}`)
    currentPage.value = res.data.data
    form.value = {
      title: currentPage.value.title,
      content: currentPage.value.content || ''
    }
    editor.commands.setContent(form.value.content || '')
    await loadHistories()
  } catch {
    const menu = menuStore.findMenuById(selectedMenuId.value)
    currentPage.value = null
    histories.value = []
    form.value = { title: menu?.name || '', content: '' }
    editor.commands.setContent('')
  }
}

async function savePage() {
  if (!selectedMenuId.value) {
    ElMessage.warning('대상 메뉴를 선택해주세요.')
    return
  }
  if (!form.value.title.trim()) {
    ElMessage.warning('제목을 입력해주세요.')
    return
  }
  saving.value = true
  try {
    const res = await api.put(`/admin/content-pages/menus/${selectedMenuId.value}`, form.value)
    currentPage.value = res.data.data
    ElMessage.success('저장되었습니다.')
    await loadHistories()
  } finally {
    saving.value = false
  }
}

async function loadHistories() {
  if (!currentPage.value?.id) return
  const res = await api.get(`/admin/content-pages/${currentPage.value.id}/histories`)
  histories.value = res.data.data || []
}

async function restoreHistory(history) {
  await ElMessageBox.confirm('선택한 이력으로 본문을 복원하시겠습니까?', '이력 복원', {
    type: 'warning',
    confirmButtonText: '복원',
    cancelButtonText: '취소'
  })
  const res = await api.post(`/admin/content-pages/${currentPage.value.id}/histories/${history.id}/restore`)
  currentPage.value = res.data.data
  form.value.content = currentPage.value.content || ''
  editor.commands.setContent(form.value.content)
  ElMessage.success('복원되었습니다.')
  await loadHistories()
}

function formatDate(value) {
  return value ? new Date(value).toLocaleString('ko-KR') : '-'
}

function previewText(html) {
  const text = (html || '').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
  return text ? text.slice(0, 70) : '빈 내용'
}

watch(contentMenus, menus => {
  if (!selectedMenuId.value && menus.length > 0) {
    selectedMenuId.value = menus[0].id
    loadPage()
  }
})

onMounted(async () => {
  await menuStore.fetchMenus()
  if (contentMenus.value.length > 0) {
    selectedMenuId.value = contentMenus.value[0].id
    loadPage()
  }
})

onBeforeUnmount(() => {
  editor.destroy()
})
</script>

<style scoped>
.admin-content-pages { display: flex; flex-direction: column; gap: 18px; }

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 16px;
}

.editor-panel,
.history-panel {
  background: var(--surface);
  border: 0.5px solid var(--border2);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow);
  padding: 18px;
}

.editor-shell {
  border: 0.5px solid var(--border);
  border-radius: var(--radius-sm);
  overflow: hidden;
  background: var(--surface);
}

.editor-shell.disabled { opacity: 0.6; pointer-events: none; }

.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px;
  border-bottom: 0.5px solid var(--border);
  background: var(--surface2);
  flex-wrap: wrap;
}

.editor-toolbar button {
  min-width: 34px;
  height: 32px;
  border: 0.5px solid var(--border);
  border-radius: var(--radius-xs);
  background: var(--surface);
  color: var(--t2);
  font-size: 14px;
  font-weight: 700;
}

.editor-toolbar button.active,
.editor-toolbar button:hover {
  background: var(--accent-bg);
  color: var(--accent-t);
  border-color: var(--accent);
}

.editor-content {
  min-height: 420px;
  padding: 18px;
  color: var(--t1);
}

.editor-content :deep(.tiptap-body) {
  min-height: 380px;
  outline: none;
  font-size: 16px;
  line-height: 1.8;
}

.editor-content :deep(p) { margin: 0 0 12px; }
.editor-content :deep(h2) { margin: 20px 0 10px; font-size: 24px; }
.editor-content :deep(ul),
.editor-content :deep(ol) { padding-left: 24px; margin: 10px 0; }

.panel-title {
  font-size: 16px;
  font-weight: 800;
  color: var(--t1);
  margin-bottom: 12px;
}

.history-empty {
  padding: 30px 0;
  text-align: center;
  color: var(--t3);
  font-size: 14px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.history-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  padding: 12px;
  border: 0.5px solid var(--border);
  border-radius: var(--radius-xs);
  background: var(--surface2);
}

.history-item strong {
  display: block;
  font-size: 13px;
  color: var(--t1);
  margin-bottom: 4px;
}

.history-item p {
  font-size: 13px;
  color: var(--t2);
  line-height: 1.5;
}

@media (max-width: 900px) {
  .content-grid { grid-template-columns: 1fr; }
}
</style>
