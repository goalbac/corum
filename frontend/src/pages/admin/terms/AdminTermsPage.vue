<template>
  <div>
    <AdminPageHeader title="약관 관리" desc="이용약관과 개인정보 처리방침의 버전과 재동의 정책을 관리합니다.">
      <el-button size="small" type="primary" @click="openCreate">새 버전 추가</el-button>
    </AdminPageHeader>

    <el-table :data="terms" v-loading="loading" border class="admin-table">
      <el-table-column label="유형" prop="type" width="120" />
      <el-table-column label="버전" prop="version" width="80" align="center" />
      <el-table-column label="상태" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isActive ? 'success' : 'info'" size="small" effect="plain">
            {{ row.isActive ? '활성' : '비활성' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="재동의" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.requireReagree ? 'warning' : 'info'" size="small" effect="plain">
            {{ row.requireReagree ? '필요' : '없음' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="내용" min-width="260">
        <template #default="{ row }">
          <span class="preview">{{ stripHtml(row.content) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="등록일" width="120" align="center">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="관리" width="180" align="center">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">수정</el-button>
          <el-button v-if="!row.isActive" size="small" type="primary" plain @click="activate(row)">활성화</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogOpen" :title="editing ? '약관 수정' : '새 약관 버전 추가'" width="760px">
      <el-form label-position="top">
        <div class="form-grid">
          <el-form-item label="유형">
            <el-select v-model="form.type" :disabled="!!editing" placeholder="유형 선택">
              <el-option label="이용약관" value="SERVICE" />
              <el-option label="개인정보 처리방침" value="PRIVACY" />
            </el-select>
          </el-form-item>
          <el-form-item label="상태">
            <el-switch v-model="form.isActive" active-text="활성" inactive-text="비활성" />
          </el-form-item>
          <el-form-item label="재동의 요청">
            <el-switch v-model="form.requireReagree" active-text="요청" inactive-text="미요청" />
          </el-form-item>
        </div>
        <el-form-item label="내용">
          <el-input v-model="form.content" type="textarea" :rows="14" resize="vertical" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">취소</el-button>
        <el-button type="primary" :loading="saving" @click="save">저장</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'

const terms = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogOpen = ref(false)
const editing = ref(null)
const form = ref({ type: 'SERVICE', content: '', isActive: true, requireReagree: false })

async function fetchTerms() {
  loading.value = true
  try {
    const res = await api.get('/admin/terms')
    terms.value = res.data.data || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.value = { type: 'SERVICE', content: '', isActive: true, requireReagree: false }
  dialogOpen.value = true
}

function openEdit(row) {
  editing.value = row
  form.value = {
    type: row.type,
    content: row.content || '',
    isActive: row.isActive,
    requireReagree: row.requireReagree,
  }
  dialogOpen.value = true
}

async function save() {
  if (!form.value.type) {
    ElMessage.warning('약관 유형을 선택해주세요.')
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await api.put(`/admin/terms/${editing.value.id}`, form.value)
    } else {
      await api.post('/admin/terms', form.value)
    }
    ElMessage.success('약관이 저장되었습니다.')
    dialogOpen.value = false
    await fetchTerms()
  } finally {
    saving.value = false
  }
}

async function activate(row) {
  await api.post(`/admin/terms/${row.id}/activate`)
  ElMessage.success('활성 약관으로 변경되었습니다.')
  await fetchTerms()
}

function stripHtml(value) {
  return (value || '').replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').slice(0, 100)
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleDateString('ko-KR')
}

onMounted(fetchTerms)
</script>

<style scoped>
.admin-table { border-radius: var(--radius-sm); overflow: hidden; }
.preview { display: block; color: var(--t2); line-height: 1.5; }
.form-grid { display: grid; grid-template-columns: 1.2fr 1fr 1fr; gap: 12px; }
@media (max-width: 768px) {
  .form-grid { grid-template-columns: 1fr; }
}
</style>
