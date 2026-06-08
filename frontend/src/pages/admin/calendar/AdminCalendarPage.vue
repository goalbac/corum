<template>
  <div class="adm-page">
    <AdminPageHeader title="캘린더 관리" desc="캘린더 및 그룹별 접근 권한 설정">
      <button class="adm-btn primary" @click="openCreate"><i class="ti ti-plus"></i> 캘린더 추가</button>
    </AdminPageHeader>

    <div class="adm-card" v-loading="loading">
      <div class="at-wrap">
        <div class="at-head">
          <div class="at-col" style="width:36px"></div>
          <div class="at-col" style="width:160px">캘린더명</div>
          <div class="at-col" style="flex:1">설명</div>
          <div class="at-col" style="width:220px">권한 그룹</div>
          <div class="at-col" style="width:70px;text-align:center">상태</div>
          <div class="at-col" style="width:90px;text-align:center">관리</div>
        </div>
        <div v-for="row in calendars" :key="row.id" class="at-row">
          <div class="at-col" style="width:36px">
            <span class="color-dot" :style="{ background: row.color || '#4f6ef7' }"></span>
          </div>
          <div class="at-col bold" style="width:160px">{{ row.name }}</div>
          <div class="at-col muted" style="flex:1">{{ row.description || '-' }}</div>
          <div class="at-col" style="width:220px;gap:4px;flex-wrap:wrap">
            <span v-for="p in (row.permissions || []).slice(0,3)" :key="p.groupId" class="adm-badge badge-info">{{ p.groupName }}</span>
            <span v-if="(row.permissions || []).length > 3" class="adm-badge badge-muted">+{{ (row.permissions || []).length - 3 }}</span>
            <span v-if="!(row.permissions || []).length" class="muted-text">없음</span>
          </div>
          <div class="at-col" style="width:70px;text-align:center">
            <span :class="['adm-badge', row.isActive ? 'badge-success' : 'badge-muted']">{{ row.isActive ? '활성' : '비활성' }}</span>
          </div>
          <div class="at-col at-actions" style="width:90px">
            <button class="act-btn" @click="openEdit(row)"><i class="ti ti-edit"></i> 수정</button>
            <button class="act-btn danger" @click="deleteCalendar(row.id)"><i class="ti ti-trash"></i></button>
          </div>
        </div>
        <div v-if="!calendars.length && !loading" class="at-empty"><i class="ti ti-calendar"></i><span>등록된 캘린더가 없습니다.</span></div>
      </div>
    </div>

    <el-dialog v-model="showForm" :title="editing ? '캘린더 수정' : '캘린더 추가'" width="520px" destroy-on-close>
      <div class="dlg-form">
        <div class="dlg-row">
          <div class="dlg-field">
            <label>캘린더명</label>
            <el-input v-model="form.name" />
          </div>
          <div class="dlg-field">
            <label>색상</label>
            <div style="display:flex;align-items:center;gap:8px">
              <input type="color" v-model="form.color" style="width:40px;height:32px;border:none;cursor:pointer;border-radius:4px" />
              <el-input v-model="form.color" style="flex:1" />
            </div>
          </div>
        </div>
        <div class="dlg-field">
          <label>설명</label>
          <el-input v-model="form.description" type="textarea" :rows="2" resize="none" />
        </div>
        <div class="dlg-checks">
          <label class="chk-item"><el-checkbox v-model="form.isActive" />활성화</label>
        </div>
        <hr class="dlg-divider" />
        <div class="dlg-section-title">그룹 권한</div>
        <div v-for="(p, i) in form.permissions" :key="i" class="perm-row">
          <el-select v-model="p.groupId" placeholder="그룹 선택" style="flex:1">
            <el-option v-for="g in flatGroups" :key="g.id" :value="g.id" :label="g.name" />
          </el-select>
          <label class="chk-item"><el-checkbox v-model="p.canRead" />읽기</label>
          <label class="chk-item"><el-checkbox v-model="p.canWrite" />쓰기</label>
          <button class="act-btn danger" @click="form.permissions.splice(i,1)"><i class="ti ti-trash"></i></button>
        </div>
        <button class="adm-btn ghost" style="width:100%" @click="addPerm"><i class="ti ti-plus"></i> 그룹 추가</button>
      </div>
      <template #footer>
        <button class="adm-btn ghost" @click="showForm = false">취소</button>
        <button class="adm-btn primary" :disabled="saving" @click="saveCalendar">
          <i v-if="saving" class="ti ti-loader-2 spinning"></i>{{ saving ? '저장 중...' : '저장' }}
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

const calendars = ref([])
const groups = ref([])
const loading = ref(false)
const saving = ref(false)
const showForm = ref(false)
const editing = ref(null)
const defaultForm = () => ({ name: '', color: '#4f6ef7', description: '', isActive: true, permissions: [] })
const form = ref(defaultForm())

const flatGroups = computed(() => {
  const result = []
  const walk = (nodes) => nodes.forEach(n => { result.push(n); if (n.children?.length) walk(n.children) })
  walk(groups.value)
  return result
})

async function fetchCalendars() {
  loading.value = true
  try { const r = await api.get('/calendars/admin'); calendars.value = r.data.data || [] }
  finally { loading.value = false }
}
async function fetchGroups() { const r = await api.get('/groups'); groups.value = r.data.data || [] }

function addPerm() { form.value.permissions.push({ groupId: null, canRead: true, canWrite: false }) }
function openCreate() { editing.value = null; form.value = defaultForm(); showForm.value = true }
function openEdit(c) { editing.value = c; form.value = { ...c, permissions: JSON.parse(JSON.stringify(c.permissions || [])) }; showForm.value = true }

async function saveCalendar() {
  if (!form.value.name) return ElMessage.warning('캘린더명을 입력해주세요.')
  saving.value = true
  try {
    editing.value ? await api.put(`/calendars/${editing.value.id}`, form.value) : await api.post('/calendars', form.value)
    ElMessage.success('저장되었습니다.')
    showForm.value = false
    fetchCalendars()
  } finally { saving.value = false }
}

async function deleteCalendar(id) {
  await ElMessageBox.confirm('캘린더를 삭제하시겠습니까?', '삭제', { type: 'warning', confirmButtonText: '삭제', cancelButtonText: '취소' })
  await api.delete(`/calendars/${id}`)
  ElMessage.success('삭제되었습니다.')
  fetchCalendars()
}

onMounted(() => { fetchCalendars(); fetchGroups() })
</script>

<style scoped>
@import '@/assets/admin-table.css';
.color-dot { width: 14px; height: 14px; border-radius: 50%; flex-shrink: 0; display: inline-block; }
.muted-text { font-size: 12px; color: var(--t4); }
.perm-row { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
</style>
