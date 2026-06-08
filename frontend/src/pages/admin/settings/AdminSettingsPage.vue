<template>
  <div>
    <AdminPageHeader title="사이트 설정" desc="사이트 기본값과 로그인, 파일, SMTP 설정을 관리합니다.">
      <el-button type="primary" size="small" :loading="saving" @click="saveSettings">
        <i class="ti ti-device-floppy" style="margin-right:4px"></i>저장
      </el-button>
    </AdminPageHeader>

    <el-form v-loading="loading" :model="form" label-position="top" class="settings-form">
      <section class="settings-section">
        <h3>기본 정보</h3>
        <div class="form-grid">
          <el-form-item label="사이트명">
            <el-input v-model="form.siteName" />
          </el-form-item>
          <el-form-item label="파비콘 URL">
            <el-input v-model="form.faviconUrl" placeholder="/favicon.svg" />
          </el-form-item>
        </div>
        <el-form-item label="사이트 설명">
          <el-input v-model="form.siteDescription" maxlength="500" show-word-limit />
        </el-form-item>
      </section>

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
      </section>

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

      <section class="settings-section">
        <h3>푸터 (Footer)</h3>
        <p class="section-desc">비워두면 기본 푸터가 표시됩니다. HTML을 직접 입력하거나 아래 미리보기를 참고하세요.</p>
        <el-form-item label="푸터 HTML">
          <el-input
            v-model="form.footerHtml"
            type="textarea"
            :rows="6"
            placeholder="<p>© 2025 단체명. All rights reserved.</p>"
          />
        </el-form-item>
        <div v-if="form.footerHtml" class="footer-preview">
          <div class="preview-label">미리보기</div>
          <div class="preview-content" v-html="form.footerHtml" />
        </div>
      </section>

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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'

const loading = ref(false)
const saving = ref(false)
const form = ref({
  siteName: '',
  siteDescription: '',
  faviconUrl: '',
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
  footerHtml: ''
})

async function fetchSettings() {
  loading.value = true
  try {
    const res = await api.get('/admin/settings')
    form.value = { ...form.value, ...(res.data.data || {}) }
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
  } finally {
    saving.value = false
  }
}

onMounted(fetchSettings)
</script>

<style scoped>
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
  color: var(--t1);
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
}

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

@media (max-width: 768px) {
  .form-grid { grid-template-columns: 1fr; }
}
</style>
