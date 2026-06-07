<template>
  <div class="board-detail" v-loading="loading">
    <template v-if="post">

      <!-- 게시글 헤더 -->
      <div class="post-header">
        <el-tag v-if="post.isNotice" type="danger" effect="plain" size="small">공지</el-tag>
        <h1 class="post-title">{{ post.title }}</h1>
        <div class="post-meta">
          <span>{{ post.writerName }}</span>
          <span class="meta-divider">·</span>
          <span>{{ formatDate(post.createdAt) }}</span>
          <span class="meta-divider">·</span>
          <span>조회 {{ post.viewCount }}</span>
        </div>
      </div>

      <!-- 첨부파일 -->
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

      <!-- 본문 -->
      <div class="post-content" v-html="post.content" />

      <!-- 액션 영역 -->
      <div class="post-actions">
        <el-button
          :type="post.liked ? 'primary' : 'default'"
          size="small"
          @click="handleLike"
        >
          <el-icon><Star /></el-icon>
          좋아요 {{ post.likeCount }}
        </el-button>
        <el-button size="small" @click="handlePrint">
          <el-icon><Printer /></el-icon> 인쇄
        </el-button>
        <div style="margin-left: auto; display: flex; gap: 8px;">
          <el-button
            v-if="canEdit"
            size="small"
            @click="$router.push(`/board/${boardId}/posts/${postId}/edit`)"
          >수정</el-button>
          <el-button
            v-if="canEdit"
            size="small"
            type="danger"
            @click="handleDelete"
          >삭제</el-button>
          <el-button size="small" @click="$router.back()">목록</el-button>
        </div>
      </div>

      <!-- 댓글 -->
      <CommentSection :post-id="postId" />

    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Paperclip, Document, Star, Printer } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import api from '@/api/axios'
import CommentSection from '@/components/board/CommentSection.vue'

const route    = useRoute()
const router   = useRouter()
const authStore = useAuthStore()

const boardId  = computed(() => route.params.boardId)
const postId   = computed(() => route.params.postId)
const post     = ref(null)
const loading  = ref(false)

const canEdit = computed(() => {
  if (!authStore.isLoggedIn) return false
  if (authStore.member?.admin) return true
  return authStore.member?.id === post.value?.memberId
})

async function fetchPost() {
  loading.value = true
  try {
    const res = await api.get(`/boards/${boardId.value}/posts/${postId.value}`)
    post.value = res.data.data
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
  router.push(`/board/${boardId.value}`)
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

onMounted(fetchPost)
</script>

<style scoped>
.board-detail {
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.post-header {
  padding: 24px 28px 20px;
  border-bottom: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.post-title {
  font-size: 20px;
  font-weight: 700;
  line-height: 1.4;
}

.post-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.meta-divider {
  color: var(--color-border);
}

.attach-area {
  padding: 12px 28px;
  background: var(--color-bg-base);
  border-bottom: 1px solid var(--color-border);
}

.attach-title {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.attach-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.attach-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-primary);
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  transition: var(--transition);
  width: fit-content;
}

.attach-item:hover {
  background: var(--color-primary-bg);
}

.file-size {
  color: var(--color-text-muted);
  font-size: 12px;
}

.post-content {
  padding: 28px;
  min-height: 200px;
  font-size: 14px;
  line-height: 1.8;
  border-bottom: 1px solid var(--color-border);
}

.post-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 28px;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-base);
}
</style>