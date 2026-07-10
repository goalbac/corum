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
            <div class="at-col" style="width:80px">유형</div>
            <div class="at-col" style="flex:1">이름</div>
            <div class="at-col" style="width:80px">위치</div>
            <div class="at-col" style="width:150px">노출 기간</div>
            <div class="at-col" style="width:70px">등록자</div>
            <div class="at-col" style="width:90px">등록일</div>
            <div class="at-col" style="width:60px;text-align:center">상태</div>
            <div class="at-col" style="width:90px;text-align:center">관리</div>
          </div>
          <div v-for="row in popups" :key="row.id" class="at-row">
            <div class="at-col" style="width:80px">
              <span :class="['adm-badge', row.contentType === 'IMAGE' ? 'badge-primary' : 'badge-purple']">{{ row.contentType === 'IMAGE' ? '이미지' : 'HTML' }}</span>
            </div>
            <div class="at-col bold" style="flex:1">{{ row.title }}</div>
            <div class="at-col muted" style="width:80px">{{ posLabel(row.position) }}</div>
            <div class="at-col muted" style="width:150px;font-size:12px">{{ fmtDate(row.startAt) }} ~ {{ fmtDate(row.endAt) }}</div>
            <div class="at-col muted" style="width:70px;font-size:12px">{{ row.createdByName || '-' }}</div>
            <div class="at-col muted" style="width:90px;font-size:12px">{{ fmtDateShort(row.createdAt) }}</div>
            <div class="at-col" style="width:60px;text-align:center">
              <span :class="['adm-badge', statusBadge(row)]">{{ statusText(row) }}</span>
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
            <div class="at-col" style="flex:1">이름</div>
            <div class="at-col" style="width:150px">노출 기간</div>
            <div class="at-col" style="width:70px">등록자</div>
            <div class="at-col" style="width:90px">등록일</div>
            <div class="at-col" style="width:60px;text-align:center">상태</div>
            <div class="at-col" style="width:90px;text-align:center">관리</div>
          </div>
          <div v-for="row in banners" :key="row.id" class="at-row">
            <div class="at-col bold" style="flex:1">{{ row.title }}</div>
            <div class="at-col muted" style="width:150px;font-size:12px">{{ fmtDate(row.startAt) }} ~ {{ fmtDate(row.endAt) }}</div>
            <div class="at-col muted" style="width:70px;font-size:12px">{{ row.createdByName || '-' }}</div>
            <div class="at-col muted" style="width:90px;font-size:12px">{{ fmtDateShort(row.createdAt) }}</div>
            <div class="at-col" style="width:60px;text-align:center">
              <span :class="['adm-badge', statusBadge(row)]">{{ statusText(row) }}</span>
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
    <el-dialog v-model="showPopupForm" :title="editingPopup ? '팝업 수정' : '팝업 추가'" width="640px" destroy-on-close>
      <div class="dlg-form">
        <div class="dlg-field">
          <label>팝업 이름</label>
          <el-input v-model="popupForm.title" />
          <div class="field-hint"><i class="ti ti-eye-off"></i> 팝업에는 표시되지 않는 관리용 이름입니다.</div>
        </div>
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

        <!-- 이미지 유형 -->
        <template v-if="popupForm.contentType === 'IMAGE'">
          <div class="dlg-field">
            <label>이미지</label>
            <div class="img-upload-area" @click="triggerFileInput" @dragover.prevent @drop.prevent="onImageDrop">
              <template v-if="popupForm.imageUrl">
                <img :src="resolveFileUrl(popupForm.imageUrl)" class="img-preview" />
                <div class="img-overlay">
                  <span><i class="ti ti-refresh"></i> 변경</span>
                </div>
              </template>
              <template v-else>
                <i class="ti ti-photo-up" style="font-size:32px;color:var(--adm-muted)"></i>
                <span style="margin-top:8px;color:var(--adm-muted);font-size:13px">클릭하거나 이미지를 드래그하세요</span>
                <span style="font-size:11px;color:var(--adm-muted);margin-top:4px">JPG, PNG, GIF, WEBP 지원</span>
              </template>
              <input ref="fileInputRef" type="file" accept="image/*" style="display:none" @change="onImageSelect" />
            </div>
            <div v-if="uploading" style="margin-top:6px;font-size:12px;color:var(--adm-muted)"><i class="ti ti-loader-2 spinning"></i> 업로드 중...</div>
          </div>
          <div class="dlg-field">
            <label>본문 <span class="optional-label">(선택)</span></label>
            <el-input v-model="popupForm.content" type="textarea" :rows="3" resize="none" placeholder="이미지 아래에 표시할 텍스트 (입력하지 않으면 이미지만 표시됩니다)" />
          </div>
          <!-- 미리보기 패널 -->
          <div v-if="popupForm.imageUrl || popupForm.content" class="popup-preview-wrap">
            <div class="popup-preview-label">미리보기</div>
            <div class="popup-preview-box">
              <div class="popup-preview-popup">
                <div class="popup-preview-header">팝업</div>
                <img v-if="popupForm.imageUrl" :src="resolveFileUrl(popupForm.imageUrl)" style="max-width:100%;display:block" />
                <div v-if="popupForm.content" class="popup-preview-body">{{ popupForm.content }}</div>
              </div>
            </div>
          </div>
        </template>

        <!-- HTML 유형 -->
        <template v-else>
          <div class="dlg-field">
            <label>HTML 내용</label>
            <el-input v-model="popupForm.content" type="textarea" :rows="5" resize="none" placeholder="<div>팝업 HTML 내용</div>" />
          </div>
          <!-- HTML 미리보기 -->
          <div v-if="popupForm.content" class="popup-preview-wrap">
            <div class="popup-preview-label">미리보기</div>
            <div class="popup-preview-box">
              <div class="popup-preview-popup">
                <div class="popup-preview-header">팝업</div>
                <div class="popup-preview-html" v-html="sanitizeHtml(popupForm.content)"></div>
              </div>
            </div>
          </div>
        </template>

        <div class="dlg-field">
          <label>링크 URL</label>
          <el-input v-model="popupForm.linkUrl" placeholder="예) /menu/46/posts/92 또는 https://example.com" />
          <div class="field-hint">
            <i class="ti ti-info-circle"></i>
            내부 페이지는 <code>/</code>로 시작하는 경로만 입력하세요 (예: <code>/menu/46/posts/92</code>).
            외부 사이트는 <code>https://</code>를 포함한 전체 주소를 입력해야 합니다.
          </div>
        </div>
        <div class="dlg-checks">
          <el-tooltip placement="top">
            <template #content>
              <div style="max-width:240px;line-height:1.6">
                <div v-if="popupForm.linkUrl">
                  입력한 URL을 새 탭에서 엽니다:<br />
                  <code style="word-break:break-all">{{ popupForm.linkUrl }}</code>
                </div>
                <div v-else>링크 URL을 새 탭에서 엽니다.</div>
                <div style="margin-top:6px;color:#ffd666">
                  <i class="ti ti-bulb"></i> 외부 주소 링크는 새 창을 권장합니다.
                </div>
              </div>
            </template>
            <label class="chk-item"><el-checkbox v-model="popupForm.linkNewWindow" />새 창</label>
          </el-tooltip>
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
        <button class="adm-btn primary" :disabled="saving || uploading" @click="savePopup">
          <i v-if="saving" class="ti ti-loader-2 spinning"></i>{{ saving ? '저장 중...' : '저장' }}
        </button>
      </template>
    </el-dialog>

    <!-- 배너 다이얼로그 -->
    <el-dialog v-model="showBannerForm" :title="editingBanner ? '배너 수정' : '배너 추가'" width="540px" destroy-on-close>
      <div class="dlg-form">
        <div class="dlg-field">
          <label>배너 이름</label>
          <el-input v-model="bannerForm.title" />
          <div class="field-hint"><i class="ti ti-eye-off"></i> 배너에는 표시되지 않는 관리용 이름입니다.</div>
        </div>
        <div class="dlg-field"><label>내용</label><el-input v-model="bannerForm.content" type="textarea" :rows="3" resize="none" /></div>
        <div class="dlg-field">
          <label>배경 색상</label>
          <div class="color-picker-row">
            <el-color-picker v-model="bannerForm.bgColor" show-alpha :predefine="bgPresets" />
            <span class="color-value">{{ bannerForm.bgColor || '기본값' }}</span>
            <button v-if="bannerForm.bgColor" class="color-clear-btn" @click="bannerForm.bgColor = null">
              <i class="ti ti-x"></i> 초기화
            </button>
          </div>
        </div>
        <div class="dlg-field">
          <label>텍스트 정렬</label>
          <el-radio-group v-model="bannerForm.textAlign">
            <el-radio-button value="left"><i class="ti ti-align-left"></i></el-radio-button>
            <el-radio-button value="center"><i class="ti ti-align-center"></i></el-radio-button>
            <el-radio-button value="right"><i class="ti ti-align-right"></i></el-radio-button>
          </el-radio-group>
        </div>
        <!-- 배너 미리보기 -->
        <div class="banner-preview-wrap">
          <div class="popup-preview-label">미리보기</div>
          <div
            class="banner-preview"
            :style="{ background: bannerForm.bgColor || 'var(--accent)', color: '#fff', textAlign: bannerForm.textAlign || 'left' }"
          >
            <span v-if="bannerForm.content">{{ bannerForm.content }}</span>
            <span v-else class="banner-preview-placeholder">배너 내용이 여기에 표시됩니다</span>
            <span v-if="bannerForm.linkUrl" class="banner-preview-link">
              <i class="ti ti-external-link"></i>
            </span>
          </div>
        </div>
        <div class="dlg-field">
          <label>링크 URL</label>
          <el-input v-model="bannerForm.linkUrl" placeholder="예) /menu/46/posts/92 또는 https://example.com" />
          <div class="field-hint">
            <i class="ti ti-info-circle"></i>
            내부 페이지는 <code>/</code>로 시작하는 경로만 입력하세요 (예: <code>/menu/46/posts/92</code>).
            외부 사이트는 <code>https://</code>를 포함한 전체 주소를 입력해야 합니다.
          </div>
        </div>
        <div class="dlg-checks">
          <el-tooltip placement="top">
            <template #content>
              <div style="max-width:240px;line-height:1.6">
                <div v-if="bannerForm.linkUrl">
                  입력한 URL을 새 탭에서 엽니다:<br />
                  <code style="word-break:break-all">{{ bannerForm.linkUrl }}</code>
                </div>
                <div v-else>링크 URL을 새 탭에서 엽니다.</div>
                <div style="margin-top:6px;color:#ffd666">
                  <i class="ti ti-bulb"></i> 외부 주소 링크는 새 창을 권장합니다.
                </div>
              </div>
            </template>
            <label class="chk-item"><el-checkbox v-model="bannerForm.linkNewWindow" />새 창</label>
          </el-tooltip>
          <label class="chk-item" style="margin-left:16px"><el-checkbox v-model="bannerForm.isActive" />활성화</label>
        </div>
        <div class="dlg-row">
          <div class="dlg-field"><label>시작일</label><el-date-picker v-model="bannerForm.startAt" type="datetime" style="width:100%" /></div>
          <div class="dlg-field"><label>종료일</label><el-date-picker v-model="bannerForm.endAt" type="datetime" style="width:100%" /></div>
        </div>
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
import { sanitizeHtml } from '@/utils/sanitize'
import { resolveFileUrl } from '@/utils/fileUrl'

const tab = ref('popups')
const popups = ref([]); const banners = ref([])
const loading = ref(false); const loadingBanner = ref(false); const saving = ref(false)
const uploading = ref(false)
const showPopupForm = ref(false); const showBannerForm = ref(false)
const editingPopup = ref(null); const editingBanner = ref(null)
const fileInputRef = ref(null)

const bgPresets = ['#1e40af', '#0f766e', '#7c3aed', '#b91c1c', '#b45309', '#374151', '#111827']

const defaultPopup = () => ({ title: '', contentType: 'IMAGE', imageUrl: '', content: '', linkUrl: '', linkNewWindow: false, position: 'CENTER', priority: 0, startAt: null, endAt: null, isActive: true, targetType: 'ALL', targetMenuIds: [] })
const defaultBanner = () => ({ title: '', content: '', linkUrl: '', linkNewWindow: false, startAt: null, endAt: null, isActive: true, bgColor: null, textAlign: 'left' })
const popupForm = ref(defaultPopup()); const bannerForm = ref(defaultBanner())

function triggerFileInput() { fileInputRef.value?.click() }

async function uploadImageFile(file) {
  if (!file) return
  const allowed = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp']
  if (!allowed.includes(file.type)) { ElMessage.error('이미지 파일만 업로드할 수 있습니다.'); return }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const r = await api.post('/admin/display/popups/image', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
    popupForm.value.imageUrl = r.data.data.url
  } catch {
    ElMessage.error('이미지 업로드에 실패했습니다.')
  } finally {
    uploading.value = false
  }
}

function onImageSelect(e) { uploadImageFile(e.target.files[0]); e.target.value = '' }
function onImageDrop(e) { uploadImageFile(e.dataTransfer.files[0]) }

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

function isExpired(row) { return row.endAt && new Date(row.endAt) < new Date() }
function statusText(row) {
  if (!row.isActive) return '비활성'
  if (isExpired(row)) return '기간만료'
  return '활성'
}
function statusBadge(row) {
  if (!row.isActive) return 'badge-muted'
  if (isExpired(row)) return 'badge-warning'
  return 'badge-success'
}
function posLabel(p) { return { CENTER: '중앙', LEFT: '왼쪽', RIGHT: '오른쪽' }[p] || p }
function fmtDate(d) { if (!d) return '-'; return new Date(d).toLocaleDateString('ko-KR', { year: '2-digit', month: '2-digit', day: '2-digit' }) }
function fmtDateShort(d) { if (!d) return '-'; const dt = new Date(d); return `${dt.getFullYear().toString().slice(2)}.${String(dt.getMonth()+1).padStart(2,'0')}.${String(dt.getDate()).padStart(2,'0')}` }

watch(tab, (v) => { if (v === 'banners' && !banners.value.length) fetchBanners() })
onMounted(() => { fetchPopups(); fetchBanners() })
</script>

<style scoped>
@import '@/assets/admin-table.css';

.img-upload-area {
  position: relative;
  border: 2px dashed var(--adm-border);
  border-radius: 8px;
  min-height: 140px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  transition: border-color 0.2s;
}
.img-upload-area:hover { border-color: var(--adm-primary); }
.img-preview { width: 100%; max-height: 240px; object-fit: contain; display: block; }
.img-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
  color: #fff;
  font-size: 14px;
  gap: 6px;
}
.img-upload-area:hover .img-overlay { opacity: 1; }

.optional-label {
  font-size: 11px;
  font-weight: 400;
  color: var(--adm-muted);
  margin-left: 4px;
}

.popup-preview-wrap { margin-top: 12px; }
.popup-preview-label { font-size: 12px; color: var(--adm-muted); margin-bottom: 6px; }
.popup-preview-box {
  background: #f0f0f0;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 80px;
}
.popup-preview-popup {
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.18);
  overflow: hidden;
  max-width: 320px;
  width: 100%;
}
.popup-preview-header {
  background: var(--adm-primary, #409eff);
  color: #fff;
  font-size: 12px;
  padding: 6px 10px;
}
.popup-preview-html {
  padding: 10px;
  font-size: 13px;
  max-height: 200px;
  overflow: auto;
}
.popup-preview-body {
  padding: 10px 12px;
  font-size: 13px;
  color: #333;
  line-height: 1.6;
  white-space: pre-wrap;
  border-top: 1px solid #f0f0f0;
}

/* 배너 미리보기 */
.banner-preview-wrap { margin-bottom: 12px; }
.banner-preview {
  display: block;
  padding: 10px 20px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  min-height: 40px;
  line-height: 2;
  transition: background 0.2s;
}
.banner-preview-placeholder { opacity: 0.55; font-weight: 400; font-style: italic; }
.banner-preview-link { font-size: 14px; opacity: 0.8; }

/* 색상 선택 */
.color-picker-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.color-value {
  font-size: 12px;
  color: var(--adm-muted);
  font-family: monospace;
}
.color-clear-btn {
  font-size: 12px;
  color: var(--adm-muted);
  background: none;
  border: 1px solid var(--adm-border);
  border-radius: 4px;
  padding: 3px 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 3px;
  transition: color 0.15s, border-color 0.15s;
}
.color-clear-btn:hover { color: var(--adm-danger, #ef4444); border-color: var(--adm-danger, #ef4444); }

.field-hint {
  margin-top: 5px;
  font-size: 12px;
  color: var(--adm-muted);
  line-height: 1.6;
}
.field-hint code {
  background: var(--adm-bg-subtle, rgba(0,0,0,0.06));
  border-radius: 3px;
  padding: 0 3px;
  font-family: monospace;
  font-size: 11px;
}
</style>
