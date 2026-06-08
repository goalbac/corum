<template>
  <div v-if="showSection" class="comment-section">

    <!-- 헤더 -->
    <div class="cs-header">
      <span class="cs-title">댓글</span>
      <span class="cs-count">{{ comments.length }}</span>
    </div>

    <!-- 댓글 비활성화 안내 -->
    <div v-if="!useComment && isAdmin" class="cs-notice">
      <i class="ti ti-info-circle"></i>
      이 게시판은 댓글을 사용하지 않습니다. 관리자만 댓글을 달 수 있습니다.
    </div>

    <!-- 댓글 목록 -->
    <div class="comment-list">
      <CommentItem
        v-for="comment in comments"
        :key="comment.id"
        :comment="comment"
        :board-id="boardId"
        :post-id="postId"
        :can-comment="canComment && useComment || isAdmin"
        :is-admin="isAdmin"
        @refresh="fetchComments"
      />
      <div v-if="!comments.length" class="cs-empty">
        <i class="ti ti-message-circle-off"></i>
        <span>아직 댓글이 없습니다</span>
      </div>
    </div>

    <!-- 댓글 작성 영역 -->
    <div v-if="showWriteArea" class="cs-write">
      <div class="cs-write-inner">
        <div class="cs-avatar">
          <img
            v-if="authStore.member?.profileImageUrl && !avatarError"
            :src="authStore.member.profileImageUrl"
            alt=""
            @error="avatarError = true"
          />
          <span v-else class="cs-avatar-fallback">
            {{ authStore.member?.name?.charAt(0) || 'U' }}
          </span>
        </div>
        <div class="cs-textarea-wrap">
          <textarea
            v-model="newComment"
            class="cs-textarea"
            :placeholder="authStore.isLoggedIn ? '댓글을 입력하세요...' : '로그인이 필요합니다.'"
            rows="3"
            @keydown.ctrl.enter="submitComment(null)"
          />
          <div class="cs-write-footer">
            <span class="cs-hint">Ctrl+Enter로 등록</span>
            <button
              class="cs-submit-btn"
              :class="{ active: newComment.trim() }"
              :disabled="submitting || !newComment.trim()"
              @click="submitComment(null)"
            >
              <i class="ti ti-send" v-if="!submitting"></i>
              <i class="ti ti-loader-2 spinning" v-else></i>
              등록
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 로그인 필요 안내 -->
    <div v-else-if="!authStore.isLoggedIn" class="cs-login-notice">
      <i class="ti ti-lock"></i>
      <span>댓글을 작성하려면 <router-link to="/login">로그인</router-link>이 필요합니다.</span>
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
const submitting = ref(false)
const avatarError = ref(false)

const showWriteArea = computed(() => {
  if (!authStore.isLoggedIn) return false
  if (props.isAdmin) return true
  return props.useComment && props.canComment
})

const showSection = computed(() => {
  if (showWriteArea.value) return true
  if (!authStore.isLoggedIn) return comments.value.length > 0
  if (props.isAdmin) return true
  return comments.value.length > 0
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
  submitting.value = true
  try {
    await api.post(`/boards/${props.boardId}/posts/${props.postId}/comments`, {
      content: newComment.value,
      parentId
    })
    newComment.value = ''
    fetchComments()
  } finally {
    submitting.value = false
  }
}

onMounted(fetchComments)
</script>

<style scoped>
.comment-section {
  border-top: 1px solid var(--border2);
}

/* ===== 헤더 ===== */
.cs-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 20px 28px 16px;
  border-bottom: 0.5px solid var(--border2);
}

.cs-title {
  font-size: 15px;
  font-weight: 800;
  color: var(--t1);
}

.cs-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 11px;
  background: var(--accent);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
}

/* ===== 관리자 알림 ===== */
.cs-notice {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 14px 28px;
  padding: 10px 14px;
  background: #fffbeb;
  border: 0.5px solid #fde68a;
  border-radius: var(--radius-xs);
  font-size: 13px;
  color: #92400e;
}

/* ===== 댓글 목록 ===== */
.comment-list {
  display: flex;
  flex-direction: column;
}

.cs-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 36px 0;
  color: var(--t4);
  font-size: 13px;
}

.cs-empty i { font-size: 28px; }

/* ===== 댓글 작성 ===== */
.cs-write {
  padding: 18px 28px 22px;
  background: var(--surface2);
  border-top: 0.5px solid var(--border2);
}

.cs-write-inner {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.cs-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  flex-shrink: 0;
  overflow: hidden;
}

.cs-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cs-avatar-fallback {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4f6ef7, #7c4ff7);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cs-textarea-wrap {
  flex: 1;
  border: 1px solid var(--border);
  border-radius: var(--radius-xs);
  background: var(--surface);
  transition: border-color 0.15s, box-shadow 0.15s;
  overflow: hidden;
}

.cs-textarea-wrap:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--accent) 12%, transparent);
}

.cs-textarea {
  width: 100%;
  padding: 12px 14px 8px;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  line-height: 1.6;
  color: var(--t1);
  resize: none;
  font-family: inherit;
  box-sizing: border-box;
}

.cs-textarea::placeholder { color: var(--t4); }

.cs-write-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px 8px;
}

.cs-hint {
  font-size: 11px;
  color: var(--t4);
}

.cs-submit-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  border-radius: var(--radius-xs);
  border: none;
  background: var(--surface2);
  color: var(--t3);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
}

.cs-submit-btn.active {
  background: var(--accent);
  color: #fff;
}

.cs-submit-btn:disabled:not(.active) { cursor: default; }
.cs-submit-btn.active:hover { opacity: 0.88; }

/* ===== 로그인 안내 ===== */
.cs-login-notice {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 20px 28px;
  font-size: 14px;
  color: var(--t3);
  border-top: 0.5px solid var(--border2);
  background: var(--surface2);
}

.cs-login-notice a { color: var(--accent); text-decoration: none; }
.cs-login-notice a:hover { text-decoration: underline; }

/* ===== 스피너 ===== */
@keyframes spin { to { transform: rotate(360deg); } }
.spinning { animation: spin 0.7s linear infinite; }

/* ===== 반응형 ===== */
@media (max-width: 768px) {
  .cs-header, .cs-write { padding-left: 18px; padding-right: 18px; }
}
</style>
