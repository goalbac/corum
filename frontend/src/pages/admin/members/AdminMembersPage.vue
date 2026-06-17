<template>
  <div class="adm-page">
    <AdminPageHeader title="회원 관리" desc="가입 회원 목록, 계정 잠금, 그룹 관리">
      <div style="display:flex;gap:8px">
        <button class="adm-btn primary" @click="openCreate"><i class="ti ti-user-plus"></i> 회원 추가</button>
        <button class="adm-btn ghost" @click="download('/admin/members/export/excel', 'members.xlsx')"><i class="ti ti-table-export"></i> 엑셀</button>
        <button class="adm-btn ghost" @click="download('/admin/members/export/pdf', 'members.pdf')"><i class="ti ti-file-type-pdf"></i> PDF</button>
      </div>
    </AdminPageHeader>

    <div class="adm-card">
      <div class="adm-toolbar">
        <input v-model="keyword" class="adm-search-input" placeholder="이름/아이디/이메일 검색" @keyup.enter="fetchMembers(1)" />
        <el-select v-model="groupFilter" style="width:160px" clearable placeholder="그룹 필터" @change="fetchMembers(1)">
          <el-option v-for="g in filterGroupOptions" :key="g.id" :value="g.id" :label="g.label" />
        </el-select>
        <el-select v-model="statusFilter" style="width:110px" @change="fetchMembers(1)">
          <el-option value="" label="전체" />
          <el-option value="active" label="활성" />
          <el-option value="locked" label="잠금" />
          <el-option value="withdrawn" label="탈퇴" />
        </el-select>
        <button class="adm-btn primary" @click="fetchMembers(1)"><i class="ti ti-search"></i></button>
      </div>

      <div class="at-wrap" v-loading="loading">
        <div class="at-head">
          <div class="at-col" style="width:60px;text-align:center">ID</div>
          <div class="at-col" style="width:100px">이름</div>
          <div class="at-col" style="width:130px">아이디</div>
          <div class="at-col" style="flex:1">이메일</div>
          <div class="at-col" style="width:140px">그룹</div>
          <div class="at-col" style="width:80px;text-align:center">상태</div>
          <div class="at-col" style="width:120px">가입일</div>
          <div class="at-col" style="width:110px;text-align:center">관리</div>
        </div>
        <div v-for="row in members" :key="row.id" class="at-row">
          <div class="at-col muted" style="width:60px;text-align:center">{{ row.id }}</div>
          <div class="at-col bold" style="width:100px">{{ row.name }}</div>
          <div class="at-col" style="width:130px">{{ row.username }}</div>
          <div class="at-col muted" style="flex:1">{{ row.email }}</div>
          <div class="at-col" style="width:140px;gap:3px;flex-wrap:wrap">
            <span v-for="g in (row.groups || []).slice(0,2)" :key="g.id" class="adm-badge badge-muted">{{ g.name }}</span>
            <span v-if="(row.groups || []).length > 2" class="adm-badge badge-muted">+{{ row.groups.length - 2 }}</span>
          </div>
          <div class="at-col" style="width:80px;text-align:center">
            <span v-if="row.withdrawnAt" class="adm-badge badge-danger">탈퇴</span>
            <span v-else-if="row.isLocked" class="adm-badge badge-warning">잠금</span>
            <span v-else class="adm-badge badge-success">활성</span>
          </div>
          <div class="at-col muted" style="width:120px;font-size:12px">{{ fmtDate(row.joinedAt) }}</div>
          <div class="at-col at-actions" style="width:110px">
            <button class="act-btn" @click="openDetail(row)"><i class="ti ti-edit"></i> 상세</button>
            <button v-if="row.isLocked" class="act-btn primary" @click="unlockMember(row)"><i class="ti ti-lock-open"></i></button>
            <button v-else class="act-btn danger" @click="forceLogout(row)"><i class="ti ti-logout"></i></button>
          </div>
        </div>
        <div v-if="!members.length && !loading" class="at-empty"><i class="ti ti-users"></i><span>회원이 없습니다.</span></div>
      </div>
      <div class="adm-pagination">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="prev,pager,next" @current-change="fetchMembers" />
      </div>
    </div>

    <!-- 상세 다이얼로그 -->
    <el-dialog v-model="showDetail" title="회원 상세" width="600px" destroy-on-close>
      <div v-if="detail" class="dlg-form">
        <div class="member-info-grid">
          <div><span class="info-label">이름</span><span>{{ detail.name }}</span></div>
          <div><span class="info-label">아이디</span><span>{{ detail.username }}</span></div>
          <div><span class="info-label">이메일</span><span>{{ detail.email }}</span></div>
          <div><span class="info-label">연락처</span><span>{{ detail.phone || '-' }}</span></div>
          <div><span class="info-label">생년월일</span><span>{{ detail.birthDate || '-' }}</span></div>
          <div><span class="info-label">성별</span><span>{{ detail.gender || '-' }}</span></div>
          <div><span class="info-label">가입일</span><span>{{ fmtDate(detail.joinedAt) }}</span></div>
          <div><span class="info-label">마지막 로그인</span><span>{{ fmtDate(detail.lastLoginAt) }}</span></div>
        </div>
        <hr class="dlg-divider" />
        <div class="dlg-section-title">그룹</div>
        <div style="display:flex;flex-wrap:wrap;gap:6px;margin-bottom:8px">
          <span v-for="g in (detail.groups||[])" :key="g.id" class="adm-badge badge-primary" style="cursor:pointer" @click="removeGroup(g.id)">
            {{ g.name }} <i class="ti ti-x" style="font-size:10px"></i>
          </span>
          <el-select v-model="addGroupId" placeholder="그룹 추가..." style="width:180px" @change="addGroup">
            <el-option v-for="g in subGroupOptions" :key="g.id" :value="g.id" :label="g.label" />
          </el-select>
        </div>
        <hr class="dlg-divider" />
        <div class="dlg-section-title">관리자 메모</div>
        <div v-for="m in (detail.memos||[])" :key="m.id" class="memo-item">
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

    <el-dialog v-model="showCreate" title="회원 추가" width="640px" destroy-on-close>
      <div class="dlg-form">
        <div class="dlg-row">
          <div class="dlg-field"><label>아이디</label><el-input v-model="createForm.username" maxlength="50" /></div>
          <div class="dlg-field"><label>이름</label><el-input v-model="createForm.name" maxlength="100" /></div>
        </div>
        <div class="dlg-row">
          <div class="dlg-field"><label>이메일</label><el-input v-model="createForm.email" maxlength="255" /></div>
          <div class="dlg-field"><label>비밀번호</label><el-input v-model="createForm.password" type="password" show-password /></div>
        </div>
        <div class="dlg-row">
          <div class="dlg-field"><label>연락처</label><el-input v-model="createForm.phone" maxlength="30" /></div>
          <div class="dlg-field"><label>성별</label><el-input v-model="createForm.gender" maxlength="10" /></div>
        </div>
        <div class="dlg-row">
          <div class="dlg-field"><label>생년월일</label><el-date-picker v-model="createForm.birthDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></div>
          <div class="dlg-field"><label>자택전화</label><el-input v-model="createForm.homePhone" maxlength="30" /></div>
        </div>
        <div class="dlg-row">
          <div class="dlg-field"><label>하는 일</label><el-input v-model="createForm.occupation" maxlength="200" /></div>
          <div class="dlg-field"><label>직장 전화</label><el-input v-model="createForm.workPhone" maxlength="30" /></div>
        </div>
        <div class="dlg-field"><label>주소</label><el-input v-model="createForm.address" maxlength="500" /></div>
        <div class="dlg-field">
          <label>그룹</label>
          <el-select v-model="createForm.groupIds" multiple clearable placeholder="그룹 선택" style="width:100%">
            <el-option v-for="g in subGroupOptions" :key="g.id" :value="g.id" :label="g.label" />
          </el-select>
        </div>
        <div class="create-options">
          <el-checkbox v-model="createForm.isActive">바로 활성화</el-checkbox>
          <el-checkbox v-model="createForm.newsletterYn">뉴스레터 수신</el-checkbox>
        </div>
      </div>
      <template #footer>
        <button class="adm-btn ghost" @click="showCreate = false">취소</button>
        <button class="adm-btn primary" :disabled="creating" @click="createMember">
          <i v-if="creating" class="ti ti-loader-2 spinning"></i>{{ creating ? '저장 중...' : '저장' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'

const members = ref([]); const loading = ref(false)
const keyword = ref(''); const groupFilter = ref(null); const statusFilter = ref('')
const page = ref(1); const size = 15; const total = ref(0)
const showDetail = ref(false); const detail = ref(null)
const groups = ref([]); const addGroupId = ref(null); const newMemo = ref('')
const showCreate = ref(false); const creating = ref(false)
const emptyCreateForm = () => ({
  username: '',
  email: '',
  password: '',
  name: '',
  gender: '',
  phone: '',
  address: '',
  birthDate: '',
  homePhone: '',
  occupation: '',
  workPhone: '',
  newsletterYn: false,
  isActive: true,
  groupIds: []
})
const createForm = ref(emptyCreateForm())

// 전체 flat (그룹 필터용)
const flatGroups = computed(() => { const r = []; const w = (n) => n.forEach(x => { r.push(x); if (x.children?.length) w(x.children) }); w(groups.value); return r })

// 그룹 추가 셀렉터용: 최상위 제외, 시스템 그룹 제외, "운영 - 정회원" 형식
const subGroupOptions = computed(() => {
  const r = []
  groups.value.forEach(root => {
    if (!root.children?.length) return
    root.children.forEach(child => {
      if (child.isSystem) return
      r.push({ id: child.id, label: `${root.name} - ${child.name}` })
    })
  })
  return r
})

// 목록 필터용 그룹 옵션도 동일 형식 (최상위 제외)
const filterGroupOptions = computed(() => subGroupOptions.value)

async function fetchMembers(p = page.value) {
  page.value = p; loading.value = true
  try { const r = await api.get('/admin/members', { params: { keyword: keyword.value, groupId: groupFilter.value, status: statusFilter.value, page: p - 1, size } }); members.value = r.data.data?.content || []; total.value = r.data.data?.totalElements || 0 }
  finally { loading.value = false }
}
async function fetchGroups() { const r = await api.get('/groups'); groups.value = r.data.data || [] }
function openCreate() { createForm.value = emptyCreateForm(); showCreate.value = true }
async function createMember() {
  if (!createForm.value.username.trim() || !createForm.value.email.trim() || !createForm.value.password || !createForm.value.name.trim()) {
    return ElMessage.warning('필수 항목을 입력해주세요.')
  }
  creating.value = true
  try {
    const username = createForm.value.username.trim()
    const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(username + createForm.value.password))
    const hashedPassword = Array.from(new Uint8Array(buf)).map(b => b.toString(16).padStart(2, '0')).join('')
    const payload = {
      ...createForm.value,
      username,
      email: createForm.value.email.trim(),
      name: createForm.value.name.trim(),
      password: hashedPassword,
      birthDate: createForm.value.birthDate || null
    }
    await api.post('/admin/members', payload)
    ElMessage.success('회원이 추가되었습니다.')
    showCreate.value = false
    fetchMembers(1)
  } finally { creating.value = false }
}
async function openDetail(row) { const r = await api.get(`/admin/members/${row.id}`); detail.value = r.data.data; showDetail.value = true }
async function unlockMember(row) { await api.put(`/admin/members/${row.id}/unlock`); ElMessage.success('계정이 잠금 해제되었습니다.'); fetchMembers() }
async function forceLogout(row) { await ElMessageBox.confirm(`${row.name} 회원을 강제 로그아웃하시겠습니까?`, '강제 로그아웃', { type: 'warning', confirmButtonText: '로그아웃', cancelButtonText: '취소' }); await api.post(`/admin/members/${row.id}/force-logout`); ElMessage.success('강제 로그아웃되었습니다.') }
async function addGroup() { if (!addGroupId.value) return; await api.post(`/admin/members/${detail.value.id}/groups`, { groupId: addGroupId.value }); addGroupId.value = null; const r = await api.get(`/admin/members/${detail.value.id}`); detail.value = r.data.data }
async function removeGroup(gid) { await api.delete(`/admin/members/${detail.value.id}/groups/${gid}`); const r = await api.get(`/admin/members/${detail.value.id}`); detail.value = r.data.data }
async function addMemo() { if (!newMemo.value.trim()) return; await api.post(`/admin/members/${detail.value.id}/memos`, { memo: newMemo.value }); newMemo.value = ''; const r = await api.get(`/admin/members/${detail.value.id}`); detail.value = r.data.data }
function download(url, filename) { api.get(url, { responseType: 'blob' }).then(r => { const a = document.createElement('a'); a.href = URL.createObjectURL(r.data); a.download = filename; a.click() }) }
function fmtDate(d) { if (!d) return '-'; return new Date(d).toLocaleDateString('ko-KR') }
onMounted(() => { fetchMembers(); fetchGroups() })
</script>

<style scoped>
@import '@/assets/admin-table.css';
.member-info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px 24px; }
.member-info-grid > div { display: flex; gap: 10px; font-size: 13px; }
.info-label { color: var(--t3); font-weight: 700; width: 80px; flex-shrink: 0; }
.memo-item { padding: 8px 10px; background: var(--surface2); border-radius: var(--radius-xs); margin-bottom: 6px; font-size: 13px; }
.memo-author { font-weight: 700; color: var(--t1); margin-right: 8px; }
.memo-date { font-size: 11px; color: var(--t4); }
.memo-item p { margin: 4px 0 0; color: var(--t2); }
.create-options { display: flex; gap: 18px; align-items: center; padding: 4px 0; }
</style>
