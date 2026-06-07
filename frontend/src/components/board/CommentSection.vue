<template>
  <div class="comment-section">
    <div class="comment-header">
      댓글 <span class="comment-count">{{ comments.length }}</span>
    </div>

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

    <div class="comment-list">
      <CommentItem
        v-for="comment in comments"
        :key="comment.id"
        :comment="comment"
        :board-id="boardId"
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

const props = defineProps({
  boardId: { type: [String, Number], required: true },
  postId: { type: [String, Number], required: true }
})
const authStore = useAuthStore()
const comments = ref([])
const newComment = ref('')

async function fetchComments() {
  const res = await api.get(`/boards/${props.boardId}/posts/${props.postId}/comments`)
  comments.value = res.data.data || []
}

async function submitComment(parentId) {
  if (!newComment.value.trim()) {
    ElMessage.warning('댓글 내용을 입력해주세요.')
    return
  }
  await api.post(`/boards/${props.boardId}/posts/${props.postId}/comments`, {
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
  padding: 22px 30px;
  background: var(--surface);
  color: var(--t1);
}

.comment-header {
  font-size: 17px;
  font-weight: 700;
  margin-bottom: 16px;
}

.comment-count { color: var(--accent-t); }

.comment-write {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  margin-bottom: 20px;
}

.comment-write .el-textarea { flex: 1; }
.comment-list { display: flex; flex-direction: column; gap: 0; }

.no-comment {
  text-align: center;
  color: var(--t3);
  font-size: 15px;
  padding: 26px 0;
}

@media (max-width: 768px) {
  .comment-section { padding: 20px 18px; }
  .comment-write { flex-direction: column; align-items: stretch; }
}
</style>
