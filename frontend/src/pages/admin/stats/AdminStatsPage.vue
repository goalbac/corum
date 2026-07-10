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

      <div v-if="tab === 'visit'" class="at-wrap log-table" v-loading="loading">
        <div class="at-head">
          <div class="at-col" style="width:150px">시각</div>
          <div class="at-col" style="width:170px">회원</div>
          <div class="at-col" style="flex:1;min-width:260px">화면</div>
          <div class="at-col" style="width:145px">IP</div>
          <div class="at-col" style="width:170px">환경</div>
        </div>
        <div v-for="row in rows" :key="row.id" class="at-row log-row">
          <div class="at-col muted" style="width:150px;font-size:12px">{{ fmtDt(row.createdAt) }}</div>
          <div class="at-col" style="width:170px"><MemberCell :row="row" /></div>
          <div class="at-col" style="flex:1;min-width:260px">
            <div class="uri-cell">
            <strong>{{ displayPath(row.requestUri) }}</strong>
            <span v-if="row.referer && displayPath(row.referer) !== displayPath(row.requestUri)">이전: {{ displayPath(row.referer) }}</span>
            </div>
          </div>
          <div class="at-col mono muted" style="width:145px;font-size:12px">{{ row.ipAddress || '-' }}</div>
          <div class="at-col muted" style="width:170px" :title="row.userAgent || ''">
            <span class="single-line">{{ shortUserAgent(row.userAgent) }}</span>
          </div>
        </div>
        <EmptyLog v-if="!rows.length && !loading" icon="ti-eye" text="방문 로그가 없습니다." />
      </div>

      <div v-if="tab === 'search'" class="at-wrap log-table" v-loading="loading">
        <div class="at-head">
          <div class="at-col" style="width:150px">시각</div>
          <div class="at-col" style="width:170px">회원</div>
          <div class="at-col" style="flex:1;min-width:220px">검색어</div>
          <div class="at-col" style="width:100px">유형</div>
          <div class="at-col" style="width:70px;text-align:center">결과</div>
          <div class="at-col" style="width:145px">IP</div>
        </div>
        <div v-for="row in rows" :key="row.id" class="at-row log-row">
          <div class="at-col muted" style="width:150px;font-size:12px">{{ fmtDt(row.createdAt) }}</div>
          <div class="at-col" style="width:170px"><MemberCell :row="row" /></div>
          <div class="at-col bold" style="flex:1;min-width:220px">{{ row.keyword }}</div>
          <div class="at-col muted" style="width:100px">{{ searchTypeLabel(row.searchType) }}</div>
          <div class="at-col" style="width:70px;text-align:center">{{ row.resultCount?.toLocaleString?.() ?? 0 }}</div>
          <div class="at-col mono muted" style="width:145px;font-size:12px">{{ row.ipAddress || '-' }}</div>
        </div>
        <EmptyLog v-if="!rows.length && !loading" icon="ti-search" text="검색 로그가 없습니다." />
      </div>

      <div v-if="tab === 'audit'" class="at-wrap log-table" v-loading="loading">
        <div class="at-head">
          <div class="at-col" style="width:150px">시각</div>
          <div class="at-col" style="width:170px">실행 회원</div>
          <div class="at-col" style="width:90px;text-align:center">동작</div>
          <div class="at-col" style="width:210px">대상</div>
          <div class="at-col" style="flex:1;min-width:240px">변경 내용</div>
          <div class="at-col" style="width:145px">IP</div>
        </div>
        <div v-for="row in rows" :key="row.id" class="at-row log-row">
          <div class="at-col muted" style="width:150px;font-size:12px">{{ fmtDt(row.createdAt) }}</div>
          <div class="at-col" style="width:170px"><MemberCell :row="row" /></div>
          <div class="at-col" style="width:90px;text-align:center"><span :class="['adm-badge', auditBadge(row.actionType)]">{{ actionLabel(row.actionType) }}</span></div>
          <div class="at-col" style="width:210px">
            <div class="target-cell">
            <strong>{{ targetLabel(row) }}</strong>
            <span>{{ row.targetTable || '-' }} #{{ row.targetId || '-' }}</span>
            </div>
          </div>
          <div class="at-col muted" style="flex:1;min-width:240px" :title="changeText(row)">
            <span class="single-line">
            <span v-if="row.beforeValue || row.afterValue">{{ compactValue(row.beforeValue) }} → {{ compactValue(row.afterValue) }}</span>
            <span v-else class="muted">-</span>
            </span>
          </div>
          <div class="at-col mono muted" style="width:145px;font-size:12px">{{ row.ipAddress || '-' }}</div>
        </div>
        <EmptyLog v-if="!rows.length && !loading" icon="ti-clipboard-list" text="감사 로그가 없습니다." />
      </div>

      <div v-if="tab === 'smtp'" class="at-wrap log-table" v-loading="loading">
        <div class="at-head">
          <div class="at-col" style="width:150px">시각</div>
          <div class="at-col" style="width:170px">요청 회원</div>
          <div class="at-col" style="width:220px">수신자</div>
          <div class="at-col" style="flex:1;min-width:240px">제목</div>
          <div class="at-col" style="width:120px">유형</div>
          <div class="at-col" style="width:80px;text-align:center">결과</div>
        </div>
        <div v-for="row in rows" :key="row.id" class="at-row log-row">
          <div class="at-col muted" style="width:150px;font-size:12px">{{ fmtDt(row.createdAt) }}</div>
          <div class="at-col" style="width:170px"><MemberCell :row="row" /></div>
          <div class="at-col mono" style="width:220px;font-size:12px">{{ row.toEmail }}</div>
          <div class="at-col" style="flex:1;min-width:240px">
            <div class="subject-cell">
            <strong>{{ row.subject || '-' }}</strong>
            <span v-if="row.errorMessage" class="error-text">{{ row.errorMessage }}</span>
            </div>
          </div>
          <div class="at-col muted" style="width:120px">{{ sendTypeLabel(row.sendType) }}</div>
          <div class="at-col" style="width:80px;text-align:center">
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

function changeText(row) {
  if (!row.beforeValue && !row.afterValue) return ''
  return `${compactValue(row.beforeValue)} → ${compactValue(row.afterValue)}`
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

.log-row {
  min-height: 58px;
  padding-top: 8px;
  padding-bottom: 8px;
}

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
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--t1);
}

.member-cell span,
.uri-cell span,
.target-cell span,
.subject-cell span,
.muted {
  color: var(--t4);
  font-size: 12px;
}

.mono,
.uri-cell {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}

.single-line {
  display: block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.error-text {
  color: #dc2626;
}

@media (max-width: 1100px) {
  .smtp-test {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
