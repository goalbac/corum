<template>
  <div class="admin-dashboard">
    <AdminPageHeader title="관리자 대시보드" desc="Corum 운영 현황" />

    <!-- 요약 카드 -->
    <div class="stat-grid">
      <div class="stat-card" v-for="s in stats" :key="s.label">
        <div class="stat-icon" :style="{ background: s.bg, color: s.color }">
          <i :class="'ti ' + s.icon"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </div>
    </div>

    <!-- 기간별 분석 테이블 -->
    <div class="section-card">
      <div class="section-head">
        <span class="section-title">기간별 분석</span>
        <div class="period-tabs">
          <button
            v-for="opt in periodOpts"
            :key="opt.days"
            :class="['period-tab', selectedDays === opt.days ? 'active' : '']"
            @click="changePeriod(opt.days)"
          >{{ opt.label }}</button>
        </div>
      </div>

      <div v-if="statsLoading" class="stats-loading">
        <i class="ti ti-loader-2 spinning"></i> 로딩 중...
      </div>
      <div v-else class="stats-table-wrap">
        <table class="stats-table">
          <thead>
            <tr>
              <th>일자</th>
              <th>방문자</th>
              <th>페이지뷰</th>
              <th>신규회원</th>
              <th>신규게시글</th>
              <th>댓글</th>
              <th>문의</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in dailyRows" :key="row.date" :class="{ today: isToday(row.date) }">
              <td class="date-col">{{ formatDate(row.date) }}</td>
              <td>{{ row.visitors.toLocaleString() }}</td>
              <td class="secondary">{{ row.pageViews.toLocaleString() }}</td>
              <td>{{ row.newMembers }}</td>
              <td>{{ row.newPosts }}</td>
              <td>{{ row.newComments }}</td>
              <td>{{ row.newInquiries }}</td>
            </tr>
          </tbody>
          <tfoot v-if="weekRow || monthRow">
            <tr v-if="weekRow" class="summary-row">
              <td class="date-col">최근 7일 합계</td>
              <td>{{ weekRow.visitors.toLocaleString() }}명</td>
              <td class="secondary">{{ weekRow.pageViews.toLocaleString() }}</td>
              <td>{{ weekRow.newMembers }}명</td>
              <td>{{ weekRow.newPosts }}</td>
              <td>{{ weekRow.newComments }}</td>
              <td>{{ weekRow.newInquiries }}</td>
            </tr>
            <tr v-if="monthRow" class="summary-row">
              <td class="date-col">이번달 합계</td>
              <td>{{ monthRow.visitors.toLocaleString() }}명</td>
              <td class="secondary">{{ monthRow.pageViews.toLocaleString() }}</td>
              <td>{{ monthRow.newMembers }}명</td>
              <td>{{ monthRow.newPosts }}</td>
              <td>{{ monthRow.newComments }}</td>
              <td>{{ monthRow.newInquiries }}</td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>

    <!-- 빠른 이동 -->
    <div class="quick-links">
      <div class="ql-title">빠른 이동</div>
      <div class="ql-grid">
        <router-link to="/admin/members" class="ql-item">
          <i class="ti ti-users"></i>
          <span>회원 관리</span>
        </router-link>
        <router-link to="/admin/terms" class="ql-item">
          <i class="ti ti-file-check"></i>
          <span>약관 관리</span>
        </router-link>
        <router-link to="/admin/content-pages" class="ql-item">
          <i class="ti ti-file-text"></i>
          <span>안내 페이지</span>
        </router-link>
        <router-link to="/admin/dashboard-widgets" class="ql-item">
          <i class="ti ti-layout-dashboard"></i>
          <span>대시보드 관리</span>
        </router-link>
        <router-link to="/admin/stats" class="ql-item">
          <i class="ti ti-chart-bar"></i>
          <span>상세 통계</span>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'

const stats = ref([
  { label: '전체 회원', value: '-', icon: 'ti-users', bg: '#EFF6FF', color: '#2563EB' },
  { label: '게시판', value: '-', icon: 'ti-layout-list', bg: '#ECFDF5', color: '#10B981' },
  { label: '미처리 문의', value: '-', icon: 'ti-mail', bg: '#FEF2F2', color: '#EF4444' },
  { label: '오늘 방문자', value: '-', icon: 'ti-users-group', bg: '#FAEEDA', color: '#F59E0B' }
])

const periodOpts = [
  { label: '7일', days: 7 },
  { label: '14일', days: 14 },
  { label: '30일', days: 30 },
]
const selectedDays = ref(14)
const statsLoading = ref(false)
const dailyRows = ref([])
const weekRow = ref(null)
const monthRow = ref(null)

const todayStr = new Date().toISOString().slice(0, 10)
function isToday(date) { return date === todayStr }

function formatDate(date) {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
}

async function fetchSummaryStats() {
  try {
    const res = await api.get('/dashboard/stats')
    const data = res.data.data || {}
    stats.value[0].value = data.memberCount ?? '-'
    stats.value[1].value = data.boardCount ?? '-'
    stats.value[2].value = data.pendingInquiryCount ?? '-'
    stats.value[3].value = data.todayUniqueVisits ?? '-'
  } catch {}
}

async function fetchDailyStats() {
  statsLoading.value = true
  try {
    const res = await api.get(`/admin/stats/daily?days=${selectedDays.value}`)
    const data = res.data.data || {}
    dailyRows.value = (data.rows || []).slice().reverse()
    weekRow.value = data.week || null
    monthRow.value = data.month || null
  } catch {
    dailyRows.value = []
  } finally {
    statsLoading.value = false
  }
}

function changePeriod(days) {
  selectedDays.value = days
  fetchDailyStats()
}

onMounted(() => {
  fetchSummaryStats()
  fetchDailyStats()
})
</script>

<style scoped>
.admin-dashboard { display: flex; flex-direction: column; gap: 24px; }

/* 요약 카드 */
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.stat-card { background: var(--surface); border: 0.5px solid var(--border2); border-radius: var(--radius-sm); padding: 18px; display: flex; align-items: center; gap: 14px; box-shadow: var(--shadow); }
.stat-icon { width: 44px; height: 44px; border-radius: var(--radius-sm); display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; }
.stat-value { font-size: 24px; font-weight: 800; color: var(--t1); }
.stat-label { font-size: 14px; color: var(--t3); margin-top: 2px; }

/* 기간별 분석 */
.section-card { background: var(--surface); border: 0.5px solid var(--border2); border-radius: var(--radius-sm); padding: 20px; box-shadow: var(--shadow); }
.section-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; gap: 12px; flex-wrap: wrap; }
.section-title { font-size: 16px; font-weight: 800; color: var(--t1); }
.period-tabs { display: flex; gap: 4px; }
.period-tab { padding: 5px 12px; border-radius: 6px; border: 1px solid var(--border); background: transparent; font-size: 13px; font-weight: 500; color: var(--t2); cursor: pointer; transition: all .15s; }
.period-tab.active { background: var(--accent); border-color: var(--accent); color: #fff; font-weight: 700; }
.period-tab:not(.active):hover { border-color: var(--accent); color: var(--accent); }

.stats-loading { display: flex; align-items: center; gap: 8px; color: var(--t3); font-size: 14px; padding: 24px 0; justify-content: center; }
@keyframes spin { to { transform: rotate(360deg); } }
.spinning { display: inline-block; animation: spin 0.8s linear infinite; }

.stats-table-wrap { overflow-x: auto; }
.stats-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.stats-table th {
  padding: 10px 14px;
  text-align: right;
  font-size: 13px;
  font-weight: 700;
  color: var(--t3);
  background: var(--surface2);
  border-bottom: 1px solid var(--border2);
  white-space: nowrap;
}
.stats-table th:first-child { text-align: left; }
.stats-table td {
  padding: 10px 14px;
  text-align: right;
  color: var(--t1);
  border-bottom: 0.5px solid var(--border2);
  white-space: nowrap;
}
.stats-table tbody tr:last-child td { border-bottom: none; }
.stats-table tbody tr:hover td { background: var(--surface2); }
.stats-table tbody tr.today td { background: color-mix(in srgb, var(--accent) 5%, transparent); font-weight: 600; }
.stats-table .date-col { text-align: left; color: var(--t2); font-weight: 500; }
.stats-table .secondary { color: var(--t3); }
tfoot .summary-row td {
  background: var(--surface2);
  font-weight: 700;
  color: var(--t1);
  border-bottom: none;
}
tfoot .summary-row:first-child td { border-top: 2px solid var(--border); }

/* 빠른 이동 */
.quick-links { background: var(--surface); border: 0.5px solid var(--border2); border-radius: var(--radius-sm); padding: 18px; box-shadow: var(--shadow); }
.ql-title { font-size: 16px; font-weight: 800; color: var(--t1); margin-bottom: 14px; }
.ql-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 10px; }
.ql-item { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 16px; border: 0.5px solid var(--border); border-radius: var(--radius-sm); color: var(--t2); font-size: 14px; transition: var(--transition); text-decoration: none; }
.ql-item i { font-size: 24px; color: var(--accent); }
.ql-item:hover { background: var(--accent-bg); color: var(--accent); border-color: var(--accent); }

@media (max-width: 900px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .ql-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 600px) {
  .ql-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
