<template>
  <div>
    <AdminPageHeader title="그룹 관리" desc="운영/일반 그룹과 하위 권한 그룹을 관리합니다.">
      <el-button type="primary" size="small" @click="openCreate(null)">
        <i class="ti ti-plus" style="margin-right:4px"></i>그룹 추가
      </el-button>
    </AdminPageHeader>

    <div v-loading="loading" class="group-tree">
      <div v-if="!groups.length" class="empty-state">등록된 그룹이 없습니다.</div>
      <template v-for="group in groups" :key="group.id">
        <div class="group-row root-row">
          <div class="group-main">
            <el-tag size="small" :type="group.type === 'ADMIN' ? 'danger' : 'success'" effect="dark">
              {{ typeLabel(group.type) }}
            </el-tag>
            <div>
              <div class="group-name">{{ group.name }}</div>
              <div class="group-desc">{{ group.description || '-' }}</div>
            </div>
          </div>
          <div class="group-actions">
            <el-tag v-if="group.isSystem" size="small" effect="dark">시스템</el-tag>
            <el-button size="small" @click="openCreate(group)">하위 추가</el-button>
            <el-button size="small" @click="openEdit(group)">수정</el-button>
          </div>
        </div>

        <div
          v-for="child in group.children"
          :key="child.id"
          class="group-row child-row"
        >
          <div class="group-main">
            <i class="ti ti-corner-down-right"></i>
            <el-tag size="small" :type="child.type === 'ADMIN' ? 'danger' : 'success'" effect="dark">
              {{ typeLabel(child.type) }}
            </el-tag>
            <div>
              <div class="group-name">{{ child.name }}</div>
              <div class="group-desc">{{ child.description || '-' }}</div>
            </div>
          </div>
          <div class="group-actions">
            <el-tag v-if="child.isSystem" size="small" effect="dark">시스템</el-tag>
            <el-button size="small" @click="openEdit(child)">수정</el-button>
            <el-button
              size="small"
              type="danger"
              plain
              :disabled="child.isSystem"
              @click="deleteGroup(child)"
            >
              삭제
            </el-button>
          </div>
        </div>
      </template>
    </div>

    <el-dialog v-model="showForm" :title="editing ? '그룹 수정' : '그룹 추가'" width="480px" destroy-on-close>
      <el-form :model="form" label-position="top">
        <el-form-item v-if="!editing" label="상위 그룹">
          <el-select v-model="form.parentId" placeholder="상위 그룹 선택" style="width:100%">
            <el-option
              v-for="group in groups"
              :key="group.id"
              :label="`${group.name} (${typeLabel(group.type)})`"
              :value="group.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="그룹명">
          <el-input v-model="form.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="설명">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit resize="none" />
        </el-form-item>
        <el-form-item label="정렬 순서">
          <el-input-number v-model="form.sortOrder" :min="0" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">취소</el-button>
        <el-button type="primary" :loading="saving" @click="saveGroup">저장</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'

const groups = ref([])
const loading = ref(false)
const saving = ref(false)
const showForm = ref(false)
const editing = ref(null)

const defaultForm = () => ({
  parentId: null,
  name: '',
  description: '',
  sortOrder: 0
})
const form = ref(defaultForm())

function typeLabel(type) {
  return type === 'ADMIN' ? '운영' : '일반'
}

async function fetchGroups() {
  loading.value = true
  try {
    const res = await api.get('/groups')
    groups.value = res.data.data || []
  } finally {
    loading.value = false
  }
}

function openCreate(parent) {
  editing.value = null
  form.value = defaultForm()
  form.value.parentId = parent?.id || groups.value[0]?.id || null
  showForm.value = true
}

function openEdit(group) {
  editing.value = group
  form.value = {
    parentId: group.parentId,
    name: group.name,
    description: group.description,
    sortOrder: group.sortOrder
  }
  showForm.value = true
}

async function saveGroup() {
  if (!form.value.name) return ElMessage.warning('그룹명을 입력해주세요.')
  if (!editing.value && !form.value.parentId) return ElMessage.warning('상위 그룹을 선택해주세요.')

  saving.value = true
  try {
    if (editing.value) {
      await api.put(`/groups/${editing.value.id}`, {
        name: form.value.name,
        description: form.value.description,
        sortOrder: form.value.sortOrder
      })
    } else {
      await api.post('/groups', form.value)
    }
    ElMessage.success('저장되었습니다.')
    showForm.value = false
    fetchGroups()
  } finally {
    saving.value = false
  }
}

async function deleteGroup(group) {
  await ElMessageBox.confirm('그룹을 삭제하시겠습니까?', '삭제', { type: 'warning' })
  await api.delete(`/groups/${group.id}`)
  ElMessage.success('삭제되었습니다.')
  fetchGroups()
}

onMounted(fetchGroups)
</script>

<style scoped>
.group-tree {
  background: var(--surface);
  border: 0.5px solid var(--border2);
  border-radius: var(--radius-sm);
  overflow: hidden;
}
.empty-state { padding: 32px; text-align: center; color: var(--t3); font-size: 14px; }
.group-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 16px;
  border-bottom: 0.5px solid var(--border2);
}
.group-row:last-child { border-bottom: none; }
.root-row { background: var(--surface); }
.child-row { background: var(--surface2); padding-left: 36px; }
.group-main { display: flex; align-items: center; gap: 10px; min-width: 0; }
.group-name { font-size: 14px; font-weight: 600; color: var(--t1); }
.group-desc { font-size: 12px; color: var(--t3); margin-top: 2px; }
.group-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
@media (max-width: 768px) {
  .group-row { flex-direction: column; }
  .group-actions { justify-content: flex-end; }
}
</style>
