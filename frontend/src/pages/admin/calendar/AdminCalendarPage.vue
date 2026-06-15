<template>
  <div class="adm-page">
    <AdminPageHeader title="캘린더 관리" desc="캘린더 및 그룹별 접근 권한 설정">
      <button class="adm-btn ghost" @click="openIcsImport(null)"><i class="ti ti-calendar-import"></i> ICS 가져오기</button>
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
          <div class="at-col" style="width:120px;text-align:center">관리</div>
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
          <div class="at-col at-actions" style="width:120px">
            <button class="act-btn" title="ICS 가져오기" @click="openIcsImport(row)"><i class="ti ti-calendar-import"></i></button>
            <button class="act-btn" @click="openEdit(row)"><i class="ti ti-edit"></i> 수정</button>
            <button class="act-btn danger" @click="deleteCalendar(row.id)"><i class="ti ti-trash"></i></button>
          </div>
        </div>
        <div v-if="!calendars.length && !loading" class="at-empty"><i class="ti ti-calendar"></i><span>등록된 캘린더가 없습니다.</span></div>
      </div>
    </div>

    <!-- 캘린더 추가/수정 다이얼로그 -->
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

    <!-- ICS 가져오기 다이얼로그 -->
    <el-dialog v-model="showIcsDialog" title="ICS 가져오기" width="560px" destroy-on-close>
      <div class="dlg-form">
        <!-- 캘린더 선택 (헤더 버튼으로 열었을 때) -->
        <div class="dlg-field" v-if="icsTargetCalendar === null">
          <label>가져올 캘린더</label>
          <el-select v-model="icsForm.calendarId" placeholder="캘린더 선택" style="width:100%">
            <el-option v-for="c in calendars" :key="c.id" :value="c.id" :label="c.name" />
          </el-select>
        </div>
        <div v-else class="ics-target-info">
          <span class="color-dot" :style="{ background: icsTargetCalendar.color || '#4f6ef7' }"></span>
          <strong>{{ icsTargetCalendar.name }}</strong> 캘린더에 가져옵니다.
        </div>

        <!-- 소스 선택 탭 -->
        <div class="ics-source-tabs">
          <button :class="['ics-tab', icsForm.sourceType === 'url' ? 'active' : '']" @click="icsForm.sourceType = 'url'">
            <i class="ti ti-link"></i> URL
          </button>
          <button :class="['ics-tab', icsForm.sourceType === 'file' ? 'active' : '']" @click="icsForm.sourceType = 'file'">
            <i class="ti ti-upload"></i> 파일 업로드
          </button>
        </div>

        <!-- URL 입력 -->
        <div v-if="icsForm.sourceType === 'url'" class="dlg-field">
          <label>ICS URL</label>
          <el-input v-model="icsForm.url" placeholder="https://..." />
          <div class="field-hint">
            <i class="ti ti-info-circle"></i>
            Google 캘린더 공휴일 URL:
            <code style="font-size:11px;word-break:break-all">https://calendar.google.com/calendar/ical/ko.south_korea%23holiday%40group.v.calendar.google.com/public/basic.ics</code>
          </div>
          <button class="adm-btn ghost btn-copy-url" @click="copyHolidayUrl">
            <i class="ti ti-copy"></i> 대한민국 공휴일 URL 붙여넣기
          </button>
        </div>

        <!-- 파일 업로드 -->
        <div v-else class="dlg-field">
          <label>ICS 파일</label>
          <div class="ics-file-drop" @click="$refs.icsFileInput.click()" @dragover.prevent @drop.prevent="onFileDrop">
            <input ref="icsFileInput" type="file" accept=".ics,text/calendar" style="display:none" @change="onFileChange" />
            <div v-if="icsForm.file">
              <i class="ti ti-file-check" style="font-size:24px;color:var(--adm-primary)"></i>
              <div style="font-size:13px;margin-top:4px;font-weight:600">{{ icsForm.file.name }}</div>
              <div style="font-size:11px;color:var(--adm-muted)">{{ (icsForm.file.size / 1024).toFixed(1) }} KB</div>
            </div>
            <div v-else>
              <i class="ti ti-cloud-upload" style="font-size:28px;color:var(--adm-muted)"></i>
              <div style="font-size:13px;margin-top:6px;color:var(--adm-muted)">클릭하거나 파일을 드래그하세요</div>
              <div style="font-size:11px;color:var(--adm-muted)">.ics 파일</div>
            </div>
          </div>
        </div>

        <!-- 연도 범위 -->
        <div class="dlg-row">
          <div class="dlg-field">
            <label>시작 연도</label>
            <el-input-number v-model="icsForm.yearFrom" :min="2020" :max="2050" style="width:100%" />
          </div>
          <div class="dlg-field">
            <label>종료 연도</label>
            <el-input-number v-model="icsForm.yearTo" :min="2020" :max="2050" style="width:100%" />
          </div>
        </div>
        <div class="field-hint" style="margin-top:-8px">
          <i class="ti ti-info-circle"></i>
          반복 공휴일(삼일절, 광복절 등)은 이 연도 범위의 이벤트로 전개됩니다.
        </div>

        <!-- 결과 표시 -->
        <div v-if="icsResult" class="ics-result" :class="icsResult.error ? 'error' : 'success'">
          <template v-if="icsResult.error">
            <i class="ti ti-alert-circle"></i> {{ icsResult.error }}
          </template>
          <template v-else>
            <i class="ti ti-check"></i>
            <strong>{{ icsResult.imported }}개</strong> 일정을 가져왔습니다.
            <span v-if="icsResult.skipped" class="muted-text">({{ icsResult.skipped }}개 건너뜀)</span>
          </template>
        </div>
      </div>
      <template #footer>
        <button class="adm-btn ghost" @click="showIcsDialog = false">닫기</button>
        <button class="adm-btn primary" :disabled="icsSaving || !canImport" @click="runImport">
          <i v-if="icsSaving" class="ti ti-loader-2 spinning"></i>{{ icsSaving ? '가져오는 중...' : 'ICS 가져오기' }}
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

const HOLIDAY_URL = 'https://calendar.google.com/calendar/ical/ko.south_korea%23holiday%40group.v.calendar.google.com/public/basic.ics'

const calendars = ref([])
const groups = ref([])
const loading = ref(false)
const saving = ref(false)
const showForm = ref(false)
const editing = ref(null)
const defaultForm = () => ({ name: '', color: '#4f6ef7', description: '', isActive: true, permissions: [] })
const form = ref(defaultForm())

// ICS
const showIcsDialog = ref(false)
const icsSaving = ref(false)
const icsTargetCalendar = ref(null)  // null = 선택해야 함
const icsResult = ref(null)
const currentYear = new Date().getFullYear()
const defaultIcsForm = () => ({
  calendarId: null,
  sourceType: 'url',
  url: HOLIDAY_URL,
  file: null,
  yearFrom: currentYear,
  yearTo: currentYear + 1,
})
const icsForm = ref(defaultIcsForm())

const canImport = computed(() => {
  const calId = icsTargetCalendar.value?.id ?? icsForm.value.calendarId
  if (!calId) return false
  if (icsForm.value.sourceType === 'url') return !!icsForm.value.url.trim()
  return !!icsForm.value.file
})

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

// ===== ICS 가져오기 =====
function openIcsImport(calendar) {
  icsTargetCalendar.value = calendar
  icsForm.value = defaultIcsForm()
  if (calendar) icsForm.value.calendarId = calendar.id
  icsResult.value = null
  showIcsDialog.value = true
}

function copyHolidayUrl() {
  icsForm.value.url = HOLIDAY_URL
  ElMessage.success('URL이 입력되었습니다.')
}

function onFileChange(e) {
  icsForm.value.file = e.target.files[0] || null
}

function onFileDrop(e) {
  const f = e.dataTransfer.files[0]
  if (f && (f.name.endsWith('.ics') || f.type === 'text/calendar')) {
    icsForm.value.file = f
  } else {
    ElMessage.warning('.ics 파일만 업로드할 수 있습니다.')
  }
}

async function runImport() {
  const calId = icsTargetCalendar.value?.id ?? icsForm.value.calendarId
  if (!calId) return ElMessage.warning('캘린더를 선택해주세요.')
  icsSaving.value = true
  icsResult.value = null
  try {
    const fd = new FormData()
    fd.append('yearFrom', icsForm.value.yearFrom)
    fd.append('yearTo', icsForm.value.yearTo)
    if (icsForm.value.sourceType === 'url') {
      fd.append('url', icsForm.value.url)
    } else {
      fd.append('file', icsForm.value.file)
    }
    const r = await api.post(`/admin/calendars/${calId}/import-ics`, fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    icsResult.value = r.data.data
  } catch (e) {
    icsResult.value = { error: e?.response?.data?.message || 'ICS 가져오기에 실패했습니다.' }
  } finally {
    icsSaving.value = false
  }
}

onMounted(() => { fetchCalendars(); fetchGroups() })
</script>

<style scoped>
@import '@/assets/admin-table.css';
.color-dot { width: 14px; height: 14px; border-radius: 50%; flex-shrink: 0; display: inline-block; }
.muted-text { font-size: 12px; color: var(--adm-muted); }
.perm-row { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }

/* ICS 다이얼로그 */
.ics-target-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--adm-bg-subtle, rgba(0,0,0,0.03));
  border-radius: 6px;
  font-size: 13px;
  margin-bottom: 4px;
}

.ics-source-tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 12px;
}
.ics-tab {
  flex: 1;
  padding: 7px;
  border: 1px solid var(--adm-border);
  border-radius: 6px;
  background: transparent;
  color: var(--adm-muted);
  font-size: 13px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  transition: all 0.15s;
}
.ics-tab.active {
  border-color: var(--adm-primary);
  color: var(--adm-primary);
  background: color-mix(in srgb, var(--adm-primary) 8%, transparent);
  font-weight: 600;
}

.ics-file-drop {
  border: 2px dashed var(--adm-border);
  border-radius: 8px;
  padding: 32px 20px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.15s;
}
.ics-file-drop:hover { border-color: var(--adm-primary); }

.btn-copy-url {
  margin-top: 6px;
  font-size: 12px;
}

.ics-result {
  padding: 10px 14px;
  border-radius: 6px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.ics-result.success {
  background: color-mix(in srgb, #16a34a 10%, transparent);
  color: #15803d;
  border: 1px solid color-mix(in srgb, #16a34a 30%, transparent);
}
.ics-result.error {
  background: color-mix(in srgb, #dc2626 10%, transparent);
  color: #dc2626;
  border: 1px solid color-mix(in srgb, #dc2626 30%, transparent);
}
</style>
