<template>
  <div>
    <AdminPageHeader title="게시판 관리" desc="게시판 생성 및 권한 설정">
      <el-button type="primary" size="small" @click="openCreate">
        <i class="ti ti-plus" style="margin-right:4px"></i>게시판 추가
      </el-button>
    </AdminPageHeader>

    <el-table :data="boards" v-loading="loading" border>
      <el-table-column label="ID" prop="id" width="60" align="center" />
      <el-table-column label="게시판명" prop="name" min-width="140" />
      <el-table-column label="유형" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="typeColor(row.boardType)" effect="plain">{{ typeLabel(row.boardType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="댓글" width="70" align="center">
        <template #default="{ row }">
          <i :class="row.useComment ? 'ti ti-check' : 'ti ti-x'" :style="{ color: row.useComment ? 'var(--green)' : 'var(--t3)' }"></i>
        </template>
      </el-table-column>
      <el-table-column label="좋아요" width="70" align="center">
        <template #default="{ row }">
          <i :class="row.useLike ? 'ti ti-check' : 'ti ti-x'" :style="{ color: row.useLike ? 'var(--green)' : 'var(--t3)' }"></i>
        </template>
      </el-table-column>
      <el-table-column label="파일 용량" width="100" align="center">
        <template #default="{ row }">
          <span class="text-muted">{{ row.fileMaxSizeMb ? row.fileMaxSizeMb + 'MB' : '전역 설정' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="상태" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.isActive ? 'success' : 'info'" effect="plain">
            {{ row.isActive ? '활성' : '비활성' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="관리" width="120" align="center">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">수정</el-button>
          <el-button size="small" type="danger" plain @click="deleteBoard(row.id)">삭제</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 게시판 생성/수정 다이얼로그 -->
    <el-dialog v-model="showForm" :title="editing ? '게시판 수정' : '게시판 추가'" width="520px" destroy-on-close>
      <el-form :model="form" label-position="top">
        <div class="form-row">
          <el-form-item label="게시판명">
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item label="유형">
            <el-select v-model="form.boardType" style="width:100%">
              <el-option value="POST"     label="글 게시판" />
              <el-option value="GALLERY"  label="갤러리" />
              <el-option value="DOCUMENT" label="자료실" />
            </el-select>
          </el-form-item>
        </div>
        <div class="check-row">
          <el-checkbox v-model="form.useComment">댓글 사용</el-checkbox>
          <el-checkbox v-model="form.useLike">좋아요 사용</el-checkbox>
          <el-checkbox v-model="form.useNotice">공지 사용</el-checkbox>
          <el-checkbox v-model="form.isActive">활성화</el-checkbox>
        </div>
        <div class="form-row">
          <el-form-item label="파일 최대 용량 (MB, 빈칸=전역 설정)">
            <el-input-number v-model="form.fileMaxSizeMb" :min="1" :max="500" style="width:100%" />
          </el-form-item>
          <el-form-item label="최대 파일 수">
            <el-input-number v-model="form.fileMaxCount" :min="1" :max="20" style="width:100%" />
          </el-form-item>
        </div>
        <el-form-item label="허용 확장자 (빈칸=전체)">
          <el-input v-model="form.fileAllowedExtensions" placeholder="jpg,png,pdf,docx" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">취소</el-button>
        <el-button type="primary" :loading="saving" @click="saveBoard">저장</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'

const boards  = ref([])
const loading = ref(false)
const saving  = ref(false)
const showForm = ref(false)
const editing  = ref(null)

const form = ref({
  name: '', boardType: 'POST', useComment: true, useLike: true,
  useNotice: true, isActive: true, fileMaxSizeMb: null,
  fileMaxCount: 5, fileAllowedExtensions: '',
})

async function fetchBoards() {
  loading.value = true
  try { const res = await api.get('/boards'); boards.value = res.data.data || [] }
  finally { loading.value = false }
}

function openCreate() {
  editing.value = null
  form.value = { name: '', boardType: 'POST', useComment: true, useLike: true, useNotice: true, isActive: true, fileMaxSizeMb: null, fileMaxCount: 5, fileAllowedExtensions: '' }
  showForm.value = true
}

function openEdit(board) {
  editing.value = board
  form.value = { ...board }
  showForm.value = true
}

async function saveBoard() {
  if (!form.value.name) return ElMessage.warning('게시판명을 입력해주세요.')
  saving.value = true
  try {
    if (editing.value) {
      await api.put(`/boards/${editing.value.id}`, form.value)
    } else {
      await api.post('/boards', form.value)
    }
    ElMessage.success('저장되었습니다.')
    showForm.value = false
    fetchBoards()
  } finally { saving.value = false }
}

async function deleteBoard(id) {
  await ElMessageBox.confirm('게시판을 삭제하시겠습니까?', '삭제', { type: 'warning' })
  await api.delete(`/boards/${id}`)
  ElMessage.success('삭제되었습니다.')
  fetchBoards()
}

function typeLabel(t) { return { POST: '글', GALLERY: '갤러리', DOCUMENT: '자료실' }[t] || t }
function typeColor(t) { return { POST: undefined, GALLERY: 'success', DOCUMENT: 'warning' }[t] }

onMounted(fetchBoards)
</script>

<style scoped>
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.check-row { display: flex; gap: 16px; flex-wrap: wrap; margin-bottom: 16px; }
.text-muted { font-size: 12px; color: var(--t3); }
</style>