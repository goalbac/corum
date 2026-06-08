<template>
  <footer class="app-footer">
    <div class="footer-inner">
      <!-- 커스텀 HTML이 있으면 렌더링, 없으면 기본 -->
      <div
        v-if="footerHtml"
        class="footer-custom"
        v-html="footerHtml"
      />
      <div v-else class="footer-default">
        <div class="footer-brand">
          <span class="footer-logo">{{ siteName || 'Corum' }}</span>
          <span v-if="siteDescription" class="footer-desc">{{ siteDescription }}</span>
        </div>
        <div class="footer-copy">
          © {{ year }} {{ siteName || 'Corum' }}. All rights reserved.
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
const year = new Date().getFullYear()

onMounted(async () => {
  try {
    const res = await api.get('/site/public')
    const d = res.data.data
    siteName.value = d.siteName || ''
    siteDescription.value = d.siteDescription || ''
    footerHtml.value = d.footerHtml || ''
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
  padding: 28px 22px;
}

.footer-default {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.footer-brand {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.footer-logo {
  font-size: 15px;
  font-weight: 800;
  color: var(--accent);
}

.footer-desc {
  font-size: 13px;
  color: var(--t3);
}

.footer-copy {
  font-size: 13px;
  color: var(--t3);
}

/* 관리자가 입력한 커스텀 footer HTML 스타일 */
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

@media (max-width: 768px) {
  .footer-default { flex-direction: column; align-items: flex-start; gap: 10px; }
  .footer-inner { padding: 20px 16px; }
}
</style>
