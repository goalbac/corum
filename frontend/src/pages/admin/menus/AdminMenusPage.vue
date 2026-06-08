<template>
  <div>
    <AdminPageHeader title="메뉴 관리" desc="드래그하여 순서를 변경하세요.">
      <el-button size="small" :loading="sortSaving" @click="saveSortOrder" :disabled="!sortChanged">
        <i class="ti ti-check" style="margin-right:4px"></i>순서 저장
      </el-button>
      <el-button type="primary" size="small" @click="openCreate(null)">
        <i class="ti ti-plus" style="margin-right:4px"></i>메뉴 추가
      </el-button>
    </AdminPageHeader>

    <div v-loading="loading" class="menu-tree">
      <div v-if="!menus.length" class="empty-state">등록된 메뉴가 없습니다.</div>

      <!-- 최상위 메뉴 드래그 영역 -->
      <div ref="rootSortable">
        <div
          v-for="menu in menus"
          :key="menu.id"
          :data-id="menu.id"
          class="sortable-node"
        >
          <div class="tree-item root-item">
            <i class="ti ti-grip-vertical drag-handle" title="드래그하여 순서 변경"></i>
            <span class="tree-type">{{ typeIcon(menu.menuType) }}</span>
            <span class="tree-name">{{ menu.name }}</span>
            <span class="tree-url">{{ menu.url || '' }}</span>
            <span v-if="!menu.isActive" class="tree-tag inactive">비활성</span>
            <span v-if="menu.isHidden" class="tree-tag hidden">숨김</span>
            <div class="tree-actions">
              <button class="tree-btn" @click="openCreate(menu)" title="하위 추가"><i class="ti ti-plus"></i></button>
              <button class="tree-btn" @click="openEdit(menu)" title="수정"><i class="ti ti-edit"></i></button>
              <button class="tree-btn danger" @click="deleteMenu(menu.id)" title="삭제"><i class="ti ti-trash"></i></button>
            </div>
          </div>

          <!-- 하위 메뉴 드래그 영역 -->
          <div
            v-if="menu.children?.length"
            :ref="el => setChildRef(el, menu.id)"
            :data-parent-id="menu.id"
            class="child-sortable"
          >
            <div
              v-for="child in menu.children"
              :key="child.id"
              :data-id="child.id"
              class="sortable-node"
            >
              <div class="tree-item child-item">
                <i class="ti ti-grip-vertical drag-handle"></i>
                <span class="tree-type">{{ typeIcon(child.menuType) }}</span>
                <span class="tree-name">{{ child.name }}</span>
                <span class="tree-url">{{ child.url || '' }}</span>
                <span v-if="!child.isActive" class="tree-tag inactive">비활성</span>
                <span v-if="child.isHidden" class="tree-tag hidden">숨김</span>
                <div class="tree-actions">
                  <button class="tree-btn" @click="openCreate(child)" title="하위 추가"><i class="ti ti-plus"></i></button>
                  <button class="tree-btn" @click="openEdit(child)" title="수정"><i class="ti ti-edit"></i></button>
                  <button class="tree-btn danger" @click="deleteMenu(child.id)" title="삭제"><i class="ti ti-trash"></i></button>
                </div>
              </div>

              <!-- 3depth 하위 -->
              <div
                v-if="child.children?.length"
                :ref="el => setChildRef(el, child.id)"
                :data-parent-id="child.id"
                class="child-sortable"
              >
                <div
                  v-for="sub in child.children"
                  :key="sub.id"
                  :data-id="sub.id"
                  class="sortable-node"
                >
                  <div class="tree-item sub-item">
                    <i class="ti ti-grip-vertical drag-handle"></i>
                    <span class="tree-type">{{ typeIcon(sub.menuType) }}</span>
                    <span class="tree-name">{{ sub.name }}</span>
                    <span class="tree-url">{{ sub.url || '' }}</span>
                    <div class="tree-actions">
                      <button class="tree-btn" @click="openEdit(sub)" title="수정"><i class="ti ti-edit"></i></button>
                      <button class="tree-btn danger" @click="deleteMenu(sub.id)" title="삭제"><i class="ti ti-trash"></i></button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 메뉴 폼 다이얼로그 -->
    <el-dialog v-model="showForm" :title="editing ? '메뉴 수정' : '메뉴 추가'" width="520px" destroy-on-close>
      <el-form :model="form" label-position="top">

        <el-form-item label="상위 메뉴">
          <el-select v-model="parentId" clearable placeholder="없음 (최상위 메뉴)" style="width:100%">
            <el-option
              v-for="m in flatMenus"
              :key="m.id"
              :value="m.id"
              :label="'　'.repeat(m.depth) + m.name"
            />
          </el-select>
        </el-form-item>

        <div class="form-row">
          <el-form-item label="메뉴명">
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item label="유형">
            <el-select v-model="form.menuType" style="width:100%">
              <el-option value="PAGE"  label="페이지" />
              <el-option value="LINK"  label="링크" />
              <el-option value="GROUP" label="그룹(폴더)" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item v-if="form.menuType === 'PAGE'" label="페이지 유형">
          <el-select v-model="form.pageType" style="width:100%">
            <el-option value="BOARD"     label="게시판" />
            <el-option value="CALENDAR"  label="캘린더 (/calendar 자동 연결)" />
            <el-option value="CONTENT"   label="안내 페이지" />
            <el-option value="DASHBOARD" label="대시보드 (/ 자동 연결)" />
          </el-select>
        </el-form-item>

        <!-- 대시보드 자동 연결 안내 -->
        <el-alert
          v-if="form.menuType === 'PAGE' && form.pageType === 'DASHBOARD'"
          title="대시보드(/)로 자동 연결됩니다."
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom:12px"
        />

        <!-- 캘린더 선택 -->
        <el-form-item v-if="form.menuType === 'PAGE' && form.pageType === 'CALENDAR'" label="연결할 캘린더 (선택)">
          <el-select v-model="form.targetId" style="width:100%" placeholder="캘린더 선택 (미선택 시 전체 캘린더 표시)" clearable>
            <el-option
              v-for="c in calendars"
              :key="c.id"
              :value="c.id"
              :label="c.name"
            />
          </el-select>
        </el-form-item>

        <el-form-item v-if="form.menuType === 'PAGE' && form.pageType === 'BOARD'" label="연결할 게시판">
          <el-select v-model="form.targetId" style="width:100%" placeholder="게시판 선택">
            <el-option
              v-for="b in boards"
              :key="b.id"
              :value="b.id"
              :label="`[${boardTypeLabel(b.boardType)}] ${b.name}`"
            />
          </el-select>
        </el-form-item>

        <template v-if="form.menuType !== 'GROUP'">
          <el-form-item label="URL 설정">
            <el-radio-group v-model="form.urlAuto">
              <el-radio :value="true">자동 (/{id})</el-radio>
              <el-radio :value="false">직접 입력</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="!form.urlAuto || form.menuType === 'LINK'" label="URL">
            <el-input v-model="form.url" :placeholder="form.menuType === 'LINK' ? 'https://...' : '/notice'" />
          </el-form-item>
        </template>

        <el-form-item label="설명">
          <el-input v-model="form.description" placeholder="페이지 상단에 표시될 설명" />
        </el-form-item>

        <div class="form-row">
          <el-form-item label="접근 권한">
            <el-select v-model="form.accessType" style="width:100%">
              <el-option value="ALL"   label="모든 사용자" />
              <el-option value="LOGIN" label="로그인 사용자" />
              <el-option value="GROUP" label="그룹 지정" />
            </el-select>
          </el-form-item>
          <el-form-item label="순서">
            <el-input-number v-model="form.sortOrder" :min="0" style="width:100%" />
          </el-form-item>
        </div>

        <div class="check-row">
          <el-checkbox v-model="form.isHidden">메뉴 숨김</el-checkbox>
          <el-checkbox v-model="form.hideIfNoPermission">권한 없을 때 숨김</el-checkbox>
          <el-checkbox v-if="form.menuType === 'LINK'" v-model="form.newWindow">새 창으로 열기</el-checkbox>
          <el-checkbox v-model="form.isActive">활성화</el-checkbox>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">취소</el-button>
        <el-button type="primary" :loading="saving" @click="saveMenu">저장</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import Sortable from 'sortablejs'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'

const menus       = ref([])
const boards      = ref([])
const calendars   = ref([])
const loading     = ref(false)
const saving      = ref(false)
const sortSaving  = ref(false)
const sortChanged = ref(false)
const showForm    = ref(false)
const editing     = ref(null)
const parentId    = ref(null)

const rootSortable = ref(null)
const childRefs    = ref({})
const sortableInstances = []

const defaultForm = () => ({
  name: '', menuType: 'PAGE', pageType: 'BOARD', url: '', targetId: null,
  urlAuto: true, newWindow: false, description: '',
  accessType: 'ALL', sortOrder: 0,
  isHidden: false, hideIfNoPermission: true, isActive: true,
})
const form = ref(defaultForm())

// 트리를 flat 배열로 (상위 메뉴 선택용)
const flatMenus = computed(() => {
  const result = []
  const flatten = (nodes, depth = 0) => {
    nodes.forEach(n => {
      result.push({ ...n, depth })
      if (n.children?.length) flatten(n.children, depth + 1)
    })
  }
  flatten(menus.value)
  return result
})

function typeIcon(t) {
  return { PAGE: '📄', LINK: '🔗', GROUP: '📁' }[t] || '📄'
}
function boardTypeLabel(t) {
  return { POST: '글', GALLERY: '갤러리', DOCUMENT: '자료실' }[t] || t
}

function setChildRef(el, id) {
  if (el) childRefs.value[id] = el
}

// Sortable 초기화
function initSortable() {
  // 기존 인스턴스 정리
  sortableInstances.forEach(s => s.destroy())
  sortableInstances.length = 0

  if (!rootSortable.value) return

  // 최상위 레벨 sortable
  const rootInstance = Sortable.create(rootSortable.value, {
    animation: 150,
    handle: '.drag-handle',
    ghostClass: 'sortable-ghost',
    dragClass: 'sortable-drag',
    onEnd: () => { sortChanged.value = true }
  })
  sortableInstances.push(rootInstance)

  // 하위 레벨 sortable
  Object.values(childRefs.value).forEach(el => {
    if (!el) return
    const inst = Sortable.create(el, {
      animation: 150,
      handle: '.drag-handle',
      ghostClass: 'sortable-ghost',
      dragClass: 'sortable-drag',
      onEnd: () => { sortChanged.value = true }
    })
    sortableInstances.push(inst)
  })
}

// 순서 저장
async function saveSortOrder() {
  sortSaving.value = true
  try {
    // 최상위 순서
    const rootIds = Array.from(rootSortable.value.children)
      .map(el => parseInt(el.dataset.id))
      .filter(Boolean)
    await api.put('/menus/sort', rootIds)

    // 하위 순서
    for (const [parentMenuId, el] of Object.entries(childRefs.value)) {
      if (!el) continue
      const childIds = Array.from(el.children)
        .map(c => parseInt(c.dataset.id))
        .filter(Boolean)
      if (childIds.length) {
        await api.put('/menus/sort', childIds)
      }
    }

    ElMessage.success('순서가 저장되었습니다.')
    sortChanged.value = false
    fetchMenus()
  } catch(e) {
    ElMessage.error('순서 저장에 실패했습니다.')
  } finally { sortSaving.value = false }
}

async function fetchMenus() {
  loading.value = true
  try {
    const res = await api.get('/menus/admin')
    menus.value = res.data.data || []
    await nextTick()
    childRefs.value = {}
    await nextTick()
    initSortable()
  } finally { loading.value = false }
}

async function fetchBoards() {
  try {
    const res = await api.get('/boards')
    boards.value = res.data.data || []
  } catch {}
}

async function fetchCalendars() {
  try {
    const res = await api.get('/calendars/admin')
    calendars.value = res.data.data || []
  } catch {}
}

function openCreate(parent) {
  editing.value  = null
  parentId.value = parent?.id || null
  form.value     = defaultForm()
  showForm.value = true
}

function openEdit(menu) {
  editing.value  = menu
  parentId.value = menu.parentId
  form.value     = { ...menu }
  showForm.value = true
}

async function saveMenu() {
  if (!form.value.name) return ElMessage.warning('메뉴명을 입력해주세요.')
  saving.value = true
  try {
    const payload = { ...form.value, parentId: parentId.value }
    if (editing.value) {
      await api.put(`/menus/${editing.value.id}`, payload)
    } else {
      await api.post('/menus', payload)
    }
    ElMessage.success('저장되었습니다.')
    showForm.value = false
    fetchMenus()
  } finally { saving.value = false }
}

async function deleteMenu(id) {
  await ElMessageBox.confirm('메뉴를 삭제하시겠습니까?', '삭제', { type: 'warning' })
  await api.delete(`/menus/${id}`)
  ElMessage.success('삭제되었습니다.')
  fetchMenus()
}

onMounted(() => { fetchMenus(); fetchBoards(); fetchCalendars() })
onBeforeUnmount(() => {
  sortableInstances.forEach(s => s.destroy())
})
</script>

<style scoped>
.menu-tree {
  background: var(--surface);
  border: 0.5px solid var(--border2);
  border-radius: var(--radius-sm);
  overflow: hidden;
}
.empty-state { padding: 32px; text-align: center; color: var(--t3); font-size: 14px; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.check-row { display: flex; gap: 16px; flex-wrap: wrap; }

.tree-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-bottom: 0.5px solid var(--border2);
  transition: var(--transition);
}
.tree-item:hover { background: var(--surface2); }

.root-item { background: var(--surface); }
.child-item { background: var(--surface2); padding-left: 40px !important; }
.sub-item { background: var(--bg); padding-left: 64px !important; }

.child-sortable { border-left: 3px solid var(--accent); margin-left: 20px; }

.drag-handle {
  font-size: 15px;
  color: var(--t3);
  cursor: grab;
  flex-shrink: 0;
  transition: color 0.15s;
}
.drag-handle:active { cursor: grabbing; }
.tree-item:hover .drag-handle { color: var(--t2); }

.tree-type { font-size: 14px; flex-shrink: 0; }
.tree-name { font-size: 13.5px; color: var(--t1); font-weight: 500; flex-shrink: 0; }
.tree-url { font-size: 12px; color: var(--t3); flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tree-tag { font-size: 10px; padding: 1px 6px; border-radius: 4px; flex-shrink: 0; }
.tree-tag.inactive { background: var(--surface2); color: var(--t3); border: 0.5px solid var(--border); }
.tree-tag.hidden { background: var(--new-bg); color: var(--new); }

.tree-actions { display: flex; gap: 4px; flex-shrink: 0; }
.tree-btn {
  background: none;
  border: 0.5px solid var(--border);
  border-radius: var(--radius-xs);
  padding: 4px 7px;
  color: var(--t2);
  cursor: pointer;
  font-size: 13px;
  transition: var(--transition);
}
.tree-btn:hover { background: var(--surface2); color: var(--t1); }
.tree-btn.danger:hover { background: var(--new-bg); color: var(--new); border-color: var(--new); }
</style>

<style>
.sortable-ghost { opacity: 0.4; background: var(--accent-bg) !important; }
.sortable-drag { background: var(--surface) !important; box-shadow: 0 4px 16px rgba(0,0,0,0.12) !important; border-radius: 8px !important; }
</style>