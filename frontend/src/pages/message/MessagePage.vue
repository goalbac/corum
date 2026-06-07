<template>
  <div class="message-page">
    <div class="tab-bar">
      <button v-for="tab in tabs" :key="tab.key" class="tab-btn" :class="{ active: activeTab === tab.key }" @click="activeTab = tab.key">
        {{ tab.label }}
        <span v-if="tab.key === 'inbox' && unreadCount > 0" class="unread-badge">{{ unreadCount }}</span>
      </button>
      <button class="tab-btn compose-btn" @click="showCompose = true">
        <i class="ti ti-send" style="margin-right:4px"></i>쪽지 보내기
      </button>
    </div>

    <div v-if="loading" class="loading-wrap"><el-icon class="is-loading"><Loading /></el-icon></div>
    <div v-else-if="!messages.length" class="empty-state">
      <i class="ti ti-mail-off"></i><p>쪽지가 없습니다.</p>
    </div>
    <div v-else class="message-list">
      <div v-for="msg in messages" :key="msg.id" class="message-row" :class="{ unread: !msg.isRead }" @click="openMessage(msg)">
        <div class="msg-sender">{{ msg.senderName }}</div>
        <div class="msg-title">
          {{ msg.title }}
          <span v-if="!msg.isRead" class="new-badge">N</span>
        </div>
        <div class="msg-date">{{ formatDate(msg.createdAt) }}</div>
        <button v-if="activeTab === 'inbox'" class="msg-del" @click.stop="deleteMessage(msg.id)">
          <i class="ti ti-trash"></i>
        </button>
      </div>
    </div>

    <div class="pagination">
      <el-pagination v-model:current-page="page" :page-size="20" :total="total"
        layout="prev, pager, next" background small @current-change="fetchMessages" />
    </div>

    <!-- 상세 -->
    <el-dialog v-model="showDetail" :title="selectedMsg?.title" width="560px" destroy-on-close>
      <div v-if="selectedMsg" class="msg-detail">
        <div class="detail-meta">
          <span>보낸이: <strong>{{ selectedMsg.senderName }}</strong></span>
          <span>{{ formatDate(selectedMsg.createdAt) }}</span>
        </div>
        <div class="detail-content">{{ selectedMsg.content }}</div>
      </div>
      <template #footer>
        <el-button @click="showDetail = false">닫기</el-button>
        <el-button type="primary" @click="replyTo">답장</el-button>
      </template>
    </el-dialog>

    <!-- 작성 -->
    <el-dialog v-model="showCompose" title="쪽지 보내기" width="520px" destroy-on-close>
      <el-form :model="composeForm" label-position="top">
        <el-form-item label="수신자 ID (쉼표로 여러 명)">
          <el-input v-model="composeForm.recipientInput" placeholder="예: 1, 2, 3" />
        </el-form-item>
        <el-form-item label="제목">
          <el-input v-model="composeForm.title" />
        </el-form-item>
        <el-form-item label="내용">
          <el-input v-model="composeForm.content" type="textarea" :rows="5" resize="none" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCompose = false">취소</el-button>
        <el-button type="primary" :loading="sending" @click="sendMessage">보내기</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api/axios'

const tabs = [{ key: 'inbox', label: '받은 쪽지함' }, { key: 'sent', label: '보낸 쪽지함' }]
const activeTab   = ref('inbox')
const messages    = ref([])
const loading     = ref(false)
const total       = ref(0)
const page        = ref(1)
const unreadCount = ref(0)
const showDetail  = ref(false)
const showCompose = ref(false)
const selectedMsg = ref(null)
const sending     = ref(false)
const composeForm = ref({ recipientInput: '', title: '', content: '' })

async function fetchMessages() {
  loading.value = true
  try {
    const url = activeTab.value === 'inbox' ? '/messages/inbox' : '/messages/sent'
    const res = await api.get(url, { params: { page: page.value - 1, size: 20 } })
    messages.value = res.data.data?.content || []
    total.value    = res.data.data?.totalElements || 0
  } finally { loading.value = false }
}

async function fetchUnread() {
  try { const res = await api.get('/messages/unread-count'); unreadCount.value = res.data.data?.count || 0 } catch {}
}

async function openMessage(msg) {
  try {
    const res = await api.get(`/messages/${msg.id}`)
    selectedMsg.value = res.data.data
    showDetail.value  = true
    if (!msg.isRead) { msg.isRead = true; unreadCount.value = Math.max(0, unreadCount.value - 1) }
  } catch {}
}

async function deleteMessage(id) {
  await ElMessageBox.confirm('쪽지를 삭제하시겠습니까?', '삭제', { type: 'warning' })
  await api.delete(`/messages/${id}`)
  ElMessage.success('삭제되었습니다.')
  fetchMessages()
}

async function sendMessage() {
  const ids = composeForm.value.recipientInput.split(',').map(s => parseInt(s.trim())).filter(Boolean)
  if (!ids.length) return ElMessage.warning('수신자를 입력해주세요.')
  if (!composeForm.value.title) return ElMessage.warning('제목을 입력해주세요.')
  sending.value = true
  try {
    await api.post('/messages', { recipientIds: ids, title: composeForm.value.title, content: composeForm.value.content })
    ElMessage.success('발송되었습니다.')
    showCompose.value = false
    composeForm.value = { recipientInput: '', title: '', content: '' }
  } finally { sending.value = false }
}

function replyTo() {
  showDetail.value = false
  composeForm.value = { recipientInput: String(selectedMsg.value.senderId), title: `RE: ${selectedMsg.value.title}`, content: '' }
  showCompose.value = true
}

function formatDate(d) {
  if (!d) return ''
  const dt = new Date(d)
  if (dt.toDateString() === new Date().toDateString()) return dt.toTimeString().slice(0,5)
  return `${dt.getFullYear()}.${String(dt.getMonth()+1).padStart(2,'0')}.${String(dt.getDate()).padStart(2,'0')}`
}

watch(activeTab, () => { page.value = 1; fetchMessages() })
onMounted(() => { fetchMessages(); fetchUnread() })
</script>

<style scoped>
.tab-bar { display: flex; align-items: center; gap: 6px; padding-bottom: 14px; border-bottom: 0.5px solid var(--border2); margin-bottom: 14px; flex-wrap: wrap; }
.tab-btn { padding: 6px 14px; border: 0.5px solid var(--border); background: var(--surface2); border-radius: var(--radius-xs); font-size: 13px; color: var(--t2); cursor: pointer; font-family: inherit; transition: var(--transition); display: flex; align-items: center; gap: 4px; }
.tab-btn:hover { color: var(--t1); background: var(--surface); }
.tab-btn.active { background: var(--accent-bg); color: var(--accent); border-color: var(--accent); font-weight: 500; }
.tab-btn.compose-btn { margin-left: auto; background: var(--accent); color: #fff; border-color: var(--accent); }
.tab-btn.compose-btn:hover { background: var(--accent-t); }
.unread-badge { background: var(--new); color: #fff; font-size: 10px; font-weight: 700; padding: 0 5px; border-radius: 10px; line-height: 1.6; }
.message-list { display: flex; flex-direction: column; }
.message-row { display: flex; align-items: center; gap: 12px; padding: 11px 8px; border-bottom: 0.5px solid var(--border2); cursor: pointer; border-radius: var(--radius-xs); transition: var(--transition); }
.message-row:hover { background: var(--surface2); }
.message-row.unread .msg-title { font-weight: 600; }
.msg-sender { font-size: 12.5px; color: var(--t2); width: 64px; flex-shrink: 0; }
.msg-title { flex: 1; font-size: 13.5px; color: var(--t1); display: flex; align-items: center; gap: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.msg-date { font-size: 12px; color: var(--t3); flex-shrink: 0; }
.msg-del { background: none; border: none; color: var(--t3); font-size: 15px; cursor: pointer; padding: 4px; border-radius: var(--radius-xs); transition: var(--transition); }
.msg-del:hover { color: var(--new); background: var(--new-bg); }
.msg-detail { display: flex; flex-direction: column; gap: 16px; }
.detail-meta { display: flex; justify-content: space-between; font-size: 13px; color: var(--t2); padding-bottom: 12px; border-bottom: 0.5px solid var(--border2); }
.detail-content { font-size: 14px; line-height: 1.8; color: var(--t1); white-space: pre-wrap; min-height: 80px; }
.loading-wrap { display: flex; justify-content: center; padding: 40px; color: var(--t3); font-size: 24px; }
.empty-state { display: flex; flex-direction: column; align-items: center; gap: 10px; padding: 48px 0; color: var(--t3); font-size: 32px; }
.empty-state p { font-size: 14px; }
.pagination { display: flex; justify-content: center; padding: 16px 0 0; }
</style>
