<template>
  <div class="dashboard">
    <div
      class="welcome-card"
      ref="welcomeRef"
      @mousemove="handleMouseMove"
      @mouseleave="handleMouseLeave"
    >
      <div class="welcome-glow" :style="glowStyle" />
      <div class="welcome-left">
        <div class="welcome-name">안녕하세요, <span class="welcome-highlight">{{ authStore.member?.name || '방문자' }}</span>님</div>
        <div class="welcome-sub">오늘도 좋은 하루 보내세요.</div>
      </div>
      <div class="welcome-date">{{ today }}</div>
    </div>

    <div class="widget-grid" v-if="widgets.length">
      <section
        v-for="widget in widgets"
        :key="widget.id"
        class="widget-card"
        :class="`widget-${widget.widgetType?.toLowerCase()}`"
      >
        <template v-if="widget.widgetType === 'RECENT_POSTS'">
          <div class="widget-head">
            <span class="widget-name">{{ widget.title || widget.targetBoardName || '최신글' }}</span>
            <router-link v-if="widget.targetBoardId" :to="boardListPath(widget)" class="widget-more">더보기</router-link>
          </div>
          <div v-if="widget.posts?.length" class="post-list">
            <router-link
              v-for="post in widget.posts"
              :key="post.id"
              :to="postPath(widget, post)"
              class="post-row"
            >
              <span class="post-title">{{ post.title }}</span>
              <span class="post-date">{{ formatDate(post.createdAt) }}</span>
            </router-link>
          </div>
          <div v-else class="empty">등록된 글이 없습니다.</div>
        </template>

        <template v-else-if="widget.widgetType === 'IMAGE_SLIDER'">
          <div class="slider">
            <a
              v-for="(slide, index) in parseConfig(widget).slides || []"
              :key="index"
              :href="slide.url || undefined"
              :target="slide.newWindow ? '_blank' : undefined"
              class="slide"
            >
              <img :src="slide.imageUrl" :alt="slide.title || widget.title" />
              <span v-if="slide.title">{{ slide.title }}</span>
            </a>
          </div>
        </template>

        <template v-else-if="widget.widgetType === 'LINK_LIST'">
          <div class="widget-head">
            <span class="widget-name">{{ widget.title || '링크 모음' }}</span>
          </div>
          <div class="link-list">
            <a
              v-for="(link, index) in parseConfig(widget).links || []"
              :key="index"
              :href="link.url"
              :target="link.newWindow ? '_blank' : undefined"
              class="link-item"
            >
              <span>{{ link.label }}</span>
              <i class="ti ti-arrow-up-right"></i>
            </a>
          </div>
        </template>

        <template v-else-if="widget.widgetType === 'MEMBER_STATS' || widget.widgetType === 'VISIT_STATS'">
          <div class="widget-head">
            <span class="widget-name">{{ widget.title || '사이트 현황' }}</span>
          </div>
          <div class="stats-grid">
            <div class="stat-item">
              <strong>{{ widget.stats?.memberCount ?? 0 }}</strong>
              <span>회원</span>
            </div>
            <div class="stat-item">
              <strong>{{ widget.stats?.boardCount ?? 0 }}</strong>
              <span>게시판</span>
            </div>
            <div class="stat-item">
              <strong>{{ widget.stats?.pendingInquiryCount ?? 0 }}</strong>
              <span>미처리 문의</span>
            </div>
            <div class="stat-item">
              <strong>{{ widget.stats?.todayVisits ?? 0 }}</strong>
              <span>오늘 방문</span>
            </div>
          </div>
        </template>
      </section>
    </div>

    <div class="empty-state" v-else-if="!loading">
      <i class="ti ti-layout-dashboard"></i>
      <p>표시할 대시보드 위젯이 없습니다.</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, reactive } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import api from '@/api/axios'

const authStore = useAuthStore()
const menuStore = useMenuStore()
const widgets = ref([])
const loading = ref(true)

const welcomeRef = ref(null)
const mouse = reactive({ x: 50, y: 50 })

const glowStyle = ref({})

function handleMouseMove(e) {
  const rect = welcomeRef.value?.getBoundingClientRect()
  if (!rect) return
  const x = ((e.clientX - rect.left) / rect.width) * 100
  const y = ((e.clientY - rect.top) / rect.height) * 100
  mouse.x = x
  mouse.y = y
  glowStyle.value = {
    background: `radial-gradient(circle at ${x}% ${y}%, rgba(255,255,255,0.22) 0%, transparent 55%)`,
    opacity: 1
  }
}

function handleMouseLeave() {
  glowStyle.value = { opacity: 0 }
}

const today = new Date().toLocaleDateString('ko-KR', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
})

function boardListPath(widget) {
  const menu = menuStore.findBoardMenu(widget.targetBoardId)
  return menu ? `/menu/${menu.id}` : `/board/${widget.targetBoardId}`
}

function postPath(widget, post) {
  return `${boardListPath(widget)}/posts/${post.id}`
}

function parseConfig(widget) {
  try {
    return widget.extraConfig ? JSON.parse(widget.extraConfig) : {}
  } catch {
    return {}
  }
}

function formatDate(d) {
  if (!d) return ''
  const dt = new Date(d)
  const now = new Date()
  const diff = (now - dt) / 1000
  if (diff < 86400 && dt.toDateString() === now.toDateString()) return dt.toTimeString().slice(0, 5)
  const gap = Math.floor((now - dt) / 86400000)
  if (gap === 0) return '오늘'
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
.dashboard { display: flex; flex-direction: column; gap: 16px; }

.welcome-card {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #4f6ef7 0%, #7c4ff7 40%, #e05fc4 80%, #f97040 100%);
  border-radius: var(--radius-sm);
  padding: 28px 28px;
  overflow: hidden;
  cursor: default;
  isolation: isolate;
}

/* 배경 패턴 */
.welcome-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 15% 85%, rgba(255,255,255,0.12) 0%, transparent 40%),
    radial-gradient(circle at 85% 20%, rgba(255,255,255,0.1) 0%, transparent 40%);
  z-index: 0;
}

.welcome-glow {
  position: absolute;
  inset: 0;
  pointer-events: none;
  transition: opacity 0.3s;
  z-index: 1;
}

.welcome-left, .welcome-date {
  position: relative;
  z-index: 2;
}

.welcome-name {
  font-size: 20px;
  font-weight: 800;
  color: #fff;
  letter-spacing: -0.3px;
}

.welcome-highlight {
  background: rgba(255,255,255,0.22);
  border-radius: 6px;
  padding: 1px 8px;
  margin: 0 1px;
}

.welcome-sub { font-size: 14px; color: rgba(255,255,255,0.8); margin-top: 6px; }
.welcome-date { font-size: 14px; color: rgba(255,255,255,0.75); text-align: right; line-height: 1.7; }

.widget-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.widget-card {
  background: var(--surface);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow);
  border: 0.5px solid var(--border2);
  padding: 18px;
  transition: background 0.25s;
}

.widget-image_slider {
  grid-column: 1 / -1;
  padding: 0;
  overflow: hidden;
}

.widget-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.widget-name { font-size: 16px; font-weight: 800; color: var(--t1); }
.widget-more { font-size: 14px; color: var(--accent-t); font-weight: 700; }

.post-list { display: flex; flex-direction: column; }

.post-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 0.5px solid var(--border2);
  gap: 10px;
  transition: var(--transition);
}

.post-row:last-child { border-bottom: none; padding-bottom: 0; }
.post-row:first-child { padding-top: 0; }
.post-row:hover .post-title { color: var(--accent-t); }

.post-title {
  font-size: 15px;
  color: var(--t1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  transition: color 0.15s;
}

.post-date {
  font-size: 13px;
  color: var(--t3);
  flex-shrink: 0;
}

.slider {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: 100%;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
}

.slide {
  position: relative;
  display: block;
  min-height: 220px;
  scroll-snap-align: start;
  background: var(--surface2);
}

.slide img {
  width: 100%;
  height: 260px;
  object-fit: cover;
  display: block;
}

.slide span {
  position: absolute;
  left: 20px;
  bottom: 18px;
  color: #fff;
  font-size: 22px;
  font-weight: 800;
  text-shadow: 0 2px 8px rgba(0,0,0,0.45);
}

.link-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.link-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 42px;
  padding: 10px 12px;
  border: 0.5px solid var(--border);
  border-radius: var(--radius-xs);
  color: var(--t1);
  background: var(--surface2);
  font-size: 15px;
  font-weight: 700;
}

.link-item:hover {
  color: var(--accent-t);
  border-color: var(--accent);
  background: var(--accent-bg);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.stat-item {
  padding: 14px 10px;
  border-radius: var(--radius-xs);
  background: var(--surface2);
  text-align: center;
}

.stat-item strong {
  display: block;
  font-size: 22px;
  font-weight: 800;
  color: var(--accent-t);
}

.stat-item span {
  display: block;
  margin-top: 2px;
  font-size: 13px;
  color: var(--t2);
}

.empty {
  font-size: 15px;
  color: var(--t3);
  padding: 12px 0;
  text-align: center;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 60px 0;
  color: var(--t3);
  font-size: 15px;
}

.empty-state i {
  font-size: 40px;
  color: var(--t3);
}

@media (max-width: 768px) {
  .widget-grid { grid-template-columns: 1fr; }
  .welcome-card { flex-direction: column; align-items: flex-start; gap: 8px; padding: 22px 20px; }
  .welcome-date { text-align: left; font-size: 13px; }
  .welcome-name { font-size: 17px; }
  .link-list,
  .stats-grid { grid-template-columns: 1fr; }
}
</style>
