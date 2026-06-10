<template>
  <div>
    <AdminPageHeader title="메뉴 관리" desc="드래그하여 순서를 변경하세요.">
      <button class="adm-btn ghost" :disabled="!sortChanged || sortSaving" @click="saveSortOrder">
        <i :class="['ti', sortSaving ? 'ti-loader-2 spinning' : 'ti-check']"></i> 순서 저장
      </button>
      <button class="adm-btn primary" @click="openCreate(null)"><i class="ti ti-plus"></i> 메뉴 추가</button>
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
              <button v-if="resourceButtonLabel(menu)" class="tree-btn resource" @click="openResourceManager(menu)">
                {{ resourceButtonLabel(menu) }}
              </button>
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
                  <button v-if="resourceButtonLabel(child)" class="tree-btn resource" @click="openResourceManager(child)">
                    {{ resourceButtonLabel(child) }}
                  </button>
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
                      <button v-if="resourceButtonLabel(sub)" class="tree-btn resource" @click="openResourceManager(sub)">
                        {{ resourceButtonLabel(sub) }}
                      </button>
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

        <!-- 1. 상위 메뉴: 추가 시만 선택 가능, 수정 시 비활성 -->
        <el-form-item label="상위 메뉴">
          <el-select
            v-model="parentId"
            clearable
            placeholder="없음 (최상위 메뉴)"
            style="width:100%"
            :disabled="!!editing"
          >
            <el-option
              v-for="m in flatMenus"
              :key="m.id"
              :value="m.id"
              :label="'　'.repeat(m.depth) + m.name"
            />
          </el-select>
          <div v-if="editing" class="form-hint">상위 메뉴는 생성 후 변경할 수 없습니다.</div>
        </el-form-item>

        <!-- 2. 메뉴명 -->
        <el-form-item label="메뉴명">
          <el-input v-model="form.name" placeholder="메뉴명을 입력하세요" />
        </el-form-item>

        <!-- 3. 설명 -->
        <el-form-item label="설명">
          <el-input v-model="form.description" placeholder="페이지 상단에 표시될 설명 (선택)" />
        </el-form-item>

        <!-- 4. 유형: 최상위(parentId 없음)이면 GROUP(폴더) 고정 -->
        <div class="form-row">
          <el-form-item label="유형">
            <template v-if="!parentId">
              <el-input model-value="그룹(폴더)" disabled style="width:100%" />
              <div class="form-hint">최상위 메뉴는 폴더로 고정됩니다.</div>
            </template>
            <el-select v-else v-model="form.menuType" style="width:100%" :disabled="!!editing">
              <el-option value="PAGE"  label="페이지" />
              <el-option value="LINK"  label="링크" />
              <el-option value="GROUP" label="그룹(폴더)" />
            </el-select>
          </el-form-item>

          <!-- 페이지 유형: parentId 있고 PAGE인 경우만 -->
          <el-form-item v-if="parentId && form.menuType === 'PAGE'" label="페이지 유형">
            <el-select v-model="form.pageType" style="width:100%" :disabled="!!editing">
              <el-option value="BOARD"     label="게시판" />
              <el-option value="CALENDAR"  label="캘린더" />
              <el-option value="CONTENT"   label="안내 페이지" />
              <el-option value="DASHBOARD" label="대시보드" />
            </el-select>
          </el-form-item>
        </div>

        <!-- 대시보드 자동 연결 안내 -->
        <el-alert
          v-if="parentId && form.menuType === 'PAGE' && form.pageType === 'DASHBOARD'"
          title="대시보드(/)로 자동 연결됩니다."
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom:12px"
        />

        <!-- 캘린더 선택 -->
        <el-form-item v-if="parentId && form.menuType === 'PAGE' && form.pageType === 'CALENDAR'" label="연결할 캘린더 (선택)">
          <el-select v-model="form.targetId" style="width:100%" placeholder="미선택 시 전체 캘린더 표시" clearable>
            <el-option v-for="c in calendars" :key="c.id" :value="c.id" :label="c.name" />
          </el-select>
        </el-form-item>

        <!-- 5. URL 설정: 추가 시 자동/직접, 수정 시 현재 URL 표시 -->
        <template v-if="form.menuType !== 'GROUP'">
          <!-- 수정 시: 현재 URL 읽기 전용 표시 -->
          <el-form-item v-if="editing" label="URL">
            <div class="url-readonly">
              <span class="url-badge">현재</span>
              <code class="url-value">{{ form.url || `/${editing.id}` }}</code>
            </div>
            <div class="form-hint" style="margin-top:5px">URL은 생성 시 결정되며 변경할 수 없습니다.</div>
          </el-form-item>
          <!-- 추가 시: 자동/직접 선택 -->
          <template v-else>
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
        </template>

        <!-- 6. 접근 권한 -->
        <el-form-item label="접근 권한">
          <el-select v-model="form.accessType" style="width:100%">
            <el-option value="ALL"   label="모든 사용자" />
            <el-option value="LOGIN" label="로그인 사용자" />
            <el-option value="GROUP" label="그룹 지정" />
          </el-select>
        </el-form-item>

        <!-- 접근 허용 그룹: 최상위 그룹 제외, "상위 - 하위" 형식 표시 -->
        <el-form-item v-if="form.accessType === 'GROUP'" label="접근 허용 그룹">
          <el-select
            v-model="form.allowedGroupIds"
            multiple
            filterable
            style="width:100%"
            placeholder="허용할 그룹을 선택하세요"
          >
            <el-option
              v-for="g in subGroupOptions"
              :key="g.id"
              :value="g.id"
              :label="g.label"
            />
          </el-select>
          <div class="form-hint">최상위 그룹은 표시되지 않습니다.</div>
        </el-form-item>

        <!-- 7. 체크박스 (순서 필드 제거) -->
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

    <el-dialog v-model="showResourceForm" :title="resourceButtonLabel(resourceMenu) || '리소스 관리'" width="720px" destroy-on-close>
      <div v-if="resourceType === 'BOARD'" class="resource-form">

        <!-- 기본 정보 -->
        <div class="res-section-title">기본 정보</div>
        <div class="form-row">
          <el-form-item label="게시판명">
            <el-input v-model="boardForm.name" />
          </el-form-item>
          <el-form-item label="게시판 유형">
            <el-select v-model="boardForm.boardType" style="width:100%">
              <el-option value="POST" label="일반 게시판" />
              <el-option value="GALLERY" label="갤러리" />
              <el-option value="WEBZINE" label="웹진" />
              <el-option value="DOCUMENT" label="자료실" />
            </el-select>
          </el-form-item>
        </div>

        <!-- 기능 설정 -->
        <div class="res-section-title">기능 설정</div>
        <div class="check-row">
          <el-checkbox v-model="boardForm.useComment">댓글 사용</el-checkbox>
          <el-checkbox v-model="boardForm.useLike">좋아요 사용</el-checkbox>
          <el-checkbox v-model="boardForm.useNotice">공지 사용</el-checkbox>
          <el-checkbox v-model="boardForm.isActive">활성화</el-checkbox>
        </div>

        <!-- 파일 설정 -->
        <div class="res-section-title">파일 설정</div>
        <div class="form-row">
          <el-form-item label="최대 용량 (MB)">
            <el-input-number v-model="boardForm.fileMaxSizeMb" :min="1" :max="500" style="width:100%" />
          </el-form-item>
          <el-form-item label="최대 파일 수">
            <el-input-number v-model="boardForm.fileMaxCount" :min="1" :max="20" style="width:100%" />
          </el-form-item>
        </div>
        <el-form-item label="허용 확장자">
          <el-input v-model="boardForm.fileAllowedExtensions" placeholder="jpg,png,pdf,docx" />
        </el-form-item>

        <!-- 그룹 권한 -->
        <div class="res-section-title">그룹 권한</div>
        <div class="perm-hint-sm">
          <i class="ti ti-info-circle"></i>
          <span><strong>관리</strong> 권한이 있는 그룹은 해당 게시판의 모든 글을 수정·삭제할 수 있습니다.</span>
        </div>

        <div class="perm-add-row">
          <el-select
            v-model="boardAddGroupId"
            placeholder="그룹 선택"
            style="flex:1"
            filterable
            no-data-text="추가 가능한 그룹이 없습니다"
          >
            <el-option v-for="g in boardAvailableGroups" :key="g.id" :value="g.id" :label="g.label" />
          </el-select>
          <button class="adm-btn primary sm" @click="addBoardPermRow" :disabled="!boardAddGroupId">
            <i class="ti ti-plus"></i> 추가
          </button>
        </div>

        <div v-loading="boardPermLoading">
          <div v-if="boardPermRows.length" class="perm-grid">
            <!-- 헤더 행 (grid 자식 6개) -->
            <div class="pg-cell pg-head pg-name">그룹</div>
            <div class="pg-cell pg-head pg-chk" title="게시물 목록·내용을 볼 수 있습니다">조회</div>
            <div class="pg-cell pg-head pg-chk" title="게시물에 댓글을 작성할 수 있습니다">댓글</div>
            <div class="pg-cell pg-head pg-chk" title="첨부파일을 다운로드할 수 있습니다">다운로드</div>
            <div class="pg-cell pg-head pg-chk pg-manage" title="게시판의 모든 글을 수정·삭제할 수 있습니다">관리</div>
            <div class="pg-cell pg-head pg-del"></div>
            <!-- 데이터 행 (v-for, 각 행 = grid 자식 6개) -->
            <template v-for="row in boardPermRows" :key="row.groupId">
              <div class="pg-cell pg-name pg-row-start">{{ row.label }}</div>
              <div class="pg-cell pg-chk">
                <el-tooltip content="게시물 목록·내용을 볼 수 있습니다" placement="top">
                  <el-checkbox v-model="row.canRead" />
                </el-tooltip>
              </div>
              <div class="pg-cell pg-chk">
                <el-tooltip content="게시물에 댓글을 작성할 수 있습니다" placement="top">
                  <el-checkbox v-model="row.canComment" />
                </el-tooltip>
              </div>
              <div class="pg-cell pg-chk">
                <el-tooltip content="첨부파일을 다운로드할 수 있습니다" placement="top">
                  <el-checkbox v-model="row.canDownload" />
                </el-tooltip>
              </div>
              <div class="pg-cell pg-chk pg-manage">
                <el-tooltip content="게시판의 모든 글을 수정·삭제할 수 있습니다" placement="top">
                  <el-checkbox v-model="row.canManage" />
                </el-tooltip>
              </div>
              <div class="pg-cell pg-del">
                <button class="del-btn" @click="removeBoardPermRow(row.groupId)" title="삭제">
                  <i class="ti ti-x"></i>
                </button>
              </div>
            </template>
          </div>
          <div v-else class="perm-empty-msg">위에서 그룹을 선택해 권한을 추가하세요.</div>
        </div>
      </div>

      <div v-else-if="resourceType === 'CALENDAR'" class="resource-form">
        <div class="form-row">
          <el-form-item label="캘린더명">
            <el-input v-model="calendarForm.name" />
          </el-form-item>
          <el-form-item label="색상">
            <div class="color-field">
              <input type="color" v-model="calendarForm.color" />
              <el-input v-model="calendarForm.color" />
            </div>
          </el-form-item>
        </div>
        <el-form-item label="설명">
          <el-input v-model="calendarForm.description" type="textarea" :rows="2" resize="none" />
        </el-form-item>
        <el-checkbox v-model="calendarForm.isActive">활성화</el-checkbox>
        <div class="perm-block">
          <div class="perm-title">캘린더 그룹 권한</div>
          <div v-for="(p, i) in calendarForm.permissions" :key="i" class="perm-row">
            <el-select v-model="p.groupId" placeholder="그룹 선택" style="flex:1">
              <el-option v-for="g in flatGroups" :key="g.id" :value="g.id" :label="g.name" />
            </el-select>
            <el-checkbox v-model="p.canRead">조회</el-checkbox>
            <el-checkbox v-model="p.canWrite">관리</el-checkbox>
            <button class="tree-btn danger" @click="calendarForm.permissions.splice(i, 1)"><i class="ti ti-trash"></i></button>
          </div>
          <button class="tree-btn resource add-perm-btn" @click="addCalendarPerm"><i class="ti ti-plus"></i> 그룹 권한 추가</button>
        </div>
      </div>

      <div v-else-if="resourceType === 'CONTENT'" class="resource-form">
        <el-form-item label="제목">
          <el-input v-model="contentForm.title" />
        </el-form-item>
        <RichEditor v-model="contentForm.content" min-height="420px" />
      </div>

      <template #footer>
        <el-button @click="showResourceForm = false">취소</el-button>
        <el-button type="primary" :loading="resourceSaving" @click="saveResource">저장</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import Sortable from 'sortablejs'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import RichEditor from '@/components/common/RichEditor.vue'
import { useMenuStore } from '@/stores/menu'
import api from '@/api/axios'

const menuStore = useMenuStore()

const menus       = ref([])
const boards      = ref([])
const calendars   = ref([])
const groups      = ref([])
const loading     = ref(false)
const saving      = ref(false)
const sortSaving  = ref(false)
const sortChanged = ref(false)
const showForm    = ref(false)
const editing     = ref(null)
const parentId    = ref(null)
const showResourceForm = ref(false)
const resourceSaving = ref(false)
const resourceMenu = ref(null)
const resourceType = ref('')
const boardForm = ref({})
const boardPermRows = ref([])
const boardPermLoading = ref(false)
const calendarForm = ref({})
const contentForm = ref({ title: '', content: '' })

const rootSortable = ref(null)
const childRefs    = ref({})
const sortableInstances = []

const defaultForm = () => ({
  name: '', menuType: 'PAGE', pageType: 'BOARD', url: '', targetId: null,
  urlAuto: true, newWindow: false, description: '',
  accessType: 'ALL', sortOrder: 0,
  isHidden: false, hideIfNoPermission: true, isActive: true,
  allowedGroupIds: [],
})
const form = ref(defaultForm())

const defaultBoardForm = () => ({
  name: '', boardType: 'POST', useComment: true, useLike: true,
  useAnonymous: false, useNotice: true, noticeCountLimit: 5,
  isActive: true, fileMaxSizeMb: null, fileMaxCount: 5,
  fileAllowedExtensions: '', permissions: [],
})

const defaultCalendarForm = () => ({
  name: '', color: '#4f6ef7', description: '', isActive: true, permissions: [],
})

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

const flatGroups = computed(() => {
  const result = []
  const walk = (nodes = []) => nodes.forEach(g => {
    result.push(g)
    if (g.children?.length) walk(g.children)
  })
  walk(groups.value)
  return result
})

// 접근 허용 그룹 선택용: 최상위·시스템 그룹 제외, "상위 - 하위" 레이블 형식
const subGroupOptions = computed(() => {
  const result = []
  const walk = (nodes = [], parentLabel = '') => {
    nodes.forEach(g => {
      const label = parentLabel ? `${parentLabel} - ${g.name}` : g.name
      if (parentLabel && !g.isSystem) {
        result.push({ id: g.id, label })
      }
      if (g.children?.length) walk(g.children, label)
    })
  }
  walk(groups.value)
  return result
})

// parentId가 없으면(최상위) 유형을 GROUP으로 자동 고정
watch(parentId, (val) => {
  if (!val && !editing.value) {
    form.value.menuType = 'GROUP'
  }
})

function typeIcon(t) {
  return { PAGE: '📄', LINK: '🔗', GROUP: '📁' }[t] || '📄'
}
function boardTypeLabel(t) {
  return { POST: '글', GALLERY: '갤러리', WEBZINE: '웹진', DOCUMENT: '자료실' }[t] || t
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
    await fetchMenus()
    menuStore.fetchMenus(true) // 네비게이션 메뉴 캐시 갱신
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

async function fetchGroups() {
  try {
    const res = await api.get('/groups')
    groups.value = res.data.data || []
  } catch {}
}

function resourceButtonLabel(menu) {
  if (!menu || menu.menuType !== 'PAGE') return ''
  if (menu.pageType === 'BOARD' && menu.targetId) return '게시판 관리'
  if (menu.pageType === 'CALENDAR' && menu.targetId) return '캘린더 관리'
  if (menu.pageType === 'CONTENT') return '안내페이지 관리'
  return ''
}

async function openResourceManager(menu) {
  resourceMenu.value = menu
  resourceType.value = menu.pageType
  resourceSaving.value = false
  if (menu.pageType === 'BOARD') {
    const res = await api.get(`/boards/${menu.targetId}`)
    boardForm.value = { ...defaultBoardForm(), ...res.data.data }
    boardAddGroupId.value = null
    boardPermRows.value = []
    boardPermLoading.value = true
    try {
      const permRes = await api.get(`/admin/boards/${menu.targetId}/permissions`)
      boardPermRows.value = (permRes.data.data || [])
        .filter(r => r.canRead || r.canComment || r.canDownload || r.canManage || r.canWrite)
        .map(r => ({
          groupId: r.groupId,
          label: r.label || (r.parentName ? `${r.parentName} - ${r.groupName}` : r.groupName),
          canRead: !!r.canRead,
          canComment: !!r.canComment,
          canDownload: !!r.canDownload,
          canManage: !!r.canManage,
        }))
    } finally {
      boardPermLoading.value = false
    }
  } else if (menu.pageType === 'CALENDAR') {
    const res = await api.get(`/calendars/admin`)
    const calendar = (res.data.data || []).find(c => Number(c.id) === Number(menu.targetId))
    calendarForm.value = {
      ...defaultCalendarForm(),
      ...(calendar || { name: menu.name, description: menu.description }),
      permissions: JSON.parse(JSON.stringify(calendar?.permissions || [])),
    }
  } else if (menu.pageType === 'CONTENT') {
    try {
      const res = await api.get(`/admin/content-pages/menus/${menu.id}`)
      contentForm.value = {
        title: res.data.data?.title || menu.name,
        content: res.data.data?.content || '',
      }
    } catch {
      contentForm.value = { title: menu.name, content: '' }
    }
  }
  showResourceForm.value = true
}

const boardAddGroupId = ref(null)

const boardAvailableGroups = computed(() => {
  const usedIds = new Set(boardPermRows.value.map(r => r.groupId))
  return subGroupOptions.value.filter(g => !usedIds.has(g.id))
})

function addBoardPermRow() {
  if (!boardAddGroupId.value) return
  const option = subGroupOptions.value.find(g => g.id === boardAddGroupId.value)
  if (!option) return
  boardPermRows.value.push({ groupId: option.id, label: option.label, canRead: true, canComment: false, canDownload: true, canManage: false })
  boardAddGroupId.value = null
}

function removeBoardPermRow(groupId) {
  boardPermRows.value = boardPermRows.value.filter(r => r.groupId !== groupId)
}

function addCalendarPerm() {
  calendarForm.value.permissions.push({ groupId: null, canRead: true, canWrite: false })
}

async function saveResource() {
  if (!resourceMenu.value) return
  resourceSaving.value = true
  try {
    if (resourceType.value === 'BOARD') {
      await api.put(`/boards/${resourceMenu.value.targetId}`, boardForm.value)
      await api.put(`/admin/boards/${resourceMenu.value.targetId}/permissions`, boardPermRows.value)
      await fetchBoards()
    } else if (resourceType.value === 'CALENDAR') {
      await api.put(`/calendars/${resourceMenu.value.targetId}`, calendarForm.value)
      await fetchCalendars()
    } else if (resourceType.value === 'CONTENT') {
      await api.put(`/admin/content-pages/menus/${resourceMenu.value.id}`, contentForm.value)
    }
    ElMessage.success('저장되었습니다.')
    showResourceForm.value = false
    await fetchMenus()
    menuStore.fetchMenus(true)
  } finally {
    resourceSaving.value = false
  }
}

function openCreate(parent) {
  editing.value  = null
  parentId.value = parent?.id || null
  form.value     = defaultForm()

  // 최상위 메뉴는 GROUP(폴더) 고정
  if (!parentId.value) {
    form.value.menuType = 'GROUP'
  }

  // 같은 레벨 형제 메뉴의 최대 sortOrder + 1 을 기본값으로
  const siblings = parent?.id
    ? (flatMenus.value.filter(m => m.parentId === parent.id))
    : menus.value
  const maxOrder = siblings.length
    ? Math.max(...siblings.map(m => m.sortOrder ?? 0))
    : -1
  form.value.sortOrder = maxOrder + 1

  showForm.value = true
}

function openEdit(menu) {
  editing.value  = menu
  parentId.value = menu.parentId
  form.value     = { ...defaultForm(), ...menu, allowedGroupIds: menu.allowedGroupIds || [] }
  showForm.value = true
}

async function saveMenu() {
  if (!form.value.name) return ElMessage.warning('메뉴명을 입력해주세요.')
  saving.value = true
  try {
    const payload = {
      ...form.value,
      parentId: parentId.value,
      allowedGroupIds: form.value.accessType === 'GROUP' ? form.value.allowedGroupIds : [],
    }
    if (editing.value) {
      await api.put(`/menus/${editing.value.id}`, payload)
    } else {
      await api.post('/menus', payload)
    }
    ElMessage.success('저장되었습니다.')
    showForm.value = false
    await fetchMenus()
    menuStore.fetchMenus(true)
  } finally { saving.value = false }
}

async function deleteMenu(id) {
  await ElMessageBox.confirm('메뉴를 삭제하시겠습니까?', '삭제', { type: 'warning' })
  await api.delete(`/menus/${id}`)
  ElMessage.success('삭제되었습니다.')
  await fetchMenus()
  menuStore.fetchMenus(true)
}

onMounted(() => { fetchMenus(); fetchBoards(); fetchCalendars(); fetchGroups() })
onBeforeUnmount(() => {
  sortableInstances.forEach(s => s.destroy())
})
</script>

<style scoped>
@import '@/assets/admin-table.css';
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
.tree-btn.resource {
  color: var(--accent);
  border-color: rgba(37,99,235,0.25);
  background: var(--accent-bg);
  font-weight: 700;
}
.tree-btn.resource:hover { color: #fff; background: var(--accent); border-color: var(--accent); }
.tree-btn.danger:hover { background: var(--new-bg); color: var(--new); border-color: var(--new); }
.resource-form { display: flex; flex-direction: column; gap: 12px; }
.perm-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 10px;
  border-top: 0.5px solid var(--border2);
}
.perm-title { font-size: 13px; font-weight: 800; color: var(--t1); }
.perm-row { display: flex; align-items: center; gap: 8px; }
.add-perm-btn { justify-content: center; min-height: 34px; }
.color-field { display: flex; align-items: center; gap: 8px; width: 100%; }
.color-field input[type="color"] {
  width: 42px;
  height: 32px;
  border: 0;
  border-radius: var(--radius-xs);
  background: transparent;
  cursor: pointer;
}

/* URL 읽기 전용 표시 */
.url-readonly {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  background: var(--surface2);
  border: 0.5px solid var(--border);
  border-radius: var(--radius-xs);
}
.url-badge {
  font-size: 10px;
  font-weight: 700;
  color: var(--accent-t);
  background: var(--accent-bg);
  border: 0.5px solid var(--accent);
  border-radius: 4px;
  padding: 1px 6px;
  flex-shrink: 0;
}
.url-value {
  font-size: 13px;
  color: var(--t1);
  font-family: 'Fira Code', 'Consolas', monospace;
}

/* 폼 힌트 텍스트 */
.form-hint {
  font-size: 12px;
  color: var(--t3);
  margin-top: 4px;
}

/* 리소스 폼 섹션 제목 */
.res-section-title {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--t3);
  padding: 4px 0 8px;
  border-bottom: 1px solid var(--border);
  margin-top: 4px;
}

/* 권한 안내 (소형) */
.perm-hint-sm {
  display: flex;
  gap: 6px;
  align-items: flex-start;
  background: color-mix(in srgb, var(--accent) 8%, transparent);
  border: 0.5px solid color-mix(in srgb, var(--accent) 30%, transparent);
  border-radius: var(--radius-xs);
  padding: 8px 12px;
  font-size: 12px;
  color: var(--t2);
  line-height: 1.5;
}
.perm-hint-sm i { color: var(--accent); font-size: 14px; flex-shrink: 0; margin-top: 1px; }

/* 권한 테이블 (리소스 폼 내) */
.perm-table-wrap { overflow-x: auto; }
.perm-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.perm-table th {
  padding: 7px 8px;
  background: var(--surface2);
  color: var(--t3);
  font-weight: 700;
  font-size: 12px;
  border-bottom: 1px solid var(--border);
}
.perm-table td {
  padding: 8px;
  border-bottom: 0.5px solid var(--border);
  vertical-align: middle;
}
.perm-table tbody tr:hover { background: var(--surface2); }
.perm-col { text-align: center; width: 72px; }
.manage-col { background: color-mix(in srgb, var(--color-danger) 5%, transparent); }
.perm-group { vertical-align: middle; }
.perm-group-inner { display: inline-flex; align-items: center; gap: 5px; flex-wrap: nowrap; }
.perm-parent { color: var(--t3); font-size: 12px; white-space: nowrap; }
.perm-parent::after { content: ' -'; }
.perm-name { font-weight: 600; color: var(--t1); white-space: nowrap; }
.perm-empty { text-align: center; color: var(--t3); padding: 16px; }
.th-inner { display: flex; flex-direction: column; align-items: center; gap: 3px; }
.col-all-btn {
  font-size: 10px; font-weight: 700; padding: 1px 6px;
  border-radius: 4px; border: 1px solid var(--accent);
  color: var(--accent-t); background: var(--accent-bg);
  cursor: pointer; transition: all .12s; white-space: nowrap;
}
.col-all-btn:hover { background: var(--accent); color: #fff; }
.row-all-btn {
  margin-left: 4px; font-size: 10px; font-weight: 700; padding: 1px 6px;
  border-radius: 4px; border: 1px solid var(--border);
  color: var(--t3); background: var(--surface2);
  cursor: pointer; transition: all .12s; white-space: nowrap; flex-shrink: 0;
}
.row-all-btn:hover { border-color: var(--accent); color: var(--accent-t); background: var(--accent-bg); }

.perm-add-row { display: flex; gap: 8px; margin-bottom: 10px; }
.adm-btn.sm { padding: 0 12px; height: 32px; font-size: 13px; }
.perm-grid {
  display: grid;
  grid-template-columns: 1fr 76px 76px 76px 76px 36px;
  border: 1px solid var(--border);
  border-radius: var(--radius-xs);
  overflow: hidden;
}
.pg-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 7px 6px;
  border-bottom: 0.5px solid var(--border);
  font-size: 13px;
}
.pg-cell:nth-last-child(-n+6) { border-bottom: none; }
.pg-head {
  background: var(--surface2);
  font-size: 11px; font-weight: 700; color: var(--t3);
  border-bottom: 1px solid var(--border) !important;
}
.pg-name { justify-content: flex-start; padding-left: 10px; font-weight: 500; color: var(--t1); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pg-head.pg-name { font-weight: 700; color: var(--t3); }
.pg-chk { }
.pg-manage { background: color-mix(in srgb, var(--color-danger) 5%, transparent); }
.pg-del { }
/* 데이터 행 hover: 6개 셀에 동시 배경 적용 불가 → 각 셀 hover로 대체 */
.pg-cell:not(.pg-head):hover { background: var(--surface2); }
.del-btn {
  width: 22px; height: 22px; border-radius: 4px;
  border: 1px solid var(--border); background: transparent; color: var(--t3);
  cursor: pointer; display: inline-flex; align-items: center; justify-content: center;
  font-size: 12px; transition: all .12s;
}
.del-btn:hover { border-color: var(--color-danger); color: var(--color-danger); background: color-mix(in srgb, var(--color-danger) 8%, transparent); }
.perm-empty-msg { text-align: center; color: var(--t3); font-size: 13px; padding: 16px 0; }
</style>

<style>
.sortable-ghost { opacity: 0.4; background: var(--accent-bg) !important; }
.sortable-drag { background: var(--surface) !important; box-shadow: 0 4px 16px rgba(0,0,0,0.12) !important; border-radius: 8px !important; }
</style>
