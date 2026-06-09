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

    <!-- 카드형 목록 (단일 컬럼 → SortableJS 안정 동작) -->
    <div v-if="!loading && widgets.length" ref="widgetSortable" class="widget-list">
      <div
        v-for="w in widgets"
        :key="w.id"
        :data-id="w.id"
        :class="['wcard', { 'is-inactive': !w.isActive }]"
      >
        <!-- 상단 바: 드래그 핸들 + 메타 -->
        <div class="wcard-top">
          <i class="ti ti-grip-vertical drag-handle" title="드래그해서 순서 변경"></i>

          <span class="wtype-badge">{{ typeLabel(w.widgetType) }}</span>
          <span class="wtitle">{{ w.title || '(제목 없음)' }}</span>

          <div class="wcard-meta">
            <!-- 크기 인디케이터 -->
            <span
              v-if="!['IMAGE_SLIDER','MEMBER_STATS','VISIT_STATS'].includes(w.widgetType)"
              class="wsize-indicator"
              :title="getSize(w) === 'full' ? '1칸 (전체 너비)' : '반칸 (절반 너비)'"
            >
              <span :class="['wsize-block', 'block-l', getSize(w) === 'full' ? 'active' : '']"></span>
              <span :class="['wsize-block', 'block-r', getSize(w) === 'full' ? 'active' : '']"></span>
              <span class="wsize-label">{{ getSize(w) === 'full' ? '1칸' : '반칸' }}</span>
            </span>
            <span v-if="!w.isActive" class="wbadge-inactive">비활성</span>
          </div>

          <!-- 수정/삭제 버튼 -->
          <div class="wcard-actions">
            <button class="act-btn" @click="openEdit(w)"><i class="ti ti-edit"></i> 수정</button>
            <button class="act-btn danger" @click="deleteWidget(w.id)"><i class="ti ti-trash"></i></button>
          </div>
        </div>

        <!-- 본문 미리보기 -->
        <div class="wcard-preview">
          <!-- RECENT_POSTS -->
          <template v-if="w.widgetType === 'RECENT_POSTS'">
            <div class="wp-post-list">
              <div v-for="post in (w.posts || []).slice(0,4)" :key="post.id" class="wp-post-row">
                <span class="wp-post-title">{{ post.title }}</span>
                <span class="wp-post-date">{{ formatDate(post.createdAt) }}</span>
              </div>
              <div v-if="!(w.posts||[]).length" class="wp-empty">
                <i class="ti ti-file-text"></i>
                {{ w.targetBoardName ? w.targetBoardName + ' 게시판' : '전체 게시판' }} 최신 글 표시
              </div>
            </div>
          </template>

          <!-- IMAGE_SLIDER -->
          <template v-else-if="w.widgetType === 'IMAGE_SLIDER'">
            <div class="wp-slides">
              <template v-if="parseConfig(w).slides?.length">
                <div
                  v-for="(s,i) in parseConfig(w).slides.slice(0,4)"
                  :key="i"
                  class="wp-slide"
                  :style="s.imageUrl ? `background-image:url(${s.imageUrl})` : ''"
                >
                  <span v-if="!s.imageUrl">{{ s.title || `슬라이드 ${i+1}` }}</span>
                </div>
              </template>
              <div v-else class="wp-empty"><i class="ti ti-photo"></i> 슬라이드 없음</div>
            </div>
          </template>

          <!-- LINK_LIST -->
          <template v-else-if="w.widgetType === 'LINK_LIST'">
            <div class="wp-links">
              <template v-if="parseConfig(w).links?.length">
                <span v-for="(l,i) in parseConfig(w).links.slice(0,8)" :key="i" class="wp-link-chip">
                  {{ l.label || l.url }}
                </span>
              </template>
              <div v-else class="wp-empty"><i class="ti ti-link"></i> 링크 없음</div>
            </div>
          </template>

          <!-- MEMBER_STATS / VISIT_STATS -->
          <template v-else-if="w.widgetType === 'MEMBER_STATS' || w.widgetType === 'VISIT_STATS'">
            <div class="wp-stats">
              <div class="wp-stat"><span class="wp-stat-n">—</span><span class="wp-stat-l">전체 회원</span></div>
              <div class="wp-stat"><span class="wp-stat-n">—</span><span class="wp-stat-l">게시판</span></div>
              <div class="wp-stat"><span class="wp-stat-n">—</span><span class="wp-stat-l">문의</span></div>
              <div class="wp-stat"><span class="wp-stat-n">—</span><span class="wp-stat-l">오늘 방문</span></div>
            </div>
          </template>

          <!-- CUSTOM -->
          <template v-else-if="w.widgetType === 'CUSTOM'">
            <div class="wp-custom" v-html="parseConfig(w).content || ''" />
            <div v-if="!parseConfig(w).content" class="wp-empty"><i class="ti ti-pencil"></i> 본문 없음</div>
          </template>
        </div>
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

        <!-- 크기 선택 -->
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
            <div style="padding-top:6px"><label class="chk-item"><el-checkbox v-model="form.isActive" />활성화</label></div>
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

function parseConfig(w) {
  try { return w.extraConfig ? (typeof w.extraConfig === 'string' ? JSON.parse(w.extraConfig) : w.extraConfig) : {} }
  catch { return {} }
}
function getSize(w) { return parseConfig(w).size || 'half' }
function typeLabel(t) {
  return { RECENT_POSTS:'최신 글', IMAGE_SLIDER:'슬라이더', LINK_LIST:'링크', MEMBER_STATS:'회원 현황', VISIT_STATS:'접속 통계', CUSTOM:'커스텀' }[t] || t
}
function formatDate(d) {
  if (!d) return ''
  const dt = new Date(d), now = new Date()
  if (dt.toDateString() === now.toDateString()) return dt.toTimeString().slice(0, 5)
  return `${dt.getMonth()+1}.${String(dt.getDate()).padStart(2,'0')}`
}

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

function onTypeChange() {
  config.value = { slides: [], links: [], content: '', size: config.value.size || 'half' }
}
function openCreate() {
  editing.value = null; form.value = defaultForm()
  config.value = { slides: [], links: [], content: '', size: 'half' }
  showForm.value = true
}
function openEdit(w) {
  editing.value = w; form.value = { ...w }
  const ec = parseConfig(w)
  config.value = { slides: ec.slides||[], links: ec.links||[], content: ec.content||'', size: ec.size||'half' }
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
    showForm.value = false; fetchWidgets()
  } finally { saving.value = false }
}
async function deleteWidget(id) {
  await ElMessageBox.confirm('위젯을 삭제하시겠습니까?', '삭제', { type:'warning', confirmButtonText:'삭제', cancelButtonText:'취소' })
  await api.delete(`/admin/dashboard/widgets/${id}`)
  ElMessage.success('삭제되었습니다.'); fetchWidgets()
}

onMounted(() => { fetchWidgets(); fetchBoards() })
onBeforeUnmount(() => { if (sortableInstance) sortableInstance.destroy() })
</script>

<style scoped>
/* ===== 위젯 리스트 ===== */
.widget-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* ===== 위젯 카드 ===== */
.wcard {
  background: var(--surface);
  border: 1px solid var(--border2);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow);
  overflow: hidden;
  user-select: none;
  transition: box-shadow 0.15s ease;
}
.wcard:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
.wcard.is-inactive { opacity: 0.6; }

/* ===== 상단 바 ===== */
.wcard-top {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: var(--surface2);
  border-bottom: 0.5px solid var(--border2);
  cursor: default;
}
.drag-handle {
  font-size: 18px;
  color: var(--t4);
  cursor: grab;
  flex-shrink: 0;
  transition: color 0.15s;
  padding: 2px;
}
.drag-handle:active { cursor: grabbing; }
.wcard:hover .drag-handle { color: var(--accent-t); }

.wtype-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 5px;
  background: var(--accent-bg);
  color: var(--accent-t);
  border: 1px solid color-mix(in srgb, var(--accent) 30%, transparent);
  flex-shrink: 0;
}
.wtitle {
  font-size: 14px;
  font-weight: 700;
  color: var(--t1);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.wcard-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

/* 크기 인디케이터 */
.wsize-indicator {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 3px 8px 3px 5px;
  border: 1px solid var(--border);
  border-radius: 5px;
  background: var(--surface);
}
.wsize-block {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 2px;
  background: var(--border);
  transition: background 0.15s;
}
.wsize-block.active { background: var(--accent); }
.wsize-label {
  font-size: 11px;
  font-weight: 700;
  color: var(--t3);
  margin-left: 3px;
}
.wbadge-inactive {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  background: #fef3cd;
  color: #b45309;
}

.wcard-actions {
  display: flex;
  gap: 5px;
  flex-shrink: 0;
}

/* ===== 미리보기 본문 ===== */
.wcard-preview {
  padding: 12px 16px;
}

/* 최신 글 */
.wp-post-list { display: flex; flex-direction: column; }
.wp-post-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 5px 0;
  border-bottom: 0.5px solid var(--border2);
  gap: 12px;
}
.wp-post-row:last-child { border-bottom: none; }
.wp-post-title { font-size: 13px; color: var(--t2); flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.wp-post-date  { font-size: 12px; color: var(--t4); flex-shrink: 0; }

/* 슬라이더 */
.wp-slides { display: flex; gap: 8px; }
.wp-slide {
  flex: 1;
  height: 60px;
  border-radius: 6px;
  background: var(--surface2) center/cover no-repeat;
  border: 0.5px solid var(--border2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: var(--t3);
}

/* 링크 */
.wp-links { display: flex; flex-wrap: wrap; gap: 5px; }
.wp-link-chip {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 20px;
  background: var(--surface2);
  border: 0.5px solid var(--border);
  color: var(--t2);
  white-space: nowrap;
}

/* 통계 */
.wp-stats { display: flex; gap: 0; }
.wp-stat { flex: 1; text-align: center; padding: 8px; }
.wp-stat-n { display: block; font-size: 20px; font-weight: 800; color: var(--t4); line-height: 1; margin-bottom: 4px; }
.wp-stat-l { font-size: 11px; color: var(--t4); }

/* 커스텀 */
.wp-custom {
  font-size: 13px; color: var(--t2); line-height: 1.6;
  overflow: hidden; display: -webkit-box;
  -webkit-line-clamp: 3; -webkit-box-orient: vertical;
}
.wp-custom :deep(p) { margin: 0 0 4px; }

/* 빈 상태 */
.wp-empty {
  display: flex; align-items: center; gap: 6px;
  font-size: 13px; color: var(--t4); padding: 6px 0;
}

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
</style>

<style>
.sortable-ghost { opacity: 0.35 !important; background: var(--accent-bg) !important; border: 2px dashed var(--accent) !important; }
.sortable-drag  { opacity: 0.96 !important; box-shadow: 0 8px 32px rgba(0,0,0,0.18) !important; }
</style>
