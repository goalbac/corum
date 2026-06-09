<template>
  <div class="comment-item" :class="{ 'is-reply': comment.depth > 0 }">
    <div class="comment-inner" :style="{ paddingLeft: `${comment.depth > 0 ? comment.depth * 28 : 28}px` }">
      <!-- 답글 화살표: flex 흐름에 포함하여 아바타 바로 앞에 위치 -->
      <i v-if="comment.depth > 0" class="ti ti-corner-down-right reply-arrow"></i>

      <div class="comment-avatar">
        <img
          v-if="comment.writerProfileImageUrl && !avatarError"
          :src="comment.writerProfileImageUrl"
          class="c-avatar-img"
          alt=""
          @error="avatarError = true"
        />
        <span v-else class="c-avatar-placeholder">{{ comment.writerName?.charAt(0) || 'U' }}</span>
      </div>

      <div class="comment-body">
        <div class="comment-meta">
          <button
            class="comment-writer"
            :class="{ clickable: authStore.isLoggedIn && comment.memberId }"
            @click="openProfile(comment.memberId)"
          >{{ comment.writerName }}</button>
          <span class="comment-date">{{ formatDate(comment.createdAt) }}</span>
        </div>

        <div v-if="comment.isDeleted" class="deleted-comment">삭제된 댓글입니다.</div>

        <template v-else-if="editMode">
          <div class="inline-write-box">
            <textarea
              v-model="editContent"
              class="inline-textarea"
              rows="3"
              @keydown.ctrl.enter="submitEdit"
            />
            <div class="inline-write-footer">
              <span class="inline-hint">Ctrl+Enter로 저장</span>
              <div class="inline-btns">
                <button class="inline-btn" @click="editMode = false">취소</button>
                <button class="inline-btn accent" @click="submitEdit">저장</button>
              </div>
            </div>
          </div>
        </template>

        <template v-else>
          <p class="comment-content">{{ comment.content }}</p>
          <div class="comment-actions">
            <button
              v-if="authStore.isLoggedIn && comment.depth < 2 && canComment"
              class="action-btn"
              @click="replyMode = !replyMode"
            >
              <i class="ti ti-corner-down-right"></i> 답글
            </button>
            <button v-if="isOwner" class="action-btn" @click="startEdit">
              <i class="ti ti-edit"></i> 수정
            </button>
            <button v-if="isOwner || isAdmin" class="action-btn danger" @click="handleDelete">
              <i class="ti ti-trash"></i> 삭제
            </button>
          </div>
        </template>
      </div>
    </div>

    <!-- 답글 입력 -->
    <div
      v-if="replyMode"
      class="reply-write-area"
      :style="{ paddingLeft: `${28 + (comment.depth + 1) * 28}px` }"
    >
      <div class="reply-avatar">
        <img
          v-if="authStore.member?.profileImageUrl && !myAvatarError"
          :src="authStore.member.profileImageUrl"
          alt=""
          @error="myAvatarError = true"
        />
        <span v-else class="c-avatar-placeholder sm">
          {{ authStore.member?.name?.charAt(0) || 'U' }}
        </span>
      </div>
      <div class="inline-write-wrap">
        <div class="inline-write-box">
          <textarea
            v-model="replyContent"
            class="inline-textarea"
            rows="3"
            placeholder="답글을 입력하세요..."
            @keydown.ctrl.enter="submitReply"
          />
          <div class="inline-write-footer">
            <span class="inline-hint">Ctrl+Enter로 등록</span>
            <div class="inline-btns">
              <button class="inline-btn" @click="replyMode = false">취소</button>
              <button
                class="inline-btn"
                :class="{ accent: replyContent.trim() }"
                :disabled="!replyContent.trim()"
                @click="submitReply"
              >
                <i class="ti ti-send"></i> 등록
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 하위 댓글 -->
    <CommentItem
      v-for="child in comment.children"
      :key="child.id"
      :comment="child"
      :board-id="boardId"
      :post-id="postId"
      :can-comment="canComment"
      :is-admin="isAdmin"
      @refresh="$emit('refresh')"
    />
  </div>

  <UserProfileModal
    v-model="profileModalVisible"
    :member-id="profileTargetId"
  />
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import api from '@/api/axios'
import UserProfileModal from '@/components/common/UserProfileModal.vue'

const props = defineProps({
  comment:    { type: Object, required: true },
  boardId:    { type: [String, Number], required: true },
  postId:     { type: [String, Number], required: true },
  canComment: { type: Boolean, default: true },
  isAdmin:    { type: Boolean, default: false },
})
const emit = defineEmits(['refresh'])

const authStore   = useAuthStore()
const avatarError   = ref(false)
const myAvatarError = ref(false)
const editMode    = ref(false)
const replyMode   = ref(false)
const editContent = ref('')
const replyContent = ref('')

const isOwner = computed(() =>
  authStore.isLoggedIn && authStore.member?.id === props.comment.memberId
)

const profileModalVisible = ref(false)
const profileTargetId     = ref(null)

function openProfile(memberId) {
  if (!memberId || !authStore.isLoggedIn) return
  profileTargetId.value     = memberId
  profileModalVisible.value = true
}

function startEdit() {
  editContent.value = props.comment.content
  editMode.value = true
}

async function submitEdit() {
  if (!editContent.value.trim()) return
  await api.put(`/boards/${props.boardId}/posts/${props.postId}/comments/${props.comment.id}`, {
    content: editContent.value
  })
  editMode.value = false
  emit('refresh')
}

async function submitReply() {
  if (!replyContent.value.trim()) return
  await api.post(`/boards/${props.boardId}/posts/${props.postId}/comments`, {
    content: replyContent.value,
    parentId: props.comment.id
  })
  replyContent.value = ''
  replyMode.value = false
  emit('refresh')
}

async function handleDelete() {
  await ElMessageBox.confirm('댓글을 삭제하시겠습니까?', '삭제', {
    type: 'warning', confirmButtonText: '삭제', cancelButtonText: '취소'
  })
  await api.delete(`/boards/${props.boardId}/posts/${props.postId}/comments/${props.comment.id}`)
  ElMessage.success('삭제되었습니다.')
  emit('refresh')
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')} ${d.toTimeString().slice(0, 5)}`
}
</script>

<style scoped>
.comment-item {
  border-bottom: 0.5px solid var(--border2);
}
.comment-item:last-child { border-bottom: none; }

/* ===== 댓글 본체 ===== */
.comment-inner {
  display: flex;
  gap: 10px;
  padding-top: 14px;
  padding-bottom: 14px;
  padding-right: 28px;
  position: relative;
}

/* 답글 화살표: flex 흐름 내 아바타 바로 앞 */
.reply-arrow {
  flex-shrink: 0;
  font-size: 15px;
  color: var(--t4);
  margin-top: 10px;
  margin-right: 2px;
}

/* 아바타 */
.comment-avatar { flex-shrink: 0; margin-top: 1px; }

.c-avatar-img {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  object-fit: cover;
  border: 1.5px solid var(--border);
}

.c-avatar-placeholder {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4f6ef7, #7c4ff7);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.c-avatar-placeholder.sm {
  width: 28px;
  height: 28px;
  font-size: 11px;
}

/* 본문 */
.comment-body { flex: 1; min-width: 0; }

.comment-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 5px;
}

.comment-writer {
  font-size: 14px;
  font-weight: 700;
  color: var(--t1);
  background: none;
  border: none;
  padding: 0;
  font-family: inherit;
  cursor: default;
}
.comment-writer.clickable {
  cursor: pointer;
}
.comment-writer.clickable:hover { color: var(--accent); }
.comment-date   { font-size: 12px; color: var(--t3); }

.comment-content {
  font-size: 14px;
  line-height: 1.7;
  color: var(--t1);
  white-space: pre-wrap;
  word-break: break-word;
}

.deleted-comment {
  font-size: 14px;
  color: var(--t4);
  font-style: italic;
}

/* 액션 버튼 */
.comment-actions {
  display: flex;
  gap: 2px;
  margin-top: 6px;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 8px;
  border: none;
  background: none;
  font-size: 12px;
  color: var(--t4);
  cursor: pointer;
  border-radius: var(--radius-xs);
  transition: var(--transition);
}

.action-btn:hover { background: var(--surface2); color: var(--t2); }
.action-btn.danger:hover { color: #e03e52; background: #fff1f2; }

/* ===== 인라인 글쓰기 박스 (수정/답글 공통) ===== */
.inline-write-box {
  border: 1px solid var(--border);
  border-radius: var(--radius-xs);
  background: var(--surface);
  transition: border-color 0.15s, box-shadow 0.15s;
  overflow: hidden;
  margin-top: 8px;
}

.inline-write-box:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--accent) 10%, transparent);
}

.inline-textarea {
  width: 100%;
  padding: 10px 12px 6px;
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  line-height: 1.6;
  color: var(--t1);
  resize: none;
  font-family: inherit;
  box-sizing: border-box;
}

.inline-textarea::placeholder { color: var(--t4); }

.inline-write-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px 7px;
}

.inline-hint { font-size: 11px; color: var(--t4); }

.inline-btns { display: flex; gap: 5px; }

.inline-btn {
  padding: 4px 11px;
  border-radius: var(--radius-xs);
  border: 0.5px solid var(--border);
  background: var(--surface2);
  color: var(--t2);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
}

.inline-btn:hover { background: var(--surface); color: var(--t1); }
.inline-btn.accent { background: var(--accent); color: #fff; border-color: var(--accent); }
.inline-btn.accent:hover { opacity: 0.88; }
.inline-btn:disabled { opacity: 0.45; cursor: default; }

/* ===== 답글 작성 영역 ===== */
.reply-write-area {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  padding-right: 28px;
  padding-bottom: 12px;
  background: var(--surface2);
  border-bottom: 0.5px solid var(--border2);
}

.reply-avatar {
  flex-shrink: 0;
  padding-top: 10px;
}

.reply-avatar img {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--border);
}

.inline-write-wrap { flex: 1; min-width: 0; }

/* ===== 답글 배경 ===== */
.is-reply > .comment-inner {
  background: var(--surface2);
}

@media (max-width: 768px) {
  .comment-inner { padding-right: 16px; }
  .reply-write-area { padding-right: 16px; }
}
</style>
