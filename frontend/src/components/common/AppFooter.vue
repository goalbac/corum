<template>
  <footer class="app-footer">
    <div class="footer-inner">
      <div class="footer-default">
        <div class="footer-left">
          <div class="footer-brand">
            <img v-if="logoUrl" :src="logoUrl" :alt="siteName" class="footer-logo-img" />
            <span v-else class="footer-logo-text">{{ siteName || 'Corum' }}</span>
          </div>
          <p v-if="siteDescription" class="footer-desc">{{ siteDescription }}</p>
          <div v-if="footerHtml" class="footer-custom" v-html="footerHtml" />
          <div class="footer-contact">
            <span v-if="contactAddress" class="contact-item">
              <i class="ti ti-map-pin"></i>{{ contactAddress }}
            </span>
            <span v-if="contactPhone" class="contact-item">
              <i class="ti ti-phone"></i>{{ contactPhone }}
            </span>
            <span v-if="adminEmail" class="contact-item">
              <i class="ti ti-mail"></i>{{ adminEmail }}
            </span>
          </div>
        </div>

        <div class="footer-right">
          <nav class="footer-nav">
            <button class="footer-link" @click="openTerms('SERVICE')">이용약관</button>
            <button class="footer-link bold" @click="openTerms('PRIVACY')">개인정보처리방침</button>
            <button class="footer-link" @click="openInquiry">문의하기</button>
          </nav>
          <div class="footer-copy">© {{ year }} {{ siteName || 'Corum' }}. All rights reserved.</div>
        </div>
      </div>
    </div>

    <!-- 약관 모달 -->
    <el-dialog
      v-model="termsVisible"
      :title="termsTitle"
      width="700px"
      top="6vh"
      class="footer-dialog"
    >
      <div v-loading="termsLoading" class="terms-body">
        <div
          v-for="term in filteredTerms"
          :key="term.id"
          class="terms-content"
          v-html="term.content || '약관 내용이 등록되지 않았습니다.'"
        />
        <el-empty v-if="!termsLoading && !filteredTerms.length" description="등록된 약관이 없습니다." />
      </div>
    </el-dialog>

    <!-- 문의하기 모달 -->
    <el-dialog
      v-model="inquiryVisible"
      title="문의하기"
      width="560px"
      top="8vh"
      class="footer-dialog"
      @closed="resetInquiry"
    >
      <!-- 접수 완료 -->
      <div v-if="inquirySubmitted" class="inquiry-success">
        <i class="ti ti-circle-check"></i>
        <h3>문의가 접수되었습니다.</h3>
        <p>담당자 확인 후 이메일 또는 연락처로 답변드리겠습니다.</p>
        <el-button @click="resetInquiry">새 문의 작성</el-button>
      </div>

      <!-- 문의 폼 -->
      <el-form
        v-else
        ref="inquiryFormRef"
        :model="inquiryForm"
        :rules="inquiryRules"
        label-position="top"
      >
        <div class="inquiry-row">
          <el-form-item label="이름" prop="writerName">
            <el-input v-model="inquiryForm.writerName" placeholder="홍길동" />
          </el-form-item>
          <el-form-item label="제목" prop="title">
            <el-input v-model="inquiryForm.title" placeholder="문의 제목을 입력하세요." />
          </el-form-item>
        </div>
        <el-form-item label="내용" prop="content">
          <el-input
            v-model="inquiryForm.content"
            type="textarea"
            :rows="6"
            placeholder="문의 내용을 입력하세요."
            resize="none"
          />
        </el-form-item>
        <div class="inquiry-row">
          <el-form-item label="연락처">
            <el-input v-model="inquiryForm.contactPhone" placeholder="010-0000-0000" />
          </el-form-item>
          <el-form-item label="이메일">
            <el-input v-model="inquiryForm.contactEmail" placeholder="example@email.com" />
          </el-form-item>
        </div>
        <div v-if="!authStore.isLoggedIn" class="inquiry-notice">
          <i class="ti ti-info-circle"></i>
          비회원으로 접수하시는 경우, 위에 입력하신 연락처 또는 이메일로 답변을 드립니다.
        </div>
      </el-form>

      <template v-if="!inquirySubmitted" #footer>
        <el-button @click="inquiryVisible = false">취소</el-button>
        <el-button type="primary" :loading="inquiryLoading" @click="submitInquiry">문의 접수</el-button>
      </template>
    </el-dialog>
  </footer>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import api from '@/api/axios'

const authStore = useAuthStore()

// 사이트 정보
const footerHtml     = ref('')
const siteName       = ref('')
const siteDescription = ref('')
const logoUrl        = ref('')
const contactAddress = ref('')
const contactPhone   = ref('')
const adminEmail     = ref('')
const year = new Date().getFullYear()

onMounted(async () => {
  try {
    const res = await api.get('/site/public')
    const d = res.data.data
    siteName.value       = d.siteName || ''
    siteDescription.value = d.siteDescription || ''
    logoUrl.value        = d.logoUrl || ''
    footerHtml.value     = d.footerHtml || ''
    contactAddress.value = d.contactAddress || ''
    contactPhone.value   = d.contactPhone || ''
    adminEmail.value     = d.adminEmail || ''
  } catch { /* ignore */ }
})

// ===== 약관 모달 =====
const termsVisible  = ref(false)
const termsLoading  = ref(false)
const termsType     = ref('SERVICE')
const termsList     = ref([])

const termsTitle = computed(() =>
  termsType.value === 'PRIVACY' ? '개인정보처리방침' : '이용약관'
)

const filteredTerms = computed(() =>
  termsList.value.filter(t => t.type === termsType.value)
)

async function openTerms(type) {
  termsType.value = type
  termsVisible.value = true
  if (termsList.value.length) return
  termsLoading.value = true
  try {
    const res = await api.get('/terms/active')
    termsList.value = res.data.data || []
  } finally {
    termsLoading.value = false
  }
}

// ===== 문의하기 모달 =====
const inquiryVisible   = ref(false)
const inquiryLoading   = ref(false)
const inquirySubmitted = ref(false)
const inquiryFormRef   = ref()

const inquiryForm = ref({
  writerName: '',
  title: '',
  content: '',
  contactPhone: '',
  contactEmail: '',
})

const inquiryRules = {
  writerName: [{ required: true, message: '이름을 입력해주세요.' }],
  title:      [{ required: true, message: '제목을 입력해주세요.' }],
  content:    [{ required: true, message: '내용을 입력해주세요.' }],
}

function openInquiry() {
  inquiryForm.value.writerName   = authStore.member?.name || ''
  inquiryForm.value.contactPhone = authStore.member?.phone || ''
  inquiryForm.value.contactEmail = authStore.member?.email || ''
  inquiryVisible.value = true
}

function resetInquiry() {
  inquirySubmitted.value = false
  inquiryForm.value = { writerName: '', title: '', content: '', contactPhone: '', contactEmail: '' }
}

async function submitInquiry() {
  const valid = await inquiryFormRef.value?.validate().catch(() => false)
  if (!valid) return
  inquiryLoading.value = true
  try {
    await api.post('/inquiries', inquiryForm.value)
    inquirySubmitted.value = true
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '접수에 실패했습니다.')
  } finally {
    inquiryLoading.value = false
  }
}
</script>

<style scoped>
.app-footer {
  background: var(--surface);
  border-top: 0.5px solid var(--border2);
}

.footer-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 32px 22px 24px;
}

.footer-default {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 32px;
  flex-wrap: wrap;
}

.footer-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.footer-brand { display: flex; align-items: center; }

.footer-logo-img {
  height: 28px;
  max-width: 120px;
  object-fit: contain;
}

.footer-logo-text {
  font-size: 16px;
  font-weight: 800;
  color: var(--accent);
}

.footer-desc {
  font-size: 13px;
  color: var(--t3);
  margin: 0;
  line-height: 1.5;
  max-width: 360px;
}

.footer-contact {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 2px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--t3);
}

.contact-item i { font-size: 13px; flex-shrink: 0; }

.footer-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
  flex-shrink: 0;
}

.footer-nav {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.footer-link {
  font-size: 13px;
  color: var(--t2);
  padding: 4px 8px;
  border-radius: var(--radius-xs);
  border: none;
  background: transparent;
  cursor: pointer;
  transition: var(--transition);
  font-family: inherit;
}

.footer-link:hover {
  color: var(--accent);
  background: var(--accent-bg);
}

.footer-link.bold { font-weight: 700; }

.footer-copy {
  font-size: 12px;
  color: var(--t3);
}

/* 커스텀 HTML — 사이트 설명과 주소 사이에 위치 */
.footer-custom {
  color: var(--t2);
  font-size: 13px;
  line-height: 1.7;
}
.footer-custom :deep(a) { color: var(--accent-t); text-decoration: none; }
.footer-custom :deep(a:hover) { text-decoration: underline; }
.footer-custom :deep(p) { margin: 0 0 2px; }

/* 약관 모달 본문 */
.terms-body {
  max-height: 65vh;
  overflow-y: auto;
  padding-right: 4px;
  color: var(--t2);
  font-size: 14px;
  line-height: 1.8;
}

.terms-content :deep(h1),
.terms-content :deep(h2),
.terms-content :deep(h3) { color: var(--t1); margin: 1.2em 0 0.4em; }
.terms-content :deep(p)  { margin: 0 0 0.6em; }
.terms-content :deep(ul),
.terms-content :deep(ol) { padding-left: 1.4em; margin: 0 0 0.6em; }

/* 문의하기 모달 */
.inquiry-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.inquiry-success {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 32px 0 16px;
  text-align: center;
}

.inquiry-success i { font-size: 52px; color: var(--green); }
.inquiry-success h3 { font-size: 17px; font-weight: 700; color: var(--t1); margin: 0; }
.inquiry-success p  { font-size: 13px; color: var(--t3); margin: 0; }

.inquiry-notice {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 12px;
  color: var(--t3);
  background: var(--surface2);
  border-radius: var(--radius-xs);
  padding: 8px 10px;
  line-height: 1.5;
}
.inquiry-notice i { font-size: 13px; flex-shrink: 0; margin-top: 1px; }

@media (max-width: 768px) {
  .footer-inner { padding: 24px 16px 20px; }
  .footer-default { flex-direction: column; gap: 20px; }
  .footer-right { align-items: flex-start; }
  .footer-nav { justify-content: flex-start; }
  .inquiry-row { grid-template-columns: 1fr; }
}
</style>
