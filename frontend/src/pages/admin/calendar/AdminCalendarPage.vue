<template>
  <div>
    <AdminPageHeader title="캘린더 관리" desc="캘린더 생성 및 그룹별 권한을 설정합니다.">
      <el-button type="primary" size="small" @click="openCreate">
        <i class="ti ti-plus" style="margin-right:4px"></i>캘린더 추가
      </el-button>
    </AdminPageHeader>

    <el-table :data="calendars" v-loading="loading" border>
      <el-table-column label="색상" width="60" align="center">
        <template #default="{ row }">
          <span class="cal-dot" :style="{ background: row.color || '#2563EB' }"></span>
        </template>
      </el-table-column>
      <el-table-column label="캘린더명" prop="name" min-width="140" />
      <el-table-column label="설명" prop="description" min-width="200" show-overflow-tooltip />
      <el-table-column label="그룹 권한" min-width="160">
        <template #default="{ row }">
          <span v-if="!row.permissions?.length" class="text-muted">공개 (제한 없음)</span>
          <div v-else class="perm-tags">
            <el-tag
              v-for="p in row.permissions" :key="p.groupId"
              size="small" effect="dark"
              :type="p.canWrite ? 'warning' : 'info'"
            >
              {{ groupName(p.groupId) }}
              <span v-if="p.canWrite"> · 쓰기</span>
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="상태" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.isActive ? 'success' : 'info'" effect="dark">
            {{ row.isActive ? '활성' : '비활성' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="관리" width="130" align="center">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">수정</el-button>
          <el-button size="small" type="danger" plain @click="deleteCalendar(row.id)">삭제</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 생성/수정 다이얼로그 -->
    <el-dialog v-model="showForm" :title="editing ? '캘린더 수정' : '캘린더 추가'" width="560px" destroy-on-close>
      <el-form :model="form" label-position="top">
        <div class="form-row">
          <el-form-item label="캘린더명">
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item label="색상">
            <el-color-picker v-model="form.color" show-alpha />
          </el-form-item>
        </div>
        <el-form-item label="설명">
          <el-input v-model="form.description" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.isActive">활성화</el-checkbox>
        </el-form-item>

        <!-- 그룹 권한 설정 -->
        <el-divider content-position="left">그룹 권한</el-divider>
        <div class="perm-hint">권한 없음 = 공개 캘린더 (모든 사용자 읽기/쓰기)</div>
        <div class="perm-table">
          <div class="perm-header">
            <span>그룹</span>
            <span>읽기</span>
            <span>쓰기</span>
            <span></span>
          </div>
          <div v-for="(perm, i) in form.permissions" :key="i" class="perm-row">
            <el-select v-model="perm.groupId" placeholder="그룹 선택" size="small" style="flex:1">
              <el-option-group v-for="g in flatGroups" :key="g.id" :label="g.name">
                <el-option
                  v-for="child in (g.children || [{ id: g.id, name: g.name }])"
                  :key="child.id"
                  :value="child.id"
                  :label="child.name"
                />
              </el-option-group>
            </el-select>
            <el-checkbox v-model="perm.canRead" />
            <el-checkbox v-model="perm.canWrite" />
            <el-button size="small" type="danger" plain circle @click="form.permissions.splice(i, 1)">
              <i class="ti ti-trash"></i>
            </el-button>
          </div>
          <el-button size="small" @click="addPerm" style="margin-top:6px">
            <i class="ti ti-plus"></i> 그룹 추가
          </el-button>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">취소</el-button>
        <el-button type="primary" :loading="saving" @click="saveCalendar">저장</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'

const loading   = ref(false)
const saving    = ref(false)
const showForm  = ref(false)
const editing   = ref(null)
const calendars = ref([])
const groups    = ref([])

const form = ref({
  name: '', color: '#2563EB', description: '', isActive: true, permissions: [],
})

// 전체 그룹을 flat하게 펼쳐서 groupName 조회에 사용
const allGroups = computed(() => {
  const result = []
  groups.value.forEach(g => {
    result.push(g)
    if (g.children) g.children.forEach(c => result.push(c))
  })
  return result
})

// el-select 옵션용 그룹 (root + children)
const flatGroups = computed(() => groups.value)

function groupName(groupId) {
  return allGroups.value.find(g => g.id === groupId)?.name || `그룹 ${groupId}`
}

function openCreate() {
  editing.value = null
  form.value = { name: '', color: '#2563EB', description: '', isActive: true, permissions: [] }
  showForm.value = true
}

function openEdit(cal) {
  editing.value = cal
  form.value = {
    name: cal.name,
    color: cal.color || '#2563EB',
    description: cal.description || '',
    isActive: cal.isActive,
    permissions: (cal.permissions || []).map(p => ({ ...p })),
  }
  showForm.value = true
}

function addPerm() {
  form.value.permissions.push({ groupId: null, canRead: true, canWrite: false })
}

async function saveCalendar() {
  if (!form.value.name) return ElMessage.warning('캘린더명을 입력해주세요.')
  saving.value = true
  try {
    const payload = {
      name: form.value.name,
      color: form.value.color,
      description: form.value.description,
      isActive: form.value.isActive,
      permissions: form.value.permissions.filter(p => p.groupId),
    }
    if (editing.value) {
      await api.put(`/calendars/${editing.value.id}`, payload)
      ElMessage.success('수정되었습니다.')
    } else {
      await api.post('/calendars', payload)
      ElMessage.success('캘린더가 추가되었습니다.')
    }
    showForm.value = false
    await fetchCalendars()
  } finally { saving.value = false }
}

async function deleteCalendar(id) {
  await ElMessageBox.confirm('캘린더와 모든 일정이 삭제됩니다. 계속하시겠습니까?', '삭제', { type: 'warning' })
  await api.delete(`/calendars/${id}`)
  ElMessage.success('삭제되었습니다.')
  await fetchCalendars()
}

async function fetchCalendars() {
  loading.value = true
  try {
    const res = await api.get('/calendars/admin')
    calendars.value = res.data.data || []
  } finally { loading.value = false }
}

onMounted(async () => {
  await fetchCalendars()
  try {
    const res = await api.get('/groups')
    groups.value = res.data.data || []
  } catch {}
})
</script>

<style scoped>
.cal-dot {
  display: inline-block;
  width: 18px;
  height: 18px;
  border-radius: 50%;
}
.perm-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.text-muted { color: var(--t3); font-size: 12px; }
.perm-hint { font-size: 12px; color: var(--t3); margin-bottom: 10px; }

.perm-table { display: flex; flex-direction: column; gap: 6px; }
.perm-header {
  display: grid;
  grid-template-columns: 1fr 60px 60px 36px;
  gap: 8px;
  font-size: 12px;
  color: var(--t3);
  font-weight: 600;
  padding: 0 4px;
  text-align: center;
}
.perm-header span:first-child { text-align: left; }
.perm-row {
  display: grid;
  grid-template-columns: 1fr 60px 60px 36px;
  gap: 8px;
  align-items: center;
}
.perm-row :deep(.el-checkbox) { justify-content: center; }

.form-row { display: grid; grid-template-columns: 1fr auto; gap: 12px; align-items: start; }
</style>
