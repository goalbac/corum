<template>
  <footer class="app-footer">
    <div class="footer-inner">
      <!-- 관리자가 커스텀 HTML을 입력한 경우 -->
      <div v-if="footerHtml" class="footer-custom" v-html="footerHtml" />

      <!-- 기본 구조형 푸터 -->
      <div v-else class="footer-default">
        <div class="footer-left">
          <div class="footer-brand">
            <img v-if="logoUrl" :src="logoUrl" :alt="siteName" class="footer-logo-img" />
            <span v-else class="footer-logo-text">{{ siteName || 'Corum' }}</span>
          </div>
          <p v-if="siteDescription" class="footer-desc">{{ siteDescription }}</p>
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
            <router-link to="/terms?type=SERVICE" class="footer-link">이용약관</router-link>
            <router-link to="/terms?type=PRIVACY" class="footer-link bold">개인정보처리방침</router-link>
            <router-link to="/inquiry" class="footer-link">문의하기</router-link>
          </nav>
          <div class="footer-copy">© {{ year }} {{ siteName || 'Corum' }}. All rights reserved.</div>
        </div>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios'

const footerHtml = ref('')
const siteName = ref('')
const siteDescription = ref('')
const logoUrl = ref('')
const contactAddress = ref('')
const contactPhone = ref('')
const adminEmail = ref('')
const year = new Date().getFullYear()

onMounted(async () => {
  try {
    const res = await api.get('/site/public')
    const d = res.data.data
    siteName.value = d.siteName || ''
    siteDescription.value = d.siteDescription || ''
    logoUrl.value = d.logoUrl || ''
    footerHtml.value = d.footerHtml || ''
    contactAddress.value = d.contactAddress || ''
    contactPhone.value = d.contactPhone || ''
    adminEmail.value = d.adminEmail || ''
  } catch { /* ignore */ }
})
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

/* ===== 기본 구조형 푸터 ===== */
.footer-default {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 32px;
  flex-wrap: wrap;
}

/* 왼쪽: 브랜드 + 연락처 */
.footer-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.footer-brand {
  display: flex;
  align-items: center;
}

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

.contact-item i {
  font-size: 13px;
  flex-shrink: 0;
  color: var(--t3);
}

/* 오른쪽: 링크 + 카피라이트 */
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
  gap: 4px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.footer-link {
  font-size: 13px;
  color: var(--t2);
  padding: 4px 8px;
  border-radius: var(--radius-xs);
  transition: var(--transition);
}

.footer-link:hover {
  color: var(--accent);
  background: var(--accent-bg);
}

.footer-link.bold {
  font-weight: 700;
}

.footer-copy {
  font-size: 12px;
  color: var(--t3);
}

/* ===== 커스텀 HTML 푸터 ===== */
.footer-custom {
  color: var(--t2);
  font-size: 14px;
  line-height: 1.7;
}

.footer-custom :deep(a) {
  color: var(--accent-t);
  text-decoration: none;
}

.footer-custom :deep(a:hover) {
  text-decoration: underline;
}

.footer-custom :deep(p) {
  margin: 0 0 4px;
}

/* ===== 반응형 ===== */
@media (max-width: 768px) {
  .footer-inner { padding: 24px 16px 20px; }
  .footer-default { flex-direction: column; gap: 20px; }
  .footer-right { align-items: flex-start; }
  .footer-nav { justify-content: flex-start; }
}
</style>
