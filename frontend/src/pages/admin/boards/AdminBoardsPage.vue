<template>
  <div class="adm-page">
    <AdminPageHeader title="게시판 관리" desc="게시판 생성 및 그룹 권한 설정">
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
          <div class="at-col" style="width:130px;text-align:center">관리</div>
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
          <div class="at-col at-actions" style="width:130px">
            <button class="act-btn" @click="openEdit(row)"><i class="ti ti-edit"></i> 수정</button>
            <button class="act-btn primary" @click="openPermissions(row)" title="그룹 권한">
              <i class="ti ti-shield-check"></i>
            </button>
            <button class="act-btn danger" @click="deleteBoard(row.id)"><i class="ti ti-trash"></i></button>
          </div>
        </div>
        <div v-if="!boards.length && !loading" class="at-empty">
          <i class="ti ti-inbox"></i><span>등록된 게시판이 없습니다.</span>
        </div>
      </div>
    </div>

    <!-- 기본 설정 다이얼로그 -->
    <el-dialog v-model="showForm" :title="editing ? '게시판 수정' : '게시판 추가'" width="500px" destroy-on-close>
      <div class="dlg-form">

        <!-- 기본 정보 -->
        <div class="dlg-section-title">기본 정보</div>
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

        <!-- 기능 설정 -->
        <div class="dlg-section-title">기능 설정</div>
        <div class="dlg-checks">
          <label class="chk-item"><el-checkbox v-model="form.useComment" />댓글 사용</label>
          <label class="chk-item"><el-checkbox v-model="form.useLike" />좋아요 사용</label>
          <label class="chk-item"><el-checkbox v-model="form.useNotice" />공지 사용</label>
          <label class="chk-item"><el-checkbox v-model="form.isActive" />활성화</label>
        </div>

        <!-- 파일 설정 -->
        <div class="dlg-section-title">파일 설정</div>
        <div class="dlg-row">
          <div class="dlg-field">
            <label>최대 용량 (MB)</label>
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
      </div>
      <template #footer>
        <button class="adm-btn ghost" @click="showForm = false">취소</button>
        <button class="adm-btn primary" :disabled="saving" @click="saveBoard">
          <i v-if="saving" class="ti ti-loader-2 spinning"></i>
          {{ saving ? '저장 중...' : '저장' }}
        </button>
      </template>
    </el-dialog>

    <!-- 그룹 권한 다이얼로그 -->
    <el-dialog
      v-model="showPerms"
      :title="`그룹 권한 설정 — ${permBoard?.name}`"
      width="680px"
      destroy-on-close
    >
      <div class="perm-hint">
        <i class="ti ti-info-circle"></i>
        <span>
          <strong>관리</strong> 권한이 있는 그룹은 해당 게시판의 모든 글을 수정·삭제할 수 있습니다.
          전체 관리자(운영 그룹 소속)는 항상 모든 게시판의 관리 권한을 갖습니다.
        </span>
      </div>

      <div class="perm-table-wrap" v-loading="permsLoading">
        <table class="perm-table">
          <thead>
            <tr>
              <th style="width:180px;text-align:left">그룹</th>
              <th class="perm-col">
                <div class="th-inner">
                  <span>조회</span>
                  <button class="col-all-btn" @click="toggleAllCol('canRead')" title="전체 선택/해제">전체</button>
                </div>
              </th>
              <th class="perm-col">
                <div class="th-inner">
                  <span>댓글</span>
                  <button class="col-all-btn" @click="toggleAllCol('canComment')" title="전체 선택/해제">전체</button>
                </div>
              </th>
              <th class="perm-col">
                <div class="th-inner">
                  <span>다운로드</span>
                  <button class="col-all-btn" @click="toggleAllCol('canDownload')" title="전체 선택/해제">전체</button>
                </div>
              </th>
              <th class="perm-col manage-col">관리</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in permRows" :key="row.groupId">
              <td class="perm-group">
                <div class="perm-group-inner">
                  <span class="perm-parent">{{ row.parentName }}</span>
                  <span class="perm-name">{{ row.groupName }}</span>
                  <span v-if="row.groupType === 'ADMIN'" class="adm-badge badge-purple" style="font-size:9px;flex-shrink:0">관리자</span>
                  <button class="row-all-btn" @click="grantAll(row)" title="조회·댓글·다운로드 전체 부여">전체</button>
                </div>
              </td>
              <td class="perm-col">
                <el-tooltip content="게시물 목록·내용을 볼 수 있습니다" placement="top">
                  <el-checkbox v-model="row.canRead" />
                </el-tooltip>
              </td>
              <td class="perm-col">
                <el-tooltip content="게시물에 댓글을 작성할 수 있습니다" placement="top">
                  <el-checkbox v-model="row.canComment" />
                </el-tooltip>
              </td>
              <td class="perm-col">
                <el-tooltip content="첨부파일을 다운로드할 수 있습니다" placement="top">
                  <el-checkbox v-model="row.canDownload" />
                </el-tooltip>
              </td>
              <td class="perm-col manage-col">
                <el-tooltip content="게시판의 모든 글을 수정·삭제할 수 있습니다" placement="top">
                  <el-checkbox v-model="row.canManage" />
                </el-tooltip>
              </td>
            </tr>
            <tr v-if="!permRows.length && !permsLoading">
              <td colspan="5" class="perm-empty">설정 가능한 그룹이 없습니다.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <template #footer>
        <button class="adm-btn ghost" @click="showPerms = false">취소</button>
        <button class="adm-btn primary" :disabled="permSaving" @click="savePermissions">
          <i v-if="permSaving" class="ti ti-loader-2 spinning"></i>
          {{ permSaving ? '저장 중...' : '저장' }}
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

// ===== 게시판 목록 =====
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

// ===== 그룹 권한 =====
const showPerms   = ref(false)
const permBoard   = ref(null)
const permRows    = ref([])
const permsLoading = ref(false)
const permSaving  = ref(false)

async function openPermissions(board) {
  permBoard.value  = board
  showPerms.value  = true
  permsLoading.value = true
  try {
    const res = await api.get(`/admin/boards/${board.id}/permissions`)
    permRows.value = res.data.data || []
  } finally {
    permsLoading.value = false
  }
}

/** 컬럼 전체 선택/해제 (토글: 하나라도 false이면 전체 true, 모두 true이면 전체 false) */
function toggleAllCol(field) {
  const allChecked = permRows.value.every(r => r[field])
  permRows.value.forEach(r => { r[field] = !allChecked })
}

/** 행 전체 부여 (조회·댓글·다운로드) */
function grantAll(row) {
  row.canRead = true
  row.canComment = true
  row.canDownload = true
}

async function savePermissions() {
  permSaving.value = true
  try {
    await api.put(`/admin/boards/${permBoard.value.id}/permissions`, permRows.value)
    ElMessage.success('권한이 저장되었습니다.')
    showPerms.value = false
  } finally {
    permSaving.value = false
  }
}

function typeLabel(t) { return { POST: '일반', GALLERY: '갤러리', WEBZINE: '웹진', DOCUMENT: '자료실' }[t] || t }
function typeBadge(t) { return { POST: 'badge-primary', GALLERY: 'badge-success', WEBZINE: 'badge-info', DOCUMENT: 'badge-warning' }[t] || '' }
onMounted(fetchBoards)
</script>

<style scoped>
@import '@/assets/admin-table.css';

/* 섹션 제목 */
.dlg-section-title {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--t3);
  padding: 4px 0 8px;
  border-bottom: 1px solid var(--border);
  margin: 16px 0 12px;
}
.dlg-section-title:first-child { margin-top: 0; }

/* 권한 안내 */
.perm-hint {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  background: color-mix(in srgb, var(--accent) 8%, transparent);
  border: 0.5px solid color-mix(in srgb, var(--accent) 30%, transparent);
  border-radius: var(--radius-xs);
  padding: 10px 14px;
  font-size: 13px;
  color: var(--t2);
  margin-bottom: 16px;
  line-height: 1.55;
}
.perm-hint i { color: var(--accent); font-size: 16px; flex-shrink: 0; margin-top: 1px; }

/* 권한 테이블 */
.perm-table-wrap { overflow-x: auto; }
.perm-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.perm-table th {
  padding: 8px 10px;
  background: var(--surface2);
  color: var(--t3);
  font-weight: 700;
  font-size: 12px;
  border-bottom: 1px solid var(--border);
}
.perm-table td {
  padding: 9px 10px;
  border-bottom: 0.5px solid var(--border);
  vertical-align: middle;
}
.perm-table tbody tr:hover { background: var(--surface2); }
.perm-col { text-align: center; width: 80px; }
.manage-col { background: color-mix(in srgb, var(--color-danger) 5%, transparent); }
.perm-group { vertical-align: middle; }
.perm-group-inner {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  flex-wrap: nowrap;
}
.perm-parent { color: var(--t3); font-size: 12px; white-space: nowrap; }
.perm-parent::after { content: ' -'; }
.perm-name { font-weight: 600; color: var(--t1); white-space: nowrap; }
.perm-empty { text-align: center; color: var(--t3); padding: 20px; }

/* 컬럼 헤더 전체선택 버튼 */
.th-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.col-all-btn {
  font-size: 10px;
  font-weight: 700;
  padding: 1px 7px;
  border-radius: 4px;
  border: 1px solid var(--accent);
  color: var(--accent-t);
  background: var(--accent-bg);
  cursor: pointer;
  transition: all .12s;
  white-space: nowrap;
}
.col-all-btn:hover { background: var(--accent); color: #fff; }

/* 행 전체부여 버튼 */
.row-all-btn {
  margin-left: 4px;
  font-size: 10px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 4px;
  border: 1px solid var(--border);
  color: var(--t3);
  background: var(--surface2);
  cursor: pointer;
  transition: all .12s;
  white-space: nowrap;
  flex-shrink: 0;
}
.row-all-btn:hover { border-color: var(--accent); color: var(--accent-t); background: var(--accent-bg); }

@keyframes spin { to { transform: rotate(360deg); } }
.spinning { animation: spin 0.7s linear infinite; display: inline-block; }
</style>
