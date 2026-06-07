<template>
  <div class="comment-item" :class="{ 'is-reply': comment.depth > 0 }">
    <div class="comment-inner" :style="{ marginLeft: `${comment.depth * 24}px` }">

      <!-- 대댓글 화살표 -->
      <el-icon v-if="comment.depth > 0" class="reply-icon"><ChatLineRound /></el-icon>

      <div class="comment-body">
        <div class="comment-meta">
          <span class="comment-writer">{{ comment.writerName }}</span>
          <span class="comment-date">{{ formatDate(comment.createdAt) }}</span>
        </div>

        <!-- 삭제된 댓글 -->
        <div v-if="comment.isDeleted" class="deleted-comment">삭제된 댓글입니다.</div>

        <!-- 수정 모드 -->
        <template v-else-if="editMode">
          <el-input v-model="editContent" type="textarea" :rows="2" resize="none" />
          <div class="edit-actions">
            <el-button size="small" type="primary" @click="submitEdit">저장</el-button>
            <el-button size="small" @click="editMode = false">취소</el-button>
          </div>
        </template>

        <!-- 일반 -->
        <template v-else>
          <p class="comment-content">{{ comment.content }}</p>
          <div class="comment-actions">
            <span
              v-if="authStore.isLoggedIn && comment.depth < 2"
              class="action-btn"
              @click="replyMode = !replyMode"
            >답글</span>
            <span
              v-if="isOwner"
              class="action-btn"
              @click="startEdit"
            >수정</span>
            <span
              v-if="isOwner"
              class="action-btn danger"
              @click="handleDelete"
            >삭제</span>
          </div>
        </template>
      </div>
    </div>

    <!-- 답글 작성 -->
    <div v-if="replyMode" class="reply-write" :style="{ marginLeft: `${(comment.depth + 1) * 24 + 8}px` }">
      <el-input v-model="replyContent" type="textarea" :rows="2" placeholder="답글을 입력하세요." resize="none" />
      <el-button size="small" type="primary" @click="submitReply">등록</el-button>
      <el-button size="small" @click="replyMode = false">취소</el-button>
    </div>

    <!-- 대댓글 재귀 -->
    <CommentItem
      v-for="child in comment.children"
      :key="child.id"
      :comment="child"
      :post-id="postId"
      @refresh="$emit('refresh')"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ChatLineRound } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import api from '@/api/axios'

const props = defineProps({
  comment: { type: Object, required: true },
  postId:  { type: [String, Number], required: true }
})
const emit = defineEmits(['refresh'])

const authStore  = useAuthStore()
const editMode   = ref(false)
const replyMode  = ref(false)
const editContent  = ref('')
const replyContent = ref('')

const isOwner = computed(() =>
  authStore.isLoggedIn && authStore.member?.id === props.comment.memberId
)

function startEdit() {
  editContent.value = props.comment.content
  editMode.value = true
}

async function submitEdit() {
  await api.put(`/boards/0/posts/${props.postId}/comments/${props.comment.id}`, {
    content: editContent.value
  })
  editMode.value = false
  emit('refresh')
}

async function submitReply() {
  if (!replyContent.value.trim()) return
  await api.post(`/boards/0/posts/${props.postId}/comments`, {
    content: replyContent.value,
    parentId: props.comment.id
  })
  replyContent.value = ''
  replyMode.value = false
  emit('refresh')
}

async function handleDelete() {
  await api.delete(`/boards/0/posts/${props.postId}/comments/${props.comment.id}`)
  ElMessage.success('삭제되었습니다.')
  emit('refresh')
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')} ${d.toTimeString().slice(0,5)}`
}
</script>

<style scoped>
.comment-item {
  border-bottom: 1px solid var(--color-border);
}
.comment-item:last-child { border-bottom: none; }

.comment-inner {
  display: flex;
  gap: 8px;
  padding: 12px 0;
}

.reply-icon {
  color: var(--color-text-muted);
  margin-top: 2px;
  flex-shrink: 0;
}

.comment-body { flex: 1; }

.comment-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.comment-writer {
  font-size: 13px;
  font-weight: 600;
}

.comment-date {
  font-size: 12px;
  color: var(--color-text-muted);
}

.comment-content {
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.deleted-comment {
  font-size: 13px;
  color: var(--color-text-muted);
  font-style: italic;
}

.comment-actions {
  display: flex;
  gap: 10px;
  margin-top: 6px;
}

.action-btn {
  font-size: 12px;
  color: var(--color-text-muted);
  cursor: pointer;
  transition: var(--transition);
}

.action-btn:hover { color: var(--color-text-primary); }
.action-btn.danger:hover { color: var(--color-danger); }

.edit-actions {
  display: flex;
  gap: 6px;
  margin-top: 6px;
}

.reply-write {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  padding: 8px 0 12px;
}

.reply-write .el-textarea { flex: 1; }
</style>
