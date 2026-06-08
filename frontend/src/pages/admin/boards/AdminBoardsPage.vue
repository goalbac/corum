<template>
  <div class="adm-page">
    <AdminPageHeader title="게시판 관리" desc="게시판 생성 및 권한 설정">
      <button class="adm-btn primary" @click="openCreate">
        <i class="ti ti-plus"></i> 게시판 추가
      </button>
    </AdminPageHeader>

    <div class="adm-card" v-loading="loading">
      <div class="at-wrap">
        <div class="at-head">
          <div class="at-col" style="width:60px;text-align:center">ID</div>
          <div class="at-col" style="flex:1">게시판명</div>
          <div class="at-col" style="width:90px;text-align:center">유형</div>
          <div class="at-col" style="width:60px;text-align:center">댓글</div>
          <div class="at-col" style="width:60px;text-align:center">좋아요</div>
          <div class="at-col" style="width:110px;text-align:center">파일 용량</div>
          <div class="at-col" style="width:70px;text-align:center">상태</div>
          <div class="at-col" style="width:100px;text-align:center">관리</div>
        </div>
        <div v-for="row in boards" :key="row.id" class="at-row">
          <div class="at-col muted" style="width:60px;text-align:center">{{ row.id }}</div>
          <div class="at-col bold" style="flex:1">{{ row.name }}</div>
          <div class="at-col" style="width:90px;text-align:center">
            <span :class="['adm-badge', typeBadge(row.boardType)]">{{ typeLabel(row.boardType) }}</span>
          </div>
          <div class="at-col" style="width:60px;text-align:center">
            <i :class="row.useComment ? 'ti ti-check green' : 'ti ti-minus muted'"></i>
          </div>
          <div class="at-col" style="width:60px;text-align:center">
            <i :class="row.useLike ? 'ti ti-check green' : 'ti ti-minus muted'"></i>
          </div>
          <div class="at-col muted" style="width:110px;text-align:center">
            {{ row.fileMaxSizeMb ? row.fileMaxSizeMb + 'MB' : '전역 설정' }}
          </div>
          <div class="at-col" style="width:70px;text-align:center">
            <span :class="['adm-badge', row.isActive ? 'badge-success' : 'badge-muted']">
              {{ row.isActive ? '활성' : '비활성' }}
            </span>
          </div>
          <div class="at-col at-actions" style="width:100px">
            <button class="act-btn" @click="openEdit(row)"><i class="ti ti-edit"></i> 수정</button>
            <button class="act-btn danger" @click="deleteBoard(row.id)"><i class="ti ti-trash"></i></button>
          </div>
        </div>
        <div v-if="!boards.length && !loading" class="at-empty">
          <i class="ti ti-inbox"></i><span>등록된 게시판이 없습니다.</span>
        </div>
      </div>
    </div>

    <!-- 다이얼로그 -->
    <el-dialog v-model="showForm" :title="editing ? '게시판 수정' : '게시판 추가'" width="480px" destroy-on-close>
      <div class="dlg-form">
        <div class="dlg-row">
          <div class="dlg-field">
            <label>게시판명</label>
            <el-input v-model="form.name" />
          </div>
          <div class="dlg-field">
            <label>유형</label>
            <el-select v-model="form.boardType" style="width:100%">
              <el-option value="POST" label="글 게시판" />
              <el-option value="GALLERY" label="갤러리" />
              <el-option value="WEBZINE" label="웹진" />
              <el-option value="DOCUMENT" label="자료실" />
            </el-select>
          </div>
        </div>
        <div class="dlg-row">
          <div class="dlg-field">
            <label>파일 최대 용량 (MB)</label>
            <el-input-number v-model="form.fileMaxSizeMb" :min="1" :max="500" style="width:100%" placeholder="전역 설정 사용" />
          </div>
          <div class="dlg-field">
            <label>최대 파일 수</label>
            <el-input-number v-model="form.fileMaxCount" :min="1" :max="20" style="width:100%" />
          </div>
        </div>
        <div class="dlg-field">
          <label>허용 확장자 <span class="label-hint">(빈칸=전체 허용)</span></label>
          <el-input v-model="form.fileAllowedExtensions" placeholder="jpg,png,pdf,docx" />
        </div>
        <div class="dlg-checks">
          <label class="chk-item"><el-checkbox v-model="form.useComment" />댓글 사용</label>
          <label class="chk-item"><el-checkbox v-model="form.useLike" />좋아요 사용</label>
          <label class="chk-item"><el-checkbox v-model="form.useNotice" />공지 사용</label>
          <label class="chk-item"><el-checkbox v-model="form.isActive" />활성화</label>
        </div>
      </div>
      <template #footer>
        <button class="adm-btn ghost" @click="showForm = false">취소</button>
        <button class="adm-btn primary" :disabled="saving" @click="saveBoard">
          <i v-if="saving" class="ti ti-loader-2 spinning"></i>
          {{ saving ? '저장 중...' : '저장' }}
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

const boards  = ref([])
const loading = ref(false)
const saving  = ref(false)
const showForm = ref(false)
const editing  = ref(null)

const defaultForm = () => ({
  name: '', boardType: 'POST', useComment: true, useLike: true,
  useNotice: true, isActive: true, fileMaxSizeMb: null,
  fileMaxCount: 5, fileAllowedExtensions: '',
})
const form = ref(defaultForm())

async function fetchBoards() {
  loading.value = true
  try { const res = await api.get('/boards'); boards.value = res.data.data || [] }
  finally { loading.value = false }
}

function openCreate() { editing.value = null; form.value = defaultForm(); showForm.value = true }
function openEdit(board) { editing.value = board; form.value = { ...board }; showForm.value = true }

async function saveBoard() {
  if (!form.value.name) return ElMessage.warning('게시판명을 입력해주세요.')
  saving.value = true
  try {
    editing.value ? await api.put(`/boards/${editing.value.id}`, form.value) : await api.post('/boards', form.value)
    ElMessage.success('저장되었습니다.')
    showForm.value = false
    fetchBoards()
  } finally { saving.value = false }
}

async function deleteBoard(id) {
  await ElMessageBox.confirm('게시판을 삭제하시겠습니까?', '삭제', { type: 'warning', confirmButtonText: '삭제', cancelButtonText: '취소' })
  await api.delete(`/boards/${id}`)
  ElMessage.success('삭제되었습니다.')
  fetchBoards()
}

function typeLabel(t) { return { POST: '일반', GALLERY: '갤러리', WEBZINE: '웹진', DOCUMENT: '자료실' }[t] || t }
function typeBadge(t) { return { POST: 'badge-primary', GALLERY: 'badge-success', WEBZINE: 'badge-info', DOCUMENT: 'badge-warning' }[t] || '' }
onMounted(fetchBoards)
</script>

<style scoped>
@import '@/assets/admin-table.css';
</style>
