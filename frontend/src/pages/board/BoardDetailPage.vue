<template>
  <div class="board-detail" v-loading="loading">
    <template v-if="post">
      <div class="post-header">
        <div v-if="post.isNotice" class="notice-badge">공지</div>
        <h1 class="post-title">{{ post.title }}</h1>
        <div class="post-meta">
          <span class="writer-info">
            <img
              v-if="post.writerProfileImageUrl && !postAvatarError"
              :src="post.writerProfileImageUrl"
              class="writer-avatar"
              alt=""
              @error="postAvatarError = true"
            />
            <span v-else class="writer-avatar-placeholder">{{ post.writerName?.charAt(0) || 'U' }}</span>
            <span class="writer-name">{{ post.writerName }}</span>
          </span>
          <span class="meta-divider">·</span>
          <span>{{ formatDate(post.createdAt) }}</span>
          <span class="meta-divider">·</span>
          <span>조회 {{ post.viewCount }}</span>
        </div>
      </div>

      <div v-if="post.files?.length" class="attach-area">
        <div class="attach-title">
          <el-icon><Paperclip /></el-icon> 첨부파일 ({{ post.files.length }})
        </div>
        <div class="attach-list">
          <a
            v-for="f in post.files"
            :key="f.id"
            :href="f.downloadUrl"
            class="attach-item"
            target="_blank"
          >
            <el-icon><Document /></el-icon>
            <span>{{ f.originalName }}</span>
            <span class="file-size">({{ formatFileSize(f.fileSize) }})</span>
          </a>
        </div>
      </div>

      <div class="post-content" v-html="post.content" />

      <!-- 액션 바 -->
      <div class="post-actions">
        <div class="actions-left">
          <button
            v-if="board?.useLike"
            :class="['like-btn', { liked: post.liked }]"
            @click="handleLike"
          >
            <i :class="post.liked ? 'ti ti-heart-filled' : 'ti ti-heart'"></i>
            <span>좋아요 {{ post.likeCount }}</span>
          </button>
          <button class="action-btn" @click="handlePrint">
            <i class="ti ti-printer"></i>
            <span>인쇄</span>
          </button>
        </div>
        <div class="actions-right">
          <button v-if="canEdit" class="action-btn" @click="router.push(`${basePath}/posts/${postId}/edit`)">
            <i class="ti ti-edit"></i>
            <span>수정</span>
          </button>
          <button v-if="canEdit" class="action-btn danger" @click="handleDelete">
            <i class="ti ti-trash"></i>
            <span>삭제</span>
          </button>
          <button class="action-btn accent" @click="router.push(basePath)">
            <i class="ti ti-layout-list"></i>
            <span>목록</span>
          </button>
        </div>
      </div>

      <!-- 이전/다음 글 -->
      <div v-if="adjacent" class="adjacent-nav">
        <router-link
          v-if="adjacent.next"
          :to="`${basePath}/posts/${adjacent.next.id}`"
          class="adj-item"
        >
          <span class="adj-label"><i class="ti ti-chevron-up"></i> 다음 글</span>
          <span class="adj-title">{{ adjacent.next.title }}</span>
        </router-link>
        <div v-else class="adj-item empty"><span class="adj-label"><i class="ti ti-chevron-up"></i> 다음 글</span><span class="adj-empty">없음</span></div>
        <router-link
          v-if="adjacent.prev"
          :to="`${basePath}/posts/${adjacent.prev.id}`"
          class="adj-item"
        >
          <span class="adj-label"><i class="ti ti-chevron-down"></i> 이전 글</span>
          <span class="adj-title">{{ adjacent.prev.title }}</span>
        </router-link>
        <div v-else class="adj-item empty"><span class="adj-label"><i class="ti ti-chevron-down"></i> 이전 글</span><span class="adj-empty">없음</span></div>
      </div>

      <CommentSection
        :board-id="boardId"
        :post-id="postId"
        :use-comment="board?.useComment ?? true"
        :can-comment="canComment"
        :is-admin="isAdmin"
      />
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Paperclip, Document } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import api from '@/api/axios'
import CommentSection from '@/components/board/CommentSection.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const menuStore = useMenuStore()

const activeMenu = computed(() => menuStore.findMenuById(route.params.menuId))
const boardId = computed(() => route.params.boardId || activeMenu.value?.targetId)
const postId = computed(() => route.params.postId)
const basePath = computed(() => route.params.menuId ? `/menu/${route.params.menuId}` : `/board/${boardId.value}`)
const post = ref(null)
const board = ref(null)
const adjacent = ref(null)
const loading = ref(false)
const postAvatarError = ref(false)

const isAdmin = computed(() => !!authStore.member?.isAdmin)

const canEdit = computed(() => {
  if (!authStore.isLoggedIn) return false
  if (isAdmin.value) return true
  return authStore.member?.id === post.value?.memberId
})

const canComment = computed(() => {
  if (isAdmin.value) return true
  const perms = board.value?.permissions || []
  if (!perms.length) return authStore.isLoggedIn
  const memberGroupIds = authStore.member?.groupIds || []
  return perms.some(p => memberGroupIds.includes(p.groupId) && p.canComment)
})

async function fetchPost() {
  if (!boardId.value || !postId.value) return
  loading.value = true
  try {
    const [postRes, boardRes, adjRes] = await Promise.all([
      api.get(`/boards/${boardId.value}/posts/${postId.value}`),
      board.value ? Promise.resolve(null) : api.get(`/boards/${boardId.value}`),
      api.get(`/boards/${boardId.value}/posts/${postId.value}/adjacent`)
    ])
    post.value = postRes.data.data
    if (boardRes) board.value = boardRes.data.data
    adjacent.value = adjRes.data.data
    postAvatarError.value = false
  } finally {
    loading.value = false
  }
}

async function handleLike() {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('로그인이 필요합니다.')
    return
  }
  const res = await api.post(`/boards/${boardId.value}/posts/${postId.value}/like`)
  post.value.liked = res.data.data.liked
  post.value.likeCount += res.data.data.liked ? 1 : -1
}

async function handleDelete() {
  await ElMessageBox.confirm('게시글을 삭제하시겠습니까?', '삭제 확인', {
    type: 'warning',
    confirmButtonText: '삭제',
    cancelButtonText: '취소'
  })
  await api.delete(`/boards/${boardId.value}/posts/${postId.value}`)
  ElMessage.success('삭제되었습니다.')
  router.push(basePath.value)
}

function handlePrint() {
  window.print()
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('ko-KR')
}

function formatFileSize(bytes) {
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / (1024 * 1024)).toFixed(1) + 'MB'
}

watch([boardId, postId], fetchPost)

onMounted(async () => {
  if (route.params.menuId) await menuStore.fetchMenus()
  fetchPost()
})
</script>

<style scoped>
.board-detail {
  color: var(--t1);
}

.post-header {
  padding: 28px 30px 22px;
  border-bottom: 1px solid var(--border2);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notice-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 4px;
  background: var(--new);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.5px;
  width: fit-content;
}

.post-title {
  font-size: 24px;
  font-weight: 800;
  line-height: 1.45;
  color: var(--t1);
}

.post-meta {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 15px;
  color: var(--t2);
  flex-wrap: wrap;
}

.meta-divider { color: var(--t3); }

.writer-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.writer-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.writer-avatar-placeholder {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--accent);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.writer-name {
  font-weight: 600;
  color: var(--t1);
}

.attach-area {
  padding: 14px 30px;
  background: var(--surface2);
  border-bottom: 1px solid var(--border2);
}

.attach-title {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 8px;
  color: var(--t1);
}

.attach-list {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.attach-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  color: var(--accent-t);
  padding: 5px 8px;
  border-radius: var(--radius-xs);
  transition: var(--transition);
  width: fit-content;
}

.attach-item:hover { background: var(--accent-bg); }
.file-size { color: var(--t3); font-size: 14px; }

.post-content {
  padding: 30px;
  min-height: 220px;
  font-size: 16px;
  line-height: 1.85;
  color: var(--t1);
}

.post-content :deep(p) { margin: 0 0 0.6em; }
.post-content :deep(h1) { font-size: 1.8em; font-weight: 800; margin: 1em 0 0.4em; }
.post-content :deep(h2) { font-size: 1.4em; font-weight: 700; margin: 0.9em 0 0.4em; }
.post-content :deep(h3) { font-size: 1.15em; font-weight: 700; margin: 0.8em 0 0.3em; }
.post-content :deep(ul) { padding-left: 1.5em; margin: 0.4em 0; }
.post-content :deep(ol) { padding-left: 1.5em; margin: 0.4em 0; }
.post-content :deep(li) { margin: 0.2em 0; }
.post-content :deep(blockquote) {
  border-left: 3px solid var(--accent);
  padding-left: 16px;
  color: var(--t2);
  margin: 0.8em 0;
}
.post-content :deep(code) {
  background: var(--surface2);
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 0.9em;
  color: var(--accent);
}
.post-content :deep(pre) {
  background: var(--surface2);
  border-radius: var(--radius-xs);
  padding: 16px;
  overflow-x: auto;
  margin: 0.8em 0;
}
.post-content :deep(pre code) { background: none; padding: 0; color: var(--t1); font-size: 14px; }
.post-content :deep(a) { color: var(--accent); text-decoration: underline; }
.post-content :deep(img) { max-width: 100%; height: auto; border-radius: var(--radius-xs); }
.post-content :deep(hr) { border: none; border-top: 1px solid var(--border); margin: 1em 0; }
.post-content :deep(table) { max-width: 100%; border-color: var(--border); }

/* ===== 액션 바 ===== */
.post-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 14px 30px;
  border-top: 1px solid var(--border2);
  border-bottom: 1px solid var(--border2);
  background: var(--surface2);
}

.actions-left,
.actions-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.action-btn,
.like-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  border: 0.5px solid var(--border);
  border-radius: var(--radius-xs);
  background: var(--surface);
  color: var(--t2);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
  white-space: nowrap;
}

.action-btn:hover { background: var(--surface2); color: var(--t1); border-color: var(--border2); }
.action-btn.danger { color: var(--el-color-danger); }
.action-btn.danger:hover { background: #fff1f0; border-color: var(--el-color-danger); }
.action-btn.accent { background: var(--accent); color: #fff; border-color: var(--accent); }
.action-btn.accent:hover { opacity: 0.88; }

.like-btn { color: var(--t3); }
.like-btn:hover { color: #e03e52; border-color: #e03e52; background: #fff5f6; }
.like-btn.liked { color: #e03e52; border-color: #e03e52; background: #fff5f6; }
.like-btn.liked i { color: #e03e52; }

/* ===== 이전/다음 글 ===== */
.adjacent-nav {
  border-bottom: 1px solid var(--border2);
}

.adj-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 30px;
  border-top: 1px solid var(--border2);
  color: var(--t2);
  transition: var(--transition);
  text-decoration: none;
}

a.adj-item:hover {
  background: var(--surface2);
  color: var(--t1);
}

a.adj-item:hover .adj-title {
  color: var(--accent-t);
}

.adj-item.empty {
  cursor: default;
  opacity: 0.5;
}

.adj-label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 700;
  color: var(--t3);
  white-space: nowrap;
  width: 72px;
  flex-shrink: 0;
}

.adj-title {
  font-size: 14px;
  color: var(--t1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.adj-empty {
  font-size: 13px;
  color: var(--t4);
}

@media (max-width: 768px) {
  .post-header,
  .attach-area,
  .post-content,
  .post-actions,
  .adj-item {
    padding-left: 18px;
    padding-right: 18px;
  }

  .post-title { font-size: 21px; }
  .post-actions { flex-wrap: wrap; gap: 8px; }
  .action-btn span, .like-btn span { display: none; }
  .action-btn, .like-btn { padding: 8px 10px; }
}
</style>
