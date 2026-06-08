<template>
  <div class="adm-page">
    <AdminPageHeader title="그룹 관리" desc="운영/일반 그룹 및 하위 그룹 관리">
      <button class="adm-btn primary" @click="openCreate(null)"><i class="ti ti-plus"></i> 그룹 추가</button>
    </AdminPageHeader>

    <div class="adm-card" v-loading="loading">
      <div class="at-wrap">
        <div class="at-head">
          <div class="at-col" style="flex:1">그룹명</div>
          <div class="at-col" style="width:100px;text-align:center">유형</div>
          <div class="at-col" style="flex:1">설명</div>
          <div class="at-col" style="width:80px;text-align:center">시스템</div>
          <div class="at-col" style="width:90px;text-align:center">관리</div>
        </div>
        <template v-for="g in groups" :key="g.id">
          <div class="at-row">
            <div class="at-col bold" style="flex:1"><i class="ti ti-folder group-icon"></i>{{ g.name }}</div>
            <div class="at-col" style="width:100px;text-align:center">
              <span :class="['adm-badge', g.type === 'ADMIN' ? 'badge-purple' : 'badge-info']">{{ g.type === 'ADMIN' ? '관리자' : '일반' }}</span>
            </div>
            <div class="at-col muted" style="flex:1">{{ g.description || '-' }}</div>
            <div class="at-col" style="width:80px;text-align:center"><i :class="g.isSystem ? 'ti ti-lock muted' : 'ti ti-minus muted'"></i></div>
            <div class="at-col at-actions" style="width:90px">
              <button class="act-btn" @click="openCreate(g)"><i class="ti ti-plus"></i></button>
              <button v-if="!g.isSystem" class="act-btn" @click="openEdit(g)"><i class="ti ti-edit"></i></button>
              <button v-if="!g.isSystem" class="act-btn danger" @click="deleteGroup(g)"><i class="ti ti-trash"></i></button>
            </div>
          </div>
          <div v-for="child in (g.children||[])" :key="child.id" class="at-row child-row">
            <div class="at-col bold" style="flex:1"><span class="child-indent"></span><i class="ti ti-users group-icon"></i>{{ child.name }}</div>
            <div class="at-col muted" style="width:100px;text-align:center;font-size:12px">하위 그룹</div>
            <div class="at-col muted" style="flex:1">{{ child.description || '-' }}</div>
            <div class="at-col" style="width:80px;text-align:center"><i :class="child.isSystem ? 'ti ti-lock muted' : 'ti ti-minus muted'"></i></div>
            <div class="at-col at-actions" style="width:90px">
              <button v-if="!child.isSystem" class="act-btn" @click="openEdit(child)"><i class="ti ti-edit"></i> 수정</button>
              <button v-if="!child.isSystem" class="act-btn danger" @click="deleteGroup(child)"><i class="ti ti-trash"></i></button>
            </div>
          </div>
        </template>
        <div v-if="!groups.length && !loading" class="at-empty"><i class="ti ti-shield"></i><span>등록된 그룹이 없습니다.</span></div>
      </div>
    </div>

    <el-dialog v-model="showForm" :title="editing ? '그룹 수정' : '그룹 추가'" width="440px" destroy-on-close>
      <div class="dlg-form">
        <div class="dlg-field" v-if="parentGroup">
          <label>상위 그룹</label>
          <div class="parent-badge"><i class="ti ti-folder"></i> {{ parentGroup.name }}</div>
        </div>
        <div class="dlg-row">
          <div class="dlg-field"><label>그룹명</label><el-input v-model="form.name" /></div>
          <div class="dlg-field">
            <label>유형</label>
            <el-select v-model="form.type" style="width:100%" :disabled="!!editing">
              <el-option value="ADMIN" label="관리자 그룹" />
              <el-option value="NORMAL" label="일반 그룹" />
            </el-select>
          </div>
        </div>
        <div class="dlg-field"><label>설명</label><el-input v-model="form.description" /></div>
        <div class="dlg-row">
          <div class="dlg-field"><label>순서</label><el-input-number v-model="form.sortOrder" :min="0" style="width:100%" /></div>
        </div>
      </div>
      <template #footer>
        <button class="adm-btn ghost" @click="showForm = false">취소</button>
        <button class="adm-btn primary" :disabled="saving" @click="saveGroup">
          <i v-if="saving" class="ti ti-loader-2 spinning"></i>{{ saving ? '저장 중...' : '저장' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'

const groups = ref([]); const loading = ref(false); const saving = ref(false)
const showForm = ref(false); const editing = ref(null); const parentGroup = ref(null)
const form = ref({ name: '', type: 'NORMAL', description: '', sortOrder: 0 })

async function fetchGroups() { loading.value = true; try { const r = await api.get('/groups'); groups.value = r.data.data || [] } finally { loading.value = false } }
function openCreate(parent) { editing.value = null; parentGroup.value = parent; form.value = { name: '', type: parent?.type || 'NORMAL', description: '', sortOrder: 0, parentId: parent?.id || null }; showForm.value = true }
function openEdit(g) { editing.value = g; parentGroup.value = null; form.value = { ...g }; showForm.value = true }
async function saveGroup() {
  if (!form.value.name) return ElMessage.warning('그룹명을 입력해주세요.')
  saving.value = true
  try {
    editing.value ? await api.put(`/groups/${editing.value.id}`, form.value) : await api.post('/groups', form.value)
    ElMessage.success('저장되었습니다.'); showForm.value = false; fetchGroups()
  } finally { saving.value = false }
}
async function deleteGroup(g) {
  await ElMessageBox.confirm(`"${g.name}" 그룹을 삭제하시겠습니까?`, '삭제', { type: 'warning', confirmButtonText: '삭제', cancelButtonText: '취소' })
  await api.delete(`/groups/${g.id}`); ElMessage.success('삭제되었습니다.'); fetchGroups()
}
onMounted(fetchGroups)
</script>

<style scoped>
@import '@/assets/admin-table.css';
.group-icon { margin-right: 6px; color: var(--t3); }
.child-row { background: var(--surface); }
.child-row:hover { background: var(--surface2); }
.child-indent { display: inline-block; width: 20px; }
.parent-badge { display: flex; align-items: center; gap: 6px; padding: 6px 10px; background: var(--surface2); border-radius: var(--radius-xs); font-size: 13px; color: var(--t2); }
</style>
