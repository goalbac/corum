<template>
  <div>
    <AdminPageHeader title="문의 관리" desc="접수된 문의 목록 및 처리">
      <el-select v-model="statusFilter" size="small" style="width:120px" @change="fetchInquiries">
        <el-option value="" label="전체" />
        <el-option value="RECEIVED"    label="접수" />
        <el-option value="IN_PROGRESS" label="처리중" />
        <el-option value="COMPLETED"   label="완료" />
      </el-select>
    </AdminPageHeader>

    <el-table :data="inquiries" v-loading="loading" border @row-click="openDetail">
      <el-table-column label="ID" prop="id" width="60" align="center" />
      <el-table-column label="제목" prop="title" min-width="200" />
      <el-table-column label="연락처" prop="contactPhone" width="130" />
      <el-table-column label="이메일" prop="contactEmail" min-width="160" />
      <el-table-column label="IP" prop="clientIp" width="120">
        <template #default="{ row }"><span class="text-muted">{{ row.clientIp }}</span></template>
      </el-table-column>
      <el-table-column label="상태" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusColor(row.status)" size="small" effect="dark">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="접수일" width="110" align="center">
        <template #default="{ row }"><span class="text-muted">{{ formatDate(row.createdAt) }}</span></template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination v-model:current-page="page" :page-size="20" :total="total"
        layout="prev, pager, next" background small @current-change="fetchInquiries" />
    </div>

    <!-- 문의 상세 -->
    <el-dialog v-model="showDetail" title="문의 상세" width="600px" destroy-on-close>
      <div v-if="selectedInquiry" class="inquiry-detail">
        <div class="detail-header">
          <div class="detail-title-text">{{ selectedInquiry.title }}</div>
          <el-select v-model="selectedInquiry.status" size="small" @change="updateStatus">
            <el-option value="RECEIVED"    label="접수" />
            <el-option value="IN_PROGRESS" label="처리중" />
            <el-option value="COMPLETED"   label="완료" />
          </el-select>
        </div>
        <div class="detail-meta">
          <span>연락처: {{ selectedInquiry.contactPhone || '-' }}</span>
          <span>이메일: {{ selectedInquiry.contactEmail || '-' }}</span>
          <span>IP: {{ selectedInquiry.clientIp }}</span>
          <span>{{ formatDate(selectedInquiry.createdAt) }}</span>
        </div>
        <div class="detail-content">{{ selectedInquiry.content }}</div>

        <div class="memo-section">
          <div class="memo-title">관리자 메모</div>
          <div v-for="memo in selectedInquiry.memos" :key="memo.id" class="memo-item">
            <div class="memo-content">{{ memo.memo }}</div>
            <div class="memo-footer">
              <span class="text-muted">{{ formatDate(memo.createdAt) }}</span>
              <button class="del-btn" @click="deleteMemo(memo.id)"><i class="ti ti-trash"></i></button>
            </div>
          </div>
          <div class="memo-add">
            <el-input v-model="newMemo" type="textarea" :rows="2" placeholder="메모 추가..." resize="none" />
            <el-button size="small" type="primary" @click="addMemo">추가</el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'

const inquiries      = ref([])
const loading        = ref(false)
const total          = ref(0)
const page           = ref(1)
const statusFilter   = ref('')
const showDetail     = ref(false)
const selectedInquiry = ref(null)
const newMemo        = ref('')

async function fetchInquiries() {
  loading.value = true
  try {
    const params = { page: page.value - 1, size: 20 }
    if (statusFilter.value) params.status = statusFilter.value
    const res = await api.get('/inquiries', { params })
    inquiries.value = res.data.data?.content || []
    total.value     = res.data.data?.totalElements || 0
  } finally { loading.value = false }
}

async function openDetail(row) {
  const res = await api.get(`/inquiries/${row.id}`)
  selectedInquiry.value = res.data.data
  showDetail.value = true
  newMemo.value = ''
}

async function updateStatus() {
  await api.patch(`/inquiries/${selectedInquiry.value.id}/status`, { status: selectedInquiry.value.status })
  ElMessage.success('상태가 변경되었습니다.')
  fetchInquiries()
}

async function addMemo() {
  if (!newMemo.value.trim()) return
  await api.post(`/inquiries/${selectedInquiry.value.id}/memos`, { memo: newMemo.value })
  newMemo.value = ''
  const res = await api.get(`/inquiries/${selectedInquiry.value.id}`)
  selectedInquiry.value = res.data.data
}

async function deleteMemo(memoId) {
  await api.delete(`/inquiries/memos/${memoId}`)
  const res = await api.get(`/inquiries/${selectedInquiry.value.id}`)
  selectedInquiry.value = res.data.data
}

function formatDate(d) {
  if (!d) return '-'
  return new Date(d).toLocaleString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function statusLabel(s) { return { RECEIVED: '접수', IN_PROGRESS: '처리중', COMPLETED: '완료' }[s] || s }
function statusColor(s) { return { RECEIVED: 'warning', IN_PROGRESS: 'primary', COMPLETED: 'success' }[s] || '' }

onMounted(fetchInquiries)
</script>

<style scoped>
.pagination { display: flex; justify-content: center; padding: 16px 0; }
.text-muted { font-size: 12px; color: var(--t3); }

.inquiry-detail { display: flex; flex-direction: column; gap: 16px; }
.detail-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.detail-title-text { font-size: 16px; font-weight: 600; color: var(--t1); flex: 1; }
.detail-meta { display: flex; flex-wrap: wrap; gap: 12px; font-size: 12.5px; color: var(--t2); padding: 10px 0; border-top: 0.5px solid var(--border2); border-bottom: 0.5px solid var(--border2); }
.detail-content { font-size: 14px; line-height: 1.8; color: var(--t1); white-space: pre-wrap; min-height: 60px; }

.memo-section { border-top: 0.5px solid var(--border2); padding-top: 14px; }
.memo-title { font-size: 13px; font-weight: 600; color: var(--t2); margin-bottom: 10px; }
.memo-item { background: var(--surface2); border-radius: var(--radius-xs); padding: 10px 12px; margin-bottom: 8px; }
.memo-content { font-size: 13px; color: var(--t1); white-space: pre-wrap; }
.memo-footer { display: flex; align-items: center; justify-content: space-between; margin-top: 6px; }
.memo-add { display: flex; gap: 8px; align-items: flex-end; margin-top: 10px; }
.memo-add .el-textarea { flex: 1; }
.del-btn { background: none; border: none; color: var(--t3); cursor: pointer; font-size: 14px; padding: 2px; transition: var(--transition); }
.del-btn:hover { color: var(--new); }
</style>
