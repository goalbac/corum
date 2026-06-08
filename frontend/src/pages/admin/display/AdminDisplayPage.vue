<template>
  <div class="adm-page">
    <AdminPageHeader title="팝업/배너 관리" desc="팝업 및 상단 배너 노출 관리">
      <button v-if="tab === 'popups'" class="adm-btn primary" @click="openPopupCreate"><i class="ti ti-plus"></i> 팝업 추가</button>
      <button v-else class="adm-btn primary" @click="openBannerCreate"><i class="ti ti-plus"></i> 배너 추가</button>
    </AdminPageHeader>

    <div class="adm-card">
      <div class="adm-tabs">
        <div :class="['adm-tab', tab === 'popups' ? 'active' : '']" @click="tab = 'popups'">팝업</div>
        <div :class="['adm-tab', tab === 'banners' ? 'active' : '']" @click="tab = 'banners'">상단 배너</div>
      </div>

      <!-- 팝업 탭 -->
      <template v-if="tab === 'popups'">
        <div class="at-wrap" v-loading="loading">
          <div class="at-head">
            <div class="at-col" style="width:100px">유형</div>
            <div class="at-col" style="flex:1">제목</div>
            <div class="at-col" style="width:100px">위치</div>
            <div class="at-col" style="width:160px">노출 기간</div>
            <div class="at-col" style="width:70px;text-align:center">상태</div>
            <div class="at-col" style="width:90px;text-align:center">관리</div>
          </div>
          <div v-for="row in popups" :key="row.id" class="at-row">
            <div class="at-col" style="width:100px">
              <span :class="['adm-badge', row.contentType === 'IMAGE' ? 'badge-primary' : 'badge-purple']">{{ row.contentType === 'IMAGE' ? '이미지' : 'HTML' }}</span>
            </div>
            <div class="at-col bold" style="flex:1">{{ row.title }}</div>
            <div class="at-col muted" style="width:100px">{{ posLabel(row.position) }}</div>
            <div class="at-col muted" style="width:160px;font-size:12px">{{ fmtDate(row.startAt) }} ~ {{ fmtDate(row.endAt) }}</div>
            <div class="at-col" style="width:70px;text-align:center">
              <span :class="['adm-badge', row.isActive ? 'badge-success' : 'badge-muted']">{{ row.isActive ? '활성' : '비활성' }}</span>
            </div>
            <div class="at-col at-actions" style="width:90px">
              <button class="act-btn" @click="openPopupEdit(row)"><i class="ti ti-edit"></i> 수정</button>
              <button class="act-btn danger" @click="deletePopup(row.id)"><i class="ti ti-trash"></i></button>
            </div>
          </div>
          <div v-if="!popups.length && !loading" class="at-empty"><i class="ti ti-layout-2"></i><span>등록된 팝업이 없습니다.</span></div>
        </div>
      </template>

      <!-- 배너 탭 -->
      <template v-if="tab === 'banners'">
        <div class="at-wrap" v-loading="loadingBanner">
          <div class="at-head">
            <div class="at-col" style="flex:1">제목</div>
            <div class="at-col" style="width:160px">노출 기간</div>
            <div class="at-col" style="width:70px;text-align:center">상태</div>
            <div class="at-col" style="width:90px;text-align:center">관리</div>
          </div>
          <div v-for="row in banners" :key="row.id" class="at-row">
            <div class="at-col bold" style="flex:1">{{ row.title }}</div>
            <div class="at-col muted" style="width:160px;font-size:12px">{{ fmtDate(row.startAt) }} ~ {{ fmtDate(row.endAt) }}</div>
            <div class="at-col" style="width:70px;text-align:center">
              <span :class="['adm-badge', row.isActive ? 'badge-success' : 'badge-muted']">{{ row.isActive ? '활성' : '비활성' }}</span>
            </div>
            <div class="at-col at-actions" style="width:90px">
              <button class="act-btn" @click="openBannerEdit(row)"><i class="ti ti-edit"></i> 수정</button>
              <button class="act-btn danger" @click="deleteBanner(row.id)"><i class="ti ti-trash"></i></button>
            </div>
          </div>
          <div v-if="!banners.length && !loadingBanner" class="at-empty"><i class="ti ti-speakerphone"></i><span>등록된 배너가 없습니다.</span></div>
        </div>
      </template>
    </div>

    <!-- 팝업 다이얼로그 -->
    <el-dialog v-model="showPopupForm" :title="editingPopup ? '팝업 수정' : '팝업 추가'" width="520px" destroy-on-close>
      <div class="dlg-form">
        <div class="dlg-field"><label>제목</label><el-input v-model="popupForm.title" /></div>
        <div class="dlg-row">
          <div class="dlg-field">
            <label>콘텐츠 유형</label>
            <el-select v-model="popupForm.contentType" style="width:100%">
              <el-option value="IMAGE" label="이미지" /><el-option value="HTML" label="HTML" />
            </el-select>
          </div>
          <div class="dlg-field">
            <label>위치</label>
            <el-select v-model="popupForm.position" style="width:100%">
              <el-option value="CENTER" label="중앙" /><el-option value="LEFT" label="왼쪽" /><el-option value="RIGHT" label="오른쪽" />
            </el-select>
          </div>
        </div>
        <div class="dlg-field" v-if="popupForm.contentType === 'IMAGE'"><label>이미지 URL</label><el-input v-model="popupForm.imageUrl" /></div>
        <div class="dlg-field" v-else><label>HTML 내용</label><el-input v-model="popupForm.content" type="textarea" :rows="4" resize="none" /></div>
        <div class="dlg-row">
          <div class="dlg-field"><label>링크 URL</label><el-input v-model="popupForm.linkUrl" /></div>
          <div class="dlg-field" style="flex-direction:row;align-items:flex-end;padding-bottom:4px">
            <label class="chk-item"><el-checkbox v-model="popupForm.linkNewWindow" />새 창</label>
          </div>
        </div>
        <div class="dlg-row">
          <div class="dlg-field"><label>시작일</label><el-date-picker v-model="popupForm.startAt" type="datetime" style="width:100%" /></div>
          <div class="dlg-field"><label>종료일</label><el-date-picker v-model="popupForm.endAt" type="datetime" style="width:100%" /></div>
        </div>
        <div class="dlg-row">
          <div class="dlg-field"><label>우선순위</label><el-input-number v-model="popupForm.priority" :min="0" style="width:100%" /></div>
          <div class="dlg-field" style="flex-direction:row;align-items:flex-end;padding-bottom:4px">
            <label class="chk-item"><el-checkbox v-model="popupForm.isActive" />활성화</label>
          </div>
        </div>
      </div>
      <template #footer>
        <button class="adm-btn ghost" @click="showPopupForm = false">취소</button>
        <button class="adm-btn primary" :disabled="saving" @click="savePopup">
          <i v-if="saving" class="ti ti-loader-2 spinning"></i>{{ saving ? '저장 중...' : '저장' }}
        </button>
      </template>
    </el-dialog>

    <!-- 배너 다이얼로그 -->
    <el-dialog v-model="showBannerForm" :title="editingBanner ? '배너 수정' : '배너 추가'" width="480px" destroy-on-close>
      <div class="dlg-form">
        <div class="dlg-field"><label>제목</label><el-input v-model="bannerForm.title" /></div>
        <div class="dlg-field"><label>내용</label><el-input v-model="bannerForm.content" type="textarea" :rows="3" resize="none" /></div>
        <div class="dlg-row">
          <div class="dlg-field"><label>링크 URL</label><el-input v-model="bannerForm.linkUrl" /></div>
          <div class="dlg-field" style="flex-direction:row;align-items:flex-end;padding-bottom:4px">
            <label class="chk-item"><el-checkbox v-model="bannerForm.linkNewWindow" />새 창</label>
          </div>
        </div>
        <div class="dlg-row">
          <div class="dlg-field"><label>시작일</label><el-date-picker v-model="bannerForm.startAt" type="datetime" style="width:100%" /></div>
          <div class="dlg-field"><label>종료일</label><el-date-picker v-model="bannerForm.endAt" type="datetime" style="width:100%" /></div>
        </div>
        <div class="dlg-checks"><label class="chk-item"><el-checkbox v-model="bannerForm.isActive" />활성화</label></div>
      </div>
      <template #footer>
        <button class="adm-btn ghost" @click="showBannerForm = false">취소</button>
        <button class="adm-btn primary" :disabled="saving" @click="saveBanner">
          <i v-if="saving" class="ti ti-loader-2 spinning"></i>{{ saving ? '저장 중...' : '저장' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'

const tab = ref('popups')
const popups = ref([]); const banners = ref([])
const loading = ref(false); const loadingBanner = ref(false); const saving = ref(false)
const showPopupForm = ref(false); const showBannerForm = ref(false)
const editingPopup = ref(null); const editingBanner = ref(null)

const defaultPopup = () => ({ title: '', contentType: 'IMAGE', imageUrl: '', content: '', linkUrl: '', linkNewWindow: false, position: 'CENTER', priority: 0, startAt: null, endAt: null, isActive: true })
const defaultBanner = () => ({ title: '', content: '', linkUrl: '', linkNewWindow: false, startAt: null, endAt: null, isActive: true })
const popupForm = ref(defaultPopup()); const bannerForm = ref(defaultBanner())

async function fetchPopups() { loading.value = true; try { const r = await api.get('/admin/display/popups'); popups.value = r.data.data || [] } finally { loading.value = false } }
async function fetchBanners() { loadingBanner.value = true; try { const r = await api.get('/admin/display/banners'); banners.value = r.data.data || [] } finally { loadingBanner.value = false } }

function openPopupCreate() { editingPopup.value = null; popupForm.value = defaultPopup(); showPopupForm.value = true }
function openPopupEdit(p) { editingPopup.value = p; popupForm.value = { ...p }; showPopupForm.value = true }
function openBannerCreate() { editingBanner.value = null; bannerForm.value = defaultBanner(); showBannerForm.value = true }
function openBannerEdit(b) { editingBanner.value = b; bannerForm.value = { ...b }; showBannerForm.value = true }

async function savePopup() {
  saving.value = true
  try {
    editingPopup.value ? await api.put(`/admin/display/popups/${editingPopup.value.id}`, popupForm.value) : await api.post('/admin/display/popups', popupForm.value)
    ElMessage.success('저장되었습니다.'); showPopupForm.value = false; fetchPopups()
  } finally { saving.value = false }
}
async function saveBanner() {
  saving.value = true
  try {
    editingBanner.value ? await api.put(`/admin/display/banners/${editingBanner.value.id}`, bannerForm.value) : await api.post('/admin/display/banners', bannerForm.value)
    ElMessage.success('저장되었습니다.'); showBannerForm.value = false; fetchBanners()
  } finally { saving.value = false }
}
async function deletePopup(id) { await ElMessageBox.confirm('팝업을 삭제하시겠습니까?', '삭제', { type: 'warning', confirmButtonText: '삭제', cancelButtonText: '취소' }); await api.delete(`/admin/display/popups/${id}`); ElMessage.success('삭제되었습니다.'); fetchPopups() }
async function deleteBanner(id) { await ElMessageBox.confirm('배너를 삭제하시겠습니까?', '삭제', { type: 'warning', confirmButtonText: '삭제', cancelButtonText: '취소' }); await api.delete(`/admin/display/banners/${id}`); ElMessage.success('삭제되었습니다.'); fetchBanners() }

function posLabel(p) { return { CENTER: '중앙', LEFT: '왼쪽', RIGHT: '오른쪽' }[p] || p }
function fmtDate(d) { if (!d) return '-'; return new Date(d).toLocaleDateString('ko-KR', { year: '2-digit', month: '2-digit', day: '2-digit' }) }

watch(tab, (v) => { if (v === 'banners' && !banners.value.length) fetchBanners() })
onMounted(() => { fetchPopups(); fetchBanners() })
</script>

<style scoped>
@import '@/assets/admin-table.css';
</style>
