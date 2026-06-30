<template>
  <div v-if="showSection" class="comment-section">

    <!-- 헤더: 댓글 + 숫자(primary 색상, 배지 아님) -->
    <div class="cs-header">
      <span class="cs-title">댓글</span>
      <span class="cs-count">{{ totalCommentCount }}</span>
    </div>

    <!-- 댓글 비활성화 안내 -->
    <div v-if="!useComment && isAdmin" class="cs-notice">
      <i class="ti ti-info-circle"></i>
      이 게시판은 댓글을 사용하지 않습니다. 관리자만 댓글을 달 수 있습니다.
    </div>

    <!-- 댓글 작성 영역 (상단) -->
    <div v-if="showWriteArea" class="cs-write">
      <div class="cs-write-inner">
        <div class="cs-avatar">
          <img
            v-if="authStore.member?.profileImageUrl && !avatarError"
            :src="authStore.member.profileImageUrl"
            alt=""
            @error="avatarError = true"
          />
          <span v-else class="cs-avatar-fallback" :style="myAvatarStyle()">
            {{ authStore.member?.name?.charAt(0) || 'U' }}
          </span>
        </div>
        <div class="cs-write-col">
          <textarea
            v-model="newComment"
            class="cs-textarea"
            placeholder="댓글을 입력하세요. 회원 간 예의를 지켜 주세요."
            rows="3"
            @keydown.ctrl.enter="submitComment(null)"
          />
          <div class="cs-write-footer">
            <button
              class="cs-submit-btn"
              :disabled="submitting || !newComment.trim()"
              @click="submitComment(null)"
            >
              <i v-if="submitting" class="ti ti-loader-2 spinning"></i>
              등록
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 로그인 필요 안내 (상단) -->
    <div v-else-if="!authStore.isLoggedIn" class="cs-login-notice">
      <i class="ti ti-lock"></i>
      <span>댓글을 작성하려면 <router-link to="/login">로그인</router-link>이 필요합니다.</span>
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
        :has-manage="hasManage"
        @refresh="fetchComments"
      />
      <div v-if="!comments.length" class="cs-empty">
        <i class="ti ti-message-circle-off"></i>
        <span>아직 댓글이 없습니다</span>
      </div>
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
  useComment:  { type: Boolean, default: true },
  canComment:  { type: Boolean, default: true },
  isAdmin:     { type: Boolean, default: false },
  hasManage:   { type: Boolean, default: false },
})

const authStore = useAuthStore()
const comments = ref([])
const newComment = ref('')
const submitting = ref(false)
const avatarError = ref(false)

function myAvatarStyle() {
  const id = authStore.member?.id
  if (!id) return { background: 'var(--primary-weak)', color: 'var(--primary)' }
  const hue = Math.round((Number(id) * 137.508) % 360)
  return { background: `hsl(${hue}, 55%, 91%)`, color: `hsl(${hue}, 55%, 32%)` }
}

function countComments(list) {
  return list.reduce((sum, c) => sum + 1 + countComments(c.children || []), 0)
}
const totalCommentCount = computed(() => countComments(comments.value))

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
  padding: 0 28px;
}

/* ===== 헤더: 배지 아님, 숫자만 primary 색 ===== */
.cs-header {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 8px 0 16px;
  font-size: 17px;
  font-weight: 700;
  color: var(--t1);
}

.cs-title { color: var(--t1); }

.cs-count { color: var(--primary); }

/* ===== 관리자 알림 ===== */
.cs-notice {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 14px;
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
  border-top: 1px solid var(--border);
}

.cs-empty i { font-size: 28px; }

/* ===== 댓글 작성 ===== */
.cs-write {
  margin-bottom: 10px;
}

.cs-write-inner {
  display: flex;
  gap: 11px;
  align-items: flex-start;
}

.cs-avatar {
  width: 40px;
  height: 40px;
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
  width: 40px;
  height: 40px;
  border-radius: 50%;
  font-size: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cs-write-col {
  flex: 1;
  min-width: 0;
}

.cs-textarea {
  width: 100%;
  min-height: 72px;
  padding: 11px 13px;
  border: 1px solid var(--border-strong, var(--border));
  border-radius: 10px;
  outline: none;
  background: var(--bg, var(--surface));
  font-size: 14px;
  line-height: 1.6;
  color: var(--t1);
  resize: vertical;
  font-family: inherit;
  box-sizing: border-box;
  transition: border-color 0.15s;
}

.cs-textarea:focus { border-color: var(--primary); }
.cs-textarea::placeholder { color: var(--t4); }

.cs-write-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.cs-submit-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 9px 20px;
  border-radius: 9px;
  border: none;
  background: var(--primary);
  color: #fff;
  font-size: 13.5px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: background 0.15s;
}

.cs-submit-btn:hover:not(:disabled) { background: var(--primary-strong); }
.cs-submit-btn:disabled { opacity: 0.45; cursor: default; }

/* ===== 로그인 안내 ===== */
.cs-login-notice {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 20px 0;
  font-size: 14px;
  color: var(--t3);
  border-top: 1px solid var(--border);
  margin-bottom: 0;
}

.cs-login-notice a { color: var(--accent); text-decoration: none; }
.cs-login-notice a:hover { text-decoration: underline; }

/* ===== 스피너 ===== */
@keyframes spin { to { transform: rotate(360deg); } }
.spinning { animation: spin 0.7s linear infinite; }

/* ===== 반응형 ===== */
@media (max-width: 768px) {
  .comment-section { padding: 0 18px; }
  .cs-textarea { font-size: 16px !important; }
}
</style>
