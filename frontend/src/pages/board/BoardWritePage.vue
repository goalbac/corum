<template>
  <div class="board-write">
    <div class="write-inner">

    <!-- 페이지 헤더 (브래드크럼 + 제목) -->
    <div class="pg-header">
      <nav v-if="writeBreadcrumbs.length" class="breadcrumb" aria-label="breadcrumb">
        <span v-for="(item, i) in writeBreadcrumbs" :key="item.id" class="bc-wrap">
          <span class="bc-item">{{ item.name }}</span>
          <span class="bc-sep">/</span>
        </span>
        <span class="bc-item last">{{ isEdit ? '수정' : '글쓰기' }}</span>
      </nav>
      <h1 class="pg-title">{{ isEdit ? '게시글 수정' : '게시글 작성' }}</h1>
    </div>

    <!-- 작성 카드 -->
    <div class="write-card">
      <!-- 옵션 행 (카테고리 있거나 관리자 수정 시에만 표시) -->
      <div v-if="boardCategories.length || showAliasPicker || (isAdmin && isEdit)" class="options-row">
        <div v-if="boardCategories.length" class="option-group">
          <label class="option-label">카테고리</label>
          <select v-model="form.categoryId" class="cat-select">
            <option :value="null" disabled>카테고리 선택</option>
            <option v-for="cat in boardCategories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
          </select>
        </div>

        <div v-if="showAliasPicker" class="option-group">
          <label class="option-label">작성자</label>
          <select v-model="form.aliasName" class="cat-select">
            <option :value="null">본인 이름</option>
            <option v-for="name in board.writerIdentities" :key="name" :value="name">{{ name }}</option>
          </select>
        </div>

        <div class="option-spacer"></div>

        <!-- 관리자 전용 날짜 -->
        <template v-if="isAdmin && isEdit">
          <div class="option-group">
            <label class="option-label">게시일</label>
            <el-date-picker
              v-model="form.createdAt"
              type="datetime"
              placeholder="게시일 변경"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width:200px"
            />
          </div>
        </template>
      </div>

      <!-- 제목 + 공지 고정 토글 인라인 -->
      <div class="title-row">
        <input
          v-model="form.title"
          class="title-input"
          placeholder="제목을 입력하세요"
        />
        <div v-if="isAdmin || board?.useNotice" class="notice-toggle-wrap">
          <span class="option-label">공지 고정</span>
          <button
            type="button"
            class="toggle-btn"
            :class="{ on: form.isNotice }"
            @click="form.isNotice = !form.isNotice"
          >
            <span class="toggle-knob"></span>
          </button>
        </div>
      </div>

      <!-- 에디터 -->
      <div class="editor-wrap">
        <RichEditor v-model="form.content" placeholder="내용을 입력하세요." min-height="360px" style="width:100%" />
      </div>

      <!-- 파일 첨부 드롭존 -->
      <div
        class="drop-zone"
        :class="{ 'drag-over': isDragOver }"
        @click="fileInputRef?.click()"
        @dragover.prevent="isDragOver = true"
        @dragleave.prevent="isDragOver = false"
        @drop.prevent="handleDrop"
      >
        <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" class="drop-icon"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
        <div class="drop-text">
          파일을 끌어다 놓거나 클릭하여 업로드
          <span v-if="board?.boardType === 'DOCUMENT'" class="drop-required">*필수</span>
        </div>
        <div class="drop-sub">
          최대 {{ board?.fileMaxSizeMb || 10 }}MB ·
          {{ board?.fileMaxCount || 10 }}개<template v-if="board?.fileAllowedExtensions"> · {{ board.fileAllowedExtensions }}</template>
        </div>
      </div>

      <!-- 첨부 파일 칩 -->
      <div v-if="files.length" class="file-chips">
        <span v-for="(f, i) in files" :key="i" class="file-chip">
          <span class="chip-ext" :style="extStyle(f.name)">{{ extLabel(f.name) }}</span>
          <span class="chip-name">{{ f.name }}</span>
          <button type="button" class="chip-remove" @click.stop="removeFile(i)" aria-label="삭제">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </span>
      </div>

      <input
        ref="fileInputRef"
        type="file"
        multiple
        style="display:none"
        @change="handleFileInput"
      />
    </div>

    <!-- 액션 버튼 (카드 밖) -->
    <div class="write-actions">
      <button type="button" class="cancel-btn" @click="router.push(basePath)">취소</button>
      <div class="action-right">
        <button type="button" class="draft-btn">임시저장</button>
        <button type="button" class="submit-btn" :disabled="loading" @click="handleSubmit">
          <span v-if="loading">처리 중...</span>
          <span v-else>{{ isEdit ? '수정하기' : '등록하기' }}</span>
        </button>
      </div>
    </div>

    </div><!-- /write-inner -->
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import RichEditor from '@/components/common/RichEditor.vue'
import api from '@/api/axios'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const menuStore = useMenuStore()

const activeMenu = computed(() => menuStore.findMenuById(route.params.menuId))
const boardId = computed(() => route.params.boardId || activeMenu.value?.targetId)
const postId = computed(() => route.params.postId)
const basePath = computed(() => route.params.menuId ? `/menu/${route.params.menuId}` : `/board/${boardId.value}`)
const isEdit = computed(() => !!postId.value)
const isAdmin = computed(() => authStore.member?.isAdmin)

// 로컬 breadcrumb: 메뉴 트리를 상위부터 순서대로
const writeBreadcrumbs = computed(() => {
  if (!activeMenu.value) return []
  const items = []
  let current = activeMenu.value
  while (current) {
    items.unshift(current)
    current = current.parent || null
  }
  return items
})

const loading = ref(false)
const files = ref([])
const isDragOver = ref(false)
const fileInputRef = ref(null)
const board = ref(null)

const form = ref({ title: '', content: '', isNotice: false, createdAt: null, likeCount: 0, categoryId: null, aliasName: null })
const boardCategories = computed(() => board.value?.categories || [])
// 대리 작성: 게시판에서 켜져 있고, 등록된 이름이 있고, 현재 사용자가 권한자이고, 새 글 작성일 때만 노출
const showAliasPicker = computed(() =>
  !isEdit.value && !!board.value?.useAliasWriter && !!board.value?.canUseAliasWriter
  && (board.value?.writerIdentities?.length > 0)
)

async function fetchBoard() {
  if (!boardId.value) return
  try {
    const res = await api.get(`/boards/${boardId.value}`)
    board.value = res.data.data
  } catch {}
}

const DEFAULT_MAX_MB = 10
const DEFAULT_MAX_COUNT = 10

function validateFiles(newFiles) {
  if (!board.value) return true
  const maxMb = board.value.fileMaxSizeMb || DEFAULT_MAX_MB
  const maxCount = board.value.fileMaxCount || DEFAULT_MAX_COUNT
  const allowedExts = board.value.fileAllowedExtensions
    ? board.value.fileAllowedExtensions.split(',').map(e => e.trim().toLowerCase())
    : null

  if (files.value.length + newFiles.length > maxCount) {
    ElMessage.warning(`파일은 최대 ${maxCount}개까지 첨부 가능합니다.`)
    return false
  }
  for (const f of newFiles) {
    if (f.size > maxMb * 1024 * 1024) {
      ElMessage.warning(`파일 크기는 ${maxMb}MB 이하여야 합니다. (${f.name})`)
      return false
    }
    if (allowedExts) {
      const ext = f.name.split('.').pop().toLowerCase()
      if (!allowedExts.includes(ext)) {
        ElMessage.warning(`허용되지 않는 파일 형식입니다: .${ext}`)
        return false
      }
    }
  }
  return true
}

function addFiles(newFiles) {
  const arr = Array.from(newFiles)
  if (!validateFiles(arr)) return
  files.value.push(...arr)
}

function handleDrop(e) {
  isDragOver.value = false
  addFiles(e.dataTransfer.files)
}

function handleFileInput(e) {
  addFiles(e.target.files)
  e.target.value = ''
}

function removeFile(index) {
  files.value.splice(index, 1)
}

async function handleSubmit() {
  if (!form.value.title.trim()) { ElMessage.warning('제목을 입력해주세요.'); return }
  if (boardCategories.value.length && !form.value.categoryId) { ElMessage.warning('카테고리를 선택해주세요.'); return }
  if (!boardId.value) { ElMessage.error('연결된 게시판을 찾을 수 없습니다.'); return }
  if (board.value?.boardType === 'DOCUMENT' && !isEdit.value && files.value.length === 0) {
    ElMessage.warning('자료실에는 파일을 반드시 첨부해야 합니다.')
    return
  }

  loading.value = true
  try {
    let savedPostId = postId.value
    if (isEdit.value) {
      await api.put(`/boards/${boardId.value}/posts/${postId.value}`, form.value)
    } else {
      const res = await api.post(`/boards/${boardId.value}/posts`, form.value)
      savedPostId = res.data.data.id
    }
    if (files.value.length > 0) {
      const formData = new FormData()
      files.value.forEach(f => formData.append('files', f))
      await api.post(`/posts/${savedPostId}/files`, formData, { headers: { 'Content-Type': 'multipart/form-data' } })
    }
    ElMessage.success(isEdit.value ? '수정되었습니다.' : '등록되었습니다.')
    router.push(`${basePath.value}/posts/${savedPostId}`)
  } finally {
    loading.value = false
  }
}

async function fetchPost() {
  if (!isEdit.value || !boardId.value) return
  const res = await api.get(`/boards/${boardId.value}/posts/${postId.value}`)
  const p = res.data.data
  form.value = {
    title: p.title,
    content: p.content,
    isNotice: p.isNotice,
    createdAt: null,
    likeCount: p.likeCount ?? 0,
    categoryId: p.categoryId ?? null,
    aliasName: null,
  }
}

const EXT_MAP = {
  pdf:  { bg: 'rgba(214,69,63,0.13)', color: '#d6453f', label: 'PDF' },
  doc:  { bg: '#e9f0fe', color: '#2f5fd6', label: 'DOC' },
  docx: { bg: '#e9f0fe', color: '#2f5fd6', label: 'DOC' },
  xls:  { bg: '#e3f5ec', color: '#1f9d6b', label: 'XLS' },
  xlsx: { bg: '#e3f5ec', color: '#1f9d6b', label: 'XLS' },
  ppt:  { bg: 'rgba(217,119,6,0.1)', color: '#d97706', label: 'PPT' },
  pptx: { bg: 'rgba(217,119,6,0.1)', color: '#d97706', label: 'PPT' },
  zip:  { bg: '#eef1f6', color: '#929aab', label: 'ZIP' },
  rar:  { bg: '#eef1f6', color: '#929aab', label: 'ZIP' },
  hwp:  { bg: 'rgba(14,138,128,0.1)', color: '#0e8a80', label: 'HWP' },
  hwpx: { bg: 'rgba(14,138,128,0.1)', color: '#0e8a80', label: 'HWP' },
}
function getExt(name) { return (name?.split('.').pop() || '').toLowerCase() }
function extStyle(name) {
  const m = EXT_MAP[getExt(name)]
  return m ? { background: m.bg, color: m.color } : { background: '#eef1f6', color: '#929aab' }
}
function extLabel(name) {
  return EXT_MAP[getExt(name)]?.label || getExt(name).toUpperCase().slice(0, 4) || 'FILE'
}

watch([boardId, postId], async () => { await fetchBoard(); fetchPost() })

onMounted(async () => {
  if (route.params.menuId) await menuStore.fetchMenus()
  await fetchBoard()
  fetchPost()
})
</script>

<style scoped>
.board-write { color: var(--t1); }

.write-inner {
  max-width: 864px;
  margin: 0 auto;
}

/* ===== 페이지 헤더 ===== */
.pg-header { margin-bottom: 18px; }

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.bc-wrap {
  display: flex;
  align-items: center;
  gap: 2px;
}

.bc-item {
  font-size: 13px;
  color: var(--t3);
  font-weight: 500;
}

.bc-item.last {
  color: var(--t2);
  font-weight: 600;
}

.bc-sep {
  font-size: 13px;
  font-weight: 600;
  color: var(--t3);
  opacity: 0.5;
  margin: 0 4px;
}

.pg-title {
  margin: 0;
  font-size: 25px;
  font-weight: 800;
  letter-spacing: -0.03em;
}

/* ===== 작성 카드 ===== */
.write-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 24px;
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* ===== 옵션 행 ===== */
.options-row {
  display: flex;
  align-items: flex-end;
  gap: 24px;
  flex-wrap: wrap;
}

.option-group { display: flex; flex-direction: column; gap: 7px; }
.option-spacer { flex: 1; }

.option-label {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--t2);
}

.cat-select {
  font-family: inherit;
  font-size: 14px;
  font-weight: 500;
  color: var(--t1);
  background: var(--bg);
  border: 1px solid var(--border-strong);
  border-radius: 9px;
  padding: 9px 12px;
  outline: none;
  cursor: pointer;
  min-width: 160px;
  transition: border-color 0.15s;
}
.cat-select:focus { border-color: var(--primary); }

/* ===== 토글 ===== */
.toggle-btn {
  width: 42px;
  height: 24px;
  border-radius: 12px;
  border: none;
  background: var(--border-strong);
  position: relative;
  cursor: pointer;
  padding: 0;
  transition: background 0.15s;
  flex-shrink: 0;
}
.toggle-btn.on { background: var(--primary); }

.toggle-knob {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fff;
  transition: left 0.15s;
  box-shadow: 0 1px 2px rgba(0,0,0,0.25);
}
.toggle-btn.on .toggle-knob { left: 20px; }

/* ===== 제목 행 ===== */
.title-row {
  display: flex;
  align-items: center;
  gap: 16px;
  border-bottom: 1px solid var(--border-strong);
  transition: border-color 0.15s;
}
.title-row:focus-within { border-color: var(--primary); }

.notice-toggle-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  padding-bottom: 2px;
}

/* ===== 제목 입력 ===== */
.title-input {
  flex: 1;
  min-width: 0;
  font-family: inherit;
  font-size: 19px;
  font-weight: 600;
  color: var(--t1);
  background: transparent;
  border: none;
  padding: 11px 2px;
  outline: none;
}
.title-input::placeholder { color: var(--t3); }

/* ===== 에디터 ===== */
.editor-wrap {
  border: 1px solid var(--border-strong);
  border-radius: 12px;
  overflow: hidden;
}

/* ===== 드롭존 ===== */
.drop-zone {
  border: 2px dashed var(--border-strong);
  border-radius: 12px;
  padding: 22px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
}
.drop-zone:hover,
.drop-zone.drag-over {
  border-color: var(--primary);
  background: var(--primary-weak);
}
.drop-zone.drag-over .drop-icon { color: var(--primary); }

.drop-icon {
  color: var(--t3);
  transition: color 0.15s;
}

.drop-text {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--t2);
  display: flex;
  align-items: center;
  gap: 6px;
}

.drop-required {
  font-size: 11px;
  font-weight: 700;
  color: var(--warn);
}

.drop-sub {
  font-size: 12px;
  color: var(--t3);
}

/* ===== 파일 칩 ===== */
.file-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.file-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--t2);
  background: var(--surface-2);
  padding: 6px 10px;
  border-radius: 8px;
  max-width: 320px;
}

.chip-ext {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 8px;
  font-weight: 800;
  flex-shrink: 0;
}

.chip-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.chip-remove {
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: none;
  color: var(--t3);
  cursor: pointer;
  padding: 0;
  flex-shrink: 0;
  transition: color 0.12s;
}
.chip-remove:hover { color: var(--danger); }

/* ===== 액션 버튼 (카드 밖) ===== */
.write-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20px;
}

.action-right {
  display: flex;
  gap: 8px;
}

.cancel-btn {
  padding: 10px 20px;
  border: 1px solid var(--border-strong);
  background: var(--surface);
  color: var(--t2);
  font-family: inherit;
  font-size: 14px;
  font-weight: 600;
  border-radius: 9px;
  cursor: pointer;
  transition: all 0.15s;
}
.cancel-btn:hover { background: var(--surface-2); }

.draft-btn {
  padding: 10px 18px;
  border: 1px solid var(--border-strong);
  background: var(--surface);
  color: var(--t1);
  font-family: inherit;
  font-size: 14px;
  font-weight: 600;
  border-radius: 9px;
  cursor: pointer;
  transition: all 0.15s;
}
.draft-btn:hover { background: var(--surface-2); }

.submit-btn {
  padding: 10px 24px;
  border: none;
  background: var(--primary);
  color: #fff;
  font-family: inherit;
  font-size: 14px;
  font-weight: 700;
  border-radius: 9px;
  cursor: pointer;
  transition: background 0.15s;
}
.submit-btn:hover:not(:disabled) { background: var(--primary-strong); }
.submit-btn:disabled { opacity: 0.6; cursor: default; }

@media (max-width: 640px) {
  .write-card { padding: 16px; }
  .title-input { font-size: 16px; }
  .options-row { gap: 14px; }
  .write-actions { flex-direction: column; align-items: stretch; gap: 8px; }
  .action-right { justify-content: flex-end; }
}
</style>
