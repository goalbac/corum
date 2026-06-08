<template>
  <div class="calendar-page">
    <div class="cal-toolbar">
      <div class="cal-left">
        <button class="cal-btn icon-btn" type="button" @click="calApi?.prev()">
          <i class="ti ti-chevron-left"></i>
        </button>
        <button class="cal-btn" type="button" @click="calApi?.today()">오늘</button>
        <button class="cal-btn icon-btn" type="button" @click="calApi?.next()">
          <i class="ti ti-chevron-right"></i>
        </button>
        <span class="cal-title">{{ currentTitle }}</span>
      </div>
      <div class="cal-right">
        <div class="view-btns">
          <button
            v-for="v in views"
            :key="v.key"
            class="cal-btn"
            :class="{ active: currentView === v.key }"
            type="button"
            @click="changeView(v.key)"
          >
            {{ v.label }}
          </button>
        </div>
        <button v-if="isAdmin" class="cal-btn" type="button" @click="openPermissionDialog">
          <i class="ti ti-lock-cog"></i> 권한
        </button>
        <button v-if="canWrite" class="cal-btn add-btn" type="button" @click="openCreate">
          <i class="ti ti-plus"></i> 일정 추가
        </button>
      </div>
    </div>

    <div class="calendar-strip">
      <span v-for="calendar in calendars" :key="calendar.id" class="calendar-badge">
        <i :style="{ backgroundColor: calendar.color || fallbackColor }"></i>
        {{ calendar.name }}
        <small v-if="calendar.canWrite">쓰기</small>
      </span>
    </div>

    <div class="cal-wrap" v-loading="loading">
      <FullCalendar ref="calRef" :options="calOptions" />
    </div>

    <el-dialog v-model="showForm" :title="editingEvent ? '일정 수정' : '일정 등록'" width="520px" destroy-on-close>
      <el-form :model="eventForm" label-position="top">
        <el-form-item label="캘린더">
          <el-select v-model="eventForm.calendarId" style="width:100%">
            <el-option
              v-for="c in writableCalendars"
              :key="c.id"
              :value="c.id"
              :label="c.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="제목">
          <el-input v-model="eventForm.title" />
        </el-form-item>
        <el-form-item label="장소">
          <el-input v-model="eventForm.location" placeholder="장소 입력" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="eventForm.isAllDay">종일</el-checkbox>
        </el-form-item>
        <div class="form-row">
          <el-form-item label="시작">
            <el-date-picker
              v-model="eventForm.startAt"
              :type="eventForm.isAllDay ? 'date' : 'datetime'"
              style="width:100%"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
            />
          </el-form-item>
          <el-form-item label="종료">
            <el-date-picker
              v-model="eventForm.endAt"
              :type="eventForm.isAllDay ? 'date' : 'datetime'"
              style="width:100%"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
            />
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

    <el-dialog v-model="showDetail" :title="selectedEvent?.title" width="420px">
      <div v-if="selectedEvent" class="event-detail">
        <div class="detail-row"><i class="ti ti-calendar"></i><span>{{ formatEventDate(selectedEvent) }}</span></div>
        <div v-if="selectedEvent.extendedProps?.calendarName" class="detail-row">
          <i class="ti ti-color-swatch"></i><span>{{ selectedEvent.extendedProps.calendarName }}</span>
        </div>
        <div v-if="selectedEvent.extendedProps?.recurrenceType !== 'NONE'" class="detail-row">
          <i class="ti ti-repeat"></i><span>{{ recurrenceLabel(selectedEvent.extendedProps.recurrenceType) }}</span>
        </div>
        <div v-if="selectedEvent.extendedProps?.location" class="detail-row">
          <i class="ti ti-map-pin"></i><span>{{ selectedEvent.extendedProps.location }}</span>
        </div>
        <div v-if="selectedEvent.extendedProps?.description" class="detail-row">
          <i class="ti ti-align-left"></i><span>{{ selectedEvent.extendedProps.description }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="showDetail = false">닫기</el-button>
        <el-button v-if="selectedCanWrite" type="primary" @click="editEvent">수정</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showPermissions" title="캘린더 권한" width="680px">
      <div class="permission-head">
        <el-select v-model="permissionCalendarId" style="width:260px" @change="fetchPermissions">
          <el-option v-for="calendar in calendars" :key="calendar.id" :value="calendar.id" :label="calendar.name" />
        </el-select>
      </div>
      <el-table :data="permissionRows" style="width:100%">
        <el-table-column label="그룹" min-width="180">
          <template #default="{ row }">{{ row.name }}</template>
        </el-table-column>
        <el-table-column label="읽기" width="110" align="center">
          <template #default="{ row }"><el-switch v-model="row.canRead" /></template>
        </el-table-column>
        <el-table-column label="쓰기" width="110" align="center">
          <template #default="{ row }"><el-switch v-model="row.canWrite" /></template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="showPermissions = false">닫기</el-button>
        <el-button type="primary" @click="savePermissions">저장</el-button>
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

const authStore = useAuthStore()
const isAdmin = computed(() => authStore.member?.admin || authStore.member?.isAdmin)
const fallbackColor = '#2563EB'
const calRef = ref()
const calApi = computed(() => calRef.value?.getApi())
const loading = ref(false)
const saving = ref(false)
const showForm = ref(false)
const showDetail = ref(false)
const showPermissions = ref(false)
const calendars = ref([])
const groups = ref([])
const permissionRows = ref([])
const permissionCalendarId = ref(null)
const currentTitle = ref('')
const currentView = ref('dayGridMonth')
const editingEvent = ref(null)
const selectedEvent = ref(null)

const writableCalendars = computed(() => calendars.value.filter(c => c.canWrite))
const canWrite = computed(() => writableCalendars.value.length > 0)
const selectedCanWrite = computed(() => {
  const calendarId = selectedEvent.value?.extendedProps?.calendarId
  return calendars.value.find(c => c.id === calendarId)?.canWrite || false
})

const views = [
  { key: 'dayGridMonth', label: '월' },
  { key: 'timeGridWeek', label: '주' },
  { key: 'timeGridDay', label: '일' },
]

const eventForm = ref(defaultEventForm())

const calOptions = {
  plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
  initialView: 'dayGridMonth',
  locale: koLocale,
  headerToolbar: false,
  height: 'auto',
  events: fetchEvents,
  eventClick: handleEventClick,
  dateClick: handleDateClick,
  datesSet: () => { currentTitle.value = calApi.value?.view.title || '' },
  eventTextColor: '#ffffff',
}

function defaultEventForm(dateStr = '') {
  return {
    calendarId: writableCalendars.value[0]?.id || null,
    title: '',
    location: '',
    description: '',
    startAt: dateStr ? `${dateStr}T09:00:00` : '',
    endAt: dateStr ? `${dateStr}T10:00:00` : '',
    isAllDay: false,
    recurrenceType: 'NONE',
    recurrenceRule: '',
  }
}

async function fetchCalendars() {
  const res = await api.get('/calendars')
  calendars.value = res.data.data || []
}

async function fetchEvents(info, success, failure) {
  loading.value = true
  try {
    const res = await api.get('/calendars/events', {
      params: { start: info.startStr, end: info.endStr }
    })
    const events = (res.data.data || []).map(e => {
      const calendar = calendars.value.find(c => c.id === e.calendarId)
      return {
        id: `${e.id}:${e.startAt}`,
        title: e.title,
        start: e.startAt,
        end: e.endAt,
        allDay: e.isAllDay,
        backgroundColor: calendar?.color || fallbackColor,
        borderColor: calendar?.color || fallbackColor,
        extendedProps: {
          eventId: e.originalEventId || e.id,
          description: e.description,
          location: e.location,
          calendarId: e.calendarId,
          calendarName: calendar?.name,
          recurrenceType: e.recurrenceType || 'NONE',
        }
      }
    })
    success(events)
  } catch (error) {
    failure(error)
  } finally {
    loading.value = false
  }
}

function handleEventClick(info) {
  selectedEvent.value = info.event
  showDetail.value = true
}

function handleDateClick(info) {
  if (!canWrite.value) return
  eventForm.value = defaultEventForm(info.dateStr)
  editingEvent.value = null
  showForm.value = true
}

function changeView(view) {
  currentView.value = view
  calApi.value?.changeView(view)
}

function openCreate() {
  editingEvent.value = null
  eventForm.value = defaultEventForm()
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
    startAt: toLocalDateTime(e.start),
    endAt: e.end ? toLocalDateTime(e.end) : '',
    isAllDay: e.allDay,
    recurrenceType: e.extendedProps.recurrenceType || 'NONE',
    recurrenceRule: '',
  }
  showForm.value = true
}

async function saveEvent() {
  if (!eventForm.value.calendarId) return ElMessage.warning('캘린더를 선택해주세요.')
  if (!eventForm.value.title) return ElMessage.warning('제목을 입력해주세요.')
  saving.value = true
  try {
    const payload = { ...eventForm.value }
    if (editingEvent.value) {
      await api.put(`/calendars/events/${editingEvent.value.extendedProps.eventId}`, payload)
    } else {
      await api.post('/calendars/events', payload)
    }
    showForm.value = false
    calApi.value?.refetchEvents()
    ElMessage.success(editingEvent.value ? '수정되었습니다.' : '등록되었습니다.')
  } finally {
    saving.value = false
  }
}

async function deleteEvent() {
  await ElMessageBox.confirm('일정을 삭제할까요?', '삭제', { type: 'warning' })
  await api.delete(`/calendars/events/${editingEvent.value.extendedProps.eventId}`)
  showForm.value = false
  calApi.value?.refetchEvents()
  ElMessage.success('삭제되었습니다.')
}

async function openPermissionDialog() {
  if (!calendars.value.length) return
  if (!groups.value.length) await fetchGroups()
  permissionCalendarId.value = permissionCalendarId.value || calendars.value[0].id
  await fetchPermissions()
  showPermissions.value = true
}

async function fetchGroups() {
  const res = await api.get('/groups')
  groups.value = flattenGroups(res.data.data || [])
}

async function fetchPermissions() {
  if (!permissionCalendarId.value) return
  const res = await api.get(`/calendars/${permissionCalendarId.value}/permissions`)
  const saved = res.data.data || []
  permissionRows.value = groups.value.map(group => {
    const permission = saved.find(p => p.groupId === group.id)
    return {
      id: group.id,
      name: group.name,
      canRead: permission?.canRead ?? false,
      canWrite: permission?.canWrite ?? false,
    }
  })
}

async function savePermissions() {
  await api.put(`/calendars/${permissionCalendarId.value}/permissions`, {
    permissions: permissionRows.value.map(row => ({
      groupId: row.id,
      canRead: row.canRead,
      canWrite: row.canWrite,
    }))
  })
  await fetchCalendars()
  calApi.value?.refetchEvents()
  ElMessage.success('권한이 저장되었습니다.')
}

function flattenGroups(items, depth = 0) {
  return items.flatMap(item => [
    { id: item.id, name: `${'　'.repeat(depth)}${item.name}` },
    ...flattenGroups(item.children || [], depth + 1)
  ])
}

function recurrenceLabel(value) {
  return ({ DAILY: '매일 반복', WEEKLY: '매주 반복', MONTHLY: '매월 반복' })[value] || '반복 없음'
}

function formatEventDate(event) {
  if (!event) return ''
  const s = event.start
  const e = event.end
  const fmt = d => `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')}`
  if (event.allDay) return e ? `${fmt(s)} ~ ${fmt(e)}` : fmt(s)
  const timeFmt = d => `${fmt(d)} ${d.toTimeString().slice(0,5)}`
  return e ? `${timeFmt(s)} ~ ${timeFmt(e)}` : timeFmt(s)
}

function toLocalDateTime(value) {
  if (!value) return ''
  const date = value instanceof Date ? value : new Date(value)
  const pad = n => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:00`
}

onMounted(async () => {
  await fetchCalendars()
  currentTitle.value = calApi.value?.view.title || ''
})
</script>

<style scoped>
.calendar-page { display: flex; flex-direction: column; gap: 14px; color: var(--t1); }
.cal-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}
.cal-left, .cal-right { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.cal-btn {
  min-height: 34px;
  padding: 6px 12px;
  border: 0.5px solid var(--border);
  background: var(--surface2);
  border-radius: var(--radius-xs);
  font-size: 14px;
  color: var(--t2);
  cursor: pointer;
  font-family: inherit;
  transition: var(--transition);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.icon-btn { width: 34px; padding: 0; }
.cal-btn:hover { color: var(--t1); background: var(--surface); }
.cal-btn.active { background: var(--accent-bg); color: var(--accent-t); border-color: var(--accent); font-weight: 700; }
.cal-btn.add-btn { background: var(--accent); color: #fff; border-color: var(--accent); }
.cal-btn.add-btn:hover { background: var(--accent-t); }
.cal-title { font-size: 16px; font-weight: 800; color: var(--t1); margin-left: 4px; }
.view-btns { display: flex; gap: 2px; }
.calendar-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 12px;
  background: var(--surface);
  border: 0.5px solid var(--border2);
  border-radius: var(--radius-sm);
}
.calendar-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--t1);
}
.calendar-badge i {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}
.calendar-badge small { color: var(--accent-t); font-size: 12px; }
.cal-wrap :deep(.fc) { font-family: inherit; color: var(--t1); }
.cal-wrap :deep(.fc-theme-standard td),
.cal-wrap :deep(.fc-theme-standard th),
.cal-wrap :deep(.fc-theme-standard .fc-scrollgrid) { border-color: var(--border2); }
.cal-wrap :deep(.fc-day-today) { background: var(--accent-bg) !important; }
.cal-wrap :deep(.fc-col-header-cell) { font-size: 14px; font-weight: 700; color: var(--t2); padding: 8px 0; }
.cal-wrap :deep(.fc-daygrid-day-number) { font-size: 14px; color: var(--t2); }
.cal-wrap :deep(.fc-event) { border-radius: 4px; border: none; font-size: 13px; cursor: pointer; }
.cal-wrap :deep(.fc-toolbar) { display: none; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.event-detail { display: flex; flex-direction: column; gap: 12px; }
.detail-row { display: flex; align-items: flex-start; gap: 10px; font-size: 15px; color: var(--t1); }
.detail-row i { font-size: 16px; color: var(--t3); margin-top: 2px; flex-shrink: 0; }
.permission-head { margin-bottom: 12px; }
@media (max-width: 600px) {
  .cal-toolbar { flex-direction: column; align-items: flex-start; }
  .form-row { grid-template-columns: 1fr; }
}
</style>
