<template>
  <div class="adm-page">
    <AdminPageHeader title="게시물 관리" desc="전체 게시물을 조회하고 일괄 수정합니다.">
      <button class="adm-btn danger" :disabled="!selected.length" @click="batchDelete">
        <i class="ti ti-trash"></i> 선택 삭제 ({{ selected.length }})
      </button>
      <button class="adm-btn primary" :disabled="!selected.length" @click="openBatchEdit">
        <i class="ti ti-edit"></i> 선택 수정 ({{ selected.length }})
      </button>
    </AdminPageHeader>

    <div class="adm-card">
      <!-- 필터 툴바 -->
      <div class="adm-toolbar">
        <div class="toolbar-left">
          <el-select v-model="filterBoardId" placeholder="전체 게시판" clearable style="width:180px" @change="onFilter">
            <el-option v-for="b in boards" :key="b.id" :value="b.id" :label="b.name" />
          </el-select>
          <el-input
            v-model="searchKw"
            placeholder="제목 또는 작성자 검색"
            style="width:220px"
            clearable
            @keyup.enter="onFilter"
          >
            <template #suffix><i class="ti ti-search" style="cursor:pointer" @click="onFilter"></i></template>
          </el-input>
        </div>
        <div class="toolbar-right">
          <label class="chk-item" style="font-size:13px">
            <el-checkbox v-model="selectAll" @change="toggleSelectAll" /> 전체 선택
          </label>
        </div>
      </div>

      <!-- 테이블 -->
      <div class="at-wrap" v-loading="loading">
        <div class="at-head">
          <div class="at-col" style="width:36px;text-align:center"><i class="ti ti-checks"></i></div>
          <div class="at-col" style="width:110px">게시판</div>
          <div class="at-col" style="flex:1">제목</div>
          <div class="at-col" style="width:90px">작성자</div>
          <div class="at-col" style="width:60px;text-align:right">조회</div>
          <div class="at-col" style="width:60px;text-align:right">좋아요</div>
          <div class="at-col" style="width:50px;text-align:center">공지</div>
          <div class="at-col" style="width:50px;text-align:center">숨김</div>
          <div class="at-col" style="width:100px">작성일</div>
          <div class="at-col" style="width:70px;text-align:center">관리</div>
        </div>
        <div v-for="row in posts" :key="row.id" class="at-row" :class="{ selected: selected.includes(row.id) }">
          <div class="at-col" style="width:36px;text-align:center">
            <el-checkbox :model-value="selected.includes(row.id)" @change="toggleSelect(row.id)" />
          </div>
          <div class="at-col muted" style="width:110px;font-size:12px">{{ row.boardName }}</div>
          <div class="at-col" style="flex:1">
            <span class="post-title" @click="openPostDetail(row)">{{ row.title }}</span>
            <span v-if="row.isNotice" class="adm-badge badge-warning ml4">공지</span>
            <span v-if="row.isHidden" class="adm-badge badge-muted ml4">숨김</span>
          </div>
          <div class="at-col muted" style="width:90px;font-size:12px">{{ row.writerName || '-' }}</div>
          <div class="at-col muted" style="width:60px;text-align:right;font-size:12px">{{ row.viewCount }}</div>
          <div class="at-col muted" style="width:60px;text-align:right;font-size:12px">{{ row.likeCount }}</div>
          <div class="at-col" style="width:50px;text-align:center">
            <span v-if="row.isNotice" class="adm-badge badge-warning">Y</span>
            <span v-else class="adm-badge badge-muted">-</span>
          </div>
          <div class="at-col" style="width:50px;text-align:center">
            <span v-if="row.isHidden" class="adm-badge badge-danger">Y</span>
            <span v-else class="adm-badge badge-muted">-</span>
          </div>
          <div class="at-col muted" style="width:100px;font-size:12px">{{ fmtDate(row.createdAt) }}</div>
          <div class="at-col at-actions" style="width:70px">
            <button class="act-btn" @click="openSingleEdit(row)"><i class="ti ti-edit"></i></button>
            <button class="act-btn danger" @click="deleteOne(row.id)"><i class="ti ti-trash"></i></button>
          </div>
        </div>
        <div v-if="!posts.length && !loading" class="at-empty">
          <i class="ti ti-notes-off"></i><span>게시물이 없습니다.</span>
        </div>
      </div>

      <!-- 페이지네이션 -->
      <div class="adm-pagination" v-if="totalPages > 1">
        <button class="adm-btn ghost" :disabled="currentPage === 0" @click="goPage(currentPage - 1)">
          <i class="ti ti-chevron-left"></i>
        </button>
        <span class="page-info">{{ currentPage + 1 }} / {{ totalPages }}</span>
        <button class="adm-btn ghost" :disabled="currentPage >= totalPages - 1" @click="goPage(currentPage + 1)">
          <i class="ti ti-chevron-right"></i>
        </button>
      </div>
    </div>

    <!-- 배치/단건 수정 다이얼로그 -->
    <el-dialog v-model="showEditDialog" :title="batchMode ? `게시물 수정 (${selected.length}건 선택)` : '게시물 수정'" width="480px" destroy-on-close>
      <div class="dlg-form">
        <div v-if="batchMode" class="batch-notice">
          <i class="ti ti-info-circle"></i>
          입력한 필드만 수정됩니다. 비워두면 기존 값을 유지합니다.
        </div>
        <div class="dlg-row">
          <div class="dlg-field">
            <label>조회수</label>
            <el-input-number v-model="editForm.viewCount" :min="0" style="width:100%" :placeholder="batchMode ? '변경 안함' : ''" :controls="!batchMode" />
          </div>
          <div class="dlg-field">
            <label>좋아요</label>
            <el-input-number v-model="editForm.likeCount" :min="0" style="width:100%" :placeholder="batchMode ? '변경 안함' : ''" :controls="!batchMode" />
          </div>
        </div>
        <div class="dlg-field">
          <label>작성자명</label>
          <el-input v-model="editForm.writerName" :placeholder="batchMode ? '변경 안함 (비워두면 유지)' : '작성자명'" />
        </div>
        <div class="dlg-field">
          <label>작성일시</label>
          <el-date-picker v-model="editForm.createdAt" type="datetime" style="width:100%"
            :placeholder="batchMode ? '변경 안함' : '작성일시'" />
        </div>
        <div class="dlg-field">
          <label>게시판 이동</label>
          <el-select v-model="editForm.targetBoardId" :placeholder="batchMode ? '이동 안함' : '게시판 선택'" clearable style="width:100%">
            <el-option v-for="b in boards" :key="b.id" :value="b.id" :label="b.name" />
          </el-select>
        </div>
      </div>
      <template #footer>
        <button class="adm-btn ghost" @click="showEditDialog = false">취소</button>
        <button class="adm-btn primary" :disabled="saving" @click="saveEdit">
          <i v-if="saving" class="ti ti-loader-2 spinning"></i>{{ saving ? '저장 중...' : '저장' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'

const posts = ref([])
const boards = ref([])
const loading = ref(false)
const saving = ref(false)
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const filterBoardId = ref(null)
const searchKw = ref('')
const selected = ref([])
const selectAll = ref(false)
const showEditDialog = ref(false)
const batchMode = ref(false)
const editingPost = ref(null)

const editForm = ref({ viewCount: null, likeCount: null, writerName: '', createdAt: null, targetBoardId: null })

async function fetchBoards() {
  const r = await api.get('/admin/posts/boards')
  boards.value = r.data.data || []
}

async function fetchPosts(page = 0) {
  loading.value = true
  selected.value = []
  selectAll.value = false
  try {
    const params = { page, size: 20 }
    if (filterBoardId.value) params.boardId = filterBoardId.value
    const r = await api.get('/admin/posts', { params })
    const d = r.data.data
    posts.value = d.items || []
    totalPages.value = d.totalPages || 0
    totalElements.value = d.totalElements || 0
    currentPage.value = d.page || 0
  } finally { loading.value = false }
}

function onFilter() { fetchPosts(0) }
function goPage(p) { fetchPosts(p) }

function toggleSelect(id) {
  const idx = selected.value.indexOf(id)
  if (idx >= 0) selected.value.splice(idx, 1)
  else selected.value.push(id)
  selectAll.value = selected.value.length === posts.value.length
}

function toggleSelectAll(v) {
  selected.value = v ? posts.value.map(p => p.id) : []
}

function openBatchEdit() {
  batchMode.value = true
  editForm.value = { viewCount: null, likeCount: null, writerName: '', createdAt: null, targetBoardId: null }
  showEditDialog.value = true
}

function openSingleEdit(row) {
  batchMode.value = false
  editingPost.value = row
  editForm.value = {
    viewCount: row.viewCount,
    likeCount: row.likeCount,
    writerName: row.writerName || '',
    createdAt: row.createdAt ? new Date(row.createdAt) : null,
    targetBoardId: null,
  }
  showEditDialog.value = true
}

function openPostDetail(row) {
  // 별도 탭으로 게시글 페이지 이동 (보드 링크가 있을 경우)
}

async function saveEdit() {
  saving.value = true
  try {
    const ids = batchMode.value ? selected.value : [editingPost.value.id]
    const payload = { ids }
    // 배치: 비어있으면 null (변경 안함), 단건: 항상 전송
    if (!batchMode.value || editForm.value.writerName) payload.writerName = editForm.value.writerName || null
    if (!batchMode.value || editForm.value.viewCount !== null) payload.viewCount = editForm.value.viewCount
    if (!batchMode.value || editForm.value.likeCount !== null) payload.likeCount = editForm.value.likeCount
    if (editForm.value.createdAt) payload.createdAt = editForm.value.createdAt
    if (editForm.value.targetBoardId) payload.targetBoardId = editForm.value.targetBoardId

    await api.put('/admin/posts/batch', payload)
    ElMessage.success('저장되었습니다.')
    showEditDialog.value = false
    fetchPosts(currentPage.value)
  } finally { saving.value = false }
}

async function deleteOne(id) {
  await ElMessageBox.confirm('게시물을 삭제하시겠습니까?', '삭제', { type: 'warning', confirmButtonText: '삭제', cancelButtonText: '취소' })
  await api.delete('/admin/posts/batch', { data: { ids: [id] } })
  ElMessage.success('삭제되었습니다.')
  fetchPosts(currentPage.value)
}

async function batchDelete() {
  if (!selected.value.length) return
  await ElMessageBox.confirm(`선택한 ${selected.value.length}개 게시물을 삭제하시겠습니까?`, '삭제', { type: 'warning', confirmButtonText: '삭제', cancelButtonText: '취소' })
  await api.delete('/admin/posts/batch', { data: { ids: selected.value } })
  ElMessage.success('삭제되었습니다.')
  fetchPosts(0)
}

function fmtDate(d) {
  if (!d) return '-'
  const dt = new Date(d)
  return `${dt.getFullYear()}.${String(dt.getMonth()+1).padStart(2,'0')}.${String(dt.getDate()).padStart(2,'0')}`
}

onMounted(() => { fetchBoards(); fetchPosts() })
</script>

<style scoped>
@import '@/assets/admin-table.css';

.toolbar-left { display: flex; align-items: center; gap: 8px; }
.toolbar-right { display: flex; align-items: center; gap: 8px; }

.post-title {
  cursor: pointer;
  color: var(--adm-text);
  font-weight: 600;
}
.post-title:hover { color: var(--adm-primary); text-decoration: underline; }

.at-row.selected { background: color-mix(in srgb, var(--adm-primary) 6%, transparent); }

.ml4 { margin-left: 4px; }

.batch-notice {
  background: color-mix(in srgb, var(--adm-primary) 8%, transparent);
  border: 1px solid color-mix(in srgb, var(--adm-primary) 25%, transparent);
  border-radius: 6px;
  padding: 8px 12px;
  font-size: 12px;
  color: var(--adm-primary);
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
