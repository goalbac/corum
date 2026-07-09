<template>
  <div>
    <AdminPageHeader title="사이트 설정" desc="사이트 기본값과 로그인, 파일, SMTP 설정을 관리합니다.">
      <button class="adm-btn primary" @click="saveSettings"><i class="ti ti-device-floppy"></i> 저장</button>
    </AdminPageHeader>

    <el-form v-loading="loading" :model="form" label-position="top" class="settings-form">

      <!-- ① 기본 정보 -->
      <section class="settings-section">
        <h3>기본 정보</h3>
        <div class="form-grid">
          <el-form-item label="사이트명">
            <el-input v-model="form.siteName" />
          </el-form-item>
          <el-form-item label="관리자 이메일">
            <el-input v-model="form.adminEmail" placeholder="admin@example.com" />
          </el-form-item>
          <el-form-item label="연락처">
            <el-input v-model="form.contactPhone" placeholder="02-1234-5678" />
          </el-form-item>
          <el-form-item label="주소">
            <el-input v-model="form.contactAddress" placeholder="서울시 강남구 테헤란로 123" />
          </el-form-item>
        </div>
        <el-form-item label="사이트 설명">
          <el-input v-model="form.siteDescription" maxlength="500" show-word-limit />
        </el-form-item>
      </section>

      <!-- ② 로고 / 파비콘 -->
      <section class="settings-section">
        <h3>로고 / 파비콘</h3>
        <div class="form-grid">
          <!-- 로고 -->
          <el-form-item label="사이트 로고">
            <div class="asset-upload">
              <div class="asset-preview logo-preview">
                <img v-if="form.logoUrl" :src="form.logoUrl" alt="로고" class="preview-img" />
                <span v-else class="preview-empty"><i class="ti ti-photo"></i></span>
              </div>
              <div class="asset-actions">
                <el-button size="small" @click="$refs.logoInput.click()">
                  <i class="ti ti-upload" style="margin-right:4px"></i>업로드
                </el-button>
                <el-button v-if="form.logoUrl" size="small" type="danger" @click="form.logoUrl = ''">
                  <i class="ti ti-trash"></i>
                </el-button>
                <input ref="logoInput" type="file" accept="image/*" style="display:none" @change="uploadLogo" />
                <span v-if="logoUploading" class="upload-hint">업로드 중...</span>
                <span v-else class="upload-hint">PNG, SVG, JPG 권장 · 헤더 좌측 표시</span>
              </div>
            </div>
          </el-form-item>

          <!-- 파비콘 -->
          <el-form-item label="파비콘">
            <div class="asset-upload">
              <div class="asset-preview favicon-preview">
                <img v-if="form.faviconUrl" :src="form.faviconUrl" alt="파비콘" class="preview-img favicon-img" />
                <span v-else class="preview-empty"><i class="ti ti-photo"></i></span>
              </div>
              <div class="asset-actions">
                <el-button size="small" @click="$refs.faviconInput.click()">
                  <i class="ti ti-upload" style="margin-right:4px"></i>업로드
                </el-button>
                <el-button v-if="form.faviconUrl" size="small" type="danger" @click="form.faviconUrl = ''">
                  <i class="ti ti-trash"></i>
                </el-button>
                <input ref="faviconInput" type="file" accept="image/*,.ico" style="display:none" @change="uploadFavicon" />
                <span v-if="faviconUploading" class="upload-hint">업로드 중...</span>
                <span v-else class="upload-hint">ICO, PNG, SVG 지원 · 브라우저 탭 표시</span>
              </div>
            </div>
          </el-form-item>
        </div>
      </section>

      <!-- ③ 점검 모드 -->
      <section class="settings-section">
        <h3>점검 모드</h3>
        <el-form-item>
          <el-switch v-model="form.maintenanceMode" active-text="사용" inactive-text="미사용" />
        </el-form-item>
        <el-form-item label="점검 안내 문구">
          <el-input v-model="form.maintenanceMessage" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="점검 종료 예정">
          <el-date-picker
            v-model="form.maintenanceUntil"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="종료 예정 시각"
            style="width:100%"
          />
        </el-form-item>
      </section>

      <!-- ④ 로그인 정책 -->
      <section class="settings-section">
        <h3>로그인 정책</h3>
        <div class="form-grid">
          <el-form-item label="로그인 실패 제한">
            <el-input-number v-model="form.loginFailLimit" :min="1" style="width:100%" />
          </el-form-item>
          <el-form-item label="세션 타임아웃(분)">
            <el-input-number v-model="form.sessionTimeoutMin" :min="1" style="width:100%" />
          </el-form-item>
        </div>
        <el-form-item>
          <el-switch v-model="form.allowConcurrentLogin" active-text="동시 로그인 허용" inactive-text="동시 로그인 제한" />
        </el-form-item>
        <el-form-item>
          <el-switch v-model="form.requireLoginSiteWide" active-text="로그인해야 사이트 이용 가능" inactive-text="비로그인 이용 허용" />
          <p class="section-desc" style="margin:6px 0 0">켜면 로그인/가입 등 일부 화면을 제외한 사이트 전체가 비로그인 접근 시 로그인 화면으로 이동합니다.</p>
        </el-form-item>
      </section>

      <!-- ⑤ 파일 정책 -->
      <section class="settings-section">
        <h3>파일 정책</h3>
        <div class="form-grid">
          <el-form-item label="기본 최대 용량(MB)">
            <el-input-number v-model="form.fileMaxSizeMb" :min="1" :max="500" style="width:100%" />
          </el-form-item>
          <el-form-item label="허용 확장자">
            <el-input v-model="form.fileAllowedExtensions" placeholder="jpg,png,pdf,docx" />
          </el-form-item>
        </div>
      </section>

      <!-- ⑤-1 메뉴 기본값 -->
      <section class="settings-section">
        <h3>메뉴 기본값</h3>
        <el-form-item label="새 메뉴 추가 시 기본 접근 권한">
          <el-select v-model="form.defaultMenuAccessType" style="width:240px">
            <el-option label="모든 사용자" value="ALL" />
            <el-option label="로그인 사용자" value="LOGIN" />
            <el-option label="그룹 지정" value="GROUP" />
          </el-select>
          <p class="section-desc" style="margin:6px 0 0">메뉴 관리에서 새 메뉴를 추가할 때 접근 권한 필드의 초기값으로 사용됩니다. (강제 적용 아님, 추가 후 개별 변경 가능)</p>
        </el-form-item>
        <el-form-item v-if="form.defaultMenuAccessType === 'GROUP'" label="기본 허용 그룹">
          <el-select
            v-model="form.defaultMenuGroupIds"
            multiple
            filterable
            style="width:100%"
            placeholder="기본으로 선택될 그룹을 선택하세요"
          >
            <el-option
              v-for="g in subGroupOptions"
              :key="g.id"
              :value="g.id"
              :label="g.label"
            />
          </el-select>
          <p class="section-desc" style="margin:6px 0 0">최상위 그룹은 표시되지 않습니다.</p>
        </el-form-item>
      </section>

      <!-- ⑥ 푸터 커스텀 HTML (고급) -->
      <section class="settings-section">
        <h3>푸터 커스텀 HTML <span class="section-badge">고급</span></h3>
        <p class="section-desc">비워두면 기본 정보(주소·연락처·약관 링크)가 자동 표시됩니다. HTML을 직접 입력하면 기본 푸터를 완전히 대체합니다.</p>
        <el-form-item label="푸터 HTML">
          <el-input
            v-model="form.footerHtml"
            type="textarea"
            :rows="5"
            placeholder="<p>© 2025 단체명. All rights reserved.</p>"
          />
        </el-form-item>
        <div v-if="form.footerHtml" class="footer-preview">
          <div class="preview-label">미리보기</div>
          <div class="preview-content" v-html="sanitizeHtml(form.footerHtml)" />
        </div>
      </section>

      <!-- ⑦ SMTP -->
      <section class="settings-section">
        <h3>SMTP</h3>
        <div class="form-grid">
          <el-form-item label="Host">
            <el-input v-model="form.smtpHost" placeholder="smtp.gmail.com" />
          </el-form-item>
          <el-form-item label="Port">
            <el-input-number v-model="form.smtpPort" :min="1" style="width:100%" />
          </el-form-item>
          <el-form-item label="Username">
            <el-input v-model="form.smtpUsername" />
          </el-form-item>
          <el-form-item label="Password">
            <el-input v-model="form.smtpPasswordEnc" type="password" show-password />
          </el-form-item>
        </div>
        <el-form-item>
          <el-switch v-model="form.smtpUseTls" active-text="TLS 사용" inactive-text="TLS 미사용" />
        </el-form-item>
      </section>

    </el-form>

    <!-- ⑦ 알림 기본 설정 -->
    <section class="settings-section" style="margin-top: 28px">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px">
        <h3 style="margin:0">알림 기본 설정</h3>
        <button class="adm-btn primary" :disabled="notifSaving" @click="saveNotifDefaults">
          <i class="ti ti-device-floppy"></i> 저장
        </button>
      </div>
      <p class="settings-desc">신규 가입 회원에게 기본으로 활성화할 알림 유형을 설정합니다.</p>
      <div class="notif-default-list" v-loading="notifLoading">
        <div class="notif-default-row notif-default-header">
          <div class="notif-default-label"></div>
          <div class="notif-default-channels">
            <span>시스템 알림</span>
            <span>이메일 알림</span>
          </div>
        </div>
        <div v-for="item in notifDefaults" :key="item.notifType" class="notif-default-row">
          <div class="notif-default-label">{{ item.label }}</div>
          <div class="notif-default-channels">
            <el-switch v-model="item.systemEnabled" />
            <el-switch v-model="item.emailEnabled" />
          </div>
        </div>
      </div>
    </section>

    <!-- ⑧ 알림 자동 정리 설정 -->
    <section class="settings-section" style="margin-top: 28px">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px">
        <h3 style="margin:0">알림 자동 정리</h3>
        <button class="adm-btn primary" :disabled="saving" @click="saveSettings">
          <i class="ti ti-device-floppy"></i> 저장
        </button>
      </div>
      <p class="settings-desc">읽은 알림을 일정 기간이 지나면 자동으로 삭제합니다. 매일 새벽 3시에 실행됩니다.</p>
      <el-form label-width="200px">
        <el-form-item label="읽은 알림 보존 기간 (일)">
          <el-input-number v-model="form.notificationRetentionDays" :min="1" :max="365" />
          <span style="margin-left:8px;color:var(--text-sub);font-size:13px">일 이상 지난 읽은 알림을 삭제합니다.</span>
        </el-form-item>
      </el-form>
    </section>

    <!-- ⑨ 웹 푸시 (VAPID) 설정 -->
    <section class="settings-section" style="margin-top: 28px">
      <div style="margin-bottom:16px">
        <h3 style="margin:0 0 8px">웹 푸시 알림 (Web Push)</h3>
        <p class="settings-desc">브라우저 탭이 닫혀 있어도 알림을 받을 수 있는 Web Push 기능입니다. VAPID 키를 생성한 후 회원이 마이페이지에서 구독할 수 있습니다.</p>
      </div>
      <el-form label-width="200px">
        <el-form-item label="현재 VAPID 공개키">
          <div v-if="vapidPublicKey" style="display:flex;align-items:center;gap:8px;flex:1">
            <el-input :value="vapidPublicKey" readonly style="font-family:monospace;font-size:12px" />
          </div>
          <span v-else style="color:var(--text-sub);font-size:13px">아직 키가 생성되지 않았습니다.</span>
        </el-form-item>
        <el-form-item label="">
          <el-button type="warning" :loading="vapidGenerating" @click="generateVapid">
            <i class="ti ti-key"></i>&nbsp;
            {{ vapidPublicKey ? 'VAPID 키 재생성' : 'VAPID 키 생성' }}
          </el-button>
          <p v-if="vapidPublicKey" style="margin:6px 0 0;font-size:12px;color:var(--danger)">
            ⚠️ 키를 재생성하면 기존 구독자들은 모두 재구독이 필요합니다.
          </p>
        </el-form-item>
      </el-form>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'
import { useSiteStore } from '@/stores/site'
import { sanitizeHtml } from '@/utils/sanitize'

const siteStore = useSiteStore()

const loading = ref(false)
const saving = ref(false)
const logoUploading = ref(false)
const faviconUploading = ref(false)

const form = ref({
  siteName: '',
  siteDescription: '',
  faviconUrl: '',
  logoUrl: '',
  contactAddress: '',
  contactPhone: '',
  adminEmail: '',
  maintenanceMode: false,
  maintenanceMessage: '',
  maintenanceUntil: null,
  loginFailLimit: 5,
  sessionTimeoutMin: 60,
  allowConcurrentLogin: true,
  fileMaxSizeMb: 10,
  fileAllowedExtensions: '',
  smtpHost: '',
  smtpPort: 587,
  smtpUsername: '',
  smtpPasswordEnc: '',
  smtpUseTls: true,
  footerHtml: '',
  notificationRetentionDays: 30,
  defaultMenuAccessType: 'ALL',
  defaultMenuGroupIds: [],
  requireLoginSiteWide: false
})

const groups = ref([])

// 접근 허용 그룹 선택용: 최상위·시스템 그룹 제외, "상위 - 하위" 레이블 형식
// (AdminMenusPage.vue의 subGroupOptions와 동일한 패턴)
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

async function fetchGroups() {
  try {
    const res = await api.get('/groups')
    groups.value = res.data.data || []
  } catch {}
}

async function fetchSettings() {
  loading.value = true
  try {
    const res = await api.get('/admin/settings')
    form.value = { ...form.value, ...(res.data.data || {}) }
    vapidPublicKey.value = res.data.data?.vapidPublicKey || ''
  } finally {
    loading.value = false
  }
}

async function saveSettings() {
  if (!form.value.siteName) return ElMessage.warning('사이트명을 입력해주세요.')
  saving.value = true
  try {
    await api.put('/admin/settings', form.value)
    ElMessage.success('저장되었습니다.')
    fetchSettings()
    siteStore.fetchSettings()
  } finally {
    saving.value = false
  }
}

async function uploadLogo(e) {
  const file = e.target.files?.[0]
  if (!file) return
  logoUploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const res = await api.post('/admin/settings/logo', fd, { headers: { 'Content-Type': undefined } })
    form.value.logoUrl = res.data.data?.logoUrl || ''
    ElMessage.success('로고가 업로드되었습니다.')
  } catch {
    ElMessage.error('로고 업로드에 실패했습니다.')
  } finally {
    logoUploading.value = false
    e.target.value = ''
  }
}

async function uploadFavicon(e) {
  const file = e.target.files?.[0]
  if (!file) return
  faviconUploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const res = await api.post('/admin/settings/favicon', fd, { headers: { 'Content-Type': undefined } })
    form.value.faviconUrl = res.data.data?.faviconUrl || ''
    ElMessage.success('파비콘이 업로드되었습니다.')
  } catch {
    ElMessage.error('파비콘 업로드에 실패했습니다.')
  } finally {
    faviconUploading.value = false
    e.target.value = ''
  }
}

// ===== 알림 기본값 =====
const notifDefaults = ref([])
const notifLoading = ref(false)
const notifSaving = ref(false)

async function fetchNotifDefaults() {
  notifLoading.value = true
  try {
    const res = await api.get('/admin/notification-defaults')
    notifDefaults.value = res.data.data || []
  } finally {
    notifLoading.value = false
  }
}

async function saveNotifDefaults() {
  notifSaving.value = true
  try {
    const payload = {}
    notifDefaults.value.forEach(d => {
      payload[d.notifType] = { system: d.systemEnabled, email: d.emailEnabled }
    })
    await api.put('/admin/notification-defaults', payload)
    ElMessage.success('저장되었습니다.')
  } finally {
    notifSaving.value = false
  }
}

// ===== VAPID 키 생성 =====
const vapidPublicKey = ref('')
const vapidGenerating = ref(false)

async function generateVapid() {
  vapidGenerating.value = true
  try {
    const res = await api.post('/admin/settings/vapid/generate')
    vapidPublicKey.value = res.data.data?.vapidPublicKey || ''
    ElMessage.success('VAPID 키가 생성되었습니다. 기존 구독자들은 재구독이 필요합니다.')
  } catch {
    ElMessage.error('VAPID 키 생성에 실패했습니다.')
  } finally {
    vapidGenerating.value = false
  }
}

onMounted(() => {
  fetchSettings()
  fetchNotifDefaults()
  fetchGroups()
})
</script>

<style scoped>
@import '@/assets/admin-table.css';
.settings-form { display: flex; flex-direction: column; gap: 16px; }

.settings-section {
  background: var(--surface);
  border: 0.5px solid var(--border2);
  border-radius: var(--radius-sm);
  padding: 18px;
}

.settings-section h3 {
  margin: 0 0 14px;
  font-size: 14px;
  font-weight: 700;
  color: var(--t1);
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-badge {
  font-size: 11px;
  font-weight: 700;
  background: var(--surface2);
  color: var(--t3);
  border: 0.5px solid var(--border);
  border-radius: 4px;
  padding: 1px 6px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.section-desc {
  font-size: 13px;
  color: var(--t3);
  margin: -8px 0 12px;
  line-height: 1.6;
}

/* 에셋 업로드 */
.asset-upload {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.asset-preview {
  flex-shrink: 0;
  background: var(--surface2);
  border: 0.5px solid var(--border);
  border-radius: var(--radius-xs);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.logo-preview {
  width: 120px;
  height: 48px;
}

.favicon-preview {
  width: 48px;
  height: 48px;
}

.preview-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.favicon-img {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

.preview-empty {
  font-size: 20px;
  color: var(--t4, var(--t3));
}

.asset-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 2px;
}

.asset-actions .el-button + .el-button {
  margin-left: 0;
}

.upload-hint {
  font-size: 12px;
  color: var(--t3);
  line-height: 1.5;
}

/* 푸터 미리보기 */
.footer-preview {
  margin-top: 10px;
  border: 0.5px solid var(--border);
  border-radius: var(--radius-xs);
  overflow: hidden;
}

.preview-label {
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 700;
  color: var(--t3);
  background: var(--surface2);
  border-bottom: 0.5px solid var(--border);
}

.preview-content {
  padding: 14px 16px;
  font-size: 14px;
  color: var(--t2);
  line-height: 1.7;
}

.settings-desc {
  font-size: 13px;
  color: var(--t3);
  margin: -8px 0 16px;
}

.notif-default-list {
  border: 1px solid var(--border2);
  border-radius: var(--radius-xs);
  overflow: hidden;
}

.notif-default-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border2);
  background: var(--surface);
  font-size: 14px;
}

.notif-default-row:last-child { border-bottom: none; }

.notif-default-header {
  background: var(--surface2);
  font-size: 12px;
  font-weight: 700;
  color: var(--t3);
}

.notif-default-label { flex: 1; }

.notif-default-channels {
  display: flex;
  gap: 40px;
  align-items: center;
}

.notif-default-channels span {
  width: 68px;
  text-align: center;
  display: inline-block;
}

@media (max-width: 768px) {
  .form-grid { grid-template-columns: 1fr; }
  .logo-preview { width: 90px; }
}
</style>
