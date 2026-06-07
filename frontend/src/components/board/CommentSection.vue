<template>
  <div class="comment-section">
    <div class="comment-header">
      댓글 <span class="comment-count">{{ comments.length }}</span>
    </div>

    <!-- 댓글 작성 -->
    <div v-if="authStore.isLoggedIn" class="comment-write">
      <el-input
        v-model="newComment"
        type="textarea"
        :rows="2"
        placeholder="댓글을 입력하세요."
        resize="none"
      />
      <el-button type="primary" size="small" @click="submitComment(null)">등록</el-button>
    </div>

    <!-- 댓글 목록 -->
    <div class="comment-list">
      <CommentItem
        v-for="comment in comments"
        :key="comment.id"
        :comment="comment"
        :post-id="postId"
        @refresh="fetchComments"
      />
    </div>

    <div v-if="!comments.length" class="no-comment">
      첫 번째 댓글을 작성해보세요.
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import api from '@/api/axios'
import CommentItem from './CommentItem.vue'

const props = defineProps({ postId: { type: [String, Number], required: true } })
const authStore = useAuthStore()
const comments = ref([])
const newComment = ref('')

async function fetchComments() {
  const res = await api.get(`/boards/0/posts/${props.postId}/comments`)
  comments.value = res.data.data || []
}

async function submitComment(parentId) {
  if (!newComment.value.trim()) {
    ElMessage.warning('댓글 내용을 입력해주세요.')
    return
  }
  await api.post(`/boards/0/posts/${props.postId}/comments`, {
    content: newComment.value,
    parentId
  })
  newComment.value = ''
  fetchComments()
}

onMounted(fetchComments)
</script>

<style scoped>
.comment-section {
  padding: 20px 28px;
}

.comment-header {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 16px;
}

.comment-count {
  color: var(--color-primary);
}

.comment-write {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  margin-bottom: 20px;
}

.comment-write .el-textarea {
  flex: 1;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.no-comment {
  text-align: center;
  color: var(--color-text-muted);
  font-size: 13px;
  padding: 24px 0;
}
</style>
