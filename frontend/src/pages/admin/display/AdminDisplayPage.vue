<template>
  <div>
    <AdminPageHeader title="팝업/배너 관리" desc="운영 공지용 팝업과 상단 배너 노출을 관리합니다.">
      <el-button v-if="activeTab === 'popups'" type="primary" size="small" @click="openPopupCreate">
        <i class="ti ti-plus" style="margin-right:4px"></i>팝업 추가
      </el-button>
      <el-button v-else type="primary" size="small" @click="openBannerCreate">
        <i class="ti ti-plus" style="margin-right:4px"></i>배너 추가
      </el-button>
    </AdminPageHeader>

    <el-tabs v-model="activeTab" class="display-tabs">
      <el-tab-pane label="팝업" name="popups">
        <el-table :data="popups" v-loading="popupLoading" border>
          <el-table-column prop="title" label="제목" min-width="180" />
          <el-table-column label="유형" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ row.contentType === 'IMAGE' ? '이미지' : 'HTML' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="위치" width="90" align="center">
            <template #default="{ row }">{{ positionLabel(row.position) }}</template>
          </el-table-column>
          <el-table-column label="대상" width="90" align="center">
            <template #default="{ row }">{{ row.targetType === 'MENU' ? '메뉴' : '전체' }}</template>
          </el-table-column>
          <el-table-column label="기간" min-width="180">
            <template #default="{ row }">{{ periodText(row.startAt, row.endAt) }}</template>
          </el-table-column>
          <el-table-column label="상태" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.isActive ? 'success' : 'info'" effect="plain">
                {{ row.isActive ? '활성' : '비활성' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="관리" width="140" align="center">
            <template #default="{ row }">
              <el-button size="small" @click="openPopupEdit(row)">수정</el-button>
              <el-button size="small" type="danger" plain @click="deletePopup(row.id)">삭제</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="배너" name="banners">
        <el-table :data="banners" v-loading="bannerLoading" border>
          <el-table-column prop="title" label="제목" min-width="180" />
          <el-table-column prop="content" label="내용" min-width="220" show-overflow-tooltip />
          <el-table-column label="기간" min-width="180">
            <template #default="{ row }">{{ periodText(row.startAt, row.endAt) }}</template>
          </el-table-column>
          <el-table-column label="상태" width="90" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="row.isActive ? 'success' : 'info'" effect="plain">
                {{ row.isActive ? '활성' : '비활성' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="관리" width="140" align="center">
            <template #default="{ row }">
              <el-button size="small" @click="openBannerEdit(row)">수정</el-button>
              <el-button size="small" type="danger" plain @click="deleteBanner(row.id)">삭제</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="showPopupForm" :title="editingPopup ? '팝업 수정' : '팝업 추가'" width="620px" destroy-on-close>
      <el-form :model="popupForm" label-position="top">
        <div class="form-grid">
          <el-form-item label="제목">
            <el-input v-model="popupForm.title" />
          </el-form-item>
          <el-form-item label="콘텐츠 유형">
            <el-select v-model="popupForm.contentType" style="width:100%">
              <el-option value="IMAGE" label="이미지" />
              <el-option value="HTML" label="HTML" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item v-if="popupForm.contentType === 'IMAGE'" label="이미지 URL">
          <el-input v-model="popupForm.imageUrl" placeholder="https://..." />
        </el-form-item>
        <el-form-item v-else label="HTML 내용">
          <el-input v-model="popupForm.content" type="textarea" :rows="5" resize="none" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="링크 URL">
            <el-input v-model="popupForm.linkUrl" placeholder="https://..." />
          </el-form-item>
          <el-form-item label="위치">
            <el-select v-model="popupForm.position" style="width:100%">
              <el-option value="LEFT" label="왼쪽" />
              <el-option value="CENTER" label="가운데" />
              <el-option value="RIGHT" label="오른쪽" />
            </el-select>
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="시작일">
            <el-date-picker v-model="popupForm.startAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
          </el-form-item>
          <el-form-item label="종료일">
            <el-date-picker v-model="popupForm.endAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="노출 대상">
            <el-select v-model="popupForm.targetType" style="width:100%">
              <el-option value="ALL" label="전체" />
              <el-option value="MENU" label="특정 메뉴" />
            </el-select>
          </el-form-item>
          <el-form-item label="우선순위">
            <el-input-number v-model="popupForm.priority" :min="0" style="width:100%" />
          </el-form-item>
        </div>
        <el-form-item v-if="popupForm.targetType === 'MENU'" label="대상 메뉴">
          <el-select v-model="popupForm.targetMenuIds" multiple style="width:100%" placeholder="메뉴 선택">
            <el-option v-for="menu in flatMenus" :key="menu.id" :label="menu.label" :value="menu.id" />
          </el-select>
        </el-form-item>
        <div class="check-row">
          <el-checkbox v-model="popupForm.linkNewWindow">새 창으로 열기</el-checkbox>
          <el-checkbox v-model="popupForm.isActive">활성화</el-checkbox>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="showPopupForm = false">취소</el-button>
        <el-button type="primary" :loading="saving" @click="savePopup">저장</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showBannerForm" :title="editingBanner ? '배너 수정' : '배너 추가'" width="560px" destroy-on-close>
      <el-form :model="bannerForm" label-position="top">
        <el-form-item label="제목">
          <el-input v-model="bannerForm.title" />
        </el-form-item>
        <el-form-item label="내용">
          <el-input v-model="bannerForm.content" type="textarea" :rows="3" resize="none" />
        </el-form-item>
        <el-form-item label="링크 URL">
          <el-input v-model="bannerForm.linkUrl" placeholder="https://..." />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="시작일">
            <el-date-picker v-model="bannerForm.startAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
          </el-form-item>
          <el-form-item label="종료일">
            <el-date-picker v-model="bannerForm.endAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
          </el-form-item>
        </div>
        <div class="check-row">
          <el-checkbox v-model="bannerForm.linkNewWindow">새 창으로 열기</el-checkbox>
          <el-checkbox v-model="bannerForm.isActive">활성화</el-checkbox>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="showBannerForm = false">취소</el-button>
        <el-button type="primary" :loading="saving" @click="saveBanner">저장</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'

const activeTab = ref('popups')
const popups = ref([])
const banners = ref([])
const menus = ref([])
const popupLoading = ref(false)
const bannerLoading = ref(false)
const saving = ref(false)
const showPopupForm = ref(false)
const showBannerForm = ref(false)
const editingPopup = ref(null)
const editingBanner = ref(null)

const defaultPopup = () => ({
  title: '',
  contentType: 'IMAGE',
  content: '',
  imageUrl: '',
  linkUrl: '',
  linkNewWindow: false,
  position: 'CENTER',
  priority: 0,
  startAt: null,
  endAt: null,
  isActive: true,
  targetType: 'ALL',
  targetMenuIds: []
})
const defaultBanner = () => ({
  title: '',
  content: '',
  linkUrl: '',
  linkNewWindow: false,
  startAt: null,
  endAt: null,
  isActive: true
})
const popupForm = ref(defaultPopup())
const bannerForm = ref(defaultBanner())

const flatMenus = computed(() => {
  const result = []
  const walk = (nodes, depth = 0) => nodes.forEach(menu => {
    result.push({ id: menu.id, label: `${'-- '.repeat(depth)}${menu.name}` })
    if (menu.children?.length) walk(menu.children, depth + 1)
  })
  walk(menus.value)
  return result
})

function positionLabel(position) {
  return { LEFT: '왼쪽', CENTER: '가운데', RIGHT: '오른쪽' }[position] || position
}

function periodText(startAt, endAt) {
  if (!startAt && !endAt) return '상시'
  return `${formatDate(startAt) || '-'} ~ ${formatDate(endAt) || '-'}`
}

function formatDate(value) {
  if (!value) return ''
  const d = new Date(value)
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
}

async function fetchPopups() {
  popupLoading.value = true
  try {
    const res = await api.get('/admin/display/popups')
    popups.value = res.data.data || []
  } finally {
    popupLoading.value = false
  }
}

async function fetchBanners() {
  bannerLoading.value = true
  try {
    const res = await api.get('/admin/display/banners')
    banners.value = res.data.data || []
  } finally {
    bannerLoading.value = false
  }
}

async function fetchMenus() {
  try {
    const res = await api.get('/menus/admin')
    menus.value = res.data.data || []
  } catch {}
}

function openPopupCreate() {
  editingPopup.value = null
  popupForm.value = defaultPopup()
  showPopupForm.value = true
}

function openPopupEdit(row) {
  editingPopup.value = row
  popupForm.value = { ...defaultPopup(), ...row, targetMenuIds: row.targetMenuIds || [] }
  showPopupForm.value = true
}

function openBannerCreate() {
  editingBanner.value = null
  bannerForm.value = defaultBanner()
  showBannerForm.value = true
}

function openBannerEdit(row) {
  editingBanner.value = row
  bannerForm.value = { ...defaultBanner(), ...row }
  showBannerForm.value = true
}

async function savePopup() {
  if (!popupForm.value.title) return ElMessage.warning('팝업 제목을 입력해주세요.')
  saving.value = true
  try {
    if (editingPopup.value) await api.put(`/admin/display/popups/${editingPopup.value.id}`, popupForm.value)
    else await api.post('/admin/display/popups', popupForm.value)
    ElMessage.success('저장되었습니다.')
    showPopupForm.value = false
    fetchPopups()
  } finally {
    saving.value = false
  }
}

async function saveBanner() {
  if (!bannerForm.value.title) return ElMessage.warning('배너 제목을 입력해주세요.')
  saving.value = true
  try {
    if (editingBanner.value) await api.put(`/admin/display/banners/${editingBanner.value.id}`, bannerForm.value)
    else await api.post('/admin/display/banners', bannerForm.value)
    ElMessage.success('저장되었습니다.')
    showBannerForm.value = false
    fetchBanners()
  } finally {
    saving.value = false
  }
}

async function deletePopup(id) {
  await ElMessageBox.confirm('팝업을 삭제하시겠습니까?', '삭제', { type: 'warning' })
  await api.delete(`/admin/display/popups/${id}`)
  ElMessage.success('삭제되었습니다.')
  fetchPopups()
}

async function deleteBanner(id) {
  await ElMessageBox.confirm('배너를 삭제하시겠습니까?', '삭제', { type: 'warning' })
  await api.delete(`/admin/display/banners/${id}`)
  ElMessage.success('삭제되었습니다.')
  fetchBanners()
}

onMounted(() => {
  fetchPopups()
  fetchBanners()
  fetchMenus()
})
</script>

<style scoped>
.display-tabs {
  background: var(--surface);
  border: 0.5px solid var(--border2);
  border-radius: var(--radius-sm);
  padding: 12px;
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.check-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
@media (max-width: 768px) {
  .form-grid { grid-template-columns: 1fr; }
}
</style>
