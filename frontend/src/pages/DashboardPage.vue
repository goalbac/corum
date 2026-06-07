<template>
  <div class="dashboard">

    <!-- 환영 카드 -->
    <el-card class="welcome-card" shadow="never">
      <div class="welcome-inner">
        <div>
          <h2 class="welcome-title">안녕하세요, {{ authStore.member?.name || '방문자' }}님 👋</h2>
          <p class="welcome-sub">오늘도 좋은 하루 되세요.</p>
        </div>
        <div class="welcome-date">{{ today }}</div>
      </div>
    </el-card>

    <!-- 최신글 위젯 -->
    <div class="widget-grid">
      <el-card
        v-for="board in recentBoards"
        :key="board.id"
        class="widget-card"
        shadow="never"
      >
        <template #header>
          <div class="widget-header">
            <span class="widget-title">{{ board.name }}</span>
            <router-link :to="`/board/${board.id}`" class="widget-more">더보기</router-link>
          </div>
        </template>

        <div v-if="board.posts?.length" class="post-list">
          <router-link
            v-for="post in board.posts"
            :key="post.id"
            :to="`/board/${board.id}/posts/${post.id}`"
            class="post-item"
          >
            <span class="post-title">{{ post.title }}</span>
            <span class="post-date">{{ formatDate(post.createdAt) }}</span>
          </router-link>
        </div>
        <div v-else class="empty-msg">등록된 글이 없습니다.</div>
      </el-card>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import api from '@/api/axios'

const authStore = useAuthStore()
const recentBoards = ref([])

const today = new Date().toLocaleDateString('ko-KR', {
  year: 'numeric', month: 'long', day: 'numeric', weekday: 'long'
})

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}.${d.getDate()}`
}

onMounted(async () => {
  try {
    const boardsRes = await api.get('/boards')
    const boards = boardsRes.data.data?.slice(0, 4) || []
    recentBoards.value = await Promise.all(
      boards.map(async (b) => {
        try {
          const postsRes = await api.get(`/boards/${b.id}/posts?size=5`)
          return { ...b, posts: postsRes.data.data?.content || [] }
        } catch {
          return { ...b, posts: [] }
        }
      })
    )
  } catch (e) {
    console.error(e)
  }
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.welcome-card {
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  border: none;
  color: #fff;
}

.welcome-card :deep(.el-card__body) {
  padding: 24px 28px;
}

.welcome-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.welcome-title {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
}

.welcome-sub {
  font-size: 13px;
  color: rgba(255,255,255,0.8);
  margin-top: 4px;
}

.welcome-date {
  font-size: 13px;
  color: rgba(255,255,255,0.7);
}

.widget-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.widget-card {
  border-radius: var(--radius-lg);
}

.widget-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.widget-title {
  font-size: 14px;
  font-weight: 600;
}

.widget-more {
  font-size: 12px;
  color: var(--color-primary);
}

.post-list {
  display: flex;
  flex-direction: column;
}

.post-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--color-border);
  gap: 12px;
  transition: var(--transition);
}

.post-item:last-child { border-bottom: none; }

.post-item:hover .post-title {
  color: var(--color-primary);
}

.post-title {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  transition: var(--transition);
}

.post-date {
  font-size: 12px;
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.empty-msg {
  font-size: 13px;
  color: var(--color-text-muted);
  text-align: center;
  padding: 16px 0;
}
</style>
