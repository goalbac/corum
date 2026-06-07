<template>
  <div>
    <AdminPageHeader title="회원 관리" desc="가입 회원 목록 및 그룹 관리">
      <el-input v-model="keyword" placeholder="이름 또는 이메일 검색" size="small" style="width:200px" clearable @keyup.enter="fetchMembers" />
      <el-button size="small" type="primary" @click="fetchMembers">검색</el-button>
    </AdminPageHeader>

    <el-table :data="members" v-loading="loading" border class="admin-table">
      <el-table-column label="ID" prop="id" width="60" align="center" />
      <el-table-column label="아이디" prop="username" width="120" />
      <el-table-column label="이름" prop="name" width="100" />
      <el-table-column label="이메일" prop="email" min-width="180" />
      <el-table-column label="연락처" prop="phone" width="130" />
      <el-table-column label="상태" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isActive ? 'success' : 'danger'" size="small" effect="plain">
            {{ row.isActive ? '활성' : '비활성' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="잠금" width="70" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isLocked" type="warning" size="small" effect="plain">잠김</el-tag>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="가입일" width="110" align="center">
        <template #default="{ row }">
          <span class="text-muted">{{ formatDate(row.joinedAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="관리" width="120" align="center">
        <template #default="{ row }">
          <el-button size="small" @click="openDetail(row)">상세</el-button>
          <el-button size="small" type="danger" plain @click="toggleLock(row)">
            {{ row.isLocked ? '잠금해제' : '잠금' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination v-model:current-page="page" :page-size="20" :total="total"
        layout="prev, pager, next" background small @current-change="fetchMembers" />
    </div>

    <!-- 회원 상세 다이얼로그 -->
    <el-dialog v-model="showDetail" title="회원 상세" width="640px" destroy-on-close>
      <div v-if="selectedMember" class="member-detail">

        <div class="detail-section">
          <div class="detail-title">기본 정보</div>
          <div class="detail-grid">
            <div class="detail-item"><span class="label">아이디</span><span>{{ selectedMember.username }}</span></div>
            <div class="detail-item"><span class="label">이름</span><span>{{ selectedMember.name }}</span></div>
            <div class="detail-item"><span class="label">이메일</span><span>{{ selectedMember.email }}</span></div>
            <div class="detail-item"><span class="label">연락처</span><span>{{ selectedMember.phone || '-' }}</span></div>
            <div class="detail-item"><span class="label">성별</span><span>{{ selectedMember.gender === 'M' ? '남성' : selectedMember.gender === 'F' ? '여성' : '-' }}</span></div>
            <div class="detail-item"><span class="label">생년월일</span><span>{{ selectedMember.birthDate || '-' }}</span></div>
            <div class="detail-item"><span class="label">주소</span><span>{{ selectedMember.address || '-' }}</span></div>
            <div class="detail-item"><span class="label">하는 일</span><span>{{ selectedMember.occupation || '-' }}</span></div>
            <div class="detail-item"><span class="label">직장 전화</span><span>{{ selectedMember.workPhone || '-' }}</span></div>
            <div class="detail-item"><span class="label">자택 전화</span><span>{{ selectedMember.homePhone || '-' }}</span></div>
            <div class="detail-item"><span class="label">뉴스레터</span><span>{{ selectedMember.newsletterYn ? '수신' : '미수신' }}</span></div>
            <div class="detail-item"><span class="label">가입일</span><span>{{ formatDate(selectedMember.joinedAt) }}</span></div>
          </div>
        </div>

        <div class="detail-section">
          <div class="detail-title">그룹</div>
          <div class="group-list">
            <el-tag v-for="g in memberGroups" :key="g.id" closable @close="revokeGroup(g.id)" style="margin:3px">
              {{ g.name }}
            </el-tag>
            <span v-if="!memberGroups.length" class="text-muted">배정된 그룹 없음</span>
          </div>
          <div class="group-assign" style="margin-top:10px">
            <el-select v-model="assignGroupId" placeholder="그룹 선택" size="small" style="width:160px">
              <el-option v-for="g in allGroups" :key="g.id" :value="g.id" :label="g.name" />
            </el-select>
            <el-button size="small" type="primary" @click="assignGroup">부여</el-button>
          </div>
        </div>

        <div class="detail-section">
          <div class="detail-title">관리자 메모</div>
          <el-input v-model="adminMemo" type="textarea" :rows="3" placeholder="관리자 메모를 입력하세요." resize="none" />
          <el-button size="small" type="primary" style="margin-top:8px" @click="saveMemo">저장</el-button>
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

const members       = ref([])
const loading       = ref(false)
const total         = ref(0)
const page          = ref(1)
const keyword       = ref('')
const showDetail    = ref(false)
const selectedMember = ref(null)
const memberGroups  = ref([])
const allGroups     = ref([])
const assignGroupId = ref(null)
const adminMemo     = ref('')

async function fetchMembers() {
  loading.value = true
  try {
    const res = await api.get('/admin/members', { params: { page: page.value - 1, size: 20, keyword: keyword.value || undefined } })
    members.value = res.data.data?.content || []
    total.value   = res.data.data?.totalElements || 0
  } finally { loading.value = false }
}

async function fetchGroups() {
  const res = await api.get('/groups')
  const flat = []
  const flatten = (nodes) => nodes.forEach(n => { flat.push(n); if (n.children?.length) flatten(n.children) })
  flatten(res.data.data || [])
  allGroups.value = flat
}

async function openDetail(member) {
  selectedMember.value = member
  adminMemo.value = ''
  showDetail.value = true
  const res = await api.get(`/member-groups/members/${member.id}`)
  memberGroups.value = res.data.data || []
  try {
    const memoRes = await api.get(`/admin/members/${member.id}/memo`)
    adminMemo.value = memoRes.data.data?.memo || ''
  } catch {}
}

async function assignGroup() {
  if (!assignGroupId.value) return
  await api.post('/member-groups', { memberId: selectedMember.value.id, groupId: assignGroupId.value })
  ElMessage.success('그룹이 부여되었습니다.')
  const res = await api.get(`/member-groups/members/${selectedMember.value.id}`)
  memberGroups.value = res.data.data || []
  assignGroupId.value = null
}

async function revokeGroup(groupId) {
  await api.delete('/member-groups', { data: { memberId: selectedMember.value.id, groupId } })
  ElMessage.success('그룹이 회수되었습니다.')
  const res = await api.get(`/member-groups/members/${selectedMember.value.id}`)
  memberGroups.value = res.data.data || []
}

async function toggleLock(member) {
  await api.patch(`/admin/members/${member.id}/lock`, { locked: !member.isLocked })
  ElMessage.success(member.isLocked ? '잠금이 해제되었습니다.' : '계정이 잠겼습니다.')
  member.isLocked = !member.isLocked
}

async function saveMemo() {
  await api.put(`/admin/members/${selectedMember.value.id}/memo`, { memo: adminMemo.value })
  ElMessage.success('메모가 저장되었습니다.')
}

function formatDate(d) {
  if (!d) return '-'
  return new Date(d).toLocaleDateString('ko-KR')
}

onMounted(() => { fetchMembers(); fetchGroups() })
</script>

<style scoped>
.admin-table { border-radius: var(--radius-sm); overflow: hidden; }
.pagination { display: flex; justify-content: center; padding: 16px 0; }
.text-muted { font-size: 12px; color: var(--t3); }

.member-detail { display: flex; flex-direction: column; gap: 20px; }
.detail-section {}
.detail-title { font-size: 13px; font-weight: 600; color: var(--t2); margin-bottom: 10px; padding-bottom: 6px; border-bottom: 0.5px solid var(--border2); }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.detail-item { display: flex; gap: 8px; font-size: 13px; color: var(--t1); }
.label { color: var(--t3); width: 72px; flex-shrink: 0; }
.group-list { display: flex; flex-wrap: wrap; gap: 4px; align-items: center; }
.group-assign { display: flex; gap: 8px; align-items: center; }
</style>
