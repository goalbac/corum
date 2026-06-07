<template>
  <div class="dashboard">
    <div class="welcome-card">
      <div class="welcome-left">
        <div class="welcome-name">안녕하세요, {{ authStore.member?.name || '방문자' }}님</div>
        <div class="welcome-sub">오늘도 좋은 하루 보내세요.</div>
      </div>
      <div class="welcome-date">{{ today }}</div>
    </div>

    <div class="widget-grid" v-if="recentBoards.length">
      <div class="widget-card" v-for="board in recentBoards" :key="board.id">
        <div class="widget-head">
          <span class="widget-name">{{ board.name }}</span>
          <router-link :to="boardListPath(board)" class="widget-more">더보기</router-link>
        </div>
        <div v-if="board.posts?.length" class="post-list">
          <router-link
            v-for="post in board.posts"
            :key="post.id"
            :to="postPath(board, post)"
            class="post-row"
          >
            <span class="post-title">
              {{ post.title }}
              <span v-if="isNew(post.createdAt)" class="new-badge">N</span>
            </span>
            <span class="post-date">{{ formatDate(post.createdAt) }}</span>
          </router-link>
        </div>
        <div v-else class="empty">등록된 글이 없습니다.</div>
      </div>
    </div>

    <div class="empty-state" v-else-if="!loading">
      <i class="ti ti-layout-dashboard"></i>
      <p>아직 게시판이 없습니다.</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import api from '@/api/axios'

const authStore = useAuthStore()
const menuStore = useMenuStore()
const recentBoards = ref([])
const loading = ref(true)

const today = new Date().toLocaleDateString('ko-KR', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
})

function boardListPath(board) {
  return board.menuId ? `/menu/${board.menuId}` : `/board/${board.id}`
}

function postPath(board, post) {
  return `${boardListPath(board)}/posts/${post.id}`
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

function isNew(d) {
  if (!d) return false
  return Date.now() - new Date(d).getTime() < 3 * 86400000
}

onMounted(async () => {
  try {
    await menuStore.fetchMenus()
    const res = await api.get('/boards')
    const boards = (res.data.data || []).slice(0, 4)
    recentBoards.value = await Promise.all(boards.map(async board => {
      const menu = menuStore.findBoardMenu(board.id)
      try {
        const pr = await api.get(`/boards/${board.id}/posts?size=5`)
        return { ...board, menuId: menu?.id, posts: pr.data.data?.content || [] }
      } catch {
        return { ...board, menuId: menu?.id, posts: [] }
      }
    }))
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.dashboard { display: flex; flex-direction: column; gap: 16px; }

.welcome-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--accent);
  border-radius: var(--radius-sm);
  padding: 20px 24px;
}

.welcome-name { font-size: 17px; font-weight: 800; color: #fff; }
.welcome-sub { font-size: 14px; color: rgba(255,255,255,0.76); margin-top: 4px; }
.welcome-date { font-size: 14px; color: rgba(255,255,255,0.7); text-align: right; line-height: 1.7; }

.widget-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.widget-card {
  background: var(--surface);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow);
  border: 0.5px solid var(--border2);
  padding: 18px 18px 10px;
  transition: background 0.25s;
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
  .welcome-card { flex-direction: column; align-items: flex-start; gap: 8px; }
  .welcome-date { text-align: left; font-size: 13px; }
}
</style>
