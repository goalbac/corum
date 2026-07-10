<template>
  <div class="adm-page">
    <AdminPageHeader title="로그" desc="방문, 검색, 감사, SMTP 발송 이력">
      <div class="smtp-test">
        <input v-model="testEmail" class="adm-search-input" placeholder="test@example.com" />
        <button class="adm-btn ghost" :disabled="sending" @click="sendTestMail">
          <i v-if="sending" class="ti ti-loader-2 spinning"></i>
          SMTP 테스트
        </button>
      </div>
    </AdminPageHeader>

    <div class="adm-card log-card">
      <div class="adm-tabs">
        <button
          v-for="item in tabs"
          :key="item.key"
          type="button"
          :class="['adm-tab', tab === item.key ? 'active' : '']"
          @click="switchTab(item.key)"
        >
          {{ item.label }}
        </button>
      </div>

      <div v-if="tab === 'visit'" class="log-table" v-loading="loading">
        <div class="log-head visit-grid">
          <div>시각</div>
          <div>회원</div>
          <div>화면</div>
          <div>IP</div>
          <div>환경</div>
        </div>
        <div v-for="row in rows" :key="row.id" class="log-row visit-grid">
          <div class="muted">{{ fmtDt(row.createdAt) }}</div>
          <div><MemberCell :row="row" /></div>
          <div class="uri-cell">
            <strong>{{ displayPath(row.requestUri) }}</strong>
            <span v-if="row.referer && row.referer !== row.requestUri">{{ displayPath(row.referer) }}</span>
          </div>
          <div class="mono muted">{{ row.ipAddress || '-' }}</div>
          <div class="ua-cell" :title="row.userAgent || ''">{{ shortUserAgent(row.userAgent) }}</div>
        </div>
        <EmptyLog v-if="!rows.length && !loading" icon="ti-eye" text="방문 로그가 없습니다." />
      </div>

      <div v-if="tab === 'search'" class="log-table" v-loading="loading">
        <div class="log-head search-grid">
          <div>시각</div>
          <div>회원</div>
          <div>검색어</div>
          <div>유형</div>
          <div>결과</div>
          <div>IP</div>
        </div>
        <div v-for="row in rows" :key="row.id" class="log-row search-grid">
          <div class="muted">{{ fmtDt(row.createdAt) }}</div>
          <div><MemberCell :row="row" /></div>
          <div class="keyword">{{ row.keyword }}</div>
          <div class="muted">{{ searchTypeLabel(row.searchType) }}</div>
          <div>{{ row.resultCount?.toLocaleString?.() ?? 0 }}</div>
          <div class="mono muted">{{ row.ipAddress || '-' }}</div>
        </div>
        <EmptyLog v-if="!rows.length && !loading" icon="ti-search" text="검색 로그가 없습니다." />
      </div>

      <div v-if="tab === 'audit'" class="log-table" v-loading="loading">
        <div class="log-head audit-grid">
          <div>시각</div>
          <div>실행 회원</div>
          <div>동작</div>
          <div>대상</div>
          <div>변경 내용</div>
          <div>IP</div>
        </div>
        <div v-for="row in rows" :key="row.id" class="log-row audit-grid">
          <div class="muted">{{ fmtDt(row.createdAt) }}</div>
          <div><MemberCell :row="row" /></div>
          <div><span :class="['adm-badge', auditBadge(row.actionType)]">{{ actionLabel(row.actionType) }}</span></div>
          <div class="target-cell">
            <strong>{{ targetLabel(row) }}</strong>
            <span>{{ row.targetTable || '-' }} #{{ row.targetId || '-' }}</span>
          </div>
          <div class="change-cell">
            <span v-if="row.beforeValue || row.afterValue">{{ compactValue(row.beforeValue) }} → {{ compactValue(row.afterValue) }}</span>
            <span v-else class="muted">-</span>
          </div>
          <div class="mono muted">{{ row.ipAddress || '-' }}</div>
        </div>
        <EmptyLog v-if="!rows.length && !loading" icon="ti-clipboard-list" text="감사 로그가 없습니다." />
      </div>

      <div v-if="tab === 'smtp'" class="log-table" v-loading="loading">
        <div class="log-head smtp-grid">
          <div>시각</div>
          <div>요청 회원</div>
          <div>수신자</div>
          <div>제목</div>
          <div>유형</div>
          <div>결과</div>
        </div>
        <div v-for="row in rows" :key="row.id" class="log-row smtp-grid">
          <div class="muted">{{ fmtDt(row.createdAt) }}</div>
          <div><MemberCell :row="row" /></div>
          <div class="mono">{{ row.toEmail }}</div>
          <div class="subject-cell">
            <strong>{{ row.subject || '-' }}</strong>
            <span v-if="row.errorMessage" class="error-text">{{ row.errorMessage }}</span>
          </div>
          <div class="muted">{{ sendTypeLabel(row.sendType) }}</div>
          <div>
            <span :class="['adm-badge', row.success ? 'badge-success' : 'badge-danger']">
              {{ row.success ? '성공' : '실패' }}
            </span>
          </div>
        </div>
        <EmptyLog v-if="!rows.length && !loading" icon="ti-mail" text="SMTP 로그가 없습니다." />
      </div>

      <div class="adm-pagination">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="prev,pager,next"
          @current-change="fetchRows"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { h, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'

const tabs = [
  { key: 'visit', label: '방문 로그' },
  { key: 'search', label: '검색 로그' },
  { key: 'audit', label: '감사 로그' },
  { key: 'smtp', label: 'SMTP 로그' },
]

const tab = ref('visit')
const rows = ref([])
const loading = ref(false)
const page = ref(1)
const size = 20
const total = ref(0)
const testEmail = ref('')
const sending = ref(false)

const endpointMap = {
  visit: '/admin/logs/visits',
  search: '/admin/logs/search',
  audit: '/admin/logs/audit',
  smtp: '/admin/logs/smtp',
}

const actionLabels = { LOGIN: '로그인', LOGOUT: '로그아웃', CREATE: '생성', UPDATE: '수정', DELETE: '삭제' }
const searchTypeLabels = { TITLE: '제목', CONTENT: '내용', WRITER: '작성자', ALL: '전체' }
const sendTypeLabels = {
  EMAIL_VERIFY: '이메일 인증',
  PASSWORD_RESET: '비밀번호 재설정',
  INQUIRY_NOTIFY: '문의 알림',
  MESSAGE_NOTIFY: '쪽지 알림',
  NEWSLETTER: '뉴스레터',
  TEST: '테스트',
}

const MemberCell = {
  props: { row: { type: Object, required: true } },
  setup(props) {
    return () => h('div', { class: 'member-cell' }, [
      h('strong', props.row.memberName || props.row.memberUsername || '비회원'),
      props.row.memberUsername
        ? h('span', `@${props.row.memberUsername}${props.row.memberId ? ` · #${props.row.memberId}` : ''}`)
        : h('span', props.row.memberId ? `#${props.row.memberId}` : '-'),
    ])
  },
}

const EmptyLog = {
  props: { icon: { type: String, required: true }, text: { type: String, required: true } },
  setup(props) {
    return () => h('div', { class: 'at-empty' }, [
      h('i', { class: `ti ${props.icon}` }),
      h('span', props.text),
    ])
  },
}

async function fetchRows(p = page.value) {
  page.value = p
  loading.value = true
  try {
    const res = await api.get(endpointMap[tab.value], { params: { page: p - 1, size } })
    rows.value = res.data.data?.content || []
    total.value = res.data.data?.totalElements || 0
  } finally {
    loading.value = false
  }
}

function switchTab(nextTab) {
  tab.value = nextTab
  rows.value = []
  page.value = 1
  fetchRows(1)
}

async function sendTestMail() {
  if (!testEmail.value) {
    ElMessage.warning('이메일을 입력해주세요.')
    return
  }
  sending.value = true
  try {
    await api.post('/admin/logs/smtp/test', { toEmail: testEmail.value })
    ElMessage.success('테스트 메일을 발송했습니다.')
    if (tab.value === 'smtp') fetchRows(1)
  } finally {
    sending.value = false
  }
}

function fmtDt(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('ko-KR')
}

function displayPath(value) {
  if (!value) return '-'
  try {
    const url = new URL(value)
    return `${url.pathname}${url.search}${url.hash}` || '/'
  } catch {
    return value
  }
}

function shortUserAgent(value) {
  if (!value) return '-'
  const browser = value.match(/(Edg|Chrome|Firefox|Safari)\/[\d.]+/)?.[0] || 'Browser'
  const os = value.includes('Windows') ? 'Windows'
    : value.includes('Mac OS') ? 'macOS'
      : value.includes('Android') ? 'Android'
        : value.includes('iPhone') ? 'iOS'
          : 'Unknown'
  return `${browser} · ${os}`
}

function targetLabel(row) {
  if (row.targetName || row.targetUsername) {
    return `${row.targetName || row.targetUsername} ${row.targetUsername ? `(@${row.targetUsername})` : ''}`
  }
  return row.targetTable || '-'
}

function compactValue(value) {
  if (!value) return '-'
  const text = String(value).replace(/\s+/g, ' ').trim()
  return text.length > 42 ? `${text.slice(0, 42)}...` : text
}

function auditBadge(type) {
  return { LOGIN: 'badge-info', LOGOUT: 'badge-muted', CREATE: 'badge-success', UPDATE: 'badge-warning', DELETE: 'badge-danger' }[type] || 'badge-muted'
}

function actionLabel(type) {
  return actionLabels[type] || type || '-'
}

function searchTypeLabel(type) {
  return searchTypeLabels[type] || type || '-'
}

function sendTypeLabel(type) {
  return sendTypeLabels[type] || type || '-'
}

onMounted(() => fetchRows())
</script>

<style scoped>
@import '@/assets/admin-table.css';

.smtp-test {
  display: flex;
  align-items: center;
  gap: 8px;
}

.smtp-test input {
  width: 210px;
}

.log-card {
  overflow: hidden;
}

.adm-tabs {
  border-bottom: 1px solid var(--adm-border);
}

.adm-tab {
  background: transparent;
  border: 0;
  cursor: pointer;
}

.log-table {
  min-height: 560px;
}

.log-head,
.log-row {
  display: grid;
  gap: 12px;
  align-items: center;
}

.log-head {
  min-height: 38px;
  padding: 0 18px;
  background: var(--adm-bg);
  border-bottom: 1px solid var(--adm-border);
  color: var(--adm-muted);
  font-size: 12px;
  font-weight: 700;
}

.log-row {
  min-height: 58px;
  padding: 10px 18px;
  border-bottom: 1px solid var(--adm-border);
  font-size: 13px;
}

.log-row:hover {
  background: rgba(47, 95, 214, 0.035);
}

.visit-grid { grid-template-columns: 150px 170px minmax(280px, 1fr) 150px 170px; }
.search-grid { grid-template-columns: 150px 170px minmax(220px, 1fr) 100px 70px 150px; }
.audit-grid { grid-template-columns: 150px 170px 90px 210px minmax(240px, 1fr) 150px; }
.smtp-grid { grid-template-columns: 150px 170px 220px minmax(240px, 1fr) 120px 80px; }

.member-cell,
.uri-cell,
.target-cell,
.subject-cell {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.member-cell strong,
.uri-cell strong,
.target-cell strong,
.subject-cell strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--adm-text);
}

.member-cell span,
.uri-cell span,
.target-cell span,
.subject-cell span,
.ua-cell,
.muted {
  color: var(--adm-muted);
}

.mono,
.uri-cell {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}

.keyword,
.change-cell,
.ua-cell {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.error-text {
  color: var(--adm-danger, #dc2626);
}

@media (max-width: 1100px) {
  .log-head {
    display: none;
  }

  .log-row {
    grid-template-columns: 1fr;
    align-items: flex-start;
  }

  .smtp-test {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
