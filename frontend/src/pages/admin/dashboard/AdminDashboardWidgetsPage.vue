<template>
  <div class="adm-page">
    <AdminPageHeader title="대시보드 관리" desc="카드를 드래그해서 순서를 변경하세요">
      <div style="display:flex;gap:8px;align-items:center">
        <transition name="fade">
          <button v-if="sortChanged" class="adm-btn ghost" :disabled="sortSaving" @click="saveSortOrder">
            <i :class="['ti', sortSaving ? 'ti-loader-2 spinning' : 'ti-check']"></i>
            {{ sortSaving ? '저장 중...' : '순서 저장' }}
          </button>
        </transition>
        <button class="adm-btn primary" @click="openCreate"><i class="ti ti-plus"></i> 위젯 추가</button>
      </div>
    </AdminPageHeader>

    <!-- 카드형 미리보기 그리드 -->
    <div v-if="!loading && widgets.length" ref="widgetSortable" class="preview-grid">
      <div
        v-for="w in widgets"
        :key="w.id"
        :data-id="w.id"
        :class="['preview-card', widgetSizeClass(w), { 'is-inactive': !w.isActive }]"
      >
        <!-- 드래그 핸들 + 배지 -->
        <div class="pc-topbar">
          <i class="ti ti-grip-horizontal drag-handle" title="드래그해서 순서 변경"></i>
          <span class="pc-type-badge">{{ typeLabel(w.widgetType) }}</span>
          <div class="pc-badges">
            <span v-if="!['IMAGE_SLIDER','MEMBER_STATS','VISIT_STATS'].includes(w.widgetType)"
                  :class="['pc-size-badge', getSize(w) === 'full' ? 'size-full' : 'size-half']">
              {{ getSize(w) === 'full' ? '1칸' : '반칸' }}
            </span>
            <span v-if="!w.isActive" class="pc-inactive-badge">비활성</span>
          </div>
        </div>

        <!-- 위젯 미리보기 본문 -->
        <div class="pc-body-wrap">
        <div class="pc-body">
          <!-- 제목 -->
          <div class="pc-title">{{ w.title || '(제목 없음)' }}</div>

          <!-- RECENT_POSTS 미리보기 -->
          <template v-if="w.widgetType === 'RECENT_POSTS'">
            <div class="pc-preview-rows">
              <div v-for="post in (w.posts || []).slice(0,5)" :key="post.id" class="pc-post-row">
                <span class="pc-post-title">{{ post.title }}</span>
                <span class="pc-post-date">{{ formatDate(post.createdAt) }}</span>
              </div>
              <div v-if="!(w.posts || []).length" class="pc-empty-hint">
                <i class="ti ti-file-text"></i> {{ w.targetBoardName ? w.targetBoardName + ' 게시판' : '전체 게시판' }} 최신 글
              </div>
            </div>
          </template>

          <!-- IMAGE_SLIDER 미리보기 -->
          <template v-else-if="w.widgetType === 'IMAGE_SLIDER'">
            <div class="pc-slider-preview">
              <div class="pc-slider-inner">
                <template v-if="parseConfig(w).slides?.length">
                  <div
                    v-for="(s, i) in parseConfig(w).slides.slice(0, 3)"
                    :key="i"
                    class="pc-slide-thumb"
                    :style="s.imageUrl ? `background-image:url(${s.imageUrl})` : ''"
                  >
                    <span v-if="!s.imageUrl" class="pc-slide-label">{{ s.title || `슬라이드 ${i+1}` }}</span>
                  </div>
                </template>
                <div v-else class="pc-empty-hint"><i class="ti ti-photo"></i> 슬라이드 {{ parseConfig(w).slides?.length || 0 }}개</div>
              </div>
            </div>
          </template>

          <!-- LINK_LIST 미리보기 -->
          <template v-else-if="w.widgetType === 'LINK_LIST'">
            <div class="pc-link-grid">
              <template v-if="parseConfig(w).links?.length">
                <div v-for="(l, i) in parseConfig(w).links.slice(0, 6)" :key="i" class="pc-link-chip">
                  {{ l.label || l.url }}
                </div>
              </template>
              <div v-else class="pc-empty-hint"><i class="ti ti-link"></i> 링크 없음</div>
            </div>
          </template>

          <!-- MEMBER_STATS / VISIT_STATS 미리보기 -->
          <template v-else-if="w.widgetType === 'MEMBER_STATS' || w.widgetType === 'VISIT_STATS'">
            <div class="pc-stats-row">
              <div class="pc-stat"><div class="pc-stat-num">—</div><div class="pc-stat-lbl">전체 회원</div></div>
              <div class="pc-stat-div"></div>
              <div class="pc-stat"><div class="pc-stat-num">—</div><div class="pc-stat-lbl">게시판</div></div>
              <div class="pc-stat-div"></div>
              <div class="pc-stat"><div class="pc-stat-num">—</div><div class="pc-stat-lbl">문의</div></div>
              <div class="pc-stat-div"></div>
              <div class="pc-stat"><div class="pc-stat-num">—</div><div class="pc-stat-lbl">오늘 방문</div></div>
            </div>
          </template>

          <!-- CUSTOM 미리보기 -->
          <template v-else-if="w.widgetType === 'CUSTOM'">
            <div class="pc-custom-body" v-html="parseConfig(w).content || '<span class=\'pc-empty-hint\'><i class=\'ti ti-pencil\'></i> 본문 없음</span>'" />
          </template>
        </div>

        <!-- 수정/삭제 오버레이 (본문 영역에만) -->
        <div class="pc-overlay">
          <button class="pc-action-btn" @click="openEdit(w)"><i class="ti ti-edit"></i> 수정</button>
          <button class="pc-action-btn danger" @click="deleteWidget(w.id)"><i class="ti ti-trash"></i></button>
        </div>
        </div><!-- /pc-body-wrap -->
      </div>
    </div>

    <div v-else-if="loading" class="adm-card" style="padding:60px;text-align:center;color:var(--t3)">
      <i class="ti ti-loader-2 spinning" style="font-size:28px"></i>
    </div>
    <div v-else class="adm-card" style="padding:60px;text-align:center;color:var(--t3)">
      <i class="ti ti-layout-dashboard" style="font-size:40px;display:block;margin-bottom:12px"></i>
      등록된 위젯이 없습니다. 위젯을 추가해보세요.
    </div>

    <!-- 위젯 폼 다이얼로그 -->
    <el-dialog
      v-model="showForm"
      :title="editing ? '위젯 수정' : '위젯 추가'"
      :width="form.widgetType === 'CUSTOM' ? '780px' : '520px'"
      destroy-on-close
    >
      <div class="dlg-form">
        <div class="dlg-row">
          <div class="dlg-field">
            <label>위젯 유형</label>
            <el-select v-model="form.widgetType" style="width:100%" @change="onTypeChange">
              <el-option value="RECENT_POSTS"  label="최신 글" />
              <el-option value="IMAGE_SLIDER"  label="이미지 슬라이더" />
              <el-option value="LINK_LIST"     label="링크 목록" />
              <el-option value="MEMBER_STATS"  label="회원 현황" />
              <el-option value="VISIT_STATS"   label="접속 통계" />
              <el-option value="CUSTOM"        label="커스텀" />
            </el-select>
          </div>
          <div class="dlg-field">
            <label>제목</label>
            <el-input v-model="form.title" />
          </div>
        </div>

        <!-- 크기 선택 (IMAGE_SLIDER/MEMBER_STATS/VISIT_STATS 제외) -->
        <div v-if="!['IMAGE_SLIDER','MEMBER_STATS','VISIT_STATS'].includes(form.widgetType)" class="dlg-row">
          <div class="dlg-field">
            <label>위젯 크기</label>
            <div class="size-picker">
              <button type="button" :class="['size-opt', config.size === 'full' ? 'active' : '']" @click="config.size = 'full'">
                <div class="size-preview full-preview"><div class="sp-block"></div></div>
                <span>1칸 (전체 너비)</span>
              </button>
              <button type="button" :class="['size-opt', config.size === 'half' ? 'active' : '']" @click="config.size = 'half'">
                <div class="size-preview half-preview"><div class="sp-block"></div><div class="sp-empty"></div></div>
                <span>반칸 (절반 너비)</span>
              </button>
            </div>
          </div>
          <div class="dlg-field">
            <label>활성화</label>
            <div style="padding-top:6px">
              <label class="chk-item"><el-checkbox v-model="form.isActive" />활성화</label>
            </div>
          </div>
        </div>

        <div v-if="['IMAGE_SLIDER','MEMBER_STATS','VISIT_STATS'].includes(form.widgetType)" style="margin-bottom:8px">
          <label class="chk-item"><el-checkbox v-model="form.isActive" />활성화</label>
        </div>

        <!-- RECENT_POSTS -->
        <template v-if="form.widgetType === 'RECENT_POSTS'">
          <hr class="dlg-divider"/>
          <div class="dlg-row">
            <div class="dlg-field">
              <label>대상 게시판</label>
              <el-select v-model="form.targetBoardId" clearable placeholder="전체" style="width:100%">
                <el-option v-for="b in boards" :key="b.id" :value="b.id" :label="b.name" />
              </el-select>
            </div>
            <div class="dlg-field">
              <label>표시 개수</label>
              <el-input-number v-model="form.postCount" :min="1" :max="20" style="width:100%" />
            </div>
          </div>
        </template>

        <!-- IMAGE_SLIDER -->
        <template v-if="form.widgetType === 'IMAGE_SLIDER'">
          <hr class="dlg-divider"/>
          <div class="dlg-section-title">슬라이드</div>
          <div v-for="(s, i) in config.slides" :key="i" class="sub-item">
            <div class="dlg-row">
              <div class="dlg-field"><label>제목</label><el-input v-model="s.title" /></div>
              <div class="dlg-field"><label>이미지 URL</label><el-input v-model="s.imageUrl" /></div>
            </div>
            <div class="dlg-row">
              <div class="dlg-field"><label>링크 URL</label><el-input v-model="s.url" /></div>
              <div class="dlg-field" style="flex-direction:row;align-items:flex-end;gap:10px;justify-content:space-between;padding-bottom:2px">
                <label class="chk-item"><el-checkbox v-model="s.newWindow" />새 창</label>
                <button class="act-btn danger" @click="config.slides.splice(i,1)"><i class="ti ti-trash"></i></button>
              </div>
            </div>
          </div>
          <button class="adm-btn ghost" style="width:100%" @click="config.slides.push({title:'',imageUrl:'',url:'',newWindow:false})">
            <i class="ti ti-plus"></i> 슬라이드 추가
          </button>
        </template>

        <!-- LINK_LIST -->
        <template v-if="form.widgetType === 'LINK_LIST'">
          <hr class="dlg-divider"/>
          <div class="dlg-section-title">링크</div>
          <div v-for="(l, i) in config.links" :key="i" class="sub-item">
            <div class="dlg-row">
              <div class="dlg-field"><label>레이블</label><el-input v-model="l.label" /></div>
              <div class="dlg-field"><label>URL</label><el-input v-model="l.url" /></div>
            </div>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <label class="chk-item"><el-checkbox v-model="l.newWindow" />새 창</label>
              <button class="act-btn danger" @click="config.links.splice(i,1)"><i class="ti ti-trash"></i></button>
            </div>
          </div>
          <button class="adm-btn ghost" style="width:100%" @click="config.links.push({label:'',url:'',newWindow:false})">
            <i class="ti ti-plus"></i> 링크 추가
          </button>
        </template>

        <!-- CUSTOM -->
        <template v-if="form.widgetType === 'CUSTOM'">
          <hr class="dlg-divider"/>
          <div class="dlg-field">
            <label>본문 내용</label>
            <RichEditor v-model="config.content" placeholder="위젯에 표시될 내용을 입력하세요." />
          </div>
        </template>
      </div>

      <template #footer>
        <button class="adm-btn ghost" @click="showForm = false">취소</button>
        <button class="adm-btn primary" :disabled="saving" @click="saveWidget">
          <i v-if="saving" class="ti ti-loader-2 spinning"></i>{{ saving ? '저장 중...' : '저장' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Sortable from 'sortablejs'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import RichEditor from '@/components/common/RichEditor.vue'
import api from '@/api/axios'

const widgets     = ref([])
const boards      = ref([])
const loading     = ref(false)
const saving      = ref(false)
const sortSaving  = ref(false)
const sortChanged = ref(false)
const showForm    = ref(false)
const editing     = ref(null)
const widgetSortable = ref(null)
let sortableInstance = null

const defaultForm = () => ({
  widgetType: 'RECENT_POSTS', title: '',
  targetBoardId: null, postCount: 5, isActive: true
})
const form   = ref(defaultForm())
const config = ref({ slides: [], links: [], content: '', size: 'half' })

// ===== 헬퍼 =====
function parseConfig(w) {
  try { return w.extraConfig ? (typeof w.extraConfig === 'string' ? JSON.parse(w.extraConfig) : w.extraConfig) : {} }
  catch { return {} }
}
function getSize(w) { return parseConfig(w).size || 'half' }
function widgetSizeClass(w) {
  if (['IMAGE_SLIDER','MEMBER_STATS','VISIT_STATS'].includes(w.widgetType)) return 'pc-full'
  return getSize(w) === 'full' ? 'pc-full' : 'pc-half'
}
function typeLabel(t) {
  return { RECENT_POSTS:'최신 글', IMAGE_SLIDER:'슬라이더', LINK_LIST:'링크', MEMBER_STATS:'회원 현황', VISIT_STATS:'접속 통계', CUSTOM:'커스텀' }[t] || t
}
function formatDate(d) {
  if (!d) return ''
  const dt = new Date(d), now = new Date()
  if (dt.toDateString() === now.toDateString()) return dt.toTimeString().slice(0, 5)
  return `${dt.getMonth()+1}.${String(dt.getDate()).padStart(2,'0')}`
}

// ===== 데이터 로드 =====
async function fetchWidgets() {
  loading.value = true
  try {
    const r = await api.get('/admin/dashboard/widgets')
    widgets.value = r.data.data || []
    await nextTick()
    initSortable()
  } finally { loading.value = false }
}
async function fetchBoards() {
  const r = await api.get('/boards'); boards.value = r.data.data || []
}

// ===== 드래그 정렬 =====
function initSortable() {
  if (sortableInstance) { sortableInstance.destroy(); sortableInstance = null }
  if (!widgetSortable.value) return
  sortableInstance = Sortable.create(widgetSortable.value, {
    animation: 200,
    handle: '.drag-handle',
    ghostClass: 'sortable-ghost',
    dragClass: 'sortable-drag',
    onEnd: () => { sortChanged.value = true }
  })
}
async function saveSortOrder() {
  if (!widgetSortable.value) return
  sortSaving.value = true
  try {
    const ids = Array.from(widgetSortable.value.children)
      .map(el => parseInt(el.dataset.id)).filter(Boolean)
    await api.put('/admin/dashboard/widgets/sort', ids)
    ElMessage.success('순서가 저장되었습니다.')
    sortChanged.value = false
    await fetchWidgets()
  } catch { ElMessage.error('순서 저장에 실패했습니다.') }
  finally { sortSaving.value = false }
}

// ===== 위젯 CRUD =====
function onTypeChange() {
  config.value = { slides: [], links: [], content: '', size: config.value.size || 'half' }
}
function openCreate() {
  editing.value = null
  form.value = defaultForm()
  config.value = { slides: [], links: [], content: '', size: 'half' }
  showForm.value = true
}
function openEdit(w) {
  editing.value = w
  form.value = { ...w }
  const ec = parseConfig(w)
  config.value = { slides: ec.slides || [], links: ec.links || [], content: ec.content || '', size: ec.size || 'half' }
  showForm.value = true
}
async function saveWidget() {
  saving.value = true
  try {
    const extra = { size: config.value.size }
    if (form.value.widgetType === 'IMAGE_SLIDER') extra.slides  = config.value.slides
    if (form.value.widgetType === 'LINK_LIST')    extra.links   = config.value.links
    if (form.value.widgetType === 'CUSTOM')       extra.content = config.value.content
    const payload = { ...form.value, extraConfig: JSON.stringify(extra) }
    editing.value
      ? await api.put(`/admin/dashboard/widgets/${editing.value.id}`, payload)
      : await api.post('/admin/dashboard/widgets', payload)
    ElMessage.success('저장되었습니다.')
    showForm.value = false
    fetchWidgets()
  } finally { saving.value = false }
}
async function deleteWidget(id) {
  await ElMessageBox.confirm('위젯을 삭제하시겠습니까?', '삭제', { type:'warning', confirmButtonText:'삭제', cancelButtonText:'취소' })
  await api.delete(`/admin/dashboard/widgets/${id}`)
  ElMessage.success('삭제되었습니다.')
  fetchWidgets()
}

onMounted(() => { fetchWidgets(); fetchBoards() })
onBeforeUnmount(() => { if (sortableInstance) sortableInstance.destroy() })
</script>

<style scoped>
/* ===== 미리보기 그리드 ===== */
.preview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
.pc-full { grid-column: 1 / -1; }
.pc-half { grid-column: span 1; }

/* ===== 미리보기 카드 ===== */
.preview-card {
  position: relative;
  background: var(--surface);
  border: 1px solid var(--border2);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow);
  overflow: hidden;
  cursor: default;
  transition: box-shadow 0.18s ease, border-color 0.18s ease;
  min-height: 160px;
  display: flex;
  flex-direction: column;
}
.preview-card.is-inactive {
  opacity: 0.55;
  filter: grayscale(0.3);
}

/* 카드 상단 바 */
.pc-topbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 14px 8px;
  border-bottom: 0.5px solid var(--border2);
  background: var(--surface2);
}
.drag-handle {
  font-size: 16px;
  color: var(--t4);
  cursor: grab;
  transition: color 0.15s;
  flex-shrink: 0;
}
.drag-handle:active { cursor: grabbing; }
.preview-card:hover .drag-handle { color: var(--t2); }
.pc-type-badge {
  font-size: 11px;
  font-weight: 700;
  color: var(--accent-t);
  background: var(--accent-bg);
  border: 1px solid color-mix(in srgb, var(--accent) 30%, transparent);
  border-radius: 5px;
  padding: 1px 7px;
}
.pc-badges { display: flex; gap: 5px; margin-left: auto; }
.pc-size-badge {
  font-size: 10px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 4px;
}
.size-full { background: #e0f0ff; color: #1d6db5; }
.size-half { background: var(--surface); border: 1px solid var(--border); color: var(--t3); }
.pc-inactive-badge {
  font-size: 10px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 4px;
  background: #fef3cd;
  color: #b45309;
}

/* 카드 본문 */
.pc-body {
  padding: 14px 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 100px;
}
.pc-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--t1);
  letter-spacing: -0.2px;
}

/* 최신 글 미리보기 */
.pc-preview-rows { display: flex; flex-direction: column; gap: 0; }
.pc-post-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  border-bottom: 0.5px solid var(--border2);
  gap: 10px;
}
.pc-post-row:last-child { border-bottom: none; }
.pc-post-title {
  font-size: 13px;
  color: var(--t2);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pc-post-date { font-size: 11.5px; color: var(--t4); flex-shrink: 0; }

/* 슬라이더 미리보기 */
.pc-slider-preview { overflow: hidden; border-radius: 6px; }
.pc-slider-inner { display: flex; gap: 6px; }
.pc-slide-thumb {
  flex: 1;
  height: 70px;
  background: var(--surface2) center/cover no-repeat;
  border-radius: 5px;
  border: 0.5px solid var(--border2);
  display: flex;
  align-items: center;
  justify-content: center;
}
.pc-slide-label { font-size: 11px; color: var(--t3); text-align: center; padding: 4px; }

/* 링크 미리보기 */
.pc-link-grid { display: flex; flex-wrap: wrap; gap: 6px; }
.pc-link-chip {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 20px;
  background: var(--surface2);
  border: 0.5px solid var(--border);
  color: var(--t2);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 140px;
}

/* 통계 미리보기 */
.pc-stats-row { display: flex; align-items: center; gap: 0; }
.pc-stat { flex: 1; text-align: center; padding: 8px 4px; }
.pc-stat-num { font-size: 22px; font-weight: 800; color: var(--t3); line-height: 1; margin-bottom: 5px; }
.pc-stat-lbl { font-size: 11px; color: var(--t4); }
.pc-stat-div { width: 1px; height: 36px; background: var(--border2); flex-shrink: 0; }

/* 커스텀 본문 미리보기 */
.pc-custom-body {
  font-size: 13px;
  color: var(--t2);
  line-height: 1.6;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
}
.pc-custom-body :deep(p)      { margin: 0 0 4px; }
.pc-custom-body :deep(strong) { font-weight: 700; }

/* 빈 상태 힌트 */
.pc-empty-hint {
  font-size: 12px;
  color: var(--t4);
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 12px 0;
}

/* 본문 래퍼 — 오버레이 기준점 */
.pc-body-wrap {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* 수정/삭제 오버레이 (pc-body-wrap 영역만 덮음) */
.pc-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  opacity: 0;
  transition: opacity 0.18s ease;
  border-radius: 0 0 var(--radius-sm) var(--radius-sm);
}
.preview-card:hover .pc-overlay { opacity: 1; }
.pc-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 8px 18px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
  border: none;
  cursor: pointer;
  background: #fff;
  color: #222;
  transition: transform 0.12s, background 0.12s;
}
.pc-action-btn:hover { transform: translateY(-1px); background: #f5f5f5; }
.pc-action-btn.danger { background: var(--color-danger, #e53e3e); color: #fff; }
.pc-action-btn.danger:hover { background: #c53030; }

/* 크기 선택 UI */
.size-picker { display: flex; gap: 10px; }
.size-opt {
  flex: 1; display: flex; flex-direction: column; align-items: center;
  gap: 7px; padding: 10px 8px;
  border: 1.5px solid var(--border); border-radius: var(--radius-xs);
  background: var(--surface); cursor: pointer; transition: all .15s;
  font-size: 12px; color: var(--t2);
}
.size-opt:hover { border-color: var(--accent); background: var(--accent-bg); }
.size-opt.active { border-color: var(--accent); background: var(--accent-bg); color: var(--accent); font-weight: 700; }
.size-preview { display: flex; gap: 3px; width: 80px; height: 22px; }
.sp-block { height: 100%; background: var(--accent); border-radius: 3px; opacity: 0.7; }
.sp-empty { height: 100%; background: var(--border); border-radius: 3px; }
.full-preview .sp-block { flex: 1; }
.half-preview .sp-block, .half-preview .sp-empty { flex: 1; }

.sub-item {
  background: var(--surface2); border: 0.5px solid var(--border2);
  border-radius: var(--radius-xs); padding: 12px;
  display: flex; flex-direction: column; gap: 10px; margin-bottom: 8px;
}

/* 순서 저장 버튼 fade */
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s, transform 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateX(8px); }

@keyframes spin { to { transform: rotate(360deg); } }
.spinning { animation: spin 0.7s linear infinite; display: inline-block; }

@media (max-width: 768px) {
  .preview-grid { grid-template-columns: 1fr; }
  .pc-full, .pc-half { grid-column: 1; }
}
</style>

<style>
.sortable-ghost { opacity: 0.35 !important; background: var(--accent-bg) !important; border: 2px dashed var(--accent) !important; }
.sortable-drag  { opacity: 0.95 !important; box-shadow: 0 12px 40px rgba(0,0,0,0.2) !important; transform: rotate(1deg) scale(1.01) !important; }
</style>
