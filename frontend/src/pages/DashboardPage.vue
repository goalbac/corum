<template>
  <div class="dashboard">

    <!-- 웰컴 카드 -->
    <div
      class="welcome-card"
      ref="welcomeRef"
      @mousemove="handleMouseMove"
      @mouseleave="handleMouseLeave"
    >
      <div class="welcome-glow" :style="glowStyle" />
      <div class="welcome-content">
        <div class="welcome-left">
          <div class="welcome-name">안녕하세요, <span class="welcome-highlight">{{ authStore.member?.name || '방문자' }}</span>님</div>
          <div class="welcome-sub">오늘도 좋은 하루 보내세요 ☀️</div>
        </div>
        <div class="welcome-right">
          <div class="welcome-date-line">{{ todayDate }}</div>
          <div class="welcome-time-line">{{ todayTime }}</div>
        </div>
      </div>
    </div>

    <!-- 위젯 영역 -->
    <div v-if="!loading && widgets.length" class="widget-area">
      <template v-for="widget in widgets" :key="widget.id">

        <!-- 이미지 슬라이더 -->
        <div v-if="widget.widgetType === 'IMAGE_SLIDER'" class="widget-full">
          <ImageSlider :slides="parseConfig(widget).slides || []" :title="widget.title" />
        </div>

        <!-- 최신 글 -->
        <div v-else-if="widget.widgetType === 'RECENT_POSTS'" class="widget-half">
          <div class="wcard">
            <div class="wcard-head">
              <span class="wcard-title">{{ widget.title || widget.targetBoardName || '최신글' }}</span>
              <router-link v-if="widget.targetBoardId" :to="boardListPath(widget)" class="wcard-more">
                더보기 <i class="ti ti-arrow-right"></i>
              </router-link>
            </div>
            <div v-if="widget.posts?.length" class="post-list">
              <router-link
                v-for="post in widget.posts"
                :key="post.id"
                :to="postPath(widget, post)"
                class="post-row"
              >
                <span class="post-title-txt">{{ post.title }}</span>
                <span class="post-date-txt">{{ formatDate(post.createdAt) }}</span>
              </router-link>
            </div>
            <div v-else class="wcard-empty">등록된 글이 없습니다.</div>
          </div>
        </div>

        <!-- 링크 모음 -->
        <div v-else-if="widget.widgetType === 'LINK_LIST'" class="widget-half">
          <div class="wcard">
            <div class="wcard-head">
              <span class="wcard-title">{{ widget.title || '링크 모음' }}</span>
            </div>
            <div v-if="parseConfig(widget).links?.length" class="link-grid">
              <a
                v-for="(link, i) in parseConfig(widget).links"
                :key="i"
                :href="link.url"
                :target="link.newWindow ? '_blank' : undefined"
                class="link-chip"
              >
                <span>{{ link.label }}</span>
                <i class="ti ti-arrow-up-right link-arrow"></i>
              </a>
            </div>
            <div v-else class="wcard-empty">링크가 없습니다.</div>
          </div>
        </div>

        <!-- 회원 현황 / 접속 통계 -->
        <div v-else-if="widget.widgetType === 'MEMBER_STATS' || widget.widgetType === 'VISIT_STATS'" class="widget-full">
          <div class="wcard stats-wcard">
            <div class="wcard-head">
              <span class="wcard-title">{{ widget.title || '사이트 현황' }}</span>
            </div>
            <div class="stats-row">
              <div class="stat-box">
                <div class="stat-num">{{ widget.stats?.memberCount ?? 0 }}</div>
                <div class="stat-lbl"><i class="ti ti-users"></i> 전체 회원</div>
              </div>
              <div class="stat-divider" />
              <div class="stat-box">
                <div class="stat-num">{{ widget.stats?.boardCount ?? 0 }}</div>
                <div class="stat-lbl"><i class="ti ti-layout-list"></i> 게시판</div>
              </div>
              <div class="stat-divider" />
              <div class="stat-box">
                <div class="stat-num accent">{{ widget.stats?.pendingInquiryCount ?? 0 }}</div>
                <div class="stat-lbl"><i class="ti ti-mail"></i> 미처리 문의</div>
              </div>
              <div class="stat-divider" />
              <div class="stat-box">
                <div class="stat-num">{{ widget.stats?.todayVisits ?? 0 }}</div>
                <div class="stat-lbl"><i class="ti ti-chart-bar"></i> 오늘 방문</div>
              </div>
            </div>
          </div>
        </div>

      </template>
    </div>

    <div v-else-if="loading" class="loading-area">
      <i class="ti ti-loader-2 spinning"></i>
    </div>

    <div v-else class="empty-state">
      <i class="ti ti-layout-dashboard"></i>
      <p>표시할 대시보드 위젯이 없습니다.</p>
    </div>

  </div>
</template>

<script setup>
import { onMounted, ref, reactive, onBeforeUnmount } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import api from '@/api/axios'
import ImageSlider from '@/components/common/ImageSlider.vue'

const authStore = useAuthStore()
const menuStore = useMenuStore()
const widgets   = ref([])
const loading   = ref(true)

// ===== 웰컴 카드 마우스 glow =====
const welcomeRef = ref(null)
const glowStyle  = ref({})

function handleMouseMove(e) {
  const rect = welcomeRef.value?.getBoundingClientRect()
  if (!rect) return
  const x = ((e.clientX - rect.left) / rect.width) * 100
  const y = ((e.clientY - rect.top) / rect.height) * 100
  glowStyle.value = {
    background: `radial-gradient(circle at ${x}% ${y}%, rgba(255,255,255,0.25) 0%, transparent 55%)`,
    opacity: 1
  }
}
function handleMouseLeave() { glowStyle.value = { opacity: 0 } }

// ===== 날짜/시각 =====
const todayDate = new Date().toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
const todayTime = ref(new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' }))
const clockTimer = setInterval(() => {
  todayTime.value = new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
}, 10000)
onBeforeUnmount(() => clearInterval(clockTimer))

// ===== 경로 헬퍼 =====
function boardListPath(widget) {
  const menu = menuStore.findBoardMenu(widget.targetBoardId)
  return menu ? `/menu/${menu.id}` : `/board/${widget.targetBoardId}`
}
function postPath(widget, post) {
  return `${boardListPath(widget)}/posts/${post.id}`
}
function parseConfig(widget) {
  try { return widget.extraConfig ? JSON.parse(widget.extraConfig) : {} }
  catch { return {} }
}

function formatDate(d) {
  if (!d) return ''
  const dt  = new Date(d)
  const now = new Date()
  if (dt.toDateString() === now.toDateString()) return dt.toTimeString().slice(0, 5)
  const gap = Math.floor((now - dt) / 86400000)
  if (gap === 1) return '1일 전'
  if (gap <= 3) return `${gap}일 전`
  return `${dt.getMonth() + 1}.${String(dt.getDate()).padStart(2, '0')}`
}

onMounted(async () => {
  try {
    await menuStore.fetchMenus()
    const res = await api.get('/dashboard/widgets')
    widgets.value = res.data.data || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ===== 웰컴 카드 ===== */
.welcome-card {
  position: relative;
  background: linear-gradient(135deg, #4f6ef7 0%, #7c4ff7 45%, #e05fc4 80%, #f97040 100%);
  border-radius: var(--radius-sm);
  padding: 28px 32px;
  overflow: hidden;
  cursor: default;
  isolation: isolate;
}
.welcome-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 10% 80%, rgba(255,255,255,0.13) 0%, transparent 45%),
    radial-gradient(circle at 90% 15%, rgba(255,255,255,0.1) 0%, transparent 40%);
  z-index: 0;
}
.welcome-glow {
  position: absolute;
  inset: 0;
  pointer-events: none;
  transition: opacity 0.3s;
  z-index: 1;
}
.welcome-content {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.welcome-name {
  font-size: 20px;
  font-weight: 800;
  color: #fff;
  letter-spacing: -0.4px;
}
.welcome-highlight {
  display: inline-block;
  background: rgba(255,255,255,0.22);
  border-radius: 7px;
  padding: 2px 10px;
}
.welcome-sub {
  font-size: 14px;
  color: rgba(255,255,255,0.8);
  margin-top: 7px;
}
.welcome-right {
  text-align: right;
  flex-shrink: 0;
}
.welcome-date-line {
  font-size: 13.5px;
  color: rgba(255,255,255,0.8);
  line-height: 1.6;
}
.welcome-time-line {
  font-size: 28px;
  font-weight: 800;
  color: #fff;
  letter-spacing: -0.5px;
  line-height: 1.2;
}

/* ===== 위젯 그리드 ===== */
.widget-area {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.widget-full  { grid-column: 1 / -1; }
.widget-half  { grid-column: span 1; }

/* ===== 공통 위젯 카드 ===== */
.wcard {
  background: var(--surface);
  border-radius: var(--radius-sm);
  border: 0.5px solid var(--border2);
  box-shadow: var(--shadow);
  padding: 20px 22px;
  height: 100%;
  box-sizing: border-box;
}
.wcard-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.wcard-title {
  font-size: 15px;
  font-weight: 800;
  color: var(--t1);
  letter-spacing: -0.2px;
}
.wcard-more {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 13px;
  font-weight: 600;
  color: var(--accent-t);
  text-decoration: none;
  transition: opacity 0.15s;
}
.wcard-more:hover { opacity: 0.75; }
.wcard-empty {
  font-size: 14px;
  color: var(--t3);
  text-align: center;
  padding: 20px 0;
}

/* ===== 최신 글 ===== */
.post-list { display: flex; flex-direction: column; }
.post-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 0;
  border-bottom: 0.5px solid var(--border2);
  gap: 12px;
  text-decoration: none;
  transition: background 0.12s;
  border-radius: 4px;
}
.post-row:first-child { padding-top: 0; }
.post-row:last-child  { border-bottom: none; padding-bottom: 0; }
.post-row:hover .post-title-txt { color: var(--accent-t); }
.post-title-txt {
  font-size: 14.5px;
  font-weight: 500;
  color: var(--t1);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.15s;
}
.post-date-txt {
  font-size: 12.5px;
  color: var(--t3);
  flex-shrink: 0;
}

/* ===== 링크 모음 ===== */
.link-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}
.link-chip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 11px 14px;
  border: 0.5px solid var(--border);
  border-radius: var(--radius-xs);
  background: var(--surface2);
  color: var(--t1);
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  transition: var(--transition);
}
.link-chip:hover {
  border-color: var(--accent);
  background: var(--accent-bg);
  color: var(--accent-t);
}
.link-arrow {
  font-size: 13px;
  color: var(--t3);
  flex-shrink: 0;
  transition: color 0.15s;
}
.link-chip:hover .link-arrow { color: var(--accent-t); }

/* ===== 통계 ===== */
.stats-wcard { padding: 22px 28px; }
.stats-row {
  display: flex;
  align-items: center;
  gap: 0;
}
.stat-box {
  flex: 1;
  text-align: center;
  padding: 16px 10px;
}
.stat-divider {
  width: 1px;
  height: 52px;
  background: var(--border2);
  flex-shrink: 0;
}
.stat-num {
  font-size: 30px;
  font-weight: 800;
  color: var(--t1);
  letter-spacing: -1px;
  line-height: 1;
  margin-bottom: 8px;
}
.stat-num.accent { color: var(--color-danger, #e53e3e); }
.stat-lbl {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  font-size: 13px;
  color: var(--t3);
  font-weight: 500;
}
.stat-lbl i { font-size: 14px; }

/* ===== 로딩 / 빈 상태 ===== */
.loading-area {
  display: flex;
  justify-content: center;
  padding: 60px;
  font-size: 28px;
  color: var(--t3);
}
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 60px;
  color: var(--t3);
  font-size: 15px;
}
.empty-state i { font-size: 40px; }

@keyframes spin { to { transform: rotate(360deg); } }
.spinning { animation: spin 0.8s linear infinite; display: inline-block; }

/* ===== 반응형 ===== */
@media (max-width: 768px) {
  .widget-area { grid-template-columns: 1fr; }
  .widget-half { grid-column: 1; }
  .welcome-card { padding: 22px 20px; }
  .welcome-content { flex-direction: column; align-items: flex-start; gap: 10px; }
  .welcome-right { text-align: left; }
  .welcome-time-line { font-size: 22px; }
  .welcome-name { font-size: 17px; }
  .stats-row { flex-wrap: wrap; }
  .stat-divider { display: none; }
  .stat-box { min-width: 45%; }
}
</style>
