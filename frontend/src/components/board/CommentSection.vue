<template>
  <div class="comment-section">
    <div class="comment-header">
      댓글 <span class="comment-count">{{ comments.length }}</span>
    </div>

    <!-- 댓글 비활성화 안내 (관리자/권한자에게만) -->
    <el-alert
      v-if="!useComment && isAdmin"
      title="이 게시판은 댓글을 사용하지 않습니다. 관리자만 댓글을 달 수 있습니다."
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom: 14px"
    />

    <!-- 댓글 작성 영역 -->
    <div v-if="showWriteArea" class="comment-write">
      <el-input
        v-model="newComment"
        type="textarea"
        :rows="2"
        placeholder="댓글을 입력하세요."
        resize="none"
      />
      <el-button type="primary" size="small" @click="submitComment(null)">등록</el-button>
    </div>
    <div v-else-if="!authStore.isLoggedIn" class="comment-login-notice">
      댓글을 작성하려면 <router-link to="/login">로그인</router-link>이 필요합니다.
    </div>

    <div class="comment-list">
      <CommentItem
        v-for="comment in comments"
        :key="comment.id"
        :comment="comment"
        :board-id="boardId"
        :post-id="postId"
        :can-comment="canComment && useComment || isAdmin"
        @refresh="fetchComments"
      />
    </div>

    <div v-if="!comments.length" class="no-comment">
      첫 번째 댓글을 작성해보세요.
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import api from '@/api/axios'
import CommentItem from './CommentItem.vue'

const props = defineProps({
  boardId:    { type: [String, Number], required: true },
  postId:     { type: [String, Number], required: true },
  useComment: { type: Boolean, default: true },
  canComment: { type: Boolean, default: true },
  isAdmin:    { type: Boolean, default: false },
})

const authStore = useAuthStore()
const comments = ref([])
const newComment = ref('')

// 댓글 입력창 표시 조건: 로그인 + (useComment ON이면 canComment, OFF면 관리자만)
const showWriteArea = computed(() => {
  if (!authStore.isLoggedIn) return false
  if (props.isAdmin) return true
  return props.useComment && props.canComment
})

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

.comment-login-notice {
  font-size: 14px;
  color: var(--t3);
  margin-bottom: 20px;
}
.comment-login-notice a { color: var(--accent); }

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
