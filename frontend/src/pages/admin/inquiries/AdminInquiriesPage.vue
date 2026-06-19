<template>
  <div class="adm-page">
    <AdminPageHeader title="문의 관리" desc="접수된 문의 목록 및 처리 상태 관리">
      <div class="adm-search">
        <input v-model="keyword" class="adm-search-input" placeholder="제목/내용 검색" @keyup.enter="fetchInquiries(1)" />
        <el-select v-model="typeFilter" style="width:120px" @change="fetchInquiries(1)">
          <el-option value="" label="전체 유형" />
          <el-option value="INQUIRY" label="문의" />
          <el-option value="BUG_REPORT" label="오류 제보" />
          <el-option value="FEATURE_REQUEST" label="기능 제안" />
        </el-select>
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
          <div class="at-col" style="width:90px;text-align:center">유형</div>
          <div class="at-col" style="flex:1">제목</div>
          <div class="at-col" style="width:90px">접수자</div>
          <div class="at-col" style="width:80px;text-align:center">상태</div>
          <div class="at-col" style="width:80px;text-align:center">답변</div>
          <div class="at-col" style="width:100px">접수일</div>
          <div class="at-col" style="width:52px;text-align:center">관리</div>
        </div>
        <div v-for="row in list" :key="row.id" class="at-row clickable" @click="openDetail(row)">
          <div class="at-col" style="width:90px;text-align:center">
            <span :class="['type-chip', typeChipClass(row.inquiryType)]">
              <i :class="`ti ${typeIcon(row.inquiryType)}`"></i>
              {{ typeLabel(row.inquiryType) }}
            </span>
          </div>
          <div class="at-col bold" style="flex:1">{{ row.title }}</div>
          <div class="at-col muted" style="width:90px;font-size:12px">{{ row.writerName || '-' }}</div>
          <div class="at-col" style="width:80px;text-align:center">
            <span :class="['adm-badge', statusBadge(row.status)]">{{ statusLabel(row.status) }}</span>
          </div>
          <div class="at-col" style="width:80px;text-align:center">
            <span v-if="row.replyContent" class="reply-done-chip"><i class="ti ti-check"></i> 완료</span>
            <span v-else class="reply-none-chip">미답변</span>
          </div>
          <div class="at-col muted" style="width:100px;font-size:12px">{{ fmtDate(row.createdAt) }}</div>
          <div class="at-col at-actions" style="width:52px" @click.stop>
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
    <el-dialog v-model="showDetail" width="620px" destroy-on-close :show-close="false" class="detail-dlg">
      <template #header>
        <div class="dlg-header">
          <div class="dlg-header-left">
            <span v-if="detail?.inquiryType && detail.inquiryType !== 'INQUIRY'"
                  :class="['mi-type-badge', detail.inquiryType === 'BUG_REPORT' ? 'type-bug' : 'type-feature']">
              <i :class="`ti ${detail.inquiryType === 'BUG_REPORT' ? 'ti-bug' : 'ti-sparkles'}`"></i>
              {{ detail.inquiryType === 'BUG_REPORT' ? '오류 제보' : '기능 제안' }}
            </span>
            <span v-else class="mi-type-badge type-inquiry">
              <i class="ti ti-mail"></i> 문의
            </span>
            <span :class="['status-dot', statusBadge(detail?.status)]">{{ statusLabel(detail?.status) }}</span>
          </div>
          <button class="dlg-close-btn" @click="showDetail = false"><i class="ti ti-x"></i></button>
        </div>
        <div class="dlg-title-row">{{ detail?.title }}</div>
      </template>

      <div v-if="detail" class="detail-body">
        <!-- 접수자 정보 -->
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label"><i class="ti ti-user"></i> 접수자</span>
            <span class="info-value">{{ detail.writerName || '(미입력)' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label"><i class="ti ti-phone"></i> 연락처</span>
            <span class="info-value">{{ detail.contactPhone || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label"><i class="ti ti-mail"></i> 이메일</span>
            <span class="info-value">{{ detail.contactEmail || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label"><i class="ti ti-calendar"></i> 접수일</span>
            <span class="info-value">{{ fmtDateTime(detail.createdAt) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label"><i class="ti ti-world"></i> IP</span>
            <span class="info-value mono">{{ detail.clientIp || '-' }}</span>
          </div>
        </div>

        <!-- 본문 -->
        <div class="detail-content-box">{{ detail.content }}</div>

        <!-- 처리 상태 -->
        <div class="status-row">
          <span class="section-label">처리 상태</span>
          <div class="status-options">
            <button
              v-for="opt in statusOptions"
              :key="opt.value"
              :class="['status-opt-btn', opt.cls, { active: detailStatus === opt.value }]"
              @click="detailStatus = opt.value"
            >
              <i :class="`ti ${opt.icon}`"></i>
              {{ opt.label }}
            </button>
          </div>
          <button class="save-status-btn" :class="{ changed: detailStatus !== detail.status }" @click="updateStatus">
            저장
          </button>
        </div>

        <!-- 답변 -->
        <div class="reply-section">
          <div class="reply-section-head">
            <span class="section-label"><i class="ti ti-message-reply"></i> 답변</span>
            <span v-if="detail.repliedAt" class="reply-meta">
              {{ detail.repliedByName || '관리자' }} · {{ fmtDateTime(detail.repliedAt) }}
              <span class="reply-edit-hint">수정 가능</span>
            </span>
          </div>

          <!-- 기존 답변 표시 (수정 모드 아닐 때) -->
          <div v-if="detail.replyContent && !replyEditMode" class="reply-box">
            <div class="reply-content">{{ detail.replyContent }}</div>
            <div class="reply-box-actions">
              <button class="reply-edit-btn" @click="startEditReply">
                <i class="ti ti-pencil"></i> 수정
              </button>
            </div>
          </div>

          <!-- 답변 입력/수정 폼 -->
          <div v-else class="reply-form">
            <el-input
              v-model="replyContent"
              type="textarea"
              :rows="4"
              :placeholder="detail.replyContent ? '답변을 수정하세요.' : '접수자에게 전달할 답변을 작성하세요.\n답변을 등록하면 처리 상태가 자동으로 처리완료로 변경됩니다.'"
              resize="none"
            />
            <div class="reply-form-actions">
              <button v-if="detail.replyContent" class="adm-btn ghost sm" @click="cancelEditReply">취소</button>
              <button class="adm-btn primary sm" :disabled="!replyContent.trim()" :class="{ saving: replySaving }" @click="submitReply">
                <i class="ti ti-send"></i>
                {{ detail.replyContent ? '답변 수정' : '답변 등록' }}
              </button>
            </div>
          </div>
        </div>

        <!-- 구분선 -->
        <hr class="memo-divider" />

        <!-- 내부 메모 -->
        <div class="memo-section">
          <div class="section-label">내부 메모 <span class="memo-hint">(접수자에게 보이지 않음)</span></div>
          <div v-if="!detail.memos?.length" class="memo-empty">작성된 메모가 없습니다.</div>
          <div v-for="m in detail.memos" :key="m.id" class="memo-bubble">
            <div class="memo-bubble-top">
              <span class="memo-author"><i class="ti ti-user-circle"></i> {{ m.createdByName || '관리자' }}</span>
              <span class="memo-ts">{{ fmtDateTime(m.createdAt) }}</span>
            </div>
            <div class="memo-text">{{ m.memo }}</div>
          </div>
          <div class="memo-input-row">
            <el-input
              v-model="newMemo"
              placeholder="내부 메모를 입력하세요 (Enter로 추가)"
              @keyup.enter="addMemo"
            />
            <button class="adm-btn primary sm" @click="addMemo"><i class="ti ti-send"></i></button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'

const list = ref([]); const loading = ref(false)
const keyword = ref(''); const statusFilter = ref(''); const typeFilter = ref('')
const page = ref(1); const size = 15; const total = ref(0)
const showDetail = ref(false); const detail = ref(null)
const detailStatus = ref(''); const newMemo = ref('')
const replyContent = ref(''); const replyEditMode = ref(false); const replySaving = ref(false)

async function fetchInquiries(p = page.value) {
  page.value = p; loading.value = true
  try { const r = await api.get('/inquiries', { params: { keyword: keyword.value, status: statusFilter.value, inquiryType: typeFilter.value, page: p - 1, size } }); list.value = r.data.data?.content || []; total.value = r.data.data?.totalElements || 0 }
  finally { loading.value = false }
}
async function openDetail(row) {
  const r = await api.get(`/inquiries/${row.id}`)
  detail.value = r.data.data
  detailStatus.value = detail.value.status
  replyContent.value = detail.value.replyContent || ''
  replyEditMode.value = !detail.value.replyContent
  showDetail.value = true
}

function startEditReply() { replyContent.value = detail.value.replyContent; replyEditMode.value = true }
function cancelEditReply() { replyContent.value = detail.value.replyContent; replyEditMode.value = false }

async function submitReply() {
  if (!replyContent.value.trim()) return
  replySaving.value = true
  try {
    const r = await api.put(`/inquiries/${detail.value.id}/reply`, { content: replyContent.value })
    detail.value = r.data.data
    detailStatus.value = detail.value.status
    replyEditMode.value = false
    ElMessage.success('답변이 등록되었습니다.')
    fetchInquiries()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '답변 등록에 실패했습니다.')
  } finally { replySaving.value = false }
}
async function updateStatus() { await api.patch(`/inquiries/${detail.value.id}/status`, { status: detailStatus.value }); ElMessage.success('상태가 변경되었습니다.'); detail.value.status = detailStatus.value; fetchInquiries() }
async function addMemo() { if (!newMemo.value.trim()) return; await api.post(`/inquiries/${detail.value.id}/memos`, { memo: newMemo.value }); newMemo.value = ''; const r = await api.get(`/inquiries/${detail.value.id}`); detail.value = r.data.data }
async function deleteInquiry(id) { await ElMessageBox.confirm('문의를 삭제하시겠습니까?', '삭제', { type: 'warning', confirmButtonText: '삭제', cancelButtonText: '취소' }); await api.delete(`/inquiries/${id}`); ElMessage.success('삭제되었습니다.'); fetchInquiries() }

const statusOptions = [
  { value: 'RECEIVED', label: '접수',    icon: 'ti-inbox',       cls: 'opt-received' },
  { value: 'CHECKING', label: '확인중',  icon: 'ti-eye',         cls: 'opt-checking' },
  { value: 'DONE',     label: '처리완료', icon: 'ti-circle-check', cls: 'opt-done' },
]

function typeLabel(t) { return { INQUIRY: '문의', BUG_REPORT: '오류제보', FEATURE_REQUEST: '기능제안' }[t] || t }
function typeIcon(t) { return { INQUIRY: 'ti-mail', BUG_REPORT: 'ti-bug', FEATURE_REQUEST: 'ti-sparkles' }[t] || 'ti-mail' }
function typeChipClass(t) { return { INQUIRY: 'chip-inquiry', BUG_REPORT: 'chip-bug', FEATURE_REQUEST: 'chip-feature' }[t] || 'chip-inquiry' }
function statusLabel(s) { return { RECEIVED: '접수', CHECKING: '확인중', DONE: '처리완료' }[s] || s }
function statusBadge(s) { return { RECEIVED: 'badge-warning', CHECKING: 'badge-info', DONE: 'badge-success' }[s] || 'badge-muted' }
function fmtDate(d) { if (!d) return '-'; return new Date(d).toLocaleDateString('ko-KR') }
function fmtDateTime(d) {
  if (!d) return '-'
  const dt = new Date(d)
  return dt.toLocaleDateString('ko-KR') + ' ' + dt.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
}
onMounted(() => fetchInquiries())
</script>

<style scoped>
@import '@/assets/admin-table.css';
.at-row.clickable { cursor: pointer; }

/* 유형 칩 (목록용) */
.type-chip {
  display: inline-flex; align-items: center; gap: 3px;
  padding: 2px 7px; border-radius: 20px; font-size: 11px; font-weight: 700; white-space: nowrap;
}
.type-chip i { font-size: 11px; }
.chip-inquiry { background: var(--surface2); color: var(--t3); }
.chip-bug     { background: #FEE2E2; color: #991B1B; }
.chip-feature { background: #EDE9FE; color: #5B21B6; }

/* 답변 상태 칩 */
.reply-done-chip {
  display: inline-flex; align-items: center; gap: 3px;
  padding: 2px 7px; border-radius: 20px; font-size: 11px; font-weight: 700;
  background: #D1FAE5; color: #065F46;
}
.reply-none-chip {
  display: inline-block;
  padding: 2px 7px; border-radius: 20px; font-size: 11px; font-weight: 700;
  background: var(--surface2); color: var(--t4);
}

/* 유형 뱃지 (다이얼로그용) */
.mi-type-badge {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 2px 8px; border-radius: 20px; font-size: 11px; font-weight: 700;
}
.mi-type-badge i { font-size: 11px; }
.type-bug     { background: #FEE2E2; color: #991B1B; }
.type-feature { background: #EDE9FE; color: #5B21B6; }
.type-inquiry { background: var(--surface2); color: var(--t3); }

/* ===== 상세 다이얼로그 ===== */
.dlg-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 8px;
}
.dlg-header-left { display: flex; align-items: center; gap: 8px; }
.status-dot {
  display: inline-flex; align-items: center;
  padding: 2px 10px; border-radius: 20px; font-size: 11px; font-weight: 700;
}
.badge-warning { background: #FEF3C7; color: #92400E; }
.badge-info    { background: #DBEAFE; color: #1E40AF; }
.badge-success { background: #D1FAE5; color: #065F46; }
.badge-muted   { background: var(--surface2); color: var(--t4); }

.dlg-close-btn {
  background: none; border: none; color: var(--t4);
  cursor: pointer; font-size: 16px; padding: 4px; line-height: 1;
  border-radius: var(--radius-xs); transition: var(--transition);
}
.dlg-close-btn:hover { background: var(--surface2); color: var(--t1); }

.dlg-title-row {
  font-size: 16px; font-weight: 700; color: var(--t1); line-height: 1.4;
}

/* 상세 본문 */
.detail-body { display: flex; flex-direction: column; gap: 16px; }

/* 접수자 정보 그리드 */
.info-grid {
  display: grid; grid-template-columns: repeat(2, 1fr); gap: 6px;
  background: var(--surface2); border-radius: var(--radius-xs); padding: 12px 14px;
}
.info-item { display: flex; align-items: baseline; gap: 8px; font-size: 13px; }
.info-label {
  color: var(--t4); font-size: 12px; white-space: nowrap;
  display: flex; align-items: center; gap: 4px; min-width: 70px;
}
.info-label i { font-size: 12px; }
.info-value { color: var(--t1); font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.info-value.mono { font-family: monospace; font-size: 12px; color: var(--t3); }

/* 본문 */
.detail-content-box {
  padding: 12px 14px;
  border: 0.5px solid var(--border2);
  border-radius: var(--radius-xs);
  font-size: 14px; line-height: 1.7; white-space: pre-wrap;
  color: var(--t1); min-height: 80px; max-height: 240px; overflow-y: auto;
}

/* 처리 상태 */
.status-row {
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
}
.section-label {
  font-size: 12px; font-weight: 700; color: var(--t4);
  letter-spacing: 0.3px; white-space: nowrap;
}
.status-options { display: flex; gap: 4px; flex: 1; }
.status-opt-btn {
  display: flex; align-items: center; gap: 5px;
  padding: 5px 12px; border-radius: var(--radius-xs);
  border: 1.5px solid var(--border2); background: var(--surface);
  font-size: 12px; font-weight: 600; color: var(--t3);
  cursor: pointer; transition: var(--transition);
}
.status-opt-btn i { font-size: 13px; }
.status-opt-btn:hover { border-color: var(--border); color: var(--t1); }
.opt-received.active { border-color: #D97706; background: #FEF3C7; color: #92400E; }
.opt-checking.active { border-color: #2563EB; background: #DBEAFE; color: #1E40AF; }
.opt-done.active     { border-color: #059669; background: #D1FAE5; color: #065F46; }

.save-status-btn {
  padding: 5px 14px; border-radius: var(--radius-xs);
  border: 1.5px solid var(--border2); background: var(--surface);
  font-size: 12px; font-weight: 700; color: var(--t4);
  cursor: pointer; transition: var(--transition); white-space: nowrap;
}
.save-status-btn.changed {
  border-color: var(--accent); background: var(--accent); color: #fff;
}

/* 답변 */
.reply-section { display: flex; flex-direction: column; gap: 10px; }
.reply-section-head {
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
}
.reply-section-head .section-label {
  display: flex; align-items: center; gap: 5px;
}
.reply-meta { font-size: 11px; color: var(--t4); display: flex; align-items: center; gap: 6px; }
.reply-edit-hint {
  padding: 1px 6px; border-radius: 10px;
  background: var(--surface2); color: var(--t4);
  font-size: 10px; cursor: pointer;
}

.reply-box {
  background: #EFF6FF;
  border: 1px solid #BFDBFE;
  border-left: 4px solid #3B82F6;
  border-radius: var(--radius-xs);
  padding: 12px 14px;
}
.reply-content {
  font-size: 14px; color: #1e3a5f; line-height: 1.7; white-space: pre-wrap;
}
.reply-box-actions {
  display: flex; justify-content: flex-end; margin-top: 8px; padding-top: 8px;
  border-top: 0.5px solid #BFDBFE;
}
.reply-edit-btn {
  display: flex; align-items: center; gap: 4px;
  background: none; border: none; color: #3B82F6;
  font-size: 12px; font-weight: 600; cursor: pointer;
  padding: 2px 6px; border-radius: 4px;
}
.reply-edit-btn:hover { background: #DBEAFE; }

.reply-form { display: flex; flex-direction: column; gap: 8px; }
.reply-form-actions { display: flex; justify-content: flex-end; gap: 6px; }

.memo-divider { border: none; border-top: 0.5px solid var(--border); margin: 4px 0; }

/* 메모 */
.memo-hint { font-weight: 400; color: var(--t4); font-size: 11px; margin-left: 4px; }
.memo-section { display: flex; flex-direction: column; gap: 8px; }
.memo-empty { font-size: 13px; color: var(--t4); padding: 8px 0; }
.memo-bubble {
  background: var(--surface2);
  border-radius: var(--radius-xs);
  padding: 10px 12px;
}
.memo-bubble-top {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 6px;
}
.memo-author {
  display: flex; align-items: center; gap: 5px;
  font-size: 12px; font-weight: 700; color: var(--t1);
}
.memo-author i { font-size: 13px; color: var(--t4); }
.memo-ts { font-size: 11px; color: var(--t4); }
.memo-text { font-size: 13px; color: var(--t2); line-height: 1.5; white-space: pre-wrap; }

.memo-input-row { display: flex; gap: 6px; align-items: center; }
.adm-btn.sm { padding: 6px 12px; font-size: 13px; }
</style>
