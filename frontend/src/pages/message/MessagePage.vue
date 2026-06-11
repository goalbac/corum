<template>
  <div class="msg-layout" :class="{ 'mobile-chat-open': isMobileChatOpen }">
    <!-- ===== 왼쪽 대화 목록 ===== -->
    <aside class="msg-sidebar" :class="{ 'mobile-hidden': isMobileChatOpen }">
      <div class="sidebar-head">
        <span class="sidebar-title">쪽지</span>
        <button class="new-btn" title="새 쪽지" @click="openNew">
          <i class="ti ti-pencil-plus"></i>
        </button>
      </div>

      <div v-if="convLoading" class="conv-loading">
        <el-icon class="is-loading" size="20"><Loading /></el-icon>
      </div>
      <div v-else-if="!conversations.length" class="conv-empty">
        <i class="ti ti-messages"></i>
        <p>대화 내역이 없습니다</p>
      </div>
      <ul v-else class="conv-list">
        <li
          v-for="conv in conversations"
          :key="conv.partnerId"
          :class="['conv-item', { active: activePartnerId === conv.partnerId }]"
          @click="selectConversation(conv)"
        >
          <div class="conv-avatar">
            <img
              v-if="conv.partnerProfileImageUrl && !convAvatarErrors[conv.partnerId]"
              :src="conv.partnerProfileImageUrl"
              :alt="conv.partnerName"
              @error="convAvatarErrors[conv.partnerId] = true"
            />
            <span v-else class="conv-avatar-fallback">{{ conv.partnerName?.charAt(0) || 'U' }}</span>
          </div>
          <div class="conv-info">
            <div class="conv-row1">
              <span class="conv-name">{{ conv.partnerName }}</span>
              <span class="conv-time">{{ relativeTime(conv.lastAt) }}</span>
            </div>
            <div class="conv-row2">
              <span class="conv-preview" :class="{ mine: conv.lastIsMine }">
                <span v-if="conv.lastIsMine" class="preview-mine">나: </span>{{ conv.lastContent }}
              </span>
              <span v-if="conv.unreadCount > 0" class="unread-dot">{{ conv.unreadCount }}</span>
            </div>
          </div>
          <button
            class="conv-del-btn"
            title="대화 삭제"
            @click.stop="deleteConversation(conv)"
          >
            <i class="ti ti-trash"></i>
          </button>
        </li>
      </ul>
    </aside>

    <!-- ===== 오른쪽 채팅 패널 ===== -->
    <main class="msg-main">
      <!-- 대화 미선택 -->
      <div v-if="!activePartnerId" class="chat-empty">
        <i class="ti ti-messages empty-icon"></i>
        <p>대화를 선택하거나 새 쪽지를 보내세요</p>
        <el-button type="primary" class="empty-new-btn" @click="openNew">
          <i class="ti ti-pencil-plus"></i>새 쪽지
        </el-button>
      </div>

      <!-- 대화 패널 -->
      <template v-else>
        <!-- 헤더 -->
        <div class="chat-header">
          <div class="chat-header-left">
            <button class="mobile-back-btn" @click="closeMobileChat" aria-label="목록으로">
              <i class="ti ti-arrow-left"></i>
            </button>
            <div class="chat-avatar">
              <img
                v-if="activePartner?.partnerProfileImageUrl && !chatAvatarErr"
                :src="activePartner.partnerProfileImageUrl"
                :alt="activePartner.partnerName"
                @error="chatAvatarErr = true"
              />
              <span v-else class="conv-avatar-fallback sm">{{ activePartner?.partnerName?.charAt(0) || 'U' }}</span>
            </div>
            <span class="chat-partner-name">{{ activePartner?.partnerName }}</span>
          </div>
        </div>

        <!-- 메시지 영역 -->
        <div ref="chatBodyRef" class="chat-body">
          <div v-if="chatLoading" class="chat-inner-loading">
            <el-icon class="is-loading" size="20"><Loading /></el-icon>
          </div>
          <template v-else>
            <div
              v-for="(msg, idx) in chatMessages"
              :key="msg.id"
            >
              <!-- 날짜 구분선 -->
              <div v-if="showDateSep(chatMessages, idx)" class="date-sep">
                <span>{{ dateLabel(msg.createdAt) }}</span>
              </div>

              <div :class="['bubble-row', msg.isMine ? 'mine' : 'theirs', { 'just-sent': msg.id === animatedMessageId }]">
                <!-- 상대방 아바타 (left side) -->
                <div v-if="!msg.isMine" class="bubble-avatar">
                  <img
                    v-if="activePartner?.partnerProfileImageUrl && !chatAvatarErr"
                    :src="activePartner.partnerProfileImageUrl"
                    alt=""
                    @error="chatAvatarErr = true"
                  />
                  <span v-else class="conv-avatar-fallback xs">{{ activePartner?.partnerName?.charAt(0) }}</span>
                </div>

                <div class="bubble-col" :class="msg.isMine ? 'col-mine' : 'col-theirs'">
                  <div :class="['bubble', msg.isMine ? 'bubble-mine' : 'bubble-theirs']">
                    <span v-if="msg.content">{{ msg.content }}</span>
                    <!-- 첨부 파일 -->
                    <div v-if="msg.files && msg.files.length" class="bubble-files">
                      <template v-for="f in msg.files" :key="f.id">
                        <img
                          v-if="f.mimeType && f.mimeType.startsWith('image/')"
                          :src="`/api/files/${f.id}/view`"
                          class="bubble-image"
                          @click="openImage(`/api/files/${f.id}/view`)"
                        />
                        <a
                          v-else
                          :href="`/api/files/${f.id}/download`"
                          class="bubble-file-link"
                          target="_blank"
                          @click.stop
                        >
                          <i class="ti ti-file-download"></i>
                          <span>{{ f.originalName }}</span>
                          <span class="file-size">{{ formatFileSize(f.fileSize) }}</span>
                        </a>
                      </template>
                    </div>
                  </div>
                  <span class="bubble-time">
                    {{ timeLabel(msg.createdAt) }}
                    <span v-if="msg.isMine && !msg.isRead" class="unread-check">1</span>
                  </span>
                </div>
              </div>
            </div>
          </template>
        </div>

        <!-- 입력창 -->
        <div class="chat-input-wrap">
          <!-- 첨부 파일 미리보기 -->
          <div v-if="attachments.length" class="attach-preview">
            <div v-for="(f, i) in attachments" :key="i" class="attach-chip">
              <img v-if="isImage(f)" :src="previewUrl(f)" class="attach-thumb" />
              <i v-else class="ti ti-file attach-file-icon"></i>
              <span class="attach-name">{{ f.name }}</span>
              <button class="attach-remove" @click="removeAttachment(i)">
                <i class="ti ti-x"></i>
              </button>
            </div>
          </div>
          <div class="chat-input-area">
            <input ref="fileInputRef" type="file" multiple class="hidden-file" @change="onFileChange" />
            <button class="attach-btn" title="파일 첨부" @click="fileInputRef.click()">
              <i class="ti ti-paperclip"></i>
            </button>
            <textarea
              ref="inputRef"
              v-model="inputText"
              class="chat-textarea"
              placeholder="메시지를 입력하세요..."
              rows="1"
              @keydown.enter.exact.prevent="sendMessage"
              @keydown.enter.shift.exact="newline"
              @input="autoResize"
            />
            <button
              :class="['send-btn', { active: inputText.trim() || attachments.length }]"
              :disabled="(!inputText.trim() && !attachments.length) || sending"
              @click="sendMessage"
            >
              <i :class="['ti', sending ? 'ti-loader-2 spinning' : 'ti-arrow-up']"></i>
            </button>
          </div>
        </div>
      </template>
    </main>

    <!-- 이미지 뷰어 -->
    <div v-if="lightboxSrc" class="lightbox" @click="lightboxSrc = null">
      <img :src="lightboxSrc" class="lightbox-img" @click.stop />
      <button class="lightbox-close" @click="lightboxSrc = null"><i class="ti ti-x"></i></button>
    </div>

    <!-- ===== 새 쪽지 다이얼로그 ===== -->
    <el-dialog
      v-model="showNew"
      title="새 쪽지"
      width="420px"
      destroy-on-close
      @closed="resetNew"
    >
      <div class="new-msg-form">
        <div class="search-row">
          <el-input
            v-model="searchQuery"
            placeholder="이름 또는 아이디로 검색"
            :prefix-icon="Search"
            clearable
            @input="searchMembers"
          />
        </div>
        <ul v-if="searchResults.length" class="search-results">
          <li
            v-for="m in searchResults"
            :key="m.id"
            class="search-result-item"
            @click="startNewConversation(m)"
          >
            <div class="sr-avatar">
              <img v-if="m.profileImageUrl" :src="m.profileImageUrl" alt="" />
              <span v-else class="conv-avatar-fallback xs">{{ m.name?.charAt(0) }}</span>
            </div>
            <div class="sr-info">
              <span class="sr-name">{{ m.name }}</span>
              <span class="sr-username">@{{ m.username }}</span>
            </div>
          </li>
        </ul>
        <div v-else-if="searchQuery && !searching" class="search-empty">검색 결과가 없습니다.</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { Loading, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api/axios'
import { useNotificationStore } from '@/stores/notification'

const route = useRoute()
const notifStore = useNotificationStore()

// ===== 모바일 목록↔채팅 전환 =====
const isMobileChatOpen = ref(false)

function openMobileChat() {
  if (window.innerWidth <= 768) isMobileChatOpen.value = true
}

function closeMobileChat() {
  isMobileChatOpen.value = false
}

// ===== 대화 목록 =====
const conversations    = ref([])
const convLoading      = ref(false)
const convAvatarErrors = ref({})
const activePartnerId  = ref(null)
const chatAvatarErr    = ref(false)

const activePartner = computed(() =>
  conversations.value.find(c => c.partnerId === activePartnerId.value)
)

async function fetchConversations({ showLoading = true } = {}) {
  if (showLoading) convLoading.value = true
  try {
    const res = await api.get('/messages/conversations')
    conversations.value = res.data.data || []
  } finally {
    if (showLoading) convLoading.value = false
  }
}

// 현재 열린 대화를 읽음 처리 (conversations 갱신 후 호출)
async function markActiveConversationRead() {
  const conv = conversations.value.find(c => c.partnerId === activePartnerId.value)
  if (!conv || conv.unreadCount <= 0) return
  const readCount = conv.unreadCount
  conv.unreadCount = 0
  notifStore.decrementMsgCount(readCount)
  await api.put(`/messages/conversations/${conv.partnerId}/read`).catch(() => {})
}

// 새 쪽지 알림이 오면 대화 목록 + 열린 채팅도 새로고침 + 읽음 처리
watch(() => notifStore.notifications[0], async (latest) => {
  if (latest?.type === 'MESSAGE') {
    await fetchConversations()
    if (activePartnerId.value) {
      await loadChat(activePartnerId.value)
      await markActiveConversationRead()
    }
  }
})

// ===== 채팅 내역 =====
const chatMessages = ref([])
const chatLoading  = ref(false)
const chatBodyRef  = ref()
const animatedMessageId = ref(null)

async function selectConversation(conv) {
  activePartnerId.value = conv.partnerId
  chatAvatarErr.value = false
  openMobileChat()
  await loadChat(conv.partnerId)
  await markActiveConversationRead()
}

async function loadChat(partnerId, { showLoading = true } = {}) {
  if (showLoading) chatLoading.value = true
  try {
    const res = await api.get(`/messages/conversations/${partnerId}`)
    chatMessages.value = res.data.data || []
    if (showLoading) chatLoading.value = false
    await nextTick()
    scrollToBottom()
  } catch (e) {
    if (showLoading) chatLoading.value = false
    throw e
  }
}

function scrollToBottom({ smooth = false } = {}) {
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      const el = chatBodyRef.value
      if (!el) return

      el.scrollTo({
        top: el.scrollHeight,
        behavior: smooth ? 'smooth' : 'auto'
      })
    })
  })
}

async function loadChatAfterSend(partnerId) {
  const previousLastId = chatMessages.value.at(-1)?.id
  await loadChat(partnerId, { showLoading: false })

  const latestMine = [...chatMessages.value].reverse()
    .find(msg => msg.isMine && msg.id !== previousLastId)

  if (latestMine?.id) {
    animatedMessageId.value = latestMine.id
    window.setTimeout(() => {
      if (animatedMessageId.value === latestMine.id) {
        animatedMessageId.value = null
      }
    }, 420)
  }

  await nextTick()
  scrollToBottom({ smooth: true })
}

// ===== 메시지 전송 =====
const inputText    = ref('')
const sending      = ref(false)
const inputRef     = ref()
const fileInputRef = ref()
const attachments  = ref([])   // File[]
const lightboxSrc  = ref(null)

function onFileChange(e) {
  const newFiles = Array.from(e.target.files || [])
  attachments.value.push(...newFiles)
  e.target.value = ''
}

function removeAttachment(i) {
  attachments.value.splice(i, 1)
}

function isImage(file) {
  return file.type.startsWith('image/')
}

function previewUrl(file) {
  return URL.createObjectURL(file)
}

function openImage(src) {
  lightboxSrc.value = src
}

function formatFileSize(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

async function sendMessage() {
  const text = inputText.value.trim()
  const hasAttach = attachments.value.length > 0
  if ((!text && !hasAttach) || !activePartnerId.value || sending.value) return
  sending.value = true
  try {
    const form = new FormData()
    form.append('recipientIds', activePartnerId.value)
    form.append('content', text)
    attachments.value.forEach(f => form.append('files', f))
    await api.post('/messages', form, { headers: { 'Content-Type': undefined } })
    inputText.value = ''
    attachments.value = []
    if (inputRef.value) inputRef.value.style.height = 'auto'
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '전송에 실패했습니다.')
    return
  } finally {
    sending.value = false
  }
  // 전송 완료 후 화면 갱신은 백그라운드로 (스피너 차단 없음)
  loadChatAfterSend(activePartnerId.value)
  fetchConversations({ showLoading: false }).then(() => markActiveConversationRead())
}

function newline(e) {
  // shift+enter는 줄바꿈 허용 (기본동작)
}

function autoResize(e) {
  const el = e.target
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 120) + 'px'
}

// ===== 대화 삭제 =====
async function deleteConversation(conv) {
  await ElMessageBox.confirm(
    `${conv.partnerName}님과의 대화를 삭제하시겠습니까?\n삭제 후 복구할 수 없습니다.`,
    '대화 삭제',
    { type: 'warning', confirmButtonText: '삭제', cancelButtonText: '취소' }
  )
  try {
    await api.delete(`/messages/conversations/${conv.partnerId}`)
    conversations.value = conversations.value.filter(c => c.partnerId !== conv.partnerId)
    if (activePartnerId.value === conv.partnerId) {
      activePartnerId.value = null
      chatMessages.value = []
    }
    ElMessage.success('대화가 삭제되었습니다.')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '삭제에 실패했습니다.')
  }
}

// ===== 새 쪽지 =====
const showNew       = ref(false)
const searchQuery   = ref('')
const searchResults = ref([])
const searching     = ref(false)
let searchTimer     = null

function openNew() { showNew.value = true }
function resetNew() { searchQuery.value = ''; searchResults.value = [] }

function searchMembers() {
  clearTimeout(searchTimer)
  if (!searchQuery.value.trim()) { searchResults.value = []; return }
  searching.value = true
  searchTimer = setTimeout(async () => {
    try {
      const res = await api.get('/members/search', { params: { q: searchQuery.value.trim() } })
      searchResults.value = res.data.data || []
    } finally {
      searching.value = false
    }
  }, 300)
}

function startNewConversation(member) {
  showNew.value = false
  // 이미 대화가 있으면 선택, 없으면 가상으로 추가
  const existing = conversations.value.find(c => c.partnerId === member.id)
  if (existing) {
    selectConversation(existing)
  } else {
    // 새 대화 상대 추가 (임시)
    conversations.value.unshift({
      partnerId: member.id,
      partnerName: member.name,
      partnerProfileImageUrl: member.profileImageUrl,
      lastContent: '',
      lastAt: new Date().toISOString(),
      unreadCount: 0,
      lastIsMine: false
    })
    activePartnerId.value = member.id
    chatMessages.value = []
    chatLoading.value = false
    openMobileChat()
  }
}

// ===== 날짜/시간 포맷 =====
function relativeTime(dateStr) {
  if (!dateStr) return ''
  const now = new Date()
  const d   = new Date(dateStr)
  const diffMs = now - d
  if (diffMs < 60000) return '방금'
  if (diffMs < 3600000) return Math.floor(diffMs / 60000) + '분 전'
  if (d.toDateString() === now.toDateString()) return d.toTimeString().slice(0, 5)
  const yest = new Date(now); yest.setDate(now.getDate() - 1)
  if (d.toDateString() === yest.toDateString()) return '어제'
  return `${d.getMonth() + 1}.${d.getDate()}`
}

function timeLabel(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const h = d.getHours()
  const m = String(d.getMinutes()).padStart(2, '0')
  return `${h < 12 ? '오전' : '오후'} ${h % 12 || 12}:${m}`
}

function dateLabel(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일`
}

function showDateSep(msgs, idx) {
  if (idx === 0) return true
  const prev = new Date(msgs[idx - 1].createdAt)
  const curr = new Date(msgs[idx].createdAt)
  return prev.toDateString() !== curr.toDateString()
}

// ===== 초기화 =====
onMounted(async () => {
  await fetchConversations()
  // ?to= 쿼리 파라미터로 특정 대화 자동 열기
  const toId = route.query.to ? Number(route.query.to) : null
  if (toId) {
    const existing = conversations.value.find(c => c.partnerId === toId)
    if (existing) {
      selectConversation(existing)
    } else {
      // 존재하지 않는 대화 → 멤버 정보 로드 후 가상 추가
      try {
        const res = await api.get(`/members/${toId}/public`)
        const m = res.data.data
        startNewConversation(m)
      } catch {}
    }
  }
})
</script>

<style scoped>
/* ===== 전체 레이아웃 ===== */
.msg-layout {
  display: grid;
  grid-template-columns: 288px minmax(0, 1fr);
  height: calc(100vh - 60px - 64px); /* 헤더 + 푸터 제외 */
  border: 0.5px solid var(--border2);
  border-radius: var(--radius);
  overflow: hidden;
  background: var(--surface);
  box-shadow: var(--shadow);
}

/* ===== 왼쪽 사이드바 ===== */
.msg-sidebar {
  border-right: 0.5px solid var(--border2);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--surface);
}

.sidebar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 16px 14px;
  border-bottom: 0.5px solid var(--border2);
  flex-shrink: 0;
}

.sidebar-title {
  font-size: 16px;
  font-weight: 800;
  color: var(--t1);
}

.new-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: var(--accent-bg);
  color: var(--accent);
  border-radius: 50%;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: var(--transition);
}
.new-btn:hover { background: var(--accent); color: #fff; }

.conv-loading,
.conv-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 40px 16px;
  color: var(--t3);
  font-size: 13px;
}
.conv-empty i { font-size: 32px; }
.conv-empty p { margin: 0; }

.conv-list {
  list-style: none;
  margin: 0;
  padding: 6px 0;
  overflow-y: auto;
  flex: 1;
}

.conv-item {
  display: flex;
  gap: 10px;
  padding: 10px 14px;
  cursor: pointer;
  border-radius: var(--radius-xs);
  margin: 0 6px 1px;
  transition: var(--transition);
}
.conv-item:hover { background: var(--surface2); }
.conv-item.active { background: var(--accent-bg); }

.conv-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--surface2);
  border: 1.5px solid var(--border);
}
.conv-avatar img { width: 100%; height: 100%; object-fit: cover; }

.conv-avatar-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #4f6ef7, #7c4ff7);
}
.conv-avatar-fallback.sm { font-size: 13px; }
.conv-avatar-fallback.xs { font-size: 11px; }

.conv-info { flex: 1; min-width: 0; overflow: hidden; }

.conv-del-btn {
  display: none;
  width: 26px;
  height: 26px;
  border: none;
  background: none;
  color: var(--t3);
  font-size: 14px;
  border-radius: var(--radius-xs);
  cursor: pointer;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  transition: var(--transition);
}
.conv-del-btn:hover { color: #e03e52; background: #fff1f2; }
.conv-item:hover .conv-del-btn { display: flex; }
.conv-row1 { display: flex; align-items: center; justify-content: space-between; margin-bottom: 3px; }
.conv-name { font-size: 14px; font-weight: 700; color: var(--t1); }
.conv-time { font-size: 11px; color: var(--t3); flex-shrink: 0; }
.conv-row2 { display: flex; align-items: center; justify-content: space-between; gap: 6px; }

.conv-preview {
  font-size: 12.5px;
  color: var(--t3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}
.preview-mine { color: var(--t4); }

.unread-dot {
  background: var(--accent);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  padding: 0 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* ===== 오른쪽 채팅 패널 ===== */
.msg-main {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--bg);
}

.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  height: 100%;
  color: var(--t3);
}
.chat-empty .empty-icon { font-size: 48px; }
.chat-empty p { font-size: 14px; margin: 0; }
.empty-new-btn :deep(.ti) {
  margin-right: 4px;
  font-size: 16px;
  line-height: 1;
}

/* 채팅 헤더 */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  border-bottom: 0.5px solid var(--border2);
  background: var(--surface);
  flex-shrink: 0;
}

.chat-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--surface2);
  border: 1.5px solid var(--border);
  flex-shrink: 0;
}
.chat-avatar img { width: 100%; height: 100%; object-fit: cover; }

.chat-partner-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--t1);
}

/* 메시지 바디 */
.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.chat-inner-loading {
  display: flex;
  justify-content: center;
  padding: 40px;
  color: var(--t3);
}

/* 날짜 구분선 */
.date-sep {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 16px 0 10px;
}
.date-sep span {
  font-size: 11px;
  color: var(--t3);
  background: var(--surface2);
  padding: 3px 10px;
  border-radius: 10px;
}

/* 메시지 버블 */
.bubble-row {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  margin-bottom: 4px;
}
.bubble-row.mine { justify-content: flex-end; }
.bubble-row.theirs { justify-content: flex-start; }
.bubble-row.just-sent .bubble-col {
  animation: sent-message-rise 280ms cubic-bezier(0.22, 1, 0.36, 1);
  transform-origin: right bottom;
}

@keyframes sent-message-rise {
  from {
    opacity: 0;
    transform: translateY(18px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.bubble-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--surface2);
  border: 1px solid var(--border);
}
.bubble-avatar img { width: 100%; height: 100%; object-fit: cover; }

.bubble-col {
  display: flex;
  flex-direction: column;
  max-width: 60%;
}
.col-mine { align-items: flex-end; }
.col-theirs { align-items: flex-start; }

.bubble {
  padding: 9px 14px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;
}

.bubble-mine {
  background: var(--accent);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.bubble-theirs {
  background: var(--surface);
  color: var(--t1);
  border: 0.5px solid var(--border2);
  border-bottom-left-radius: 4px;
}

.bubble-time {
  font-size: 10.5px;
  color: var(--t3);
  margin-top: 3px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.unread-check {
  font-size: 10px;
  color: var(--accent);
  font-weight: 700;
}

/* 버블 내 파일/이미지 */
.bubble-files {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 4px;
}

.bubble-image {
  max-width: 220px;
  max-height: 200px;
  border-radius: 8px;
  object-fit: cover;
  cursor: zoom-in;
  display: block;
}

.bubble-file-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background: rgba(0,0,0,0.08);
  border-radius: 8px;
  font-size: 12px;
  color: inherit;
  text-decoration: none;
  max-width: 220px;
}
.bubble-mine .bubble-file-link { background: rgba(255,255,255,0.2); }
.bubble-file-link:hover { opacity: 0.8; }
.bubble-file-link i { font-size: 16px; flex-shrink: 0; }
.bubble-file-link span { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.file-size {
  font-size: 10px;
  opacity: 0.7;
  flex-shrink: 0;
}

/* 라이트박스 */
.lightbox {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.85);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.lightbox-img {
  max-width: 90vw;
  max-height: 90vh;
  object-fit: contain;
  border-radius: 8px;
}

.lightbox-close {
  position: absolute;
  top: 20px;
  right: 24px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: rgba(255,255,255,0.15);
  color: #fff;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.lightbox-close:hover { background: rgba(255,255,255,0.3); }

/* 입력창 */
.chat-input-wrap {
  border-top: 0.5px solid var(--border2);
  background: var(--surface);
  flex-shrink: 0;
}

.attach-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 10px 16px 0;
}

.attach-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px 4px 4px;
  background: var(--surface2);
  border: 1px solid var(--border);
  border-radius: 8px;
  max-width: 200px;
}

.attach-thumb {
  width: 32px;
  height: 32px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
}

.attach-file-icon { font-size: 20px; color: var(--t3); flex-shrink: 0; }

.attach-name {
  font-size: 12px;
  color: var(--t1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 110px;
}

.attach-remove {
  border: none;
  background: none;
  color: var(--t3);
  cursor: pointer;
  font-size: 12px;
  padding: 0;
  flex-shrink: 0;
  line-height: 1;
}
.attach-remove:hover { color: #ef4444; }

.chat-input-area {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 10px 12px;
}

.hidden-file { display: none; }

.attach-btn {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: var(--t3);
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: var(--transition);
}
.attach-btn:hover { background: var(--surface2); color: var(--accent); }

.chat-textarea {
  flex: 1;
  min-height: 36px;
  max-height: 120px;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: var(--surface2);
  color: var(--t1);
  font-size: 14px;
  font-family: inherit;
  line-height: 1.5;
  resize: none;
  outline: none;
  overflow-y: auto;
  transition: border-color 0.15s;
}
.chat-textarea:focus { border-color: var(--accent); }
.chat-textarea::placeholder { color: var(--t4); }

.send-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: var(--border2);
  color: var(--t3);
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: not-allowed;
  transition: var(--transition);
  flex-shrink: 0;
}
.send-btn.active {
  background: var(--accent);
  color: #fff;
  cursor: pointer;
}
.send-btn.active:hover { opacity: 0.88; }

.spinning {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ===== 새 쪽지 다이얼로그 ===== */
.new-msg-form { display: flex; flex-direction: column; gap: 12px; }
.search-row {}

.search-results {
  list-style: none;
  padding: 0;
  margin: 0;
  max-height: 260px;
  overflow-y: auto;
  border: 0.5px solid var(--border2);
  border-radius: var(--radius-xs);
}

.search-result-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  cursor: pointer;
  transition: var(--transition);
  border-bottom: 0.5px solid var(--border2);
}
.search-result-item:last-child { border-bottom: none; }
.search-result-item:hover { background: var(--surface2); }

.sr-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--surface2);
  border: 1px solid var(--border);
  flex-shrink: 0;
}
.sr-avatar img { width: 100%; height: 100%; object-fit: cover; }

.sr-info { display: flex; flex-direction: column; }
.sr-name { font-size: 14px; font-weight: 600; color: var(--t1); }
.sr-username { font-size: 12px; color: var(--t3); }

.search-empty { font-size: 13px; color: var(--t3); text-align: center; padding: 16px; }

/* ===== 모바일 뒤로가기 버튼 (데스크탑에서 숨김) ===== */
.mobile-back-btn {
  display: none;
}

/* ===== 반응형 ===== */
@media (max-width: 768px) {
  .msg-layout {
    grid-template-columns: 1fr;
    height: calc(100vh - var(--header-height) - 56px);
    position: relative;
    overflow: hidden;
  }

  /* 목록 화면 */
  .msg-sidebar {
    position: absolute;
    inset: 0;
    border-right: none;
    border-bottom: none;
    max-height: none;
    transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.28s;
    z-index: 1;
    overflow-y: auto;
  }

  .msg-sidebar.mobile-hidden {
    transform: translateX(-100%);
    opacity: 0;
    pointer-events: none;
  }

  /* 채팅 화면 */
  .msg-main {
    position: absolute;
    inset: 0;
    min-height: 0;
    transform: translateX(100%);
    opacity: 0;
    transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.28s;
    z-index: 2;
  }

  .mobile-chat-open .msg-main {
    transform: translateX(0);
    opacity: 1;
  }

  /* 뒤로가기 버튼 표시 */
  .mobile-back-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border: none;
    background: transparent;
    color: var(--t2);
    font-size: 20px;
    border-radius: 50%;
    cursor: pointer;
    flex-shrink: 0;
    transition: var(--transition);
  }

  .mobile-back-btn:hover { background: var(--surface2); color: var(--t1); }

  .bubble-col { max-width: 80%; }
}
</style>
