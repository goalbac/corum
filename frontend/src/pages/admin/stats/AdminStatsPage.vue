<template>
  <div class="admin-stats">
    <AdminPageHeader title="통계 / 로그" desc="방문, 검색, 감사, SMTP 발송 이력을 확인합니다.">
      <el-input v-model="testEmail" placeholder="test@example.com" size="small" class="test-email" />
      <el-button size="small" type="primary" :loading="sending" @click="sendTestMail">SMTP 테스트</el-button>
    </AdminPageHeader>

    <div class="stat-grid">
      <div class="stat-card">
        <strong>{{ stats.todayVisits ?? 0 }}</strong>
        <span>오늘 방문</span>
      </div>
      <div class="stat-card">
        <strong>{{ stats.todayUniqueVisits ?? 0 }}</strong>
        <span>오늘 고유 방문</span>
      </div>
      <div class="stat-card">
        <strong>{{ stats.memberCount ?? 0 }}</strong>
        <span>회원</span>
      </div>
      <div class="stat-card">
        <strong>{{ stats.pendingInquiryCount ?? 0 }}</strong>
        <span>미처리 문의</span>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="log-tabs" @tab-change="fetchLogs">
      <el-tab-pane label="방문 로그" name="visits">
        <el-table :data="logs.visits" v-loading="loading" border>
          <el-table-column prop="createdAt" label="일시" width="180" :formatter="dateFormatter" />
          <el-table-column prop="memberId" label="회원 ID" width="100" />
          <el-table-column prop="ipAddress" label="IP" width="150" />
          <el-table-column prop="requestUri" label="요청 URI" min-width="240" show-overflow-tooltip />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="검색 로그" name="search">
        <el-table :data="logs.search" v-loading="loading" border>
          <el-table-column prop="createdAt" label="일시" width="180" :formatter="dateFormatter" />
          <el-table-column prop="memberId" label="회원 ID" width="100" />
          <el-table-column prop="keyword" label="검색어" min-width="180" />
          <el-table-column prop="searchType" label="유형" width="120" />
          <el-table-column prop="resultCount" label="결과" width="90" align="center" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="감사 로그" name="audit">
        <el-table :data="logs.audit" v-loading="loading" border>
          <el-table-column prop="createdAt" label="일시" width="180" :formatter="dateFormatter" />
          <el-table-column prop="memberId" label="회원 ID" width="100" />
          <el-table-column prop="actionType" label="액션" width="120" />
          <el-table-column prop="targetTable" label="대상" width="140" />
          <el-table-column prop="targetId" label="대상 ID" width="100" />
          <el-table-column prop="ipAddress" label="IP" width="150" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="SMTP 로그" name="smtp">
        <el-table :data="logs.smtp" v-loading="loading" border>
          <el-table-column prop="createdAt" label="일시" width="180" :formatter="dateFormatter" />
          <el-table-column prop="sendType" label="유형" width="140" />
          <el-table-column prop="toEmail" label="수신자" min-width="180" />
          <el-table-column prop="subject" label="제목" min-width="220" show-overflow-tooltip />
          <el-table-column label="결과" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.success ? 'success' : 'danger'" size="small">{{ row.success ? '성공' : '실패' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="errorMessage" label="오류" min-width="220" show-overflow-tooltip />
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <div class="pagination">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        background
        small
        @current-change="fetchLogs"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'

const activeTab = ref('visits')
const loading = ref(false)
const sending = ref(false)
const testEmail = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)
const stats = ref({})
const logs = ref({ visits: [], search: [], audit: [], smtp: [] })

async function fetchStats() {
  const res = await api.get('/dashboard/stats')
  stats.value = res.data.data || {}
}

async function fetchLogs() {
  loading.value = true
  try {
    const res = await api.get(`/admin/logs/${activeTab.value}`, {
      params: { page: page.value - 1, size: size.value }
    })
    logs.value[activeTab.value] = res.data.data?.content || []
    total.value = res.data.data?.totalElements || 0
  } finally {
    loading.value = false
  }
}

async function sendTestMail() {
  if (!testEmail.value) {
    ElMessage.warning('받는 이메일을 입력해주세요.')
    return
  }
  sending.value = true
  try {
    await api.post('/admin/logs/smtp/test', { toEmail: testEmail.value })
    ElMessage.success('테스트 메일을 발송했습니다.')
    activeTab.value = 'smtp'
    page.value = 1
    await fetchLogs()
  } finally {
    sending.value = false
  }
}

function dateFormatter(row, column, value) {
  return value ? new Date(value).toLocaleString('ko-KR') : '-'
}

onMounted(async () => {
  await Promise.all([fetchStats(), fetchLogs()])
})
</script>

<style scoped>
.admin-stats { display: flex; flex-direction: column; gap: 18px; }
.test-email { width: 220px; }
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.stat-card {
  padding: 18px;
  border: 0.5px solid var(--border2);
  border-radius: var(--radius-sm);
  background: var(--surface);
  box-shadow: var(--shadow);
}
.stat-card strong {
  display: block;
  font-size: 26px;
  font-weight: 800;
  color: var(--accent-t);
}
.stat-card span {
  display: block;
  margin-top: 4px;
  font-size: 14px;
  color: var(--t2);
}
.log-tabs {
  padding: 16px;
  border: 0.5px solid var(--border2);
  border-radius: var(--radius-sm);
  background: var(--surface);
  box-shadow: var(--shadow);
}
.pagination { display: flex; justify-content: center; }
@media (max-width: 768px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .test-email { width: 100%; }
}
</style>
