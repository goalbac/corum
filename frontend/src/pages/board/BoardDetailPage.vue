<template>
  <div class="board-detail" v-loading="loading">
    <div v-if="accessDenied" class="access-denied">
      <i class="ti ti-lock access-denied-icon"></i>
      <h2>접근할 수 없는 페이지입니다.</h2>
      <p>이 게시글을 볼 수 있는 권한이 없습니다.</p>
      <button class="action-btn primary" @click="goHome">
        <i class="ti ti-home"></i>
        <span class="btn-label">홈으로 가기</span>
      </button>
    </div>

    <template v-else-if="post">

      <!-- ===== 게시글 헤더 ===== -->
      <div class="post-header">
        <div class="post-header-top">
          <span v-if="post.isNotice" class="notice-tag">공지</span>
          <span v-if="post.categoryName" class="category-tag">{{ post.categoryName }}</span>
          <h1 class="post-title">{{ post.title }}</h1>
        </div>

        <div class="post-meta">
          <div class="writer-info">
            <img
              v-if="post.writerProfileImageUrl && !postAvatarError"
              :src="post.writerProfileImageUrl"
              class="writer-avatar"
              alt=""
              @error="postAvatarError = true"
            />
            <span v-else class="writer-avatar-fallback">{{ post.writerName?.charAt(0) || 'U' }}</span>
            <div class="writer-texts">
              <button
                class="writer-name"
                :class="{ clickable: authStore.isLoggedIn && post.memberId }"
                @click="openProfile(post.memberId)"
              >{{ post.writerName }}</button>
              <span class="post-date">{{ formatDate(post.createdAt) }}</span>
            </div>
          </div>
          <div class="meta-stats">
            <span class="meta-stat"><i class="ti ti-eye"></i> {{ post.viewCount }}</span>
            <span v-if="board?.useLike" class="meta-stat"><i class="ti ti-heart"></i> {{ post.likeCount }}</span>
          </div>
        </div>
      </div>

      <!-- ===== 첨부파일 ===== -->
      <div v-if="post.files?.length" class="attach-area">
        <div class="attach-header">
          <i class="ti ti-paperclip"></i>
          <span>첨부파일 {{ post.files.length }}개</span>
        </div>
        <div class="attach-list">
          <a
            v-for="f in post.files"
            :key="f.id"
            :href="f.downloadUrl"
            class="attach-item"
            target="_blank"
          >
            <div class="attach-icon">
              <i :class="fileIcon(f.originalName)"></i>
            </div>
            <div class="attach-info">
              <span class="attach-name">{{ f.originalName }}</span>
              <span class="attach-size">{{ formatFileSize(f.fileSize) }}</span>
            </div>
            <i class="ti ti-download attach-dl"></i>
          </a>
        </div>
      </div>

      <!-- ===== 본문 ===== -->
      <div class="post-content" v-html="post.content" />

      <!-- ===== 액션 바 ===== -->
      <div class="post-actions">
        <div class="actions-left">
          <button
            v-if="board?.useLike"
            :class="['like-btn', { liked: post.liked }]"
            @click="handleLike"
          >
            <i :class="post.liked ? 'ti ti-heart-filled' : 'ti ti-heart'"></i>
            <span>{{ post.likeCount }}</span>
          </button>
          <button class="action-btn ghost" @click="handlePrint">
            <i class="ti ti-printer"></i>
            <span class="btn-label">인쇄</span>
          </button>
          <button class="action-btn ghost" @click="openShareDialog">
            <i class="ti ti-send"></i>
            <span class="btn-label">보내기</span>
          </button>
        </div>
        <div class="actions-right">
          <button v-if="canEdit" class="action-btn ghost"
            @click="router.push(`${basePath}/posts/${postId}/edit`)">
            <i class="ti ti-edit"></i>
            <span class="btn-label">수정</span>
          </button>
          <button v-if="canEdit" class="action-btn danger" @click="handleDelete">
            <i class="ti ti-trash"></i>
            <span class="btn-label">삭제</span>
          </button>
          <button class="action-btn primary" @click="router.push(basePath)">
            <i class="ti ti-layout-list"></i>
            <span class="btn-label">목록</span>
          </button>
        </div>
      </div>

      <CommentSection
        :board-id="boardId"
        :post-id="postId"
        :use-comment="board?.useComment ?? true"
        :can-comment="canComment"
        :is-admin="isAdmin"
        :has-manage="hasManage"
      />

      <!-- ===== 이전/다음 글 ===== -->
      <div v-if="adjacent" class="adjacent-nav">
        <component
          :is="adjacent.next ? 'router-link' : 'div'"
          v-bind="adjacent.next ? { to: `${basePath}/posts/${adjacent.next.id}` } : {}"
          :class="['adj-item', { empty: !adjacent.next }]"
        >
          <span class="adj-label">
            <i class="ti ti-chevron-up"></i> 다음 글
          </span>
          <span v-if="adjacent.next" class="adj-title">{{ adjacent.next.title }}</span>
          <span v-else class="adj-empty">없음</span>
        </component>
        <component
          :is="adjacent.prev ? 'router-link' : 'div'"
          v-bind="adjacent.prev ? { to: `${basePath}/posts/${adjacent.prev.id}` } : {}"
          :class="['adj-item', { empty: !adjacent.prev }]"
        >
          <span class="adj-label">
            <i class="ti ti-chevron-down"></i> 이전 글
          </span>
          <span v-if="adjacent.prev" class="adj-title">{{ adjacent.prev.title }}</span>
          <span v-else class="adj-empty">없음</span>
        </component>
      </div>
    </template>
  </div>

  <UserProfileModal
    v-model="profileModalVisible"
    :member-id="profileTargetId"
  />

  <el-dialog
    v-model="shareDialogVisible"
    title="게시글 보내기"
    width="460px"
    destroy-on-close
    @closed="resetShareDialog"
  >
    <div class="share-form">
      <el-select
        v-model="shareRecipientIds"
        multiple
        filterable
        remote
        reserve-keyword
        :remote-method="searchShareMembers"
        :loading="shareSearching"
        placeholder="받는 사람 이름 또는 아이디 검색"
        class="share-recipient-select"
      >
        <el-option
          v-for="member in shareMemberOptions"
          :key="member.id"
          :label="`${member.name} (@${member.username})`"
          :value="member.id"
        >
          <div class="share-member-option">
            <span class="share-member-name">{{ member.name }}</span>
            <span class="share-member-username">@{{ member.username }}</span>
          </div>
        </el-option>
      </el-select>

      <el-input
        v-model="shareMessage"
        type="textarea"
        :rows="3"
        maxlength="500"
        show-word-limit
        placeholder="함께 보낼 메시지를 입력하세요."
      />

      <div class="share-preview-card">
        <div class="share-preview-label">게시글 미리보기</div>
        <div class="share-preview-title">{{ post?.title }}</div>
        <div class="share-preview-meta">
          <span>{{ board?.name || '게시판' }}</span>
          <span>{{ post?.writerName }}</span>
        </div>
        <p v-if="sharePreviewDescription" class="share-preview-desc">{{ sharePreviewDescription }}</p>
      </div>
    </div>

    <template #footer>
      <el-button @click="shareDialogVisible = false">취소</el-button>
      <el-button
        type="primary"
        :loading="shareSending"
        :disabled="!shareRecipientIds.length"
        @click="sendPostMessage"
      >
        보내기
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import api from '@/api/axios'
import CommentSection from '@/components/board/CommentSection.vue'
import UserProfileModal from '@/components/common/UserProfileModal.vue'

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
const profileModalVisible = ref(false)
const profileTargetId     = ref(null)

function openProfile(memberId) {
  if (!memberId || !authStore.isLoggedIn) return
  profileTargetId.value     = memberId
  profileModalVisible.value = true
}
const adjacent = ref(null)
const loading = ref(false)
const postAvatarError = ref(false)
const accessDenied = ref(false)
const shareDialogVisible = ref(false)
const shareRecipientIds = ref([])
const shareMemberOptions = ref([])
const shareMessage = ref('')
const shareSearching = ref(false)
const shareSending = ref(false)
let shareSearchTimer = null

const isAdmin = computed(() => !!authStore.member?.isAdmin)

const hasManage = computed(() => {
  if (!authStore.isLoggedIn) return false
  if (isAdmin.value) return true
  const perms = board.value?.permissions || []
  if (!perms.length) return false
  const memberGroupIds = authStore.member?.groupIds || []
  return perms.some(p => memberGroupIds.includes(p.groupId) && p.canManage)
})

const canEdit = computed(() => {
  if (!authStore.isLoggedIn) return false
  if (hasManage.value) return true
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
  if (!postId.value) return
  if (!boardId.value) {
    if (route.params.menuId && menuStore.loaded) showAccessDenied()
    return
  }
  loading.value = true
  accessDenied.value = false
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
  } catch (error) {
    if (error.response?.status === 403) {
      showAccessDenied()
      return
    }
    throw error
  } finally {
    loading.value = false
  }
}

async function handleLike() {
  if (!authStore.isLoggedIn) { ElMessage.warning('로그인이 필요합니다.'); return }
  const res = await api.post(`/boards/${boardId.value}/posts/${postId.value}/like`)
  post.value.liked = res.data.data.liked
  post.value.likeCount += res.data.data.liked ? 1 : -1
}

async function handleDelete() {
  await ElMessageBox.confirm('게시글을 삭제하시겠습니까?', '삭제 확인', {
    type: 'warning', confirmButtonText: '삭제', cancelButtonText: '취소'
  })
  await api.delete(`/boards/${boardId.value}/posts/${postId.value}`)
  ElMessage.success('삭제되었습니다.')
  router.push(basePath.value)
}

function handlePrint() { window.print() }

const sharePreviewDescription = computed(() => {
  const text = stripHtml(post.value?.content || '').trim()
  return text.length > 120 ? `${text.slice(0, 120)}...` : text
})

function openShareDialog() {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('로그인이 필요합니다.')
    return
  }
  shareDialogVisible.value = true
}

function resetShareDialog() {
  shareRecipientIds.value = []
  shareMemberOptions.value = []
  shareMessage.value = ''
  shareSearching.value = false
  clearTimeout(shareSearchTimer)
}

function searchShareMembers(query) {
  clearTimeout(shareSearchTimer)
  const keyword = query.trim()
  if (!keyword) {
    shareMemberOptions.value = []
    shareSearching.value = false
    return
  }
  shareSearching.value = true
  shareSearchTimer = setTimeout(async () => {
    try {
      const res = await api.get('/members/search', { params: { q: keyword } })
      const myId = authStore.member?.id
      shareMemberOptions.value = (res.data.data || []).filter(member => member.id !== myId)
    } finally {
      shareSearching.value = false
    }
  }, 250)
}

async function sendPostMessage() {
  if (!shareRecipientIds.value.length || shareSending.value) return
  shareSending.value = true
  try {
    const form = new FormData()
    shareRecipientIds.value.forEach(id => form.append('recipientIds', id))
    form.append('content', JSON.stringify({
      type: 'POST_LINK',
      text: shareMessage.value.trim(),
      preview: {
        title: post.value?.title || '',
        description: sharePreviewDescription.value,
        boardName: board.value?.name || '게시판',
        writerName: post.value?.writerName || '',
        path: router.resolve(`${basePath.value}/posts/${postId.value}`).href
      }
    }))
    await api.post('/messages', form, { headers: { 'Content-Type': undefined } })
    ElMessage.success('게시글을 쪽지로 보냈습니다.')
    shareDialogVisible.value = false
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '게시글 보내기에 실패했습니다.')
  } finally {
    shareSending.value = false
  }
}

function showAccessDenied() {
  post.value = null
  adjacent.value = null
  accessDenied.value = true
  loading.value = false
}

function goHome() {
  router.push('/')
}

function stripHtml(html) {
  if (!html) return ''
  const div = document.createElement('div')
  div.innerHTML = html
  return div.textContent || div.innerText || ''
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('ko-KR', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

function formatFileSize(bytes) {
  if (!bytes) return '0B'
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / (1024 * 1024)).toFixed(1) + 'MB'
}

function fileIcon(name) {
  const ext = name?.split('.').pop()?.toLowerCase() || ''
  if (['jpg','jpeg','png','gif','webp','svg'].includes(ext)) return 'ti ti-photo'
  if (['pdf'].includes(ext)) return 'ti ti-file-type-pdf'
  if (['doc','docx'].includes(ext)) return 'ti ti-file-type-doc'
  if (['xls','xlsx'].includes(ext)) return 'ti ti-file-type-xls'
  if (['zip','rar','7z'].includes(ext)) return 'ti ti-file-zip'
  return 'ti ti-file'
}

watch([boardId, postId], fetchPost)
onMounted(async () => {
  if (route.params.menuId) await menuStore.fetchMenus()
  if (route.params.menuId && !activeMenu.value) {
    showAccessDenied()
    return
  }
  fetchPost()
})
</script>

<style scoped>
.board-detail { color: var(--t1); }

.access-denied {
  min-height: 360px;
  padding: 64px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  text-align: center;
  color: var(--t2);
}

.access-denied-icon {
  font-size: 42px;
  color: var(--t3);
}

.access-denied .action-btn i {
  font-size: 15px;
  color: inherit;
}

.access-denied h2 {
  margin: 8px 0 0;
  font-size: 22px;
  font-weight: 800;
  color: var(--t1);
}

.access-denied p {
  margin: 0 0 8px;
  font-size: 14px;
}

/* ===== 헤더 ===== */
.post-header {
  padding: 26px 28px 20px;
  border-bottom: 1px solid var(--border2);
  background: var(--surface2);
}

.post-header-top {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 14px;
}

.notice-tag {
  display: inline-flex;
  align-items: center;
  padding: 3px 9px;
  border-radius: 4px;
  background: var(--accent);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
  margin-top: 4px;
  flex-shrink: 0;
}
.category-tag {
  display: inline-flex;
  align-items: center;
  padding: 3px 9px;
  border-radius: 4px;
  border: 1px solid var(--accent);
  color: var(--accent);
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
  margin-top: 4px;
  flex-shrink: 0;
}

.post-title {
  font-size: 22px;
  font-weight: 800;
  line-height: 1.45;
  color: var(--t1);
  word-break: break-word;
}

.post-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.writer-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.writer-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  border: 1.5px solid var(--border);
}

.writer-avatar-fallback {
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
  flex-shrink: 0;
}

.writer-texts {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  text-align: left;
}

.writer-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--t1);
  background: none;
  border: none;
  padding: 0;
  font-family: inherit;
  cursor: default;
  text-align: left;
}
.writer-name.clickable {
  cursor: pointer;
}
.writer-name.clickable:hover { color: var(--accent); }

.post-date {
  font-size: 12px;
  color: var(--t3);
}

.meta-stats {
  display: flex;
  align-items: center;
  gap: 12px;
}

.meta-stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--t3);
}

.meta-stat i { font-size: 13px; }

/* ===== 첨부파일 ===== */
.attach-area {
  padding: 14px 28px;
  background: var(--surface2);
  border-bottom: 1px solid var(--border2);
}

.attach-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 700;
  color: var(--t3);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 10px;
}

.attach-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.attach-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: var(--radius-xs);
  border: 0.5px solid var(--border);
  background: var(--surface);
  color: var(--t1);
  transition: var(--transition);
  text-decoration: none;
}

.attach-item:hover {
  border-color: var(--accent);
  background: var(--accent-bg);
}

.attach-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: var(--accent-bg);
  color: var(--accent-t);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  flex-shrink: 0;
}

.attach-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.attach-name {
  font-size: 13px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attach-size {
  font-size: 11px;
  color: var(--t3);
}

.attach-dl {
  font-size: 15px;
  color: var(--t3);
  flex-shrink: 0;
}

/* ===== 본문 ===== */
.post-content {
  padding: 32px 28px;
  min-height: 180px;
  font-size: 15px;
  line-height: 1.9;
  color: var(--t1);
}

.post-content :deep(p) { margin: 0 0 0.7em; }
.post-content :deep(h1) { font-size: 1.7em; font-weight: 800; margin: 1em 0 0.4em; }
.post-content :deep(h2) { font-size: 1.35em; font-weight: 700; margin: 0.9em 0 0.4em; }
.post-content :deep(h3) { font-size: 1.1em; font-weight: 700; margin: 0.8em 0 0.3em; }
.post-content :deep(ul), .post-content :deep(ol) { padding-left: 1.5em; margin: 0.4em 0; }
.post-content :deep(li) { margin: 0.2em 0; }
.post-content :deep(blockquote) {
  border-left: 3px solid var(--accent);
  padding: 4px 0 4px 16px;
  color: var(--t2);
  margin: 0.8em 0;
  background: var(--accent-bg);
  border-radius: 0 4px 4px 0;
}
.post-content :deep(code) {
  background: var(--surface2);
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 0.88em;
  color: #e03e52;
  font-family: monospace;
}
.post-content :deep(pre) {
  background: #1e1e2e;
  border-radius: var(--radius-xs);
  padding: 18px;
  overflow-x: auto;
  margin: 0.8em 0;
}
.post-content :deep(pre code) { background: none; padding: 0; color: #cdd6f4; font-size: 13px; }
.post-content :deep(a) { color: var(--accent); text-decoration: underline; }
.post-content :deep(img) { max-width: 100%; height: auto; border-radius: var(--radius-xs); }
.post-content :deep(hr) { border: none; border-top: 1px solid var(--border); margin: 1.2em 0; }
.post-content :deep(table) { border-collapse: collapse; max-width: 100%; }
.post-content :deep(th), .post-content :deep(td) {
  border: 1px solid var(--border);
  padding: 6px 10px;
  font-size: 14px;
}
.post-content :deep(th) { background: var(--surface2); font-weight: 700; }

/* ===== 액션 바 ===== */
.post-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 28px;
  border-top: 1px solid var(--border2);
  border-bottom: 1px solid var(--border2);
  background: var(--surface2);
}

.actions-left, .actions-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.like-btn, .action-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  border-radius: var(--radius-xs);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
  white-space: nowrap;
  border: 0.5px solid var(--border);
  background: var(--surface);
  color: var(--t2);
}

.action-btn.ghost:hover { background: var(--surface2); color: var(--t1); }

.action-btn.danger {
  color: #e03e52;
  border-color: #fecdd3;
}
.action-btn.danger:hover { background: #fff1f2; border-color: #e03e52; }

.action-btn.primary {
  background: var(--accent);
  color: #fff;
  border-color: var(--accent);
}
.action-btn.primary:hover { opacity: 0.88; }

.like-btn { color: var(--t3); }
.like-btn:hover { color: #e03e52; border-color: #fecdd3; background: #fff1f2; }
.like-btn.liked { color: #e03e52; border-color: #fecdd3; background: #fff1f2; }

.share-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.share-recipient-select {
  width: 100%;
}

.share-member-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.share-member-name {
  font-weight: 700;
  color: var(--t1);
}

.share-member-username {
  font-size: 12px;
  color: var(--t3);
}

.share-preview-card {
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-xs);
  background: var(--surface2);
}

.share-preview-label {
  margin-bottom: 6px;
  font-size: 11px;
  font-weight: 800;
  color: var(--t3);
}

.share-preview-title {
  font-size: 14px;
  font-weight: 800;
  color: var(--t1);
  word-break: break-word;
}

.share-preview-meta {
  display: flex;
  gap: 8px;
  margin-top: 4px;
  font-size: 12px;
  color: var(--t3);
}

.share-preview-desc {
  margin: 8px 0 0;
  font-size: 12.5px;
  line-height: 1.5;
  color: var(--t2);
  word-break: break-word;
}

/* ===== 이전/다음 ===== */
.adjacent-nav { border-bottom: 1px solid var(--border2); }

.adj-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 13px 28px;
  border-top: 0.5px solid var(--border2);
  text-decoration: none;
  color: var(--t2);
  transition: var(--transition);
}

a.adj-item:hover { background: var(--surface2); }
a.adj-item:hover .adj-title { color: var(--accent-t); }

.adj-item.empty { opacity: 0.45; cursor: default; }

.adj-label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 700;
  color: var(--t3);
  white-space: nowrap;
  width: 68px;
  flex-shrink: 0;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.adj-title {
  font-size: 14px;
  color: var(--t1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.12s;
}

.adj-empty { font-size: 13px; color: var(--t4); }

/* ===== 반응형 ===== */
@media (max-width: 768px) {
  .post-header, .attach-area, .post-content, .post-actions, .adj-item { padding-left: 18px; padding-right: 18px; }
  .post-title { font-size: 18px; }
  .btn-label { display: none; }
  .like-btn, .action-btn { padding: 7px 10px; }
  :deep(.el-input__inner),
  :deep(.el-textarea__inner) { font-size: 16px !important; }
}
</style>
