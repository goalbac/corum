<template>
  <div class="adm-page">
    <AdminPageHeader title="그룹 관리" desc="운영/일반 하위 그룹 관리">
      <div style="display:flex;gap:8px">
        <button v-if="sortChanged" class="adm-btn primary" :disabled="sortSaving" @click="saveSortOrder">
          <i v-if="sortSaving" class="ti ti-loader-2 spinning"></i>
          {{ sortSaving ? '저장 중...' : '순서 저장' }}
        </button>
        <button class="adm-btn ghost" v-if="sortChanged" @click="fetchGroups">취소</button>
        <button class="adm-btn primary" @click="openCreate(null)"><i class="ti ti-plus"></i> 그룹 추가</button>
      </div>
    </AdminPageHeader>

    <div class="adm-card" v-loading="loading">
      <div class="at-wrap">
        <div class="at-head">
          <div class="at-col" style="width:32px"></div>
          <div class="at-col" style="flex:1">그룹명</div>
          <div class="at-col" style="width:110px;text-align:center">소속</div>
          <div class="at-col" style="flex:1">설명</div>
          <div class="at-col" style="width:70px;text-align:center">시스템</div>
          <div class="at-col" style="width:100px;text-align:center">관리</div>
        </div>

        <template v-for="g in groups" :key="g.id">
          <!-- 최상위 그룹 행 -->
          <div class="at-row root-row">
            <div class="at-col" style="width:32px"></div>
            <div class="at-col bold" style="flex:1">
              <i class="ti ti-folder-filled group-icon root-icon"></i>
              {{ g.name }}
              <span :class="['type-badge', g.type === 'ADMIN' ? 'type-admin' : 'type-normal']">
                {{ g.type === 'ADMIN' ? '관리자' : '일반' }}
              </span>
            </div>
            <div class="at-col muted" style="width:110px;text-align:center;font-size:12px">최상위 그룹</div>
            <div class="at-col muted" style="flex:1">{{ g.description || '-' }}</div>
            <div class="at-col" style="width:70px;text-align:center">
              <i v-if="g.isSystem" class="ti ti-lock muted" title="시스템 고정"></i>
              <i v-else class="ti ti-minus muted"></i>
            </div>
            <div class="at-col at-actions" style="width:100px">
              <button class="act-btn" @click="openCreate(g)" title="하위 그룹 추가">
                <i class="ti ti-plus"></i> 추가
              </button>
            </div>
          </div>

          <!-- 하위 그룹 — 드래그 가능 컨테이너 -->
          <div :ref="el => { if (el) childRefs[g.id] = el }" :data-parent-id="g.id">
            <div
              v-for="child in (g.children || [])"
              :key="child.id"
              class="at-row child-row"
              :data-id="child.id"
            >
              <div class="at-col" style="width:32px;text-align:center">
                <i v-if="!child.isSystem" class="ti ti-grip-vertical drag-handle" title="드래그하여 순서 변경"></i>
              </div>
              <div class="at-col bold" style="flex:1">
                <span class="child-indent"></span>
                <i class="ti ti-users group-icon"></i>
                {{ child.name }}
              </div>
              <div class="at-col" style="width:110px;text-align:center">
                <span :class="['adm-badge', g.type === 'ADMIN' ? 'badge-purple' : 'badge-info']">
                  {{ g.name }}
                </span>
              </div>
              <div class="at-col muted" style="flex:1">{{ child.description || '-' }}</div>
              <div class="at-col" style="width:70px;text-align:center">
                <i v-if="child.isSystem" class="ti ti-lock muted" title="시스템 고정"></i>
                <i v-else class="ti ti-minus muted"></i>
              </div>
              <div class="at-col at-actions" style="width:100px">
                <button v-if="!child.isSystem" class="act-btn" @click="openEdit(child)">
                  <i class="ti ti-edit"></i> 수정
                </button>
                <button v-if="!child.isSystem" class="act-btn danger" @click="deleteGroup(child)">
                  <i class="ti ti-trash"></i>
                </button>
              </div>
            </div>
          </div>

          <!-- 하위 그룹 없을 때 -->
          <div v-if="!(g.children || []).length" class="at-row empty-children">
            <div class="at-col muted" style="flex:1;padding-left:56px;font-size:12px">하위 그룹 없음</div>
          </div>
        </template>

        <div v-if="!groups.length && !loading" class="at-empty">
          <i class="ti ti-shield"></i><span>등록된 그룹이 없습니다.</span>
        </div>
      </div>
    </div>

    <!-- 그룹 폼 다이얼로그 -->
    <el-dialog v-model="showForm" :title="editing ? '그룹 수정' : '하위 그룹 추가'" width="440px" destroy-on-close>
      <div class="dlg-form">

        <!-- 상위 그룹 선택 (필수, 최상위 그룹만 선택 가능) -->
        <div class="dlg-field" v-if="!editing">
          <label>상위 그룹 <span class="required">*</span></label>
          <el-select v-model="form.parentId" style="width:100%" placeholder="상위 그룹을 선택하세요">
            <el-option
              v-for="g in topGroups"
              :key="g.id"
              :value="g.id"
              :label="g.name + (g.type === 'ADMIN' ? ' (관리자 그룹)' : ' (일반 그룹)')"
            />
          </el-select>
          <div v-if="inferredType" class="inferred-type">
            <i class="ti ti-info-circle"></i>
            유형이 <strong>{{ inferredType === 'ADMIN' ? '관리자' : '일반' }}</strong>으로 자동 설정됩니다.
          </div>
        </div>

        <!-- 수정 시: 상위 그룹 및 유형 읽기 전용 -->
        <div v-if="editing" class="dlg-row">
          <div class="dlg-field">
            <label>상위 그룹</label>
            <div class="readonly-badge"><i class="ti ti-folder"></i> {{ parentGroupName }}</div>
            <div class="form-hint">상위 그룹은 변경할 수 없습니다.</div>
          </div>
          <div class="dlg-field">
            <label>유형</label>
            <div class="readonly-badge">
              <span :class="['type-badge', editing.type === 'ADMIN' ? 'type-admin' : 'type-normal']">
                {{ editing.type === 'ADMIN' ? '관리자 그룹' : '일반 그룹' }}
              </span>
            </div>
          </div>
        </div>

        <div class="dlg-field">
          <label>그룹명 <span class="required">*</span></label>
          <el-input v-model="form.name" placeholder="예: 회원관리팀, 정회원" />
        </div>

        <div class="dlg-field">
          <label>설명</label>
          <el-input v-model="form.description" placeholder="그룹에 대한 설명 (선택)" />
        </div>

      </div>
      <template #footer>
        <button class="adm-btn ghost" @click="showForm = false">취소</button>
        <button class="adm-btn primary" :disabled="saving" @click="saveGroup">
          <i v-if="saving" class="ti ti-loader-2 spinning"></i>
          {{ saving ? '저장 중...' : '저장' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Sortable from 'sortablejs'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'

const groups   = ref([])
const loading  = ref(false)
const saving   = ref(false)
const showForm = ref(false)
const editing  = ref(null)
const form     = ref({ name: '', description: '', parentId: null })

// 드래그 관련
const childRefs      = ref({})
const sortableInstances = []
const sortChanged    = ref(false)
const sortSaving     = ref(false)

const topGroups = computed(() => groups.value.filter(g => g.parentId == null))

const inferredType = computed(() => {
  if (!form.value.parentId) return null
  return topGroups.value.find(g => g.id === form.value.parentId)?.type || null
})

const parentGroupName = computed(() => {
  if (!editing.value?.parentId) return '-'
  for (const g of groups.value) {
    if (g.id === editing.value.parentId) return g.name
    for (const c of (g.children || [])) {
      if (c.id === editing.value.parentId) return c.name
    }
  }
  return '-'
})

async function fetchGroups() {
  loading.value = true
  sortChanged.value = false
  try {
    const r = await api.get('/groups')
    groups.value = r.data.data || []
    await nextTick()
    initSortable()
  } finally {
    loading.value = false
  }
}

// ===== 드래그 초기화 =====
function initSortable() {
  sortableInstances.forEach(s => s.destroy())
  sortableInstances.length = 0

  // 각 최상위 그룹의 하위 컨테이너마다 Sortable 생성
  for (const el of Object.values(childRefs.value)) {
    if (!el) continue
    const inst = Sortable.create(el, {
      animation: 150,
      handle: '.drag-handle',
      ghostClass: 'sortable-ghost',
      dragClass: 'sortable-drag',
      onEnd: () => { sortChanged.value = true }
    })
    sortableInstances.push(inst)
  }
}

// ===== 순서 저장 =====
async function saveSortOrder() {
  sortSaving.value = true
  try {
    for (const [parentId, el] of Object.entries(childRefs.value)) {
      if (!el) continue
      const ids = Array.from(el.children)
        .map(c => parseInt(c.dataset.id))
        .filter(Boolean)
      if (ids.length) await api.put('/groups/sort', ids)
    }
    ElMessage.success('순서가 저장되었습니다.')
    sortChanged.value = false
    await fetchGroups()
  } catch {
    ElMessage.error('순서 저장에 실패했습니다.')
  } finally {
    sortSaving.value = false
  }
}

function openCreate(parent) {
  editing.value = null
  form.value = { name: '', description: '', parentId: parent?.id || null }
  showForm.value = true
}

function openEdit(g) {
  editing.value = g
  form.value = { name: g.name, description: g.description || '', parentId: g.parentId }
  showForm.value = true
}

async function saveGroup() {
  if (!form.value.name?.trim()) return ElMessage.warning('그룹명을 입력해주세요.')
  if (!editing.value && !form.value.parentId) return ElMessage.warning('상위 그룹을 선택해주세요.')
  saving.value = true
  try {
    if (editing.value) {
      await api.put(`/groups/${editing.value.id}`, {
        name: form.value.name,
        description: form.value.description
      })
    } else {
      await api.post('/groups', {
        name: form.value.name,
        description: form.value.description,
        parentId: form.value.parentId
        // sortOrder 미지정 → 서버에서 자동으로 마지막 순서 배정
      })
    }
    ElMessage.success('저장되었습니다.')
    showForm.value = false
    fetchGroups()
  } finally {
    saving.value = false
  }
}

async function deleteGroup(g) {
  await ElMessageBox.confirm(`"${g.name}" 그룹을 삭제하시겠습니까?`, '삭제', {
    type: 'warning', confirmButtonText: '삭제', cancelButtonText: '취소'
  })
  await api.delete(`/groups/${g.id}`)
  ElMessage.success('삭제되었습니다.')
  fetchGroups()
}

onMounted(fetchGroups)
</script>

<style scoped>
@import '@/assets/admin-table.css';

.root-row {
  background: var(--surface2) !important;
  border-left: 3px solid var(--accent);
}
.root-row:hover { background: var(--color-bg-hover) !important; }
.child-row { background: var(--surface); }
.child-row:hover { background: var(--surface2); }
.empty-children { background: var(--surface); }
.empty-children:hover { background: var(--surface); }
.child-indent { display: inline-block; width: 24px; }
.group-icon { margin-right: 5px; font-size: 14px; color: var(--t3); }
.root-icon  { color: var(--accent-t) !important; }

/* 드래그 핸들 */
.drag-handle {
  color: var(--t4);
  cursor: grab;
  font-size: 15px;
  transition: color .15s;
}
.drag-handle:hover { color: var(--t2); }
:global(.sortable-ghost) { opacity: 0.4; background: var(--accent-bg); }
:global(.sortable-drag)  { background: var(--surface); box-shadow: var(--shadow-card); border-radius: var(--radius-xs); }

.type-badge {
  display: inline-block;
  font-size: 10px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 4px;
  margin-left: 6px;
  vertical-align: middle;
}
.type-admin  { background: #f0e6ff; color: #7c3aed; }
.type-normal { background: #e0f0ff; color: #1d6db5; }

.inferred-type {
  margin-top: 6px;
  font-size: 12px;
  color: var(--accent-t);
  display: flex;
  align-items: center;
  gap: 5px;
}
.readonly-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 10px;
  background: var(--surface2);
  border: 0.5px solid var(--border);
  border-radius: var(--radius-xs);
  font-size: 13px;
  color: var(--t2);
}
.form-hint { font-size: 12px; color: var(--t3); margin-top: 4px; }
.required   { color: var(--color-danger); }

@keyframes spin { to { transform: rotate(360deg); } }
.spinning { animation: spin 0.7s linear infinite; display: inline-block; }
</style>
