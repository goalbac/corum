<template>
  <div class="board-write">

    <!-- 페이지 헤더 -->
    <div class="pg-header">
      <h1 class="pg-title">{{ isEdit ? '게시글 수정' : '게시글 작성' }}</h1>
    </div>

    <!-- 작성 카드 -->
    <div class="write-card">
      <!-- 옵션 행 (카테고리 있거나 관리자 수정 시에만 표시) -->
      <div v-if="boardCategories.length || (isAdmin && isEdit)" class="options-row">
        <div v-if="boardCategories.length" class="option-group">
          <label class="option-label">카테고리</label>
          <select v-model="form.categoryId" class="cat-select">
            <option :value="null" disabled>카테고리 선택</option>
            <option v-for="cat in boardCategories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
          </select>
        </div>

        <div class="option-spacer"></div>

        <!-- 관리자 전용 날짜/좋아요 수 -->
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

      <!-- 파일 첨부 -->
      <div class="attach-section">
        <div class="attach-header">
          <span class="attach-label">
            파일 첨부
            <span v-if="board?.boardType === 'DOCUMENT'" class="attach-required">*필수</span>
          </span>
          <span class="attach-tip">
            최대 {{ board?.fileMaxSizeMb || 10 }}MB · {{ board?.fileMaxCount || 10 }}개
            <template v-if="board?.fileAllowedExtensions"> · 허용: {{ board.fileAllowedExtensions }}</template>
          </span>
        </div>
        <el-upload
          v-model:file-list="fileList"
          :auto-upload="false"
          multiple
          :on-change="handleFileChange"
          class="file-uploader"
        >
          <button type="button" class="attach-btn">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
            파일 선택
          </button>
        </el-upload>
      </div>

      <!-- 액션 버튼 -->
      <div class="write-actions">
        <button type="button" class="cancel-btn" @click="router.push(basePath)">취소</button>
        <button type="button" class="submit-btn" :disabled="loading" @click="handleSubmit">
          <span v-if="loading" class="loading-dots">처리 중...</span>
          <span v-else>{{ isEdit ? '수정하기' : '등록하기' }}</span>
        </button>
      </div>
    </div>

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

const loading = ref(false)
const fileList = ref([])
const files = ref([])
const board = ref(null)

const form = ref({ title: '', content: '', isNotice: false, createdAt: null, likeCount: 0, categoryId: null })
const boardCategories = computed(() => board.value?.categories || [])

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

function handleFileChange(file) {
  if (file.raw) {
    if (!validateFiles([file.raw])) {
      fileList.value = fileList.value.filter(f => f.uid !== file.uid)
      return
    }
    files.value.push(file.raw)
  }
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
  }
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

/* ===== 페이지 헤더 ===== */
.pg-header { margin-bottom: 18px; }
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
.toggle-group { flex-direction: row; align-items: center; gap: 9px; }

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

/* ===== 제목 행 (제목 + 공지 고정 토글) ===== */
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

/* ===== 파일 첨부 ===== */
.attach-section {
  border-top: 1px solid var(--border);
  padding-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.attach-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.attach-label {
  font-size: 13px;
  font-weight: 700;
  color: var(--t2);
  display: flex;
  align-items: center;
  gap: 6px;
}

.attach-required {
  font-size: 11px;
  font-weight: 700;
  color: var(--warn);
}

.attach-tip {
  font-size: 12px;
  color: var(--t3);
}

.attach-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid var(--border-strong);
  background: var(--surface);
  color: var(--t2);
  font-family: inherit;
  font-weight: 600;
  font-size: 13px;
  padding: 8px 14px;
  border-radius: 9px;
  cursor: pointer;
  transition: all 0.15s;
}
.attach-btn:hover { border-color: var(--primary); color: var(--primary); }

/* ===== 액션 버튼 ===== */
.write-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
  border-top: 1px solid var(--border);
  padding-top: 16px;
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
.cancel-btn:hover { border-color: var(--primary); color: var(--primary); }

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

/* el-upload 내부 버튼 숨기고 우리 버튼 사용 */
:deep(.el-upload) { display: inline-block; }
:deep(.el-upload-list) {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
:deep(.el-upload-list__item) {
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 6px 12px;
  font-size: 13px;
  color: var(--t2);
}
:deep(.el-upload-list__item .el-upload-list__item-name) { color: var(--t2); }

@media (max-width: 640px) {
  .write-card { padding: 16px; }
  .title-input { font-size: 16px; }
  .options-row { gap: 14px; }
}
</style>
