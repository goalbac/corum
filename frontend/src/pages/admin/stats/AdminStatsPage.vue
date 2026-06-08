<template>
  <div class="adm-page">
    <AdminPageHeader title="통계 / 로그" desc="방문, 검색, 감사, SMTP 발송 이력">
      <div style="display:flex;gap:8px;align-items:center">
        <input v-model="testEmail" class="adm-search-input" placeholder="test@example.com" style="width:180px" />
        <button class="adm-btn ghost" :disabled="sending" @click="sendTestMail">
          <i v-if="sending" class="ti ti-loader-2 spinning"></i>SMTP 테스트
        </button>
      </div>
    </AdminPageHeader>

    <!-- 통계 카드 -->
    <div class="stat-grid">
      <div class="stat-card"><div class="stat-card-label">오늘 방문</div><div class="stat-card-value">{{ stats.todayVisits ?? '-' }}</div></div>
      <div class="stat-card"><div class="stat-card-label">오늘 순방문</div><div class="stat-card-value">{{ stats.todayUnique ?? '-' }}</div></div>
      <div class="stat-card"><div class="stat-card-label">이번달 방문</div><div class="stat-card-value">{{ stats.monthVisits ?? '-' }}</div></div>
      <div class="stat-card"><div class="stat-card-label">총 회원수</div><div class="stat-card-value">{{ stats.totalMembers ?? '-' }}</div></div>
    </div>

    <div class="adm-card">
      <div class="adm-tabs">
        <div :class="['adm-tab', tab === 'visit' ? 'active' : '']" @click="switchTab('visit')">방문 로그</div>
        <div :class="['adm-tab', tab === 'search' ? 'active' : '']" @click="switchTab('search')">검색 로그</div>
        <div :class="['adm-tab', tab === 'audit' ? 'active' : '']" @click="switchTab('audit')">감사 로그</div>
        <div :class="['adm-tab', tab === 'smtp' ? 'active' : '']" @click="switchTab('smtp')">SMTP 로그</div>
      </div>

      <!-- 방문 로그 -->
      <div class="at-wrap" v-if="tab === 'visit'" v-loading="loading">
        <div class="at-head">
          <div class="at-col" style="width:140px">IP</div>
          <div class="at-col" style="flex:1">요청 URI</div>
          <div class="at-col" style="width:120px">회원</div>
          <div class="at-col" style="width:140px">시각</div>
        </div>
        <div v-for="row in rows" :key="row.id" class="at-row">
          <div class="at-col muted" style="width:140px">{{ row.ipAddress }}</div>
          <div class="at-col" style="flex:1;font-size:12px;font-family:monospace">{{ row.requestUri }}</div>
          <div class="at-col muted" style="width:120px">{{ row.memberName || '-' }}</div>
          <div class="at-col muted" style="width:140px;font-size:12px">{{ fmtDt(row.createdAt) }}</div>
        </div>
        <div v-if="!rows.length && !loading" class="at-empty"><i class="ti ti-eye"></i><span>방문 로그가 없습니다.</span></div>
      </div>

      <!-- 검색 로그 -->
      <div class="at-wrap" v-if="tab === 'search'" v-loading="loading">
        <div class="at-head">
          <div class="at-col" style="flex:1">검색어</div>
          <div class="at-col" style="width:100px">유형</div>
          <div class="at-col" style="width:80px;text-align:center">결과수</div>
          <div class="at-col" style="width:120px">회원</div>
          <div class="at-col" style="width:140px">시각</div>
        </div>
        <div v-for="row in rows" :key="row.id" class="at-row">
          <div class="at-col bold" style="flex:1">{{ row.keyword }}</div>
          <div class="at-col muted" style="width:100px">{{ row.searchType }}</div>
          <div class="at-col muted" style="width:80px;text-align:center">{{ row.resultCount }}</div>
          <div class="at-col muted" style="width:120px">{{ row.memberName || '-' }}</div>
          <div class="at-col muted" style="width:140px;font-size:12px">{{ fmtDt(row.createdAt) }}</div>
        </div>
        <div v-if="!rows.length && !loading" class="at-empty"><i class="ti ti-search"></i><span>검색 로그가 없습니다.</span></div>
      </div>

      <!-- 감사 로그 -->
      <div class="at-wrap" v-if="tab === 'audit'" v-loading="loading">
        <div class="at-head">
          <div class="at-col" style="width:110px">액션</div>
          <div class="at-col" style="width:120px">대상 테이블</div>
          <div class="at-col" style="width:80px;text-align:center">대상 ID</div>
          <div class="at-col" style="width:120px">회원</div>
          <div class="at-col" style="width:120px">IP</div>
          <div class="at-col" style="width:140px">시각</div>
        </div>
        <div v-for="row in rows" :key="row.id" class="at-row">
          <div class="at-col" style="width:110px"><span :class="['adm-badge', auditBadge(row.actionType)]">{{ row.actionType }}</span></div>
          <div class="at-col muted" style="width:120px">{{ row.targetTable }}</div>
          <div class="at-col muted" style="width:80px;text-align:center">{{ row.targetId }}</div>
          <div class="at-col muted" style="width:120px">{{ row.memberName || '-' }}</div>
          <div class="at-col muted" style="width:120px">{{ row.ipAddress }}</div>
          <div class="at-col muted" style="width:140px;font-size:12px">{{ fmtDt(row.createdAt) }}</div>
        </div>
        <div v-if="!rows.length && !loading" class="at-empty"><i class="ti ti-clipboard-list"></i><span>감사 로그가 없습니다.</span></div>
      </div>

      <!-- SMTP 로그 -->
      <div class="at-wrap" v-if="tab === 'smtp'" v-loading="loading">
        <div class="at-head">
          <div class="at-col" style="width:130px">수신자</div>
          <div class="at-col" style="flex:1">제목</div>
          <div class="at-col" style="width:110px">유형</div>
          <div class="at-col" style="width:70px;text-align:center">결과</div>
          <div class="at-col" style="width:140px">시각</div>
        </div>
        <div v-for="row in rows" :key="row.id" class="at-row">
          <div class="at-col muted" style="width:130px">{{ row.toEmail }}</div>
          <div class="at-col" style="flex:1">{{ row.subject }}</div>
          <div class="at-col muted" style="width:110px;font-size:12px">{{ row.sendType }}</div>
          <div class="at-col" style="width:70px;text-align:center">
            <span :class="['adm-badge', row.success ? 'badge-success' : 'badge-danger']">{{ row.success ? '성공' : '실패' }}</span>
          </div>
          <div class="at-col muted" style="width:140px;font-size:12px">{{ fmtDt(row.createdAt) }}</div>
        </div>
        <div v-if="!rows.length && !loading" class="at-empty"><i class="ti ti-mail"></i><span>SMTP 로그가 없습니다.</span></div>
      </div>

      <div class="adm-pagination">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="prev,pager,next" @current-change="fetchRows" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'

const tab = ref('visit'); const rows = ref([]); const loading = ref(false)
const page = ref(1); const size = 20; const total = ref(0)
const stats = ref({}); const testEmail = ref(''); const sending = ref(false)

const endpointMap = { visit: '/admin/stats/visit-logs', search: '/admin/stats/search-logs', audit: '/admin/audit-logs', smtp: '/admin/stats/smtp-logs' }

async function fetchStats() { try { const r = await api.get('/admin/stats/summary'); stats.value = r.data.data || {} } catch {} }
async function fetchRows(p = page.value) {
  page.value = p; loading.value = true
  try { const r = await api.get(endpointMap[tab.value], { params: { page: p - 1, size } }); rows.value = r.data.data?.content || []; total.value = r.data.data?.totalElements || 0 }
  finally { loading.value = false }
}
function switchTab(t) { tab.value = t; rows.value = []; page.value = 1; fetchRows(1) }
async function sendTestMail() {
  if (!testEmail.value) return ElMessage.warning('이메일을 입력해주세요.')
  sending.value = true
  try { await api.post('/admin/settings/smtp/test', { email: testEmail.value }); ElMessage.success('테스트 메일이 발송되었습니다.') }
  finally { sending.value = false }
}
function fmtDt(d) { if (!d) return '-'; return new Date(d).toLocaleString('ko-KR') }
function auditBadge(t) { return { LOGIN: 'badge-info', LOGOUT: 'badge-muted', CREATE: 'badge-success', UPDATE: 'badge-warning', DELETE: 'badge-danger' }[t] || 'badge-muted' }
onMounted(() => { fetchStats(); fetchRows() })
</script>

<style scoped>
@import '@/assets/admin-table.css';
</style>
