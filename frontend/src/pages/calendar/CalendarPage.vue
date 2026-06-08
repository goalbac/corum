<template>
  <div class="calendar-page">

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
          <button v-for="v in views" :key="v.key" class="cal-btn" :class="{ active: currentView === v.key }" @click="changeView(v.key)">{{ v.label }}</button>
        </div>
        <button v-if="canWrite" class="cal-btn add-btn" @click="openCreate">
          <i class="ti ti-plus"></i> 일정 추가
        </button>
      </div>
    </div>

    <!-- 캘린더 -->
    <div class="cal-wrap" v-loading="loading">
      <FullCalendar ref="calRef" :options="calOptions" />
    </div>

    <!-- 일정 등록/수정 다이얼로그 -->
    <el-dialog v-model="showForm" :title="editingEvent ? '일정 수정' : '일정 등록'" width="480px" destroy-on-close>
      <el-form :model="eventForm" label-position="top">
        <el-form-item label="캘린더">
          <el-select v-model="eventForm.calendarId" style="width:100%">
            <el-option v-for="c in calendars" :key="c.id" :value="c.id" :label="c.name" />
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
    <el-dialog v-model="showDetail" :title="selectedEvent?.title" width="400px">
      <div v-if="selectedEvent" class="event-detail">
        <div class="detail-row"><i class="ti ti-calendar"></i><span>{{ formatEventDate(selectedEvent) }}</span></div>
        <div v-if="selectedEvent.extendedProps?.location" class="detail-row">
          <i class="ti ti-map-pin"></i><span>{{ selectedEvent.extendedProps.location }}</span>
        </div>
        <div v-if="selectedEvent.extendedProps?.description" class="detail-row">
          <i class="ti ti-align-left"></i><span>{{ selectedEvent.extendedProps.description }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="showDetail = false">닫기</el-button>
        <el-button v-if="canWrite" type="primary" @click="editEvent">수정</el-button>
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
const canWrite   = computed(() => authStore.member?.admin)
const calRef     = ref()
const calApi     = computed(() => calRef.value?.getApi())
const loading    = ref(false)
const saving     = ref(false)
const showForm   = ref(false)
const showDetail = ref(false)
const calendars  = ref([])
const currentTitle = ref('')
const currentView  = ref('dayGridMonth')
const editingEvent = ref(null)
const selectedEvent = ref(null)

const views = [
  { key: 'dayGridMonth', label: '월' },
  { key: 'timeGridWeek', label: '주' },
  { key: 'timeGridDay',  label: '일' },
]

const eventForm = ref({
  calendarId: null, title: '', location: '', description: '',
  startAt: '', endAt: '', isAllDay: false, recurrenceType: 'NONE',
})

const calOptions = {
  plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
  initialView: 'dayGridMonth',
  locale: koLocale,
  headerToolbar: false,
  height: 'auto',
  events: fetchEvents,
  eventClick: handleEventClick,
  dateClick: handleDateClick,
  datesSet: (info) => { currentTitle.value = calApi.value?.view.title || '' },
  eventColor: '#2563EB',
  eventTextColor: '#ffffff',
}

async function fetchEvents(info, success, failure) {
  try {
    const res = await api.get('/calendars/events', {
      params: { start: info.startStr, end: info.endStr }
    })
    const events = (res.data.data || []).map(e => ({
      id: String(e.id),
      title: e.title,
      start: e.startAt,
      end: e.endAt,
      allDay: e.isAllDay,
      extendedProps: { description: e.description, location: e.location, calendarId: e.calendarId }
    }))
    success(events)
  } catch { failure() }
}

function handleEventClick(info) {
  selectedEvent.value = info.event
  showDetail.value = true
}

function handleDateClick(info) {
  if (!canWrite.value) return
  eventForm.value.startAt = info.dateStr + 'T09:00:00'
  eventForm.value.endAt   = info.dateStr + 'T10:00:00'
  editingEvent.value = null
  showForm.value = true
}

function changeView(view) {
  currentView.value = view
  calApi.value?.changeView(view)
}

function openCreate() {
  editingEvent.value = null
  eventForm.value = { calendarId: calendars.value[0]?.id || null, title: '', location: '', description: '', startAt: '', endAt: '', isAllDay: false, recurrenceType: 'NONE' }
  showForm.value = true
}

function editEvent() {
  showDetail.value = false
  const e = selectedEvent.value
  editingEvent.value = e
  eventForm.value = {
    calendarId: e.extendedProps.calendarId,
    title: e.title,
    location: e.extendedProps.location || '',
    description: e.extendedProps.description || '',
    startAt: e.startStr,
    endAt: e.endStr,
    isAllDay: e.allDay,
    recurrenceType: 'NONE',
  }
  showForm.value = true
}

async function saveEvent() {
  if (!eventForm.value.title) return ElMessage.warning('제목을 입력해주세요.')
  saving.value = true
  try {
    const payload = { ...eventForm.value }
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
  } catch {}
  currentTitle.value = calApi.value?.view.title || ''
})
</script>

<style scoped>
.calendar-page { display: flex; flex-direction: column; gap: 14px; }

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
.cal-btn.add-btn { background: var(--accent); color: #fff; border-color: var(--accent); }
.cal-btn.add-btn:hover { background: var(--accent-t); }

.cal-title { font-size: 15px; font-weight: 700; color: var(--t1); margin-left: 4px; }
.view-btns { display: flex; gap: 2px; }

.cal-wrap :deep(.fc) { font-family: inherit; }
/* 다크모드 호환 - FullCalendar 기본 흰 배경/검정 텍스트 재정의 */
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

.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }

.event-detail { display: flex; flex-direction: column; gap: 12px; }
.detail-row { display: flex; align-items: flex-start; gap: 10px; font-size: 14px; color: var(--t1); }
.detail-row i { font-size: 16px; color: var(--t3); margin-top: 2px; flex-shrink: 0; }

@media (max-width: 600px) {
  .cal-toolbar { flex-direction: column; align-items: flex-start; }
  .form-row { grid-template-columns: 1fr; }
}
</style>