<template>
  <div class="adm-page">
    <AdminPageHeader title="약관 관리" desc="이용약관 및 개인정보처리방침 버전 관리">
      <button class="adm-btn primary" @click="openCreate"><i class="ti ti-plus"></i> 새 버전 추가</button>
    </AdminPageHeader>

    <div class="adm-card">
      <div class="adm-tabs">
        <div :class="['adm-tab', tab === 'SERVICE' ? 'active' : '']" @click="tab = 'SERVICE'">이용약관</div>
        <div :class="['adm-tab', tab === 'PRIVACY' ? 'active' : '']" @click="tab = 'PRIVACY'">개인정보처리방침</div>
      </div>
      <div class="at-wrap" v-loading="loading">
        <div class="at-head">
          <div class="at-col" style="width:100px">버전</div>
          <div class="at-col" style="width:80px;text-align:center">활성</div>
          <div class="at-col" style="width:100px;text-align:center">재동의 필요</div>
          <div class="at-col" style="flex:1">내용 미리보기</div>
          <div class="at-col" style="width:120px">등록일</div>
          <div class="at-col" style="width:90px;text-align:center">관리</div>
        </div>
        <div v-for="row in filteredTerms" :key="row.id" class="at-row">
          <div class="at-col bold" style="width:100px">v{{ row.version }}</div>
          <div class="at-col" style="width:80px;text-align:center">
            <span :class="['adm-badge', row.isActive ? 'badge-success' : 'badge-muted']">{{ row.isActive ? '활성' : '비활성' }}</span>
          </div>
          <div class="at-col" style="width:100px;text-align:center">
            <i :class="row.requireReagree ? 'ti ti-check green' : 'ti ti-minus muted'"></i>
          </div>
          <div class="at-col muted wrap" style="flex:1;font-size:12px">{{ row.content?.substring(0, 80) }}...</div>
          <div class="at-col muted" style="width:120px;font-size:12px">{{ fmtDate(row.createdAt) }}</div>
          <div class="at-col at-actions" style="width:90px">
            <button class="act-btn" @click="openEdit(row)"><i class="ti ti-edit"></i> 수정</button>
            <button class="act-btn danger" @click="deleteTerm(row.id)"><i class="ti ti-trash"></i></button>
          </div>
        </div>
        <div v-if="!filteredTerms.length && !loading" class="at-empty"><i class="ti ti-file-check"></i><span>등록된 약관이 없습니다.</span></div>
      </div>
    </div>

    <el-dialog v-model="showForm" :title="editing ? '약관 수정' : '약관 추가'" width="680px" destroy-on-close>
      <div class="dlg-form">
        <div class="dlg-row">
          <div class="dlg-field">
            <label>약관 유형</label>
            <el-select v-model="form.type" style="width:100%" :disabled="!!editing">
              <el-option value="SERVICE" label="이용약관" />
              <el-option value="PRIVACY" label="개인정보처리방침" />
            </el-select>
          </div>
          <div class="dlg-field">
            <label>버전</label>
            <el-input v-model="form.version" placeholder="예: 1.0, 2025-06" />
          </div>
        </div>
        <div class="dlg-field">
          <label>내용</label>
          <el-input v-model="form.content" type="textarea" :rows="12" resize="none" />
        </div>
        <div class="dlg-checks">
          <label class="chk-item"><el-checkbox v-model="form.isActive" />활성화</label>
          <label class="chk-item"><el-checkbox v-model="form.requireReagree" />변경 후 재동의 요청</label>
        </div>
      </div>
      <template #footer>
        <button class="adm-btn ghost" @click="showForm = false">취소</button>
        <button class="adm-btn primary" :disabled="saving" @click="saveTerm">
          <i v-if="saving" class="ti ti-loader-2 spinning"></i>{{ saving ? '저장 중...' : '저장' }}
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

const terms = ref([]); const loading = ref(false); const saving = ref(false)
const showForm = ref(false); const editing = ref(null); const tab = ref('SERVICE')
const defaultForm = () => ({ type: 'SERVICE', version: '', content: '', isActive: false, requireReagree: false })
const form = ref(defaultForm())

const filteredTerms = computed(() => terms.value.filter(t => t.type === tab.value))

async function fetchTerms() { loading.value = true; try { const r = await api.get('/admin/terms'); terms.value = r.data.data || [] } finally { loading.value = false } }
function openCreate() { editing.value = null; form.value = { ...defaultForm(), type: tab.value }; showForm.value = true }
function openEdit(t) { editing.value = t; form.value = { ...t }; showForm.value = true }
async function saveTerm() {
  if (!form.value.version || !form.value.content) return ElMessage.warning('버전과 내용을 입력해주세요.')
  saving.value = true
  try {
    editing.value ? await api.put(`/admin/terms/${editing.value.id}`, form.value) : await api.post('/admin/terms', form.value)
    ElMessage.success('저장되었습니다.'); showForm.value = false; fetchTerms()
  } finally { saving.value = false }
}
async function deleteTerm(id) { await ElMessageBox.confirm('약관을 삭제하시겠습니까?', '삭제', { type: 'warning', confirmButtonText: '삭제', cancelButtonText: '취소' }); await api.delete(`/admin/terms/${id}`); ElMessage.success('삭제되었습니다.'); fetchTerms() }
function fmtDate(d) { if (!d) return '-'; return new Date(d).toLocaleDateString('ko-KR') }
onMounted(fetchTerms)
</script>

<style scoped>
@import '@/assets/admin-table.css';
</style>
