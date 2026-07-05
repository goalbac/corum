<template>
  <!-- depth에 따라 margin-left로 들여쓰기 -->
  <div
    class="comment-item"
    :style="{ marginLeft: `${comment.depth * 24}px` }"
  >
    <div class="comment-inner">
      <div class="comment-avatar">
        <img
          v-if="comment.writerProfileImageUrl && !avatarError"
          :src="comment.writerProfileImageUrl"
          class="c-avatar-img"
          alt=""
          @error="avatarError = true"
        />
        <span v-else class="c-avatar-placeholder" :style="avatarStyle(comment.memberId)">
          {{ comment.writerName?.charAt(0) || 'U' }}
        </span>
      </div>

      <div class="comment-body">
        <div class="comment-meta">
          <button
            class="comment-writer"
            :class="{ clickable: authStore.isLoggedIn && comment.memberId }"
            @click="openProfile(comment.memberId)"
          >{{ comment.writerName }}<template v-if="comment.actualWriterName">({{ comment.actualWriterName }})</template></button>
          <span class="comment-date">{{ formatDate(comment.createdAt) }}</span>
        </div>

        <div v-if="comment.isDeleted" class="deleted-comment">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/></svg>
          삭제된 댓글입니다.
        </div>

        <template v-else-if="editMode">
          <div class="inline-write-box">
            <textarea
              v-model="editContent"
              class="inline-textarea"
              rows="3"
              @keydown.ctrl.enter="submitEdit"
            />
            <div class="inline-write-footer">
              <div class="inline-btns">
                <button class="inline-btn" @click="editMode = false">취소</button>
                <button class="inline-btn accent" @click="submitEdit">저장</button>
              </div>
            </div>
          </div>
        </template>

        <template v-else>
          <p class="comment-content">{{ comment.content }}</p>
          <div class="comment-footer">
            <!-- 리액션 -->
            <EmojiReactionBar
              :reactions="localReactions"
              :my-reactions="localMyReactions"
              :disabled="!authStore.isLoggedIn"
              @toggle="handleReaction"
            />
            <!-- 액션 버튼 -->
            <div class="comment-actions">
              <button
                v-if="authStore.isLoggedIn && comment.depth < 2 && canComment"
                class="action-btn"
                @click="replyMode = !replyMode"
              >
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 17 4 12 9 7"/><path d="M20 18v-2a4 4 0 0 0-4-4H4"/></svg>
                답글
              </button>
              <button v-if="isOwner || hasManage" class="action-btn" @click="startEdit">
                <i class="ti ti-edit"></i> 수정
              </button>
              <button v-if="isOwner || isAdmin || hasManage" class="action-btn danger" @click="handleDelete">
                <i class="ti ti-trash"></i> 삭제
              </button>
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- 답글 입력 -->
    <div v-if="replyMode" class="reply-write-area">
      <div class="reply-avatar">
        <span class="c-avatar-placeholder sm" :style="avatarStyle(authStore.member?.id)">
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
            <div class="inline-btns">
              <button class="inline-btn" @click="replyMode = false">취소</button>
              <button
                class="inline-btn"
                :class="{ accent: replyContent.trim() }"
                :disabled="!replyContent.trim()"
                @click="submitReply"
              >등록</button>
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
      :has-manage="hasManage"
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
import EmojiReactionBar from '@/components/common/EmojiReactionBar.vue'

const props = defineProps({
  comment:    { type: Object, required: true },
  boardId:    { type: [String, Number], required: true },
  postId:     { type: [String, Number], required: true },
  canComment:  { type: Boolean, default: true },
  isAdmin:     { type: Boolean, default: false },
  hasManage:   { type: Boolean, default: false },
})
const emit = defineEmits(['refresh'])

const authStore   = useAuthStore()
const avatarError   = ref(false)
const editMode    = ref(false)
const replyMode   = ref(false)
const editContent = ref('')
const replyContent = ref('')

const localReactions   = ref({ ...(props.comment.reactions   ?? {}) })
const localMyReactions = ref([...(props.comment.myReactions  ?? [])])

async function handleReaction(emojiType) {
  const res = await api.post(
    `/boards/${props.boardId}/posts/${props.postId}/comments/${props.comment.id}/reactions`,
    { emojiType }
  )
  localReactions.value   = res.data.data.reactions
  localMyReactions.value = res.data.data.myReactions
}

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
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${mm}.${dd} ${d.toTimeString().slice(0, 5)}`
}

// 멤버 ID 기반 황금각(137.508°) HSL hue — CSS 변수로 넘겨 다크모드 자동 대응
function avatarStyle(id) {
  if (!id) return {}
  const hue = Math.round((Number(id) * 137.508) % 360)
  return { '--avatar-hue': hue }
}
</script>

<style scoped>
/* ===== 댓글 아이템: border-top으로 위 구분선 ===== */
.comment-item {
  border-top: 1px solid var(--border);
}

/* ===== 댓글 본체 ===== */
.comment-inner {
  display: flex;
  gap: 11px;
  padding: 15px 0;
}

/* 아바타 */
.comment-avatar { flex-shrink: 0; }

.c-avatar-img {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
}

.c-avatar-placeholder {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  font-size: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: hsl(var(--avatar-hue, 215), 55%, 91%);
  color: hsl(var(--avatar-hue, 215), 55%, 32%);
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
  font-size: 13.5px;
  font-weight: 700;
  color: var(--t1);
  background: none;
  border: none;
  padding: 0;
  font-family: inherit;
  cursor: default;
}
.comment-writer.clickable { cursor: pointer; }
.comment-writer.clickable:hover { color: var(--accent); }

.comment-date { font-size: 12px; color: var(--t3); }

.comment-content {
  font-size: 14px;
  line-height: 1.65;
  color: var(--t2);
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
}

.deleted-comment {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--t3);
  font-style: italic;
}

/* 리액션 + 액션 묶음 */
.comment-footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 9px;
}

/* 액션 버튼 */
.comment-actions { display: flex; gap: 10px; }

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: none;
  background: none;
  font-size: 12px;
  font-weight: 600;
  color: var(--t3);
  cursor: pointer;
  transition: color 0.12s;
  font-family: inherit;
}

.action-btn:hover { color: var(--primary); }
.action-btn.danger:hover { color: #e03e52; }

/* ===== 인라인 글쓰기 박스 (수정/답글 공통) ===== */
.inline-write-box {
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--surface);
  overflow: hidden;
  margin-top: 8px;
  transition: border-color 0.15s;
}
.inline-write-box:focus-within { border-color: var(--primary); }

.inline-textarea {
  width: 100%;
  padding: 10px 12px;
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
.inline-textarea::placeholder { color: var(--t4); }

.inline-write-footer {
  display: flex;
  justify-content: flex-end;
  padding: 4px 8px 8px;
}

.inline-btns { display: flex; gap: 5px; }

.inline-btn {
  padding: 4px 11px;
  border-radius: 7px;
  border: 0.5px solid var(--border);
  background: var(--surface2);
  color: var(--t2);
  font-size: 12px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: background 0.12s;
}
.inline-btn:hover { background: var(--surface); }
.inline-btn.accent { background: var(--primary); color: #fff; border-color: var(--primary); }
.inline-btn.accent:hover { opacity: 0.88; }
.inline-btn:disabled { opacity: 0.45; cursor: default; }

/* ===== 답글 작성 영역 ===== */
.reply-write-area {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  padding-bottom: 12px;
}

.reply-avatar { flex-shrink: 0; padding-top: 8px; }

.inline-write-wrap { flex: 1; min-width: 0; }

@media (max-width: 768px) {
  .comment-inner { padding: 12px 0; }
}
</style>
