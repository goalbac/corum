<template>
  <div class="adm-page">
    <AdminPageHeader title="대시보드 관리" desc="대시보드 위젯 추가 및 순서 설정">
      <button class="adm-btn ghost" :disabled="!sortChanged || sortSaving" @click="saveSortOrder">
        <i :class="['ti', sortSaving ? 'ti-loader-2 spinning' : 'ti-check']"></i> 순서 저장
      </button>
      <button class="adm-btn primary" @click="openCreate"><i class="ti ti-plus"></i> 위젯 추가</button>
    </AdminPageHeader>

    <div class="adm-card" v-loading="loading">
      <div class="at-wrap">
        <div class="at-head">
          <div class="at-col" style="width:52px;text-align:center">순서</div>
          <div class="at-col" style="width:130px">유형</div>
          <div class="at-col" style="flex:1">제목</div>
          <div class="at-col" style="width:140px">대상 게시판</div>
          <div class="at-col" style="width:70px;text-align:center">상태</div>
          <div class="at-col" style="width:90px;text-align:center">관리</div>
        </div>
        <div ref="widgetSortable">
        <div v-for="row in widgets" :key="row.id" :data-id="row.id" class="at-row sortable-widget-row">
          <div class="at-col muted order-col" style="width:52px;text-align:center">
            <i class="ti ti-grip-vertical drag-handle" title="드래그해서 순서 변경"></i>
            <span>{{ row.sortOrder }}</span>
          </div>
          <div class="at-col" style="width:130px">
            <span class="adm-badge badge-primary">{{ typeLabel(row.widgetType) }}</span>
          </div>
          <div class="at-col bold" style="flex:1">{{ row.title }}</div>
          <div class="at-col muted" style="width:140px">{{ row.targetBoardName || '-' }}</div>
          <div class="at-col" style="width:70px;text-align:center">
            <span :class="['adm-badge', row.isActive ? 'badge-success' : 'badge-muted']">{{ row.isActive ? '활성' : '비활성' }}</span>
          </div>
          <div class="at-col at-actions" style="width:90px">
            <button class="act-btn" @click="openEdit(row)"><i class="ti ti-edit"></i> 수정</button>
            <button class="act-btn danger" @click="deleteWidget(row.id)"><i class="ti ti-trash"></i></button>
          </div>
        </div>
        </div>
        <div v-if="!widgets.length && !loading" class="at-empty"><i class="ti ti-layout-dashboard"></i><span>등록된 위젯이 없습니다.</span></div>
      </div>
    </div>

    <el-dialog v-model="showForm" :title="editing ? '위젯 수정' : '위젯 추가'" width="520px" destroy-on-close>
      <div class="dlg-form">
        <div class="dlg-row">
          <div class="dlg-field">
            <label>위젯 유형</label>
            <el-select v-model="form.widgetType" style="width:100%" @change="onTypeChange">
              <el-option value="RECENT_POSTS"  label="최신 글" />
              <el-option value="IMAGE_SLIDER"  label="이미지 슬라이더" />
              <el-option value="LINK_LIST"     label="링크 목록" />
              <el-option value="MEMBER_STATS"  label="회원 현황" />
              <el-option value="VISIT_STATS"   label="접속 통계" />
            </el-select>
          </div>
          <div class="dlg-field">
            <label>제목</label>
            <el-input v-model="form.title" />
          </div>
        </div>
        <div class="dlg-row">
          <div class="dlg-field">
            <label>순서</label>
            <el-input-number v-model="form.sortOrder" :min="0" style="width:100%" />
          </div>
          <div class="dlg-field" style="flex-direction:row;align-items:flex-end;padding-bottom:4px">
            <label class="chk-item"><el-checkbox v-model="form.isActive" />활성화</label>
          </div>
        </div>

        <template v-if="form.widgetType === 'RECENT_POSTS'">
          <hr class="dlg-divider"/>
          <div class="dlg-row">
            <div class="dlg-field">
              <label>대상 게시판</label>
              <el-select v-model="form.targetBoardId" clearable placeholder="전체" style="width:100%">
                <el-option v-for="b in boards" :key="b.id" :value="b.id" :label="b.name" />
              </el-select>
            </div>
            <div class="dlg-field">
              <label>표시 개수</label>
              <el-input-number v-model="form.postCount" :min="1" :max="20" style="width:100%" />
            </div>
          </div>
        </template>

        <template v-if="form.widgetType === 'IMAGE_SLIDER'">
          <hr class="dlg-divider"/>
          <div class="dlg-section-title">슬라이드</div>
          <div v-for="(s, i) in config.slides" :key="i" class="sub-item">
            <div class="dlg-row">
              <div class="dlg-field"><label>제목</label><el-input v-model="s.title" /></div>
              <div class="dlg-field"><label>이미지 URL</label><el-input v-model="s.imageUrl" /></div>
            </div>
            <div class="dlg-row">
              <div class="dlg-field"><label>링크 URL</label><el-input v-model="s.url" /></div>
              <div class="dlg-field" style="flex-direction:row;align-items:flex-end;gap:10px;justify-content:space-between;padding-bottom:2px">
                <label class="chk-item"><el-checkbox v-model="s.newWindow" />새 창</label>
                <button class="act-btn danger" @click="config.slides.splice(i,1)"><i class="ti ti-trash"></i></button>
              </div>
            </div>
          </div>
          <button class="adm-btn ghost" style="width:100%" @click="config.slides.push({title:'',imageUrl:'',url:'',newWindow:false})">
            <i class="ti ti-plus"></i> 슬라이드 추가
          </button>
        </template>

        <template v-if="form.widgetType === 'LINK_LIST'">
          <hr class="dlg-divider"/>
          <div class="dlg-section-title">링크</div>
          <div v-for="(l, i) in config.links" :key="i" class="sub-item">
            <div class="dlg-row">
              <div class="dlg-field"><label>레이블</label><el-input v-model="l.label" /></div>
              <div class="dlg-field"><label>URL</label><el-input v-model="l.url" /></div>
            </div>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <label class="chk-item"><el-checkbox v-model="l.newWindow" />새 창</label>
              <button class="act-btn danger" @click="config.links.splice(i,1)"><i class="ti ti-trash"></i></button>
            </div>
          </div>
          <button class="adm-btn ghost" style="width:100%" @click="config.links.push({label:'',url:'',newWindow:false})">
            <i class="ti ti-plus"></i> 링크 추가
          </button>
        </template>
      </div>
      <template #footer>
        <button class="adm-btn ghost" @click="showForm = false">취소</button>
        <button class="adm-btn primary" :disabled="saving" @click="saveWidget">
          <i v-if="saving" class="ti ti-loader-2 spinning"></i>{{ saving ? '저장 중...' : '저장' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import Sortable from 'sortablejs'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'

const widgets = ref([])
const boards  = ref([])
const loading = ref(false)
const saving  = ref(false)
const sortSaving = ref(false)
const sortChanged = ref(false)
const showForm = ref(false)
const editing  = ref(null)
const widgetSortable = ref(null)
let sortableInstance = null

const defaultForm = () => ({ widgetType: 'RECENT_POSTS', title: '', targetBoardId: null, postCount: 5, sortOrder: 0, isActive: true })
const form   = ref(defaultForm())
const config = ref({ slides: [], links: [] })

async function fetchWidgets() {
  loading.value = true
  try {
    const r = await api.get('/admin/dashboard/widgets')
    widgets.value = r.data.data || []
    await nextTick()
    initSortable()
  }
  finally { loading.value = false }
}
async function fetchBoards() { const r = await api.get('/boards'); boards.value = r.data.data || [] }

function initSortable() {
  if (sortableInstance) {
    sortableInstance.destroy()
    sortableInstance = null
  }
  if (!widgetSortable.value) return

  sortableInstance = Sortable.create(widgetSortable.value, {
    animation: 150,
    handle: '.drag-handle',
    ghostClass: 'sortable-ghost',
    dragClass: 'sortable-drag',
    onEnd: () => { sortChanged.value = true }
  })
}

async function saveSortOrder() {
  if (!widgetSortable.value) return
  sortSaving.value = true
  try {
    const widgetIds = Array.from(widgetSortable.value.children)
      .map(el => parseInt(el.dataset.id))
      .filter(Boolean)
    await api.put('/admin/dashboard/widgets/sort', widgetIds)
    ElMessage.success('순서가 저장되었습니다.')
    sortChanged.value = false
    await fetchWidgets()
  } catch (e) {
    ElMessage.error('순서 저장에 실패했습니다.')
  } finally {
    sortSaving.value = false
  }
}

function onTypeChange() { config.value = { slides: [], links: [] } }
function openCreate() { editing.value = null; form.value = defaultForm(); config.value = { slides: [], links: [] }; showForm.value = true }
function openEdit(w) {
  editing.value = w
  form.value = { ...w }
  const ec = w.extraConfig ? (typeof w.extraConfig === 'string' ? JSON.parse(w.extraConfig) : w.extraConfig) : {}
  config.value = { slides: ec.slides || [], links: ec.links || [] }
  showForm.value = true
}

async function saveWidget() {
  saving.value = true
  try {
    const extra = {}
    if (form.value.widgetType === 'IMAGE_SLIDER') extra.slides = config.value.slides
    if (form.value.widgetType === 'LINK_LIST') extra.links = config.value.links
    const payload = { ...form.value, extraConfig: JSON.stringify(extra) }
    editing.value ? await api.put(`/admin/dashboard/widgets/${editing.value.id}`, payload) : await api.post('/admin/dashboard/widgets', payload)
    ElMessage.success('저장되었습니다.')
    showForm.value = false
    fetchWidgets()
  } finally { saving.value = false }
}

async function deleteWidget(id) {
  await ElMessageBox.confirm('위젯을 삭제하시겠습니까?', '삭제', { type: 'warning', confirmButtonText: '삭제', cancelButtonText: '취소' })
  await api.delete(`/admin/dashboard/widgets/${id}`)
  ElMessage.success('삭제되었습니다.')
  fetchWidgets()
}

function typeLabel(t) { return { RECENT_POSTS: '최신 글', IMAGE_SLIDER: '슬라이더', LINK_LIST: '링크', MEMBER_STATS: '회원 현황', VISIT_STATS: '접속 통계' }[t] || t }
onMounted(() => { fetchWidgets(); fetchBoards() })
onBeforeUnmount(() => {
  if (sortableInstance) sortableInstance.destroy()
})
</script>

<style scoped>
@import '@/assets/admin-table.css';
.sub-item { background: var(--surface2); border: 0.5px solid var(--border2); border-radius: var(--radius-xs); padding: 12px; display: flex; flex-direction: column; gap: 10px; margin-bottom: 8px; }
.order-col { display: inline-flex; align-items: center; justify-content: center; gap: 4px; }
.drag-handle {
  font-size: 15px;
  color: var(--t3);
  cursor: grab;
  transition: color 0.15s;
}
.drag-handle:active { cursor: grabbing; }
.sortable-widget-row:hover .drag-handle { color: var(--t2); }
</style>

<style>
.sortable-ghost { opacity: 0.4; background: var(--accent-bg) !important; }
.sortable-drag { background: var(--surface) !important; box-shadow: 0 4px 16px rgba(0,0,0,0.12) !important; border-radius: 8px !important; }
</style>
