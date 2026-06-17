<template>
  <div class="admin-dashboard">
    <AdminPageHeader title="관리자 대시보드" desc="Corum 운영 현황" />

    <!-- 요약 카드 -->
    <div class="stat-grid">
      <router-link v-for="s in stats" :key="s.label" :to="s.to" class="stat-card">
        <div class="stat-icon" :style="{ background: s.bg, color: s.color }">
          <i :class="'ti ' + s.icon"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </router-link>
    </div>

    <!-- 방문자 차트 + 기간별 분석 -->
    <div class="two-col">
      <!-- 방문자 차트 -->
      <div class="section-card chart-card">
        <div class="section-head">
          <span class="section-title">방문자</span>
          <div class="chart-legend">
            <span class="legend-item"><span class="legend-dot pageview"></span>페이지뷰</span>
            <span class="legend-item"><span class="legend-dot visitor"></span>방문자</span>
          </div>
        </div>
        <div v-if="statsLoading" class="stats-loading chart-loading">
          <i class="ti ti-loader-2 spinning"></i>
        </div>
        <svg v-else :viewBox="`0 0 ${CW} ${CH}`" class="visitor-chart" preserveAspectRatio="xMidYMid meet">
            <defs>
              <linearGradient id="pvGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#93C5FD" stop-opacity="0.5"/>
                <stop offset="100%" stop-color="#93C5FD" stop-opacity="0.03"/>
              </linearGradient>
              <linearGradient id="vGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#2563EB" stop-opacity="0.35"/>
                <stop offset="100%" stop-color="#2563EB" stop-opacity="0.02"/>
              </linearGradient>
            </defs>
            <!-- Y 격자선 -->
            <g v-for="t in chartYTicks" :key="t">
              <line :x1="CPX" :y1="chartYPos(t)" :x2="CW - CPX" :y2="chartYPos(t)"
                stroke="currentColor" stroke-opacity="0.08" stroke-width="1"/>
              <text :x="CPX - 6" :y="chartYPos(t) + 4" text-anchor="end" font-size="10" fill="currentColor" opacity="0.4">{{ t }}</text>
            </g>
            <!-- 페이지뷰 영역 -->
            <path :d="pvAreaPath" fill="url(#pvGrad)"/>
            <path :d="pvLinePath" fill="none" stroke="#93C5FD" stroke-width="2" stroke-linejoin="round"/>
            <!-- 방문자 영역 -->
            <path :d="vAreaPath" fill="url(#vGrad)"/>
            <path :d="vLinePath" fill="none" stroke="#2563EB" stroke-width="2.5" stroke-linejoin="round"/>
            <!-- 방문자 점 -->
            <circle v-for="(r, i) in chartRows" :key="i"
              :cx="chartXPos(i)" :cy="chartYPos(r.visitors)" r="3.5"
              fill="#2563EB" stroke="#fff" stroke-width="1.5"/>
            <!-- X축 레이블 -->
            <text v-for="l in chartXLabels" :key="l.label"
              :x="l.x" :y="CH - 8"
              text-anchor="middle" font-size="10" fill="currentColor" opacity="0.4">{{ l.label }}</text>
          </svg>
      </div>

      <!-- 기간별 분석 -->
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
                <th>신규회원</th>
                <th>게시글</th>
                <th>댓글</th>
                <th>문의</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in dailyRows" :key="row.date" :class="{ today: isToday(row.date) }">
                <td class="date-col">{{ formatDate(row.date) }}</td>
                <td>{{ row.visitors }}</td>
                <td>{{ row.newMembers }}</td>
                <td>{{ row.newPosts }}</td>
                <td>{{ row.newComments }}</td>
                <td>{{ row.newInquiries }}</td>
              </tr>
            </tbody>
            <tfoot>
              <tr v-if="weekRow" class="summary-row">
                <td class="date-col">최근 7일</td>
                <td>{{ weekRow.visitors }}명</td>
                <td>{{ weekRow.newMembers }}명</td>
                <td>{{ weekRow.newPosts }}</td>
                <td>{{ weekRow.newComments }}</td>
                <td>{{ weekRow.newInquiries }}</td>
              </tr>
              <tr v-if="monthRow" class="summary-row">
                <td class="date-col">이번달</td>
                <td>{{ monthRow.visitors }}명</td>
                <td>{{ monthRow.newMembers }}명</td>
                <td>{{ monthRow.newPosts }}</td>
                <td>{{ monthRow.newComments }}</td>
                <td>{{ monthRow.newInquiries }}</td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>
    </div>

    <!-- 빠른 이동 -->
    <div class="quick-links">
      <div class="ql-title">빠른 이동</div>
      <div class="ql-grid">
        <router-link to="/admin/members" class="ql-item">
          <i class="ti ti-users"></i><span>회원 관리</span>
        </router-link>
        <router-link to="/admin/terms" class="ql-item">
          <i class="ti ti-file-check"></i><span>약관 관리</span>
        </router-link>
        <router-link to="/admin/content-pages" class="ql-item">
          <i class="ti ti-file-text"></i><span>안내 페이지</span>
        </router-link>
        <router-link to="/admin/dashboard-widgets" class="ql-item">
          <i class="ti ti-layout-dashboard"></i><span>대시보드 관리</span>
        </router-link>
        <router-link to="/admin/stats" class="ql-item">
          <i class="ti ti-chart-bar"></i><span>상세 통계</span>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'

// ===== 차트 상수 =====
const CW = 560, CH = 200, CPX = 38, CPY = 16, CPB = 32

// ===== 데이터 =====
const stats = ref([
  { label: '전체 회원', value: '-', icon: 'ti-users', bg: '#EFF6FF', color: '#2563EB', to: '/admin/members' },
  { label: '오늘 방문자', value: '-', icon: 'ti-users-group', bg: '#FAEEDA', color: '#F59E0B', to: '/admin/stats' },
  { label: '오늘 페이지뷰', value: '-', icon: 'ti-chart-bar', bg: '#ECFDF5', color: '#10B981', to: '/admin/stats' },
  { label: '오늘 신규게시글', value: '-', icon: 'ti-pencil-plus', bg: '#FEF2F2', color: '#EF4444', to: '/admin/boards' },
])

const periodOpts = [{ label: '7일', days: 7 }, { label: '14일', days: 14 }, { label: '30일', days: 30 }]
const selectedDays = ref(7)
const statsLoading = ref(false)
const dailyRows = ref([])
const weekRow = ref(null)
const monthRow = ref(null)

// 차트용: 최신순으로 들어온 rows를 날짜 오름차순으로
const chartRows = computed(() => [...dailyRows.value].reverse())

// ===== 차트 계산 =====
const chartMaxY = computed(() => {
  const vals = chartRows.value.flatMap(r => [r.visitors, r.pageViews])
  return Math.max(...vals, 10)
})

function chartXPos(i) {
  const n = chartRows.value.length
  return CPX + (i / Math.max(n - 1, 1)) * (CW - CPX * 2)
}
function chartYPos(val) {
  return CPY + (1 - val / chartMaxY.value) * (CH - CPY - CPB)
}

const pvLinePath = computed(() => {
  if (!chartRows.value.length) return ''
  return 'M ' + chartRows.value.map((r, i) => `${chartXPos(i)},${chartYPos(r.pageViews)}`).join(' L ')
})
const pvAreaPath = computed(() => {
  if (!chartRows.value.length) return ''
  const n = chartRows.value.length
  const bottom = CH - CPB
  return pvLinePath.value + ` L ${chartXPos(n - 1)},${bottom} L ${chartXPos(0)},${bottom} Z`
})
const vLinePath = computed(() => {
  if (!chartRows.value.length) return ''
  return 'M ' + chartRows.value.map((r, i) => `${chartXPos(i)},${chartYPos(r.visitors)}`).join(' L ')
})
const vAreaPath = computed(() => {
  if (!chartRows.value.length) return ''
  const n = chartRows.value.length
  const bottom = CH - CPB
  return vLinePath.value + ` L ${chartXPos(n - 1)},${bottom} L ${chartXPos(0)},${bottom} Z`
})
const chartYTicks = computed(() => {
  const max = chartMaxY.value
  const step = Math.ceil(max / 4 / 5) * 5 || 1
  return [0, 1, 2, 3, 4].map(i => i * step).filter(v => v <= max * 1.05)
})
const chartXLabels = computed(() => {
  const rows = chartRows.value
  if (!rows.length) return []
  const step = Math.max(1, Math.floor(rows.length / 6))
  return rows
    .filter((_, i) => i % step === 0 || i === rows.length - 1)
    .map((r, _, arr) => {
      const idx = rows.indexOf(r)
      const d = new Date(r.date)
      return { x: chartXPos(idx), label: `${d.getMonth() + 1}/${String(d.getDate()).padStart(2, '0')}` }
    })
})

// ===== 유틸 =====
const todayStr = new Date().toISOString().slice(0, 10)
function isToday(date) { return date === todayStr }
function formatDate(date) {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

// ===== API =====
async function fetchSummaryStats() {
  try {
    const res = await api.get('/dashboard/stats')
    const d = res.data.data || {}
    stats.value[0].value = d.memberCount ?? '-'
    stats.value[1].value = d.todayUniqueVisits ?? '-'
    stats.value[2].value = d.todayVisits ?? '-'
  } catch {}
}

async function fetchDailyStats() {
  statsLoading.value = true
  try {
    const res = await api.get(`/admin/stats/daily?days=${selectedDays.value}`)
    const data = res.data.data || {}
    const rows = (data.rows || []).slice().reverse()
    dailyRows.value = rows
    weekRow.value = data.week || null
    monthRow.value = data.month || null
    const todayRow = rows.find(r => r.date === todayStr)
    stats.value[3].value = todayRow?.newPosts ?? 0
  } catch {
    dailyRows.value = []
  } finally {
    statsLoading.value = false
  }
}

function changePeriod(days) { selectedDays.value = days; fetchDailyStats() }

onMounted(() => { fetchSummaryStats(); fetchDailyStats() })
</script>

<style scoped>
.admin-dashboard { display: flex; flex-direction: column; gap: 20px; }

/* 요약 카드 */
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.stat-card {
  background: var(--surface); border: 0.5px solid var(--border2);
  border-radius: var(--radius-sm); padding: 18px;
  display: flex; align-items: center; gap: 14px;
  box-shadow: var(--shadow); text-decoration: none;
  transition: box-shadow .15s, border-color .15s;
}
.stat-card:hover { border-color: var(--accent); box-shadow: 0 4px 16px rgba(0,0,0,.1); }
.stat-icon { width: 44px; height: 44px; border-radius: var(--radius-sm); display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; }
.stat-value { font-size: 24px; font-weight: 800; color: var(--t1); }
.stat-label { font-size: 13px; color: var(--t3); margin-top: 2px; }

/* 2컬럼 레이아웃 */
.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; align-items: stretch; }

/* 섹션 카드 */
.section-card { background: var(--surface); border: 0.5px solid var(--border2); border-radius: var(--radius-sm); padding: 20px; box-shadow: var(--shadow); display: flex; flex-direction: column; }
.chart-card { min-height: 0; }
.visitor-chart { flex: 1; min-height: 0; }
.chart-loading { flex: 1; }
.section-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; gap: 12px; flex-wrap: wrap; }
.section-title { font-size: 15px; font-weight: 800; color: var(--t1); }

/* 차트 */
.chart-legend { display: flex; gap: 12px; }
.legend-item { display: flex; align-items: center; gap: 5px; font-size: 12px; color: var(--t3); }
.legend-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.legend-dot.pageview { background: #93C5FD; }
.legend-dot.visitor { background: #2563EB; }
.visitor-chart { width: 100%; height: 100%; display: block; color: var(--t1); }

/* 기간 탭 */
.period-tabs { display: flex; gap: 4px; }
.period-tab { padding: 4px 10px; border-radius: 6px; border: 1px solid var(--border); background: transparent; font-size: 12px; font-weight: 500; color: var(--t2); cursor: pointer; transition: all .15s; }
.period-tab.active { background: var(--accent); border-color: var(--accent); color: #fff; font-weight: 700; }
.period-tab:not(.active):hover { border-color: var(--accent); color: var(--accent); }

/* 통계 테이블 */
.stats-loading { display: flex; align-items: center; gap: 8px; color: var(--t3); font-size: 14px; padding: 24px 0; justify-content: center; }
@keyframes spin { to { transform: rotate(360deg); } }
.spinning { display: inline-block; animation: spin 0.8s linear infinite; }
.stats-table-wrap { overflow-x: auto; }
.stats-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.stats-table th { padding: 8px 10px; text-align: right; font-size: 12px; font-weight: 700; color: var(--t3); background: var(--surface2); border-bottom: 1px solid var(--border2); white-space: nowrap; }
.stats-table th:first-child { text-align: left; }
.stats-table td { padding: 8px 10px; text-align: right; color: var(--t1); border-bottom: 0.5px solid var(--border2); white-space: nowrap; }
.stats-table tbody tr:last-child td { border-bottom: none; }
.stats-table tbody tr:hover td { background: var(--surface2); }
.stats-table tbody tr.today td { background: color-mix(in srgb, var(--accent) 5%, transparent); font-weight: 600; }
.stats-table .date-col { text-align: left; color: var(--t2); font-weight: 500; font-size: 12px; }
tfoot .summary-row td { background: var(--surface2); font-weight: 700; color: var(--t1); border-bottom: none; }
tfoot .summary-row:first-child td { border-top: 1.5px solid var(--border); }

/* 빠른 이동 */
.quick-links { background: var(--surface); border: 0.5px solid var(--border2); border-radius: var(--radius-sm); padding: 18px; box-shadow: var(--shadow); }
.ql-title { font-size: 15px; font-weight: 800; color: var(--t1); margin-bottom: 12px; }
.ql-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 10px; }
.ql-item { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 14px; border: 0.5px solid var(--border); border-radius: var(--radius-sm); color: var(--t2); font-size: 13px; transition: var(--transition); text-decoration: none; }
.ql-item i { font-size: 22px; color: var(--accent); }
.ql-item:hover { background: var(--accent-bg); color: var(--accent); border-color: var(--accent); }

@media (max-width: 1100px) {
  .two-col { grid-template-columns: 1fr; }
}
@media (max-width: 900px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .ql-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 600px) {
  .ql-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
