<template>
  <div class="adm-page">
    <AdminPageHeader title="문의 관리" desc="접수된 문의 목록 및 처리 상태 관리">
      <div class="adm-search">
        <input v-model="keyword" class="adm-search-input" placeholder="제목/내용 검색" @keyup.enter="fetchInquiries(1)" />
        <el-select v-model="statusFilter" style="width:110px" @change="fetchInquiries(1)">
          <el-option value="" label="전체 상태" />
          <el-option value="RECEIVED" label="접수" />
          <el-option value="CHECKING" label="확인중" />
          <el-option value="DONE" label="처리완료" />
        </el-select>
      </div>
    </AdminPageHeader>

    <div class="adm-card" v-loading="loading">
      <div class="at-wrap">
        <div class="at-head">
          <div class="at-col" style="flex:1">제목</div>
          <div class="at-col" style="width:120px">연락처</div>
          <div class="at-col" style="width:120px">이메일</div>
          <div class="at-col" style="width:80px;text-align:center">상태</div>
          <div class="at-col" style="width:120px">접수일</div>
          <div class="at-col" style="width:60px;text-align:center">관리</div>
        </div>
        <div v-for="row in list" :key="row.id" class="at-row clickable" @click="openDetail(row)">
          <div class="at-col bold" style="flex:1">{{ row.title }}</div>
          <div class="at-col muted" style="width:120px">{{ row.contactPhone || '-' }}</div>
          <div class="at-col muted" style="width:120px">{{ row.contactEmail || '-' }}</div>
          <div class="at-col" style="width:80px;text-align:center">
            <span :class="['adm-badge', statusBadge(row.status)]">{{ statusLabel(row.status) }}</span>
          </div>
          <div class="at-col muted" style="width:120px;font-size:12px">{{ fmtDate(row.createdAt) }}</div>
          <div class="at-col at-actions" style="width:60px" @click.stop>
            <button class="act-btn danger" @click="deleteInquiry(row.id)"><i class="ti ti-trash"></i></button>
          </div>
        </div>
        <div v-if="!list.length && !loading" class="at-empty"><i class="ti ti-mail"></i><span>접수된 문의가 없습니다.</span></div>
      </div>
      <div class="adm-pagination">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="prev,pager,next" @current-change="fetchInquiries" />
      </div>
    </div>

    <!-- 상세 다이얼로그 -->
    <el-dialog v-model="showDetail" title="문의 상세" width="580px" destroy-on-close>
      <div v-if="detail" class="dlg-form">
        <div class="detail-meta">
          <span><b>제목</b> {{ detail.title }}</span>
          <span><b>연락처</b> {{ detail.contactPhone || '-' }}</span>
          <span><b>이메일</b> {{ detail.contactEmail || '-' }}</span>
          <span><b>IP</b> {{ detail.clientIp || '-' }}</span>
          <span><b>접수일</b> {{ fmtDate(detail.createdAt) }}</span>
        </div>
        <div class="detail-content">{{ detail.content }}</div>
        <hr class="dlg-divider" />
        <div class="dlg-row">
          <div class="dlg-field">
            <label>처리 상태</label>
            <el-select v-model="detailStatus" style="width:100%">
              <el-option value="RECEIVED" label="접수" />
              <el-option value="CHECKING" label="확인중" />
              <el-option value="DONE" label="처리완료" />
            </el-select>
          </div>
          <div style="display:flex;align-items:flex-end">
            <button class="adm-btn primary" @click="updateStatus">상태 저장</button>
          </div>
        </div>
        <div class="dlg-section-title" style="margin-top:4px">메모</div>
        <div v-for="m in detail.memos" :key="m.id" class="memo-item">
          <span class="memo-author">{{ m.createdByName }}</span>
          <span class="memo-date">{{ fmtDate(m.createdAt) }}</span>
          <p>{{ m.memo }}</p>
        </div>
        <div style="display:flex;gap:8px;margin-top:6px">
          <el-input v-model="newMemo" placeholder="메모 입력" style="flex:1" @keyup.enter="addMemo" />
          <button class="adm-btn primary" @click="addMemo">추가</button>
        </div>
      </div>
      <template #footer>
        <button class="adm-btn ghost" @click="showDetail = false">닫기</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'

const list = ref([]); const loading = ref(false)
const keyword = ref(''); const statusFilter = ref('')
const page = ref(1); const size = 15; const total = ref(0)
const showDetail = ref(false); const detail = ref(null)
const detailStatus = ref(''); const newMemo = ref('')

async function fetchInquiries(p = page.value) {
  page.value = p; loading.value = true
  try { const r = await api.get('/admin/inquiries', { params: { keyword: keyword.value, status: statusFilter.value, page: p - 1, size } }); list.value = r.data.data?.content || []; total.value = r.data.data?.totalElements || 0 }
  finally { loading.value = false }
}
async function openDetail(row) {
  const r = await api.get(`/admin/inquiries/${row.id}`)
  detail.value = r.data.data; detailStatus.value = detail.value.status; showDetail.value = true
}
async function updateStatus() { await api.put(`/admin/inquiries/${detail.value.id}/status`, { status: detailStatus.value }); ElMessage.success('상태가 변경되었습니다.'); detail.value.status = detailStatus.value; fetchInquiries() }
async function addMemo() { if (!newMemo.value.trim()) return; await api.post(`/admin/inquiries/${detail.value.id}/memos`, { memo: newMemo.value }); newMemo.value = ''; const r = await api.get(`/admin/inquiries/${detail.value.id}`); detail.value = r.data.data }
async function deleteInquiry(id) { await ElMessageBox.confirm('문의를 삭제하시겠습니까?', '삭제', { type: 'warning', confirmButtonText: '삭제', cancelButtonText: '취소' }); await api.delete(`/admin/inquiries/${id}`); ElMessage.success('삭제되었습니다.'); fetchInquiries() }

function statusLabel(s) { return { RECEIVED: '접수', CHECKING: '확인중', DONE: '처리완료' }[s] || s }
function statusBadge(s) { return { RECEIVED: 'badge-warning', CHECKING: 'badge-info', DONE: 'badge-success' }[s] || 'badge-muted' }
function fmtDate(d) { if (!d) return '-'; return new Date(d).toLocaleDateString('ko-KR') }
onMounted(() => fetchInquiries())
</script>

<style scoped>
@import '@/assets/admin-table.css';
.at-row.clickable { cursor: pointer; }
.detail-meta { display: flex; flex-wrap: wrap; gap: 8px 20px; font-size: 13px; color: var(--t2); padding: 10px 12px; background: var(--surface2); border-radius: var(--radius-xs); }
.detail-meta b { color: var(--t3); font-weight: 700; margin-right: 4px; }
.detail-content { padding: 12px; border: 0.5px solid var(--border2); border-radius: var(--radius-xs); font-size: 14px; line-height: 1.6; white-space: pre-wrap; min-height: 80px; }
.memo-item { padding: 8px 10px; background: var(--surface2); border-radius: var(--radius-xs); margin-bottom: 6px; font-size: 13px; }
.memo-author { font-weight: 700; color: var(--t1); margin-right: 8px; }
.memo-date { font-size: 11px; color: var(--t4); }
.memo-item p { margin: 4px 0 0; color: var(--t2); }
</style>
