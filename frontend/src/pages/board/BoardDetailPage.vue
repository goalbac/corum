<template>
  <div class="board-detail" v-loading="loading">
    <div v-if="accessDenied" class="access-denied">
      <i class="ti ti-lock access-denied-icon"></i>
      <h2>접근할 수 없는 페이지입니다.</h2>
      <p>이 게시글을 볼 수 있는 권한이 없습니다.</p>
      <button class="act-btn" @click="goHome">홈으로 가기</button>
    </div>

    <template v-else-if="post">
      <div class="detail-wrap">

        <!-- ===== 목록으로 ===== -->
        <button class="back-btn" @click="router.push(basePath)">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
          {{ board?.name || '목록' }}으로
        </button>

        <!-- ===== 공지·NEW 배지 ===== -->
        <div v-if="post.isNotice || isNew" class="post-kicker">
          <span v-if="post.isNotice" class="notice-tag">공지</span>
          <span v-if="isNew" class="new-tag">NEW</span>
        </div>

        <!-- ===== 제목 ===== -->
        <h1 class="post-title">{{ post.title }}</h1>

        <!-- ===== 메타 행 ===== -->
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
            <div>
              <div class="writer-name-row">
                <button
                  class="writer-name"
                  :class="{ clickable: authStore.isLoggedIn && post.memberId }"
                  @click="openProfile(post.memberId)"
                >{{ post.writerName }}</button>
                <span v-if="post.writerGroupName" class="writer-group">{{ post.writerGroupName }}</span>
              </div>
              <div class="post-stats">{{ formatDate(post.createdAt) }} · 조회 {{ post.viewCount }}<template v-if="board?.useComment"> · 댓글 {{ post.commentCount ?? 0 }}</template></div>
            </div>
          </div>
          <div class="meta-actions">
            <button class="meta-btn" @click="openShareDialog">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.6" y1="13.5" x2="15.4" y2="17.5"/><line x1="15.4" y1="6.5" x2="8.6" y2="10.5"/></svg>
              공유
            </button>
            <button v-if="canEdit" class="meta-btn" @click="router.push(`${basePath}/posts/${postId}/edit`)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"/></svg>
              수정
            </button>
            <button v-if="canEdit" class="meta-btn danger" @click="handleDelete">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/></svg>
              삭제
            </button>
          </div>
        </div>

        <!-- ===== 첨부파일 ===== -->
        <div v-if="post.files?.length" class="attach-card">
          <div class="attach-header">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 11.5 12.5 20a5 5 0 0 1-7-7l8.5-8.5a3.3 3.3 0 0 1 4.7 4.7L9.7 16.5a1.6 1.6 0 0 1-2.3-2.3l7.8-7.8"/></svg>
            첨부파일 <span class="attach-cnt">{{ post.files.length }}</span>
          </div>
          <div class="attach-list">
            <div v-for="f in post.files" :key="f.id" class="attach-item">
              <span class="ext-badge" :style="extStyle(f.originalName)">{{ extLabel(f.originalName) }}</span>
              <span class="attach-name">{{ f.originalName }}</span>
              <span class="attach-size">{{ formatFileSize(f.fileSize) }}</span>
              <a :href="f.downloadUrl" target="_blank" class="dl-btn">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
              </a>
            </div>
          </div>
        </div>

        <!-- ===== 본문 ===== -->
        <div class="post-content" v-html="post.content" />

        <!-- ===== 반응 / 공감 ===== -->
        <div v-if="board?.useLike" class="reaction-row">
          <span class="reaction-label">이 글에 공감해 주세요</span>
          <EmojiReactionBar
            :reactions="post.reactions"
            :my-reactions="post.myReactions"
            :disabled="!authStore.isLoggedIn"
            @toggle="handleReaction"
          />
          <button class="print-btn" @click="handlePrint">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 6 2 18 2 18 9"/><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><rect x="6" y="14" width="12" height="8"/></svg>
            인쇄
          </button>
        </div>
        <div v-else class="print-row">
          <button class="print-btn" @click="handlePrint">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 6 2 18 2 18 9"/><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><rect x="6" y="14" width="12" height="8"/></svg>
            인쇄
          </button>
        </div>

        <!-- ===== 댓글 ===== -->
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
            <span class="adj-label"><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><polyline points="18 15 12 9 6 15"/></svg> 다음 글</span>
            <span v-if="adjacent.next" class="adj-title">{{ adjacent.next.title }}</span>
            <span v-else class="adj-empty">없음</span>
          </component>
          <component
            :is="adjacent.prev ? 'router-link' : 'div'"
            v-bind="adjacent.prev ? { to: `${basePath}/posts/${adjacent.prev.id}` } : {}"
            :class="['adj-item', { empty: !adjacent.prev }]"
          >
            <span class="adj-label"><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><polyline points="6 9 12 15 18 9"/></svg> 이전 글</span>
            <span v-if="adjacent.prev" class="adj-title">{{ adjacent.prev.title }}</span>
            <span v-else class="adj-empty">없음</span>
          </component>
        </div>

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
import EmojiReactionBar from '@/components/common/EmojiReactionBar.vue'

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

const isNew = computed(() => {
  if (!post.value?.createdAt) return false
  return (Date.now() - new Date(post.value.createdAt).getTime()) < 3 * 24 * 60 * 60 * 1000
})

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

async function handleReaction(emojiType) {
  if (!authStore.isLoggedIn) { ElMessage.warning('로그인이 필요합니다.'); return }
  const res = await api.post(`/boards/${boardId.value}/posts/${postId.value}/reactions`, { emojiType })
  post.value.reactions = res.data.data.reactions
  post.value.myReactions = res.data.data.myReactions
  post.value.likeCount = res.data.data.totalCount
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

const EXT_MAP = {
  pdf:  { bg: 'rgba(214,69,63,0.12)', color: '#d6453f', label: 'PDF' },
  doc:  { bg: '#e9f0fe', color: '#2f5fd6', label: 'DOC' },
  docx: { bg: '#e9f0fe', color: '#2f5fd6', label: 'DOC' },
  xls:  { bg: '#e3f5ec', color: '#1f9d6b', label: 'XLS' },
  xlsx: { bg: '#e3f5ec', color: '#1f9d6b', label: 'XLS' },
  ppt:  { bg: 'rgba(217,119,6,0.1)', color: '#d97706', label: 'PPT' },
  pptx: { bg: 'rgba(217,119,6,0.1)', color: '#d97706', label: 'PPT' },
  zip:  { bg: '#eef1f6', color: '#929aab', label: 'ZIP' },
  rar:  { bg: '#eef1f6', color: '#929aab', label: 'ZIP' },
  hwp:  { bg: 'rgba(14,138,128,0.1)', color: '#0e8a80', label: 'HWP' },
  hwpx: { bg: 'rgba(14,138,128,0.1)', color: '#0e8a80', label: 'HWP' },
}

function getExt(name) {
  return (name?.split('.').pop() || '').toLowerCase()
}

function extStyle(name) {
  const ext = getExt(name)
  const m = EXT_MAP[ext]
  return m ? { background: m.bg, color: m.color } : { background: '#eef1f6', color: '#929aab' }
}

function extLabel(name) {
  const ext = getExt(name)
  return EXT_MAP[ext]?.label || ext.toUpperCase().slice(0, 4) || 'FILE'
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

/* ===== 접근 거부 ===== */
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
.access-denied-icon { font-size: 42px; color: var(--t3); }
.access-denied h2 { margin: 8px 0 0; font-size: 22px; font-weight: 800; color: var(--t1); }
.access-denied p  { margin: 0 0 8px; font-size: 14px; }
.act-btn {
  padding: 9px 20px;
  border: none;
  background: var(--primary);
  color: #fff;
  font-family: inherit;
  font-size: 14px;
  font-weight: 600;
  border-radius: 9px;
  cursor: pointer;
}

/* ===== 래퍼 ===== */
.detail-wrap {
  max-width: 864px;
  margin: 0 auto;
}

/* ===== 목록 버튼 ===== */
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: none;
  background: transparent;
  color: var(--t3);
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  margin-bottom: 16px;
  transition: color 0.15s;
}
.back-btn:hover { color: var(--primary); }

/* ===== 공지·NEW 뱃지 ===== */
.post-kicker {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-bottom: 11px;
}
.notice-tag {
  font-size: 12px;
  font-weight: 700;
  color: var(--primary);
  background: var(--primary-weak);
  padding: 3px 10px;
  border-radius: 6px;
}
.new-tag {
  font-size: 9.5px;
  font-weight: 800;
  color: var(--new);
  border: 1px solid var(--new);
  padding: 1px 6px;
  border-radius: 5px;
}

/* ===== 제목 ===== */
.post-title {
  margin: 0 0 18px;
  font-size: 27px;
  font-weight: 800;
  letter-spacing: -0.03em;
  line-height: 1.36;
  color: var(--t1);
  word-break: break-word;
}

/* ===== 메타 행 ===== */
.post-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--border);
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.writer-info {
  display: flex;
  align-items: center;
  gap: 11px;
}

.writer-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  border: 1.5px solid var(--border);
}

.writer-avatar-fallback {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.writer-name-row {
  display: flex;
  align-items: center;
  gap: 7px;
}

.writer-name {
  font-size: 14.5px;
  font-weight: 700;
  color: var(--t1);
  background: none;
  border: none;
  padding: 0;
  font-family: inherit;
  cursor: default;
}
.writer-name.clickable { cursor: pointer; }
.writer-name.clickable:hover { color: var(--primary); }

.writer-group {
  font-size: 10.5px;
  font-weight: 700;
  color: var(--primary);
  background: var(--primary-weak);
  padding: 1px 7px;
  border-radius: 5px;
}

.post-stats {
  font-size: 12.5px;
  color: var(--t3);
  margin-top: 3px;
}

.meta-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.meta-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: 1px solid var(--border-strong);
  background: var(--surface);
  color: var(--t2);
  font-family: inherit;
  font-weight: 600;
  font-size: 13px;
  padding: 7px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}
.meta-btn:hover { background: var(--surface-2); }
.meta-btn.danger { color: var(--danger); }
.meta-btn.danger:hover { background: rgba(214,69,63,0.08); }

/* ===== 첨부파일 카드 ===== */
.attach-card {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 15px 17px;
  margin-bottom: 28px;
  background: var(--surface);
}

.attach-header {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
  font-weight: 700;
  color: var(--t2);
  margin-bottom: 12px;
}
.attach-cnt { color: var(--t3); font-weight: 600; }

.attach-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.attach-item {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 9px 11px;
  border-radius: 9px;
  background: var(--bg);
  transition: background 0.15s;
}
.attach-item:hover { background: var(--surface-2); }

.ext-badge {
  width: 32px;
  height: 32px;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 9px;
  font-weight: 800;
  flex-shrink: 0;
}

.attach-name {
  flex: 1;
  min-width: 0;
  font-size: 13.5px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.attach-size {
  font-size: 12px;
  color: var(--t3);
  flex-shrink: 0;
}

.dl-btn {
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 7px;
  background: var(--primary-weak);
  color: var(--primary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.15s;
  text-decoration: none;
}
.dl-btn:hover { background: var(--primary); color: #fff; }

/* ===== 본문 ===== */
.post-content {
  font-size: 15.5px;
  line-height: 1.9;
  color: var(--t1);
  letter-spacing: -0.01em;
  margin-bottom: 28px;
}

.post-content :deep(p) { margin: 0 0 0.7em; }
.post-content :deep(h1) { font-size: 1.7em; font-weight: 800; margin: 1em 0 0.4em; }
.post-content :deep(h2) { font-size: 1.35em; font-weight: 700; margin: 0.9em 0 0.4em; }
.post-content :deep(h3) { font-size: 1.1em; font-weight: 700; margin: 0.8em 0 0.3em; }
.post-content :deep(ul), .post-content :deep(ol) { padding-left: 1.5em; margin: 0.4em 0; }
.post-content :deep(li) { margin: 0.2em 0; }
.post-content :deep(blockquote) {
  border-left: 3px solid var(--primary);
  padding: 14px 18px;
  background: var(--primary-weak);
  border-radius: 0 10px 10px 0;
  font-size: 14.5px;
  color: var(--t2);
  margin: 0 0 22px;
}
.post-content :deep(code) {
  background: var(--surface-2);
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 0.88em;
  color: #e03e52;
  font-family: monospace;
}
.post-content :deep(pre) {
  background: #1e1e2e;
  border-radius: 10px;
  padding: 18px;
  overflow-x: auto;
  margin: 0.8em 0;
}
.post-content :deep(pre code) { background: none; padding: 0; color: #cdd6f4; font-size: 13px; }
.post-content :deep(a) { color: var(--primary); text-decoration: underline; }
.post-content :deep(img) { max-width: 100%; height: auto; border-radius: 12px; }
.post-content :deep(hr) { border: none; border-top: 1px solid var(--border); margin: 1.2em 0; }
.post-content :deep(table) { border-collapse: collapse; max-width: 100%; }
.post-content :deep(th), .post-content :deep(td) {
  border: 1px solid var(--border);
  padding: 6px 10px;
  font-size: 14px;
}
.post-content :deep(th) { background: var(--surface-2); font-weight: 700; }

/* ===== 반응 행 ===== */
.reaction-row, .print-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 18px 0;
  border-top: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
  margin-bottom: 8px;
}

.reaction-label {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--t3);
  margin-right: 4px;
}

.print-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: 1px solid var(--border-strong);
  background: var(--surface);
  color: var(--t2);
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  padding: 6px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-left: auto;
  transition: background 0.15s;
}
.print-btn:hover { background: var(--surface-2); }

/* ===== 이전/다음 글 ===== */
.adjacent-nav { border-bottom: 1px solid var(--border); margin-top: 8px; }

.adj-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 13px 0;
  border-top: 1px solid var(--border);
  text-decoration: none;
  color: var(--t2);
  transition: color 0.12s;
}

a.adj-item:hover .adj-title { color: var(--primary); }
.adj-item.empty { opacity: 0.45; cursor: default; }

.adj-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 700;
  color: var(--t3);
  white-space: nowrap;
  width: 68px;
  flex-shrink: 0;
}

.adj-title {
  font-size: 14px;
  color: var(--t1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.12s;
}

.adj-empty { font-size: 13px; color: var(--t3); }

/* ===== 공유 다이얼로그 ===== */
.share-form { display: flex; flex-direction: column; gap: 14px; }
.share-recipient-select { width: 100%; }
.share-member-option { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.share-member-name { font-weight: 700; color: var(--t1); }
.share-member-username { font-size: 12px; color: var(--t3); }
.share-preview-card {
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--surface-2);
}
.share-preview-label { margin-bottom: 6px; font-size: 11px; font-weight: 800; color: var(--t3); }
.share-preview-title { font-size: 14px; font-weight: 800; color: var(--t1); word-break: break-word; }
.share-preview-meta { display: flex; gap: 8px; margin-top: 4px; font-size: 12px; color: var(--t3); }
.share-preview-desc { margin: 8px 0 0; font-size: 12.5px; line-height: 1.5; color: var(--t2); word-break: break-word; }

/* ===== 반응형 ===== */
@media (max-width: 768px) {
  .post-title { font-size: 20px; }
  :deep(.el-input__inner),
  :deep(.el-textarea__inner) { font-size: 16px !important; }
}
</style>
