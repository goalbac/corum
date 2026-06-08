<template>
  <div class="acp-root">
    <AdminPageHeader title="안내 페이지 관리" desc="CONTENT 메뉴의 안내 페이지를 편집합니다." />

    <div class="acp-layout">

      <!-- ===== 왼쪽: 메뉴 목록 + 버전 이력 ===== -->
      <aside class="acp-sidebar">

        <!-- 메뉴 목록 -->
        <div class="sidebar-section menu-section">
          <div class="section-label">
            <i class="ti ti-layout-list"></i> 안내 페이지
          </div>
          <div v-if="!contentMenus.length" class="sidebar-empty">
            <i class="ti ti-inbox"></i>
            <span>CONTENT 타입 메뉴가 없습니다</span>
          </div>
          <div class="menu-list">
            <button
              v-for="menu in contentMenus"
              :key="menu.id"
              :class="['menu-item', { active: selectedMenuId === menu.id }]"
              @click="selectMenu(menu.id)"
            >
              <div class="menu-item-inner">
                <div class="menu-item-info">
                  <span v-if="menu.parentName" class="menu-parent">{{ menu.parentName }}</span>
                  <span class="menu-name">{{ menu.name }}</span>
                </div>
                <i class="ti ti-chevron-right menu-arrow"></i>
              </div>
            </button>
          </div>
        </div>

        <!-- 버전 이력 -->
        <div class="sidebar-section history-section">
          <div class="section-label">
            <i class="ti ti-history"></i> 버전 이력
            <span v-if="histories.length" class="history-badge">{{ histories.length }}</span>
          </div>
          <div v-if="!selectedMenuId" class="sidebar-empty">
            <i class="ti ti-cursor-text"></i>
            <span>메뉴를 선택하세요</span>
          </div>
          <div v-else-if="!currentPage?.id" class="sidebar-empty">
            <i class="ti ti-clock-off"></i>
            <span>저장 후 이력이 생성됩니다</span>
          </div>
          <div v-else-if="!histories.length" class="sidebar-empty">
            <i class="ti ti-clock-off"></i>
            <span>저장된 이력이 없습니다</span>
          </div>
          <div v-else class="history-list">
            <div
              v-for="(h, idx) in histories"
              :key="h.id"
              :class="['history-item', { 'history-selected': previewHistoryId === h.id }]"
            >
              <div class="history-meta">
                <span class="history-idx">v{{ histories.length - idx }}</span>
                <div class="history-info">
                  <span class="history-author">
                    <i class="ti ti-user-circle"></i>
                    {{ h.createdByName || '알 수 없음' }}
                  </span>
                  <span class="history-date">{{ formatDate(h.createdAt) }}</span>
                </div>
              </div>
              <p class="history-preview">{{ previewText(h.content) }}</p>
              <div class="history-actions">
                <button class="hist-btn" @click="previewHistory(h)">
                  <i class="ti ti-eye"></i> 미리보기
                </button>
                <button class="hist-btn accent" @click="restoreHistory(h)">
                  <i class="ti ti-restore"></i> 복원
                </button>
              </div>
            </div>
          </div>
        </div>

      </aside>

      <!-- ===== 오른쪽: 에디터 ===== -->
      <main class="acp-editor">

        <!-- 미선택 안내 -->
        <div v-if="!selectedMenuId" class="editor-placeholder">
          <div class="placeholder-icon">
            <i class="ti ti-file-text"></i>
          </div>
          <p class="placeholder-title">안내 페이지를 선택하세요</p>
          <p class="placeholder-desc">왼쪽 목록에서 편집할 메뉴를 클릭하세요.</p>
        </div>

        <!-- 에디터 본체 -->
        <template v-else>

          <!-- 에디터 헤더 -->
          <div class="editor-header">
            <div class="editor-header-left">
              <div class="editor-breadcrumb" v-if="selectedMenu?.parentName">
                {{ selectedMenu.parentName }} <i class="ti ti-chevron-right"></i>
              </div>
              <h2 class="editor-menu-name">{{ selectedMenu?.name }}</h2>
            </div>
            <div class="editor-header-right">
              <!-- 미리보기 모드 배너 -->
              <div v-if="previewHistoryId" class="preview-mode-banner">
                <i class="ti ti-eye"></i>
                이전 버전 미리보기 중
                <button class="exit-preview-btn" @click="exitPreview">
                  <i class="ti ti-x"></i> 닫기
                </button>
              </div>
              <button v-else class="save-btn" :class="{ loading: saving }" :disabled="saving" @click="savePage">
                <i v-if="saving" class="ti ti-loader-2 spinning"></i>
                <i v-else class="ti ti-device-floppy"></i>
                {{ saving ? '저장 중...' : '저장' }}
              </button>
            </div>
          </div>

          <!-- 제목 -->
          <div class="editor-title-row">
            <input
              v-model="form.title"
              class="editor-title-input"
              placeholder="페이지 제목 입력..."
              :disabled="!!previewHistoryId"
            />
          </div>

          <!-- 에디터 영역 -->
          <div class="editor-body" v-loading="pageLoading">
            <!-- 미리보기 모드 -->
            <div v-if="previewHistoryId" class="history-preview-content" v-html="previewContent" />
            <!-- 편집 모드 -->
            <RichEditor
              v-else
              v-model="form.content"
              placeholder="안내 페이지 내용을 입력하세요."
              min-height="520px"
            />
          </div>

        </template>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import RichEditor from '@/components/common/RichEditor.vue'
import { useMenuStore } from '@/stores/menu'
import api from '@/api/axios'

const menuStore = useMenuStore()

const selectedMenuId  = ref(null)
const currentPage     = ref(null)
const histories       = ref([])
const saving          = ref(false)
const pageLoading     = ref(false)
const form            = ref({ title: '', content: '' })
const previewHistoryId = ref(null)
const previewContent   = ref('')

// ===== CONTENT 메뉴 목록 (sortOrder 기준 정렬) =====
const contentMenus = computed(() =>
  menuStore.flatMenus
    .filter(m => m.menuType === 'PAGE' && m.pageType === 'CONTENT')
    .map(m => ({
      id: m.id,
      name: m.name,
      parentName: m.top?.id !== m.id ? m.top?.name : null,
      sortOrder: m.sortOrder ?? 0,
    }))
    .sort((a, b) => a.sortOrder - b.sortOrder)
)

const selectedMenu = computed(() =>
  contentMenus.value.find(m => m.id === selectedMenuId.value) || null
)

// ===== 메뉴 선택 =====
async function selectMenu(menuId) {
  if (selectedMenuId.value === menuId) return
  selectedMenuId.value = menuId
  previewHistoryId.value = null
  previewContent.value = ''
  await loadPage()
}

async function loadPage() {
  if (!selectedMenuId.value) return
  pageLoading.value = true
  try {
    const res = await api.get(`/admin/content-pages/menus/${selectedMenuId.value}`)
    currentPage.value = res.data.data
    form.value = {
      title:   currentPage.value.title,
      content: currentPage.value.content || ''
    }
    await loadHistories()
  } catch {
    const menu = menuStore.findMenuById(selectedMenuId.value)
    currentPage.value = null
    histories.value   = []
    form.value = { title: menu?.name || '', content: '' }
  } finally {
    pageLoading.value = false
  }
}

async function loadHistories() {
  if (!currentPage.value?.id) return
  const res = await api.get(`/admin/content-pages/${currentPage.value.id}/histories`)
  histories.value = res.data.data || []
}

// ===== 저장 =====
async function savePage() {
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

// ===== 이력 미리보기 =====
function previewHistory(h) {
  previewHistoryId.value = h.id
  previewContent.value   = h.content || ''
}

function exitPreview() {
  previewHistoryId.value = null
  previewContent.value   = ''
}

// ===== 이력 복원 =====
async function restoreHistory(history) {
  await ElMessageBox.confirm(
    `${formatDate(history.createdAt)} 버전으로 복원하시겠습니까?`,
    '이력 복원',
    { type: 'warning', confirmButtonText: '복원', cancelButtonText: '취소' }
  )
  const res = await api.post(
    `/admin/content-pages/${currentPage.value.id}/histories/${history.id}/restore`
  )
  currentPage.value = res.data.data
  form.value.content = currentPage.value.content || ''
  exitPreview()
  ElMessage.success('복원되었습니다.')
  await loadHistories()
}

// ===== 유틸 =====
function formatDate(value) {
  if (!value) return '-'
  const d = new Date(value)
  return d.toLocaleString('ko-KR', {
    month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

function previewText(html) {
  const text = (html || '').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
  return text ? text.slice(0, 55) + (text.length > 55 ? '…' : '') : '(빈 내용)'
}

// ===== 초기화 =====
watch(contentMenus, menus => {
  if (!selectedMenuId.value && menus.length > 0) {
    selectMenu(menus[0].id)
  }
})

onMounted(async () => {
  await menuStore.fetchMenus()
  if (contentMenus.value.length > 0) {
    selectMenu(contentMenus.value[0].id)
  }
})
</script>

<style scoped>
.acp-root { display: flex; flex-direction: column; gap: 16px; height: 100%; }

/* ===== 레이아웃 ===== */
.acp-layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 16px;
  align-items: start;
}

/* ===== 사이드바 ===== */
.acp-sidebar {
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: sticky;
  top: 16px;
}

.sidebar-section {
  background: var(--surface);
  border: 0.5px solid var(--border2);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow);
  overflow: hidden;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 14px 10px;
  font-size: 11px;
  font-weight: 800;
  color: var(--t3);
  text-transform: uppercase;
  letter-spacing: 0.6px;
  border-bottom: 0.5px solid var(--border2);
  background: var(--surface2);
}

.history-badge {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--accent);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
}

.sidebar-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 22px 16px;
  color: var(--t4);
  font-size: 12px;
  text-align: center;
}

.sidebar-empty i { font-size: 22px; }

/* ===== 메뉴 목록 ===== */
.menu-list { display: flex; flex-direction: column; }

.menu-item {
  width: 100%;
  background: none;
  border: none;
  border-bottom: 0.5px solid var(--border2);
  padding: 0;
  cursor: pointer;
  transition: background 0.12s;
  text-align: left;
}

.menu-item:last-child { border-bottom: none; }
.menu-item:hover { background: var(--surface2); }
.menu-item.active { background: var(--accent-bg); }

.menu-item-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  gap: 8px;
}

.menu-item-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.menu-parent {
  font-size: 10px;
  color: var(--t4);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--t1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu-item.active .menu-name { color: var(--accent-t); }

.menu-arrow {
  font-size: 13px;
  color: var(--t4);
  flex-shrink: 0;
  transition: color 0.12s;
}

.menu-item.active .menu-arrow { color: var(--accent); }

/* ===== 버전 이력 ===== */
.history-section { max-height: 420px; display: flex; flex-direction: column; }
.history-list {
  overflow-y: auto;
  flex: 1;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.history-item {
  padding: 10px 12px;
  border: 0.5px solid var(--border);
  border-radius: var(--radius-xs);
  background: var(--surface2);
  transition: border-color 0.12s;
}

.history-item.history-selected {
  border-color: var(--accent);
  background: var(--accent-bg);
}

.history-meta {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 5px;
}

.history-idx {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 20px;
  padding: 0 5px;
  border-radius: 4px;
  background: var(--surface);
  border: 0.5px solid var(--border);
  font-size: 10px;
  font-weight: 800;
  color: var(--t3);
  flex-shrink: 0;
}

.history-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.history-author {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 700;
  color: var(--t1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-author i { font-size: 12px; color: var(--t3); }

.history-date {
  font-size: 11px;
  color: var(--t3);
}

.history-preview {
  font-size: 11px;
  color: var(--t3);
  line-height: 1.5;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.history-actions {
  display: flex;
  gap: 5px;
}

.hist-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 9px;
  border-radius: var(--radius-xs);
  border: 0.5px solid var(--border);
  background: var(--surface);
  color: var(--t2);
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
}

.hist-btn:hover { background: var(--surface2); color: var(--t1); }
.hist-btn.accent { color: var(--accent-t); border-color: var(--accent); }
.hist-btn.accent:hover { background: var(--accent-bg); }

/* ===== 에디터 패널 ===== */
.acp-editor {
  background: var(--surface);
  border: 0.5px solid var(--border2);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow);
  display: flex;
  flex-direction: column;
  min-height: 600px;
  overflow: hidden;
}

/* 플레이스홀더 */
.editor-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 60px 40px;
  color: var(--t3);
}

.placeholder-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--surface2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: var(--t4);
}

.placeholder-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--t2);
}

.placeholder-desc {
  font-size: 13px;
  color: var(--t4);
}

/* 에디터 헤더 */
.editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 20px;
  border-bottom: 0.5px solid var(--border2);
  background: var(--surface2);
  flex-wrap: wrap;
}

.editor-header-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.editor-breadcrumb {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--t4);
}

.editor-breadcrumb i { font-size: 10px; }

.editor-menu-name {
  font-size: 16px;
  font-weight: 800;
  color: var(--t1);
}

.editor-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 미리보기 배너 */
.preview-mode-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 14px;
  border-radius: var(--radius-xs);
  background: #fffbeb;
  border: 0.5px solid #fde68a;
  color: #92400e;
  font-size: 13px;
  font-weight: 600;
}

.exit-preview-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 9px;
  border-radius: var(--radius-xs);
  border: 0.5px solid #fde68a;
  background: #fef3c7;
  color: #92400e;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: var(--transition);
}

.exit-preview-btn:hover { background: #fde68a; }

/* 저장 버튼 */
.save-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  border-radius: var(--radius-xs);
  border: none;
  background: var(--accent);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.15s;
}

.save-btn:hover:not(:disabled) { opacity: 0.88; }
.save-btn:disabled { opacity: 0.6; cursor: default; }

/* 제목 입력 */
.editor-title-row {
  padding: 14px 20px 0;
  border-bottom: 0.5px solid var(--border2);
}

.editor-title-input {
  width: 100%;
  border: none;
  outline: none;
  font-size: 20px;
  font-weight: 800;
  color: var(--t1);
  background: transparent;
  padding: 0 0 14px;
  font-family: inherit;
  box-sizing: border-box;
}

.editor-title-input::placeholder { color: var(--t4); }
.editor-title-input:disabled { color: var(--t3); }

/* 에디터 본문 */
.editor-body {
  flex: 1;
  padding: 16px 20px;
}

.history-preview-content {
  padding: 8px 0;
  font-size: 15px;
  line-height: 1.9;
  color: var(--t1);
}

/* 스피너 */
@keyframes spin { to { transform: rotate(360deg); } }
.spinning { animation: spin 0.7s linear infinite; }

/* ===== 반응형 ===== */
@media (max-width: 1024px) {
  .acp-layout { grid-template-columns: 220px 1fr; }
}

@media (max-width: 768px) {
  .acp-layout { grid-template-columns: 1fr; }
  .acp-sidebar { position: static; }
}
</style>
