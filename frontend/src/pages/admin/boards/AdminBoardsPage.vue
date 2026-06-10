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

    <!-- 게시판 설정 다이얼로그 -->
    <el-dialog v-model="showForm" :title="editing ? '게시판 수정' : '게시판 추가'" width="640px" destroy-on-close>
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

        <!-- 그룹 권한 -->
        <div class="dlg-section-title">그룹 권한</div>
        <div class="perm-hint">
          <i class="ti ti-info-circle"></i>
          <span><strong>관리</strong> 권한이 있는 그룹은 해당 게시판의 모든 글을 수정·삭제할 수 있습니다.</span>
        </div>

        <div class="perm-add-row">
          <el-select
            v-model="addGroupId"
            placeholder="그룹 선택"
            style="flex:1"
            filterable
            no-data-text="추가 가능한 그룹이 없습니다"
          >
            <el-option v-for="g in availableGroups" :key="g.id" :value="g.id" :label="g.label" />
          </el-select>
          <button class="adm-btn primary sm" @click="addPermRow" :disabled="!addGroupId">
            <i class="ti ti-plus"></i> 추가
          </button>
        </div>

        <div v-loading="permLoading">
          <div v-if="boardPermRows.length" class="perm-grid">
            <div class="pg-cell pg-head pg-name">그룹</div>
            <div class="pg-cell pg-head pg-chk" title="게시물 목록·내용을 볼 수 있습니다">조회</div>
            <div class="pg-cell pg-head pg-chk" title="게시물을 작성할 수 있습니다">쓰기</div>
            <div class="pg-cell pg-head pg-chk" title="게시물에 댓글을 작성할 수 있습니다">댓글</div>
            <div class="pg-cell pg-head pg-chk" title="첨부파일을 다운로드할 수 있습니다">다운로드</div>
            <div class="pg-cell pg-head pg-chk pg-manage" title="게시판의 모든 글을 수정·삭제할 수 있습니다">관리</div>
            <div class="pg-cell pg-head pg-del"></div>
            <template v-for="row in boardPermRows" :key="row.groupId">
              <div class="pg-cell pg-name">{{ row.label }}</div>
              <div class="pg-cell pg-chk">
                <el-tooltip content="게시물 목록·내용을 볼 수 있습니다" placement="top">
                  <el-checkbox v-model="row.canRead" />
                </el-tooltip>
              </div>
              <div class="pg-cell pg-chk">
                <el-tooltip content="게시물을 작성할 수 있습니다" placement="top">
                  <el-checkbox v-model="row.canWrite" />
                </el-tooltip>
              </div>
              <div class="pg-cell pg-chk">
                <el-tooltip content="게시물에 댓글을 작성할 수 있습니다" placement="top">
                  <el-checkbox v-model="row.canComment" />
                </el-tooltip>
              </div>
              <div class="pg-cell pg-chk">
                <el-tooltip content="첨부파일을 다운로드할 수 있습니다" placement="top">
                  <el-checkbox v-model="row.canDownload" />
                </el-tooltip>
              </div>
              <div class="pg-cell pg-chk pg-manage">
                <el-tooltip content="게시판의 모든 글을 수정·삭제할 수 있습니다" placement="top">
                  <el-checkbox v-model="row.canManage" />
                </el-tooltip>
              </div>
              <div class="pg-cell pg-del">
                <button class="del-btn" @click="removePermRow(row.groupId)" title="삭제">
                  <i class="ti ti-x"></i>
                </button>
              </div>
            </template>
          </div>
          <div v-else class="perm-empty-msg">위에서 그룹을 선택해 권한을 추가하세요.</div>
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
import { ref, computed, onMounted } from 'vue'
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

const groups = ref([])

const subGroupOptions = computed(() => {
  const result = []
  const walk = (nodes = [], parentLabel = '') => {
    nodes.forEach(g => {
      const label = parentLabel ? `${parentLabel} - ${g.name}` : g.name
      if (parentLabel && !g.isSystem) {
        result.push({ id: g.id, label })
      }
      if (g.children?.length) walk(g.children, label)
    })
  }
  walk(groups.value)
  return result
})

const boardPermRows = ref([])
const permLoading   = ref(false)
const addGroupId    = ref(null)

const availableGroups = computed(() => {
  const usedIds = new Set(boardPermRows.value.map(r => r.groupId))
  return subGroupOptions.value.filter(g => !usedIds.has(g.id))
})

function addPermRow() {
  if (!addGroupId.value) return
  const option = subGroupOptions.value.find(g => g.id === addGroupId.value)
  if (!option) return
  boardPermRows.value.push({ groupId: option.id, label: option.label, canRead: true, canWrite: false, canComment: false, canDownload: true, canManage: false })
  addGroupId.value = null
}

function removePermRow(groupId) {
  boardPermRows.value = boardPermRows.value.filter(r => r.groupId !== groupId)
}

async function fetchBoards() {
  loading.value = true
  try { const res = await api.get('/boards'); boards.value = res.data.data || [] }
  finally { loading.value = false }
}

async function fetchGroups() {
  try { const res = await api.get('/groups'); groups.value = res.data.data || [] }
  catch {}
}

function openCreate() {
  editing.value = null
  form.value = defaultForm()
  boardPermRows.value = []
  addGroupId.value = null
  showForm.value = true
}

async function openEdit(board) {
  editing.value = board
  form.value = { ...board }
  addGroupId.value = null
  boardPermRows.value = []
  permLoading.value = true
  showForm.value = true
  try {
    const res = await api.get(`/admin/boards/${board.id}/permissions`)
    boardPermRows.value = (res.data.data || [])
      .filter(r => r.canRead || r.canComment || r.canDownload || r.canManage || r.canWrite)
      .map(r => ({
        groupId: r.groupId,
        label: r.label || (r.parentName ? `${r.parentName} - ${r.groupName}` : r.groupName),
        canRead: !!r.canRead,
        canWrite: !!r.canWrite,
        canComment: !!r.canComment,
        canDownload: !!r.canDownload,
        canManage: !!r.canManage,
      }))
  } finally {
    permLoading.value = false
  }
}

async function saveBoard() {
  if (!form.value.name) return ElMessage.warning('게시판명을 입력해주세요.')
  saving.value = true
  try {
    const payload = { ...form.value, permissions: boardPermRows.value }
    editing.value ? await api.put(`/boards/${editing.value.id}`, payload) : await api.post('/boards', payload)
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

onMounted(() => { fetchBoards(); fetchGroups() })
</script>

<style scoped>
@import '@/assets/admin-table.css';

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

.perm-hint {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  background: color-mix(in srgb, var(--accent) 8%, transparent);
  border: 0.5px solid color-mix(in srgb, var(--accent) 30%, transparent);
  border-radius: var(--radius-xs);
  padding: 8px 12px;
  font-size: 12px;
  color: var(--t2);
  margin-bottom: 12px;
  line-height: 1.5;
}
.perm-hint i { color: var(--accent); font-size: 15px; flex-shrink: 0; margin-top: 1px; }

.perm-add-row { display: flex; gap: 8px; margin-bottom: 10px; }
.adm-btn.sm { padding: 0 12px; height: 32px; font-size: 13px; }

.perm-grid {
  display: grid;
  grid-template-columns: 1fr 76px 76px 76px 76px 76px 36px;
  border: 1px solid var(--border);
  border-radius: var(--radius-xs);
  overflow: hidden;
}
.pg-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 7px 6px;
  border-bottom: 0.5px solid var(--border);
  font-size: 13px;
}
.pg-cell:nth-last-child(-n+7) { border-bottom: none; }
.pg-head {
  background: var(--surface2);
  font-size: 11px; font-weight: 700; color: var(--t3);
  border-bottom: 1px solid var(--border) !important;
}
.pg-name { justify-content: flex-start; padding-left: 10px; font-weight: 500; color: var(--t1); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pg-head.pg-name { font-weight: 700; color: var(--t3); }
.pg-cell:not(.pg-head).pg-manage { background: color-mix(in srgb, var(--color-danger) 5%, transparent); }
.pg-cell:not(.pg-head):hover { background: var(--surface2); }

.del-btn {
  width: 22px;
  height: 22px;
  border-radius: 4px;
  border: 1px solid var(--border);
  background: transparent;
  color: var(--t3);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  transition: all .12s;
}
.del-btn:hover { border-color: var(--color-danger); color: var(--color-danger); background: color-mix(in srgb, var(--color-danger) 8%, transparent); }

.perm-empty-msg { text-align: center; color: var(--t3); font-size: 13px; padding: 16px 0; }

@keyframes spin { to { transform: rotate(360deg); } }
.spinning { animation: spin 0.7s linear infinite; display: inline-block; }
</style>
