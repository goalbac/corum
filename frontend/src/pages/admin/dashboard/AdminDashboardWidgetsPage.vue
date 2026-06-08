<template>
  <div class="admin-dashboard-widgets">
    <AdminPageHeader title="대시보드 관리" desc="메인 화면 위젯의 노출 순서와 내용을 설정합니다.">
      <el-button type="primary" size="small" @click="openCreate">위젯 추가</el-button>
    </AdminPageHeader>

    <el-table :data="widgets" v-loading="loading" border>
      <el-table-column prop="sortOrder" label="순서" width="80" align="center" />
      <el-table-column label="유형" width="140">
        <template #default="{ row }">{{ typeLabel(row.widgetType) }}</template>
      </el-table-column>
      <el-table-column prop="title" label="제목" min-width="180" />
      <el-table-column label="대상 게시판" min-width="150">
        <template #default="{ row }">{{ row.targetBoardName || '-' }}</template>
      </el-table-column>
      <el-table-column label="표시" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isActive ? 'success' : 'info'" size="small">{{ row.isActive ? '표시' : '숨김' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="관리" width="150" align="center">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">수정</el-button>
          <el-button size="small" type="danger" @click="deleteWidget(row.id)">삭제</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showForm" :title="editing ? '위젯 수정' : '위젯 추가'" width="720px" destroy-on-close>
      <el-form :model="form" label-position="top">
        <div class="form-grid">
          <el-form-item label="위젯 유형">
            <el-select v-model="form.widgetType" style="width:100%">
              <el-option label="최신글" value="RECENT_POSTS" />
              <el-option label="이미지 슬라이더" value="IMAGE_SLIDER" />
              <el-option label="링크 모음" value="LINK_LIST" />
              <el-option label="회원 현황" value="MEMBER_STATS" />
              <el-option label="방문 통계" value="VISIT_STATS" />
            </el-select>
          </el-form-item>
          <el-form-item label="제목">
            <el-input v-model="form.title" />
          </el-form-item>
          <el-form-item v-if="form.widgetType === 'RECENT_POSTS'" label="대상 게시판">
            <el-select v-model="form.targetBoardId" clearable style="width:100%" placeholder="전체 게시판">
              <el-option v-for="board in boards" :key="board.id" :label="board.name" :value="board.id" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="form.widgetType === 'RECENT_POSTS'" label="글 개수">
            <el-input-number v-model="form.postCount" :min="1" :max="20" style="width:100%" />
          </el-form-item>
          <el-form-item label="정렬 순서">
            <el-input-number v-model="form.sortOrder" :min="0" style="width:100%" />
          </el-form-item>
          <el-form-item label="노출 여부">
            <el-switch v-model="form.isActive" active-text="표시" inactive-text="숨김" />
          </el-form-item>
        </div>

        <section v-if="form.widgetType === 'IMAGE_SLIDER'" class="config-section">
          <div class="section-title">슬라이드 설정</div>
          <div v-for="(slide, index) in config.slides" :key="index" class="config-row">
            <el-input v-model="slide.title" placeholder="제목" />
            <el-input v-model="slide.imageUrl" placeholder="이미지 URL" />
            <el-input v-model="slide.url" placeholder="이동 URL" />
            <el-checkbox v-model="slide.newWindow">새 창</el-checkbox>
            <button type="button" class="icon-btn danger" @click="config.slides.splice(index, 1)">
              <i class="ti ti-trash"></i>
            </button>
          </div>
          <el-button size="small" @click="config.slides.push({ title: '', imageUrl: '', url: '', newWindow: false })">슬라이드 추가</el-button>
        </section>

        <section v-if="form.widgetType === 'LINK_LIST'" class="config-section">
          <div class="section-title">링크 설정</div>
          <div v-for="(link, index) in config.links" :key="index" class="config-row links">
            <el-input v-model="link.label" placeholder="링크명" />
            <el-input v-model="link.url" placeholder="URL" />
            <el-checkbox v-model="link.newWindow">새 창</el-checkbox>
            <button type="button" class="icon-btn danger" @click="config.links.splice(index, 1)">
              <i class="ti ti-trash"></i>
            </button>
          </div>
          <el-button size="small" @click="config.links.push({ label: '', url: '', newWindow: false })">링크 추가</el-button>
        </section>
      </el-form>

      <template #footer>
        <el-button @click="showForm = false">취소</el-button>
        <el-button type="primary" :loading="saving" @click="saveWidget">저장</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'

const widgets = ref([])
const boards = ref([])
const loading = ref(false)
const saving = ref(false)
const showForm = ref(false)
const editing = ref(null)

const form = ref(defaultForm())
const config = ref(defaultConfig())

function defaultForm() {
  return {
    widgetType: 'RECENT_POSTS',
    title: '',
    targetBoardId: null,
    postCount: 5,
    sortOrder: 0,
    isActive: true,
    extraConfig: ''
  }
}

function defaultConfig() {
  return {
    slides: [{ title: '', imageUrl: '', url: '', newWindow: false }],
    links: [{ label: '', url: '', newWindow: false }]
  }
}

function typeLabel(type) {
  return {
    RECENT_POSTS: '최신글',
    IMAGE_SLIDER: '이미지 슬라이더',
    LINK_LIST: '링크 모음',
    MEMBER_STATS: '회원 현황',
    VISIT_STATS: '방문 통계'
  }[type] || type
}

async function fetchWidgets() {
  loading.value = true
  try {
    const res = await api.get('/admin/dashboard/widgets')
    widgets.value = res.data.data || []
  } finally {
    loading.value = false
  }
}

async function fetchBoards() {
  const res = await api.get('/boards')
  boards.value = res.data.data || []
}

function openCreate() {
  editing.value = null
  form.value = defaultForm()
  form.value.sortOrder = widgets.value.length + 1
  config.value = defaultConfig()
  showForm.value = true
}

function openEdit(row) {
  editing.value = row
  form.value = {
    widgetType: row.widgetType,
    title: row.title || '',
    targetBoardId: row.targetBoardId,
    postCount: row.postCount || 5,
    sortOrder: row.sortOrder || 0,
    isActive: row.isActive,
    extraConfig: row.extraConfig || ''
  }
  config.value = { ...defaultConfig(), ...parseConfig(row.extraConfig) }
  showForm.value = true
}

async function saveWidget() {
  const payload = { ...form.value, extraConfig: buildExtraConfig() }
  saving.value = true
  try {
    if (editing.value) await api.put(`/admin/dashboard/widgets/${editing.value.id}`, payload)
    else await api.post('/admin/dashboard/widgets', payload)
    ElMessage.success('저장되었습니다.')
    showForm.value = false
    await fetchWidgets()
  } finally {
    saving.value = false
  }
}

async function deleteWidget(id) {
  await ElMessageBox.confirm('위젯을 삭제하시겠습니까?', '삭제 확인', {
    type: 'warning',
    confirmButtonText: '삭제',
    cancelButtonText: '취소'
  })
  await api.delete(`/admin/dashboard/widgets/${id}`)
  ElMessage.success('삭제되었습니다.')
  await fetchWidgets()
}

function parseConfig(value) {
  try {
    return value ? JSON.parse(value) : {}
  } catch {
    return {}
  }
}

function buildExtraConfig() {
  if (form.value.widgetType === 'IMAGE_SLIDER') {
    return JSON.stringify({
      slides: config.value.slides.filter(slide => slide.imageUrl)
    })
  }
  if (form.value.widgetType === 'LINK_LIST') {
    return JSON.stringify({
      links: config.value.links.filter(link => link.label && link.url)
    })
  }
  return ''
}

watch(() => form.value.widgetType, type => {
  if (type !== 'RECENT_POSTS') form.value.targetBoardId = null
})

onMounted(async () => {
  await Promise.all([fetchWidgets(), fetchBoards()])
})
</script>

<style scoped>
.admin-dashboard-widgets { display: flex; flex-direction: column; gap: 18px; }

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.config-section {
  margin-top: 10px;
  padding: 14px;
  border: 0.5px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--surface2);
}

.section-title {
  font-size: 15px;
  font-weight: 800;
  color: var(--t1);
  margin-bottom: 10px;
}

.config-row {
  display: grid;
  grid-template-columns: 1fr 1.5fr 1fr auto auto;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}

.config-row.links {
  grid-template-columns: 1fr 1.7fr auto auto;
}

.icon-btn {
  width: 32px;
  height: 32px;
  border: 0.5px solid var(--border);
  border-radius: var(--radius-xs);
  background: var(--surface);
  color: var(--t2);
}

.icon-btn.danger {
  color: var(--new);
}

@media (max-width: 800px) {
  .form-grid,
  .config-row,
  .config-row.links {
    grid-template-columns: 1fr;
  }
}
</style>
