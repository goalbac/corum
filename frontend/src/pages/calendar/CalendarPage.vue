<template>
  <div class="cal-layout">

    <!-- 사이드바: 캘린더 목록 -->
    <aside class="cal-sidebar">
      <div class="sidebar-section">
        <div class="sidebar-title">캘린더</div>
        <label
          v-for="cal in calendars"
          :key="cal.id"
          class="cal-check-item"
        >
          <input
            type="checkbox"
            :checked="visibleCalendars.has(cal.id)"
            @change="toggleCalendar(cal.id)"
          />
          <span class="cal-dot" :style="{ background: cal.color || '#2563EB' }"></span>
          <span class="cal-check-name">{{ cal.name }}</span>
        </label>
      </div>
      <div v-if="writableCalendars.length" class="sidebar-section">
        <button class="sidebar-add-btn" @click="openCreate">
          <i class="ti ti-plus"></i> 일정 추가
        </button>
      </div>
    </aside>

    <!-- 메인 캘린더 영역 -->
    <div class="cal-main">
      <!-- 툴바 -->
      <div class="cal-toolbar">
        <div class="cal-left">
          <button class="cal-btn" @click="calApi?.prev()"><i class="ti ti-chevron-left"></i></button>
          <button class="cal-btn" @click="calApi?.today()">오늘</button>
          <button class="cal-btn" @click="calApi?.next()"><i class="ti ti-chevron-right"></i></button>
          <span class="cal-title">{{ currentTitle }}</span>
        </div>
        <div class="cal-right">
          <div class="view-btns">
            <button v-for="v in views" :key="v.key" class="cal-btn"
              :class="{ active: currentView === v.key }" @click="changeView(v.key)">
              {{ v.label }}
            </button>
          </div>
        </div>
      </div>

      <!-- 캘린더 -->
      <div class="cal-wrap" v-loading="loading">
        <FullCalendar ref="calRef" :options="calOptions" />
      </div>
    </div>

    <!-- 일정 등록/수정 다이얼로그 -->
    <el-dialog v-model="showForm" :title="editingEvent ? '일정 수정' : '일정 등록'" width="520px" destroy-on-close>
      <el-form :model="eventForm" label-position="top">
        <el-form-item label="캘린더">
          <el-select v-model="eventForm.calendarId" style="width:100%">
            <el-option v-for="c in writableCalendars" :key="c.id" :value="c.id" :label="c.name">
              <span class="cal-option">
                <span class="cal-dot" :style="{ background: c.color || '#2563EB' }"></span>
                {{ c.name }}
              </span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="제목">
          <el-input v-model="eventForm.title" />
        </el-form-item>
        <el-form-item label="장소">
          <el-input v-model="eventForm.location" placeholder="장소 (선택)" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="eventForm.isAllDay">종일</el-checkbox>
        </el-form-item>
        <div class="form-row">
          <el-form-item label="시작">
            <el-date-picker v-model="eventForm.startAt" :type="eventForm.isAllDay ? 'date' : 'datetime'"
              style="width:100%" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DDTHH:mm:ss" />
          </el-form-item>
          <el-form-item label="종료">
            <el-date-picker v-model="eventForm.endAt" :type="eventForm.isAllDay ? 'date' : 'datetime'"
              style="width:100%" format="YYYY-MM-DD HH:mm" value-format="YYYY-MM-DDTHH:mm:ss" />
          </el-form-item>
        </div>
        <el-form-item label="반복">
          <el-select v-model="eventForm.recurrenceType" style="width:100%">
            <el-option value="NONE" label="반복 없음" />
            <el-option value="DAILY" label="매일" />
            <el-option value="WEEKLY" label="매주" />
            <el-option value="MONTHLY" label="매월" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="eventForm.recurrenceType !== 'NONE'" label="반복 종료일">
          <el-date-picker v-model="recurrenceEndDate" type="date" style="width:100%"
            format="YYYY-MM-DD" value-format="YYYY-MM-DD" placeholder="종료일 (선택)" />
        </el-form-item>
        <el-form-item label="내용">
          <el-input v-model="eventForm.description" type="textarea" :rows="3" resize="none" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button v-if="editingEvent" type="danger" @click="deleteEvent">삭제</el-button>
        <el-button @click="showForm = false">취소</el-button>
        <el-button type="primary" :loading="saving" @click="saveEvent">저장</el-button>
      </template>
    </el-dialog>

    <!-- 일정 상세 팝업 -->
    <el-dialog v-model="showDetail" width="400px" destroy-on-close>
      <template #header>
        <div class="detail-header">
          <span class="cal-dot lg" :style="{ background: selectedEventColor }"></span>
          <span>{{ selectedEvent?.title }}</span>
        </div>
      </template>
      <div v-if="selectedEvent" class="event-detail">
        <div class="detail-row"><i class="ti ti-calendar-event"></i><span>{{ selectedEventCalName }}</span></div>
        <div class="detail-row"><i class="ti ti-clock"></i><span>{{ formatEventDate(selectedEvent) }}</span></div>
        <div v-if="selectedEvent.extendedProps?.location" class="detail-row">
          <i class="ti ti-map-pin"></i><span>{{ selectedEvent.extendedProps.location }}</span>
        </div>
        <div v-if="selectedEvent.extendedProps?.recurrenceType && selectedEvent.extendedProps.recurrenceType !== 'NONE'" class="detail-row">
          <i class="ti ti-repeat"></i><span>{{ recurrenceLabel(selectedEvent.extendedProps.recurrenceType) }}</span>
        </div>
        <div v-if="selectedEvent.extendedProps?.description" class="detail-row">
          <i class="ti ti-align-left"></i><span style="white-space:pre-wrap">{{ selectedEvent.extendedProps.description }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="showDetail = false">닫기</el-button>
        <el-button v-if="canEditSelected" type="primary" @click="editEvent">수정</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import FullCalendar from '@fullcalendar/vue3'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import koLocale from '@fullcalendar/core/locales/ko'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import api from '@/api/axios'

const authStore  = useAuthStore()
const calRef     = ref()
const calApi     = computed(() => calRef.value?.getApi())
const loading    = ref(false)
const saving     = ref(false)
const showForm   = ref(false)
const showDetail = ref(false)
const calendars  = ref([])
const visibleCalendars = ref(new Set())
const currentTitle = ref('')
const currentView  = ref('dayGridMonth')
const editingEvent = ref(null)
const selectedEvent = ref(null)
const recurrenceEndDate = ref(null)

const views = [
  { key: 'dayGridMonth', label: '월' },
  { key: 'timeGridWeek', label: '주' },
  { key: 'timeGridDay',  label: '일' },
]

const eventForm = ref({
  calendarId: null, title: '', location: '', description: '',
  startAt: '', endAt: '', isAllDay: false, recurrenceType: 'NONE',
})

// 쓰기 가능한 캘린더 (권한 없는 캘린더 = 공개 쓰기, 또는 canWrite 권한 있는 캘린더)
const writableCalendars = computed(() => {
  if (!authStore.isLoggedIn) return []
  return calendars.value.filter(c => {
    if (!c.permissions || c.permissions.length === 0) return true // 공개
    return c.permissions.some(p => p.canWrite)
  })
})

const selectedEventColor = computed(() => {
  const calId = selectedEvent.value?.extendedProps?.calendarId
  return calendars.value.find(c => c.id === calId)?.color || '#2563EB'
})

const selectedEventCalName = computed(() => {
  const calId = selectedEvent.value?.extendedProps?.calendarId
  return calendars.value.find(c => c.id === calId)?.name || ''
})

const canEditSelected = computed(() => {
  const calId = selectedEvent.value?.extendedProps?.calendarId
  const cal = calendars.value.find(c => c.id === calId)
  if (!cal) return false
  if (!cal.permissions || cal.permissions.length === 0) return authStore.isLoggedIn
  return cal.permissions.some(p => p.canWrite)
})

function recurrenceLabel(type) {
  return { DAILY: '매일 반복', WEEKLY: '매주 반복', MONTHLY: '매월 반복' }[type] || ''
}

function toggleCalendar(id) {
  const s = new Set(visibleCalendars.value)
  if (s.has(id)) s.delete(id)
  else s.add(id)
  visibleCalendars.value = s
  calApi.value?.refetchEvents()
}

const calOptions = computed(() => ({
  plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
  initialView: 'dayGridMonth',
  locale: koLocale,
  headerToolbar: false,
  height: 'auto',
  events: fetchEvents,
  eventClick: handleEventClick,
  dateClick: handleDateClick,
  datesSet: () => { currentTitle.value = calApi.value?.view.title || '' },
}))

async function fetchEvents(info, success, failure) {
  try {
    const res = await api.get('/calendars/events', {
      params: { start: info.startStr, end: info.endStr }
    })
    const events = (res.data.data || [])
      .filter(e => visibleCalendars.value.has(e.calendarId))
      .map(e => {
        const cal = calendars.value.find(c => c.id === e.calendarId)
        return {
          id: String(e.id),
          title: e.title,
          start: e.startAt,
          end: e.endAt,
          allDay: e.isAllDay,
          backgroundColor: cal?.color || '#2563EB',
          borderColor: cal?.color || '#2563EB',
          textColor: '#ffffff',
          extendedProps: {
            description: e.description,
            location: e.location,
            calendarId: e.calendarId,
            recurrenceType: e.recurrenceType,
            createdBy: e.createdBy,
          }
        }
      })
    success(events)
  } catch { failure() }
}

function handleEventClick(info) {
  selectedEvent.value = info.event
  showDetail.value = true
}

function handleDateClick(info) {
  if (!writableCalendars.value.length) return
  eventForm.value = {
    calendarId: writableCalendars.value[0]?.id || null,
    title: '', location: '', description: '',
    startAt: info.dateStr + 'T09:00:00',
    endAt: info.dateStr + 'T10:00:00',
    isAllDay: false, recurrenceType: 'NONE',
  }
  recurrenceEndDate.value = null
  editingEvent.value = null
  showForm.value = true
}

function changeView(view) {
  currentView.value = view
  calApi.value?.changeView(view)
}

function openCreate() {
  editingEvent.value = null
  recurrenceEndDate.value = null
  eventForm.value = {
    calendarId: writableCalendars.value[0]?.id || null,
    title: '', location: '', description: '',
    startAt: '', endAt: '', isAllDay: false, recurrenceType: 'NONE',
  }
  showForm.value = true
}

function editEvent() {
  showDetail.value = false
  const e = selectedEvent.value
  editingEvent.value = e
  recurrenceEndDate.value = null
  eventForm.value = {
    calendarId: e.extendedProps.calendarId,
    title: e.title,
    location: e.extendedProps.location || '',
    description: e.extendedProps.description || '',
    startAt: e.startStr,
    endAt: e.endStr || '',
    isAllDay: e.allDay,
    recurrenceType: e.extendedProps.recurrenceType || 'NONE',
  }
  showForm.value = true
}

async function saveEvent() {
  if (!eventForm.value.title) return ElMessage.warning('제목을 입력해주세요.')
  saving.value = true
  try {
    const payload = {
      ...eventForm.value,
      recurrenceRule: recurrenceEndDate.value
        ? JSON.stringify({ until: recurrenceEndDate.value })
        : null,
    }
    if (editingEvent.value) {
      await api.put(`/calendars/events/${editingEvent.value.id}`, payload)
    } else {
      await api.post('/calendars/events', payload)
    }
    showForm.value = false
    calApi.value?.refetchEvents()
    ElMessage.success(editingEvent.value ? '수정되었습니다.' : '등록되었습니다.')
  } finally { saving.value = false }
}

async function deleteEvent() {
  await ElMessageBox.confirm('일정을 삭제하시겠습니까?', '삭제', { type: 'warning' })
  await api.delete(`/calendars/events/${editingEvent.value.id}`)
  showForm.value = false
  calApi.value?.refetchEvents()
  ElMessage.success('삭제되었습니다.')
}

function formatEventDate(event) {
  if (!event) return ''
  const s = new Date(event.startStr)
  const e = event.endStr ? new Date(event.endStr) : null
  const fmt = d => `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')}`
  if (event.allDay) return e ? `${fmt(s)} ~ ${fmt(e)}` : fmt(s)
  const timeFmt = d => `${fmt(d)} ${d.toTimeString().slice(0,5)}`
  return e ? `${timeFmt(s)} ~ ${timeFmt(e)}` : timeFmt(s)
}

onMounted(async () => {
  try {
    const res = await api.get('/calendars')
    calendars.value = res.data.data || []
    visibleCalendars.value = new Set(calendars.value.map(c => c.id))
  } catch {}
  currentTitle.value = calApi.value?.view.title || ''
})
</script>

<style scoped>
.cal-layout {
  display: flex;
  gap: 20px;
  min-height: 0;
}

/* ===== 사이드바 ===== */
.cal-sidebar {
  width: 200px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.sidebar-section { display: flex; flex-direction: column; gap: 4px; }
.sidebar-title {
  font-size: 11px;
  font-weight: 600;
  color: var(--t3);
  text-transform: uppercase;
  letter-spacing: .05em;
  padding: 4px 0;
  border-bottom: 1px solid var(--border);
  margin-bottom: 6px;
}
.cal-check-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 6px;
  border-radius: var(--radius-xs);
  cursor: pointer;
  font-size: 13px;
  color: var(--t2);
  transition: var(--transition);
}
.cal-check-item:hover { background: var(--surface2); color: var(--t1); }
.cal-check-item input { accent-color: var(--accent); cursor: pointer; }
.cal-check-name { flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.sidebar-add-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: var(--accent);
  color: #fff;
  border: none;
  border-radius: var(--radius-xs);
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
  width: 100%;
  justify-content: center;
  transition: var(--transition);
}
.sidebar-add-btn:hover { background: var(--accent-t); }

/* ===== 메인 ===== */
.cal-main { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 12px; }

.cal-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}
.cal-left, .cal-right { display: flex; align-items: center; gap: 6px; }

.cal-btn {
  padding: 6px 12px;
  border: 0.5px solid var(--border);
  background: var(--surface2);
  border-radius: var(--radius-xs);
  font-size: 13px;
  color: var(--t2);
  cursor: pointer;
  font-family: inherit;
  transition: var(--transition);
  display: flex;
  align-items: center;
  gap: 4px;
}
.cal-btn:hover { color: var(--t1); background: var(--surface); }
.cal-btn.active { background: var(--accent-bg); color: var(--accent); border-color: var(--accent); font-weight: 500; }

.cal-title { font-size: 15px; font-weight: 700; color: var(--t1); margin-left: 4px; }
.view-btns { display: flex; gap: 2px; }

/* ===== FullCalendar 다크모드 ===== */
.cal-wrap :deep(.fc) { font-family: inherit; }
.cal-wrap :deep(.fc-theme-standard td),
.cal-wrap :deep(.fc-theme-standard th),
.cal-wrap :deep(.fc-theme-standard .fc-scrollgrid),
.cal-wrap :deep(.fc-theme-standard .fc-scrollgrid-section > td) {
  border-color: var(--border) !important;
}
.cal-wrap :deep(.fc-daygrid-day) { background: var(--surface) !important; }
.cal-wrap :deep(.fc-day-other) { background: var(--surface2) !important; }
.cal-wrap :deep(.fc-day-today) { background: var(--accent-bg) !important; }
.cal-wrap :deep(.fc-col-header-cell) {
  background: var(--surface2) !important;
  font-size: 13px; font-weight: 500; color: var(--t2); padding: 8px 0;
}
.cal-wrap :deep(.fc-col-header-cell a) { color: var(--t2) !important; }
.cal-wrap :deep(.fc-daygrid-day-number) { font-size: 13px; color: var(--t2); }
.cal-wrap :deep(.fc-daygrid-day-number:hover) { color: var(--accent); }
.cal-wrap :deep(.fc-event) { border-radius: 4px; border: none; font-size: 12px; cursor: pointer; }
.cal-wrap :deep(.fc-toolbar) { display: none; }
.cal-wrap :deep(.fc-more-link) { color: var(--accent-t) !important; }

/* ===== 공통 ===== */
.cal-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
.cal-dot.lg { width: 14px; height: 14px; }

.cal-option { display: flex; align-items: center; gap: 8px; }

.detail-header { display: flex; align-items: center; gap: 10px; font-size: 15px; font-weight: 600; color: var(--t1); }
.event-detail { display: flex; flex-direction: column; gap: 12px; }
.detail-row { display: flex; align-items: flex-start; gap: 10px; font-size: 14px; color: var(--t1); }
.detail-row i { font-size: 16px; color: var(--t3); margin-top: 2px; flex-shrink: 0; }

.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }

@media (max-width: 768px) {
  .cal-layout { flex-direction: column; }
  .cal-sidebar { width: 100%; flex-direction: row; flex-wrap: wrap; }
  .sidebar-section { min-width: 140px; }
  .form-row { grid-template-columns: 1fr; }
}
</style>
