<template>
  <div class="adm-page">
    <AdminPageHeader title="댓글 관리" desc="최근 작성된 댓글을 조회하고 관리합니다.">
      <button class="adm-btn danger" :disabled="!selected.length" @click="batchDelete">
        <i class="ti ti-trash"></i> 선택 삭제 ({{ selected.length }})
      </button>
      <button class="adm-btn primary" :disabled="!selected.length" @click="openDateEdit">
        <i class="ti ti-calendar-time"></i> 작성일시 수정 ({{ selected.length }})
      </button>
    </AdminPageHeader>

    <div class="adm-card">
      <div class="adm-toolbar">
        <div class="toolbar-left">
          <span class="total-count">총 {{ totalElements.toLocaleString() }}개</span>
        </div>
        <div class="toolbar-right">
          <label class="chk-item" style="font-size:13px">
            <el-checkbox v-model="selectAll" @change="toggleSelectAll" /> 전체 선택
          </label>
        </div>
      </div>

      <div v-loading="loading" class="comment-list">
        <div v-for="c in comments" :key="c.id"
          class="comment-card"
          :class="{ selected: selected.includes(c.id), deleted: c.isDeleted }">
          <!-- 선택 체크박스 -->
          <div class="cc-check">
            <el-checkbox :model-value="selected.includes(c.id)" @change="toggleSelect(c.id)" />
          </div>

          <!-- 아바타 -->
          <div class="cc-avatar">
            <img v-if="c.profileImageUrl" :src="resolveFileUrl(c.profileImageUrl)" class="avatar-img" @error="e => e.target.style.display='none'" />
            <span v-else class="avatar-fallback">{{ (c.writerName || '?').charAt(0) }}</span>
          </div>

          <!-- 본문 -->
          <div class="cc-body">
            <div class="cc-meta">
              <span class="cc-writer">{{ c.writerName || '(알 수 없음)' }}</span>
              <span class="cc-sep">·</span>
              <span class="cc-board">{{ c.boardName }}</span>
              <i class="ti ti-chevron-right cc-arrow"></i>
              <span class="cc-post" :title="c.postTitle">{{ c.postTitle }}</span>
              <span class="cc-sep">·</span>
              <span class="cc-date">{{ fmtDatetime(c.createdAt) }}</span>
              <span v-if="c.depth > 0" class="adm-badge badge-info" style="margin-left:6px;font-size:10px">
                {{ c.depth === 1 ? '답글' : '대댓글' }}
              </span>
              <span v-if="c.isDeleted" class="adm-badge badge-muted" style="margin-left:6px;font-size:10px">삭제됨</span>
            </div>
            <div class="cc-content" :class="{ deleted: c.isDeleted }">{{ c.content }}</div>
          </div>

          <!-- 단건 삭제 -->
          <div class="cc-actions">
            <button class="act-btn danger" @click="deleteOne(c.id)" title="삭제"><i class="ti ti-trash"></i></button>
          </div>
        </div>

        <div v-if="!comments.length && !loading" class="at-empty">
          <i class="ti ti-message-off"></i><span>댓글이 없습니다.</span>
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

    <!-- 작성일시 수정 다이얼로그 -->
    <el-dialog v-model="showDateDialog" :title="`작성일시 수정 (${selected.length}건)`" width="380px" destroy-on-close>
      <div class="dlg-form">
        <div class="batch-notice">
          <i class="ti ti-info-circle"></i>
          선택한 {{ selected.length }}개 댓글의 작성일시를 동일한 값으로 변경합니다.
        </div>
        <div class="dlg-field">
          <label>변경할 작성일시</label>
          <el-date-picker v-model="newDate" type="datetime" style="width:100%" />
        </div>
      </div>
      <template #footer>
        <button class="adm-btn ghost" @click="showDateDialog = false">취소</button>
        <button class="adm-btn primary" :disabled="saving || !newDate" @click="saveDateEdit">
          <i v-if="saving" class="ti ti-loader-2 spinning"></i>{{ saving ? '저장 중...' : '저장' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'
import { resolveFileUrl } from '@/utils/fileUrl'

const comments = ref([])
const loading = ref(false)
const saving = ref(false)
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const selected = ref([])
const selectAll = ref(false)
const showDateDialog = ref(false)
const newDate = ref(null)

async function fetchComments(page = 0) {
  loading.value = true
  selected.value = []
  selectAll.value = false
  try {
    const r = await api.get('/admin/comments', { params: { page, size: 20 } })
    const d = r.data.data
    comments.value = d.items || []
    totalPages.value = d.totalPages || 0
    totalElements.value = d.totalElements || 0
    currentPage.value = d.page || 0
  } finally { loading.value = false }
}

function goPage(p) { fetchComments(p) }

function toggleSelect(id) {
  const idx = selected.value.indexOf(id)
  if (idx >= 0) selected.value.splice(idx, 1)
  else selected.value.push(id)
  selectAll.value = selected.value.length === comments.value.length
}

function toggleSelectAll(v) {
  selected.value = v ? comments.value.map(c => c.id) : []
}

function openDateEdit() {
  newDate.value = null
  showDateDialog.value = true
}

async function saveDateEdit() {
  if (!newDate.value) return
  saving.value = true
  try {
    await api.put('/admin/comments/batch', { ids: selected.value, createdAt: newDate.value })
    ElMessage.success('작성일시가 수정되었습니다.')
    showDateDialog.value = false
    fetchComments(currentPage.value)
  } finally { saving.value = false }
}

async function deleteOne(id) {
  await ElMessageBox.confirm('댓글을 삭제하시겠습니까?', '삭제', { type: 'warning', confirmButtonText: '삭제', cancelButtonText: '취소' })
  await api.delete('/admin/comments/batch', { data: { ids: [id] } })
  ElMessage.success('삭제되었습니다.')
  fetchComments(currentPage.value)
}

async function batchDelete() {
  if (!selected.value.length) return
  await ElMessageBox.confirm(`선택한 ${selected.value.length}개 댓글을 삭제하시겠습니까?`, '삭제', { type: 'warning', confirmButtonText: '삭제', cancelButtonText: '취소' })
  await api.delete('/admin/comments/batch', { data: { ids: selected.value } })
  ElMessage.success('삭제되었습니다.')
  fetchComments(0)
}

function fmtDatetime(d) {
  if (!d) return '-'
  const dt = new Date(d)
  return `${dt.getFullYear()}.${String(dt.getMonth()+1).padStart(2,'0')}.${String(dt.getDate()).padStart(2,'0')} ${String(dt.getHours()).padStart(2,'0')}:${String(dt.getMinutes()).padStart(2,'0')}`
}

onMounted(() => { fetchComments() })
</script>

<style scoped>
@import '@/assets/admin-table.css';

.toolbar-left { display: flex; align-items: center; gap: 8px; }
.toolbar-right { display: flex; align-items: center; gap: 8px; }
.total-count { font-size: 13px; color: var(--adm-muted); font-weight: 600; }

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.comment-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 18px;
  border-bottom: 0.5px solid var(--adm-border);
  transition: background 0.15s;
}
.comment-card:last-child { border-bottom: none; }
.comment-card:hover { background: var(--adm-bg-subtle, rgba(0,0,0,0.02)); }
.comment-card.selected { background: color-mix(in srgb, var(--adm-primary) 6%, transparent); }
.comment-card.deleted { opacity: 0.6; }

.cc-check { flex-shrink: 0; padding-top: 2px; }

.cc-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--adm-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}
.avatar-img { width: 100%; height: 100%; object-fit: cover; }
.avatar-fallback {
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  line-height: 1;
}

.cc-body { flex: 1; min-width: 0; }

.cc-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 5px;
  font-size: 12px;
}

.cc-writer { font-weight: 700; color: var(--adm-text); }
.cc-sep { color: var(--adm-muted); }
.cc-board { color: var(--adm-muted); }
.cc-arrow { font-size: 10px; color: var(--adm-muted); }
.cc-post {
  color: var(--adm-primary);
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}
.cc-post:hover { text-decoration: underline; }
.cc-date { color: var(--adm-muted); }

.cc-content {
  font-size: 13px;
  color: var(--adm-text);
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.cc-content.deleted { color: var(--adm-muted); font-style: italic; }

.cc-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  opacity: 0;
  transition: opacity 0.15s;
}
.comment-card:hover .cc-actions { opacity: 1; }

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
