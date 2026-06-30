<template>
  <div class="cal-layout">

    <!-- 사이드바 (모바일 제외, 단일 캘린더 메뉴 제외) -->
    <div v-if="!isSingleCalendar && normalCalendars.length > 0" class="cal-sidebar">
      <div class="sidebar-section">
        <div class="sidebar-label">내 캘린더</div>
        <div class="sidebar-cal-list">
          <label
            v-for="cal in normalCalendars"
            :key="cal.id"
            class="sidebar-cal-item"
            @click.prevent="toggleCalendar(cal.id)"
          >
            <span
              class="sidebar-check"
              :style="{
                borderColor: cal.color || '#2563EB',
                background: visibleCalendars.has(cal.id) ? (cal.color || '#2563EB') : 'transparent'
              }"
            >
              <svg v-if="visibleCalendars.has(cal.id)" width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
            </span>
            <span class="sidebar-cal-name" :style="{ color: visibleCalendars.has(cal.id) ? 'var(--t1)' : 'var(--t3)' }">{{ cal.name }}</span>
          </label>
        </div>
        <template v-if="holidayCalendars.length">
          <div class="sidebar-divider"></div>
          <label
            v-for="cal in holidayCalendars"
            :key="cal.id"
            class="sidebar-cal-item"
            @click.prevent="showHolidayCalendar = !showHolidayCalendar; calApi?.refetchEvents()"
          >
            <span
              class="sidebar-check"
              :style="{
                borderColor: cal.color || '#dc2626',
                background: showHolidayCalendar ? (cal.color || '#dc2626') : 'transparent'
              }"
            >
              <svg v-if="showHolidayCalendar" width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
            </span>
            <span class="sidebar-cal-name" :style="{ color: showHolidayCalendar ? 'var(--t1)' : 'var(--t3)' }">{{ cal.name }}</span>
          </label>
        </template>
      </div>
    </div>

    <!-- 메인 캘린더 영역 -->
    <div class="cal-main">
      <!-- 툴바 -->
      <div class="cal-toolbar">
        <div class="cal-left">
          <h2 class="cal-title">{{ currentTitle }}</h2>
          <div class="cal-nav-group">
            <button class="cal-nav-btn" @click="calApi?.prev()">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
            </button>
            <button class="cal-nav-btn next" @click="calApi?.next()">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
            </button>
          </div>
          <button class="cal-today-btn" @click="calApi?.today()">오늘</button>
        </div>
        <div class="cal-right">
          <!-- 모바일 전용 캘린더 필터 드롭다운 -->
          <div v-if="isMobile && !isSingleCalendar" class="cal-filter-wrap" ref="filterWrap">
            <button class="cal-today-btn" @click="showFilter = !showFilter">
              <i class="ti ti-filter"></i>
              <span v-if="visibleCalendars.size + (showHolidayCalendar && holidayCalendars.length ? 1 : 0) < normalCalendars.length + holidayCalendars.length" class="filter-badge">{{ visibleCalendars.size + (showHolidayCalendar && holidayCalendars.length ? 1 : 0) }}</span>
            </button>
            <div v-if="showFilter" class="cal-filter-panel">
              <div class="filter-title">표시할 캘린더</div>
              <label v-for="cal in normalCalendars" :key="cal.id" class="cal-check-item">
                <input type="checkbox" :checked="visibleCalendars.has(cal.id)" @change="toggleCalendar(cal.id)" />
                <span class="cal-dot" :style="{ background: cal.color || '#2563EB' }"></span>
                <span class="cal-check-name">{{ cal.name }}</span>
              </label>
              <template v-if="holidayCalendars.length">
                <div class="filter-divider"></div>
                <label v-for="cal in holidayCalendars" :key="cal.id" class="cal-check-item">
                  <input type="checkbox" v-model="showHolidayCalendar" @change="calApi?.refetchEvents()" />
                  <span class="cal-dot" :style="{ background: cal.color || '#dc2626' }"></span>
                  <span class="cal-check-name">{{ cal.name }}</span>
                </label>
              </template>
              <div class="filter-actions">
                <button class="filter-link" @click="selectAll">전체 선택</button>
                <button class="filter-link" @click="deselectAll">전체 해제</button>
              </div>
            </div>
          </div>
          <!-- 뷰 탭 (segmented control) -->
          <div class="view-tabs">
            <button
              v-for="v in views"
              :key="v.key"
              class="view-tab-btn"
              :class="{ active: currentView === v.key }"
              @click="changeView(v.key)"
            >{{ v.label }}</button>
          </div>
          <!-- 일정 등록 버튼 -->
          <button v-if="writableCalendars.length" class="cal-add-btn" @click="openCreate">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            <span class="btn-text">일정 등록</span>
          </button>
        </div>
      </div>

      <!-- 캘린더 카드 -->
      <div class="cal-card" v-loading="loading">
        <FullCalendar ref="calRef" :options="calOptions" />
      </div>
    </div>

    <!-- 일정 등록/수정 다이얼로그 -->
    <el-dialog v-model="showForm" :title="editingEvent ? '일정 수정' : '일정 등록'"
      :width="isMobile ? '95vw' : '520px'"
      :style="isMobile ? 'max-height:90dvh;overflow-y:auto' : ''"
      destroy-on-close>
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
              style="width:100%"
              :format="eventForm.isAllDay ? 'YYYY-MM-DD' : 'YYYY-MM-DD HH:mm'"
              :value-format="eventForm.isAllDay ? 'YYYY-MM-DD' : 'YYYY-MM-DDTHH:mm:ss'" />
          </el-form-item>
          <el-form-item label="종료">
            <el-date-picker v-model="eventForm.endAt" :type="eventForm.isAllDay ? 'date' : 'datetime'"
              style="width:100%"
              :format="eventForm.isAllDay ? 'YYYY-MM-DD' : 'YYYY-MM-DD HH:mm'"
              :value-format="eventForm.isAllDay ? 'YYYY-MM-DD' : 'YYYY-MM-DDTHH:mm:ss'" />
          </el-form-item>
        </div>
        <!-- 반복 설정 -->
        <el-form-item label="반복">
          <el-select v-model="eventForm.recurrenceType" style="width:100%" @change="onRecurrenceTypeChange">
            <el-option value="NONE" label="반복 없음" />
            <el-option value="DAILY" label="매일" />
            <el-option value="WEEKLY" label="매주" />
            <el-option value="MONTHLY" label="매월" />
            <el-option value="YEARLY" label="매년" />
          </el-select>
        </el-form-item>

        <template v-if="eventForm.recurrenceType !== 'NONE'">
          <!-- 반복 간격 -->
          <el-form-item>
            <div class="rec-interval-row">
              <span class="rec-label">매</span>
              <el-input-number v-model="recurrenceRule.interval" :min="1" :max="99" controls-position="right" style="width:90px" />
              <span class="rec-label">{{ intervalUnitLabel }}</span>
            </div>
          </el-form-item>

          <!-- 매주: 요일 선택 -->
          <el-form-item v-if="eventForm.recurrenceType === 'WEEKLY'" label="반복 요일">
            <div class="rec-days">
              <button v-for="(d, i) in dayLabels" :key="i" type="button"
                class="rec-day-btn"
                :class="{ active: recurrenceRule.days.includes(i) }"
                @click="toggleDay(i)">{{ d }}</button>
            </div>
          </el-form-item>

          <!-- 매월: 기준 선택 -->
          <el-form-item v-if="eventForm.recurrenceType === 'MONTHLY'" label="반복 기준">
            <el-radio-group v-model="recurrenceRule.monthlyType">
              <el-radio value="DATE">{{ monthlyDateLabel }}</el-radio>
              <el-radio value="WEEKDAY">{{ monthlyWeekdayLabel }}</el-radio>
            </el-radio-group>
          </el-form-item>

          <!-- 반복 종료 -->
          <el-form-item label="종료">
            <div class="rec-end-row">
              <el-select v-model="recurrenceRule.endType" style="width:110px">
                <el-option value="NONE" label="없음" />
                <el-option value="DATE" label="날짜" />
                <el-option value="COUNT" label="횟수" />
              </el-select>
              <el-date-picker v-if="recurrenceRule.endType === 'DATE'"
                v-model="recurrenceRule.until" type="date"
                format="YYYY-MM-DD" value-format="YYYY-MM-DD"
                placeholder="종료일" style="flex:1" />
              <template v-if="recurrenceRule.endType === 'COUNT'">
                <el-input-number v-model="recurrenceRule.count" :min="1" :max="999" controls-position="right" style="width:90px" />
                <span class="rec-label">회</span>
              </template>
            </div>
          </el-form-item>
        </template>
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
    <el-dialog v-model="showDetail" :width="isMobile ? '95vw' : '400px'" destroy-on-close>
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
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import FullCalendar from '@fullcalendar/vue3'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import koLocale from '@fullcalendar/core/locales/ko'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import { useRoute } from 'vue-router'
import api from '@/api/axios'

const authStore  = useAuthStore()
const menuStore  = useMenuStore()
const route      = useRoute()
const isMobile   = computed(() => window.innerWidth <= 768)
const calRef     = ref()
const calApi     = computed(() => calRef.value?.getApi())
const loading    = ref(false)
const saving     = ref(false)
const showForm   = ref(false)
const showDetail = ref(false)
const calendars  = ref([])
const visibleCalendars = ref(new Set())
// 캘린더 목록 로드 완료를 기다리는 게이트 (fetchEvents가 필터 준비 전에
// 먼저 실행되어 모든 일정이 걸러지는 경쟁 조건 방지)
let resolveCalendarsReady
const calendarsReady = new Promise(r => { resolveCalendarsReady = r })
const currentTitle = ref('')
const currentView  = ref('dayGridMonth')
const editingEvent = ref(null)
const selectedEvent = ref(null)
const recurrenceEndDate = ref(null) // 하위 호환용 (미사용)
const recurrenceRule = ref({
  interval: 1,
  days: [],        // WEEKLY 요일 (0=일,1=월,...,6=토)
  monthlyType: 'DATE', // 'DATE' or 'WEEKDAY'
  endType: 'NONE', // 'NONE' | 'DATE' | 'COUNT'
  until: null,
  count: 10,
})
const showFilter = ref(false)
const filterWrap = ref(null)
// 날짜별 공휴일 정보 (HOLIDAY 캘린더 이벤트에서 추출)
const holidayMap = ref({}) // { "YYYY-MM-DD": { name, isHoliday } }
// 대한민국의 휴일 캘린더 표시 여부 (메뉴 설정 또는 localStorage)
const showHolidayCalendar = ref(true)

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
    if (authStore.member?.isAdmin) return true
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

// 현재 사용자가 읽을 수 있는 캘린더만 필터
const readableCalendars = computed(() => {
  return calendars.value.filter(c => {
    if (authStore.member?.isAdmin) return true
    if (!c.permissions || c.permissions.length === 0) return true
    const myGroupIds = authStore.member?.groupIds || []
    return c.permissions.some(p => p.canRead && myGroupIds.includes(p.groupId))
  })
})
// 일반 캘린더 (HOLIDAY 제외, 권한 있는 것만)
const normalCalendars = computed(() => readableCalendars.value.filter(c => c.calendarType !== 'HOLIDAY'))
// HOLIDAY 캘린더 (권한 있는 것만)
const holidayCalendars = computed(() => readableCalendars.value.filter(c => c.calendarType === 'HOLIDAY'))
// HOLIDAY 캘린더 색상
const holidayCalendarColor = computed(() => holidayCalendars.value[0]?.color || '#dc2626')
const hasHolidayCalendar = computed(() => holidayCalendars.value.length > 0)

// 메뉴에 캘린더가 고정된 경우 (필터 UI 숨김)
const isSingleCalendar = computed(() => {
  const activeMenu = menuStore.findMenuById(route.params?.menuId)
  return !!(activeMenu?.targetCalendarIds?.length || activeMenu?.targetId)
})

const canEditSelected = computed(() => {
  const calId = selectedEvent.value?.extendedProps?.calendarId
  const cal = calendars.value.find(c => c.id === calId)
  if (!cal) return false
  if (authStore.member?.isAdmin) return true
  if (!cal.permissions || cal.permissions.length === 0) return authStore.isLoggedIn
  return cal.permissions.some(p => p.canWrite)
})

function recurrenceLabel(type) {
  return { DAILY: '매일 반복', WEEKLY: '매주 반복', MONTHLY: '매월 반복' }[type] || ''
}

function selectAll() {
  visibleCalendars.value = new Set(normalCalendars.value.map(c => c.id))
  showHolidayCalendar.value = true
  localStorage.setItem('cal_show_holiday', 'true')
  calApi.value?.refetchEvents()
}
function deselectAll() {
  visibleCalendars.value = new Set()
  showHolidayCalendar.value = false
  localStorage.setItem('cal_show_holiday', 'false')
  calApi.value?.refetchEvents()
}

function toggleCalendar(id) {
  const s = new Set(visibleCalendars.value)
  if (s.has(id)) s.delete(id)
  else s.add(id)
  visibleCalendars.value = s
  calApi.value?.refetchEvents()
}

function fmtKorTime(date) {
  if (!date) return ''
  const h = date.getHours(), m = date.getMinutes()
  const ap = h < 12 ? '오전' : '오후'
  const hr = h % 12 || 12
  return m === 0 ? `${ap} ${hr}시` : `${ap} ${hr}:${String(m).padStart(2,'0')}`
}

function eventContent(arg) {
  const { event, view } = arg
  const color    = event.backgroundColor || '#2563EB'
  const viewType = view.type
  const isMobileView = window.innerWidth <= 768

  const el = document.createElement('div')
  el.style.setProperty('--ev-color', color)

  if (viewType === 'dayGridMonth') {
    if (event.allDay) {
      // 종일: 기존 pill 스타일
      el.className = 'fc-ev-custom'
      const n = document.createElement('div')
      n.className = 'fc-ev-title'
      n.textContent = event.title
      el.appendChild(n)
    } else if (isMobileView) {
      // 모바일: 기존 스타일
      el.className = 'fc-ev-custom'
      const t = document.createElement('div')
      t.className = 'fc-ev-time'
      t.textContent = arg.timeText || ''
      el.appendChild(t)
      const n = document.createElement('div')
      n.className = 'fc-ev-title'
      n.textContent = event.title
      el.appendChild(n)
    } else {
      // 데스크탑: 구글 캘린더 스타일 (● 오전 8시 제목)
      el.className = 'fc-ev-goog'
      const dot = document.createElement('span')
      dot.className = 'fc-ev-goog-dot'
      el.appendChild(dot)
      const label = document.createElement('span')
      label.className = 'fc-ev-goog-label'
      label.textContent = `${fmtKorTime(event.start)} ${event.title}`
      el.appendChild(label)
    }
  } else {
    // 주/일 뷰: 타임그리드 블록
    el.className = 'fc-ev-block'
    const title = document.createElement('div')
    title.className = 'fc-ev-block-title'
    title.textContent = event.title
    el.appendChild(title)
    if (event.extendedProps?.location) {
      const loc = document.createElement('div')
      loc.className = 'fc-ev-block-loc'
      loc.textContent = event.extendedProps.location
      el.appendChild(loc)
    }
  }
  return { domNodes: [el] }
}

const calOptions = computed(() => {
  const hMap = holidayMap.value // 공휴일 맵이 바뀌면 dayCellContent 재생성
  return {
    plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
    initialView: 'dayGridMonth',
    locale: koLocale,
    headerToolbar: false,
    height: 'auto',
    dayMaxEvents: 5,
    dayCellContent: (arg) => {
      const d = arg.date
      const dateStr = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
      const dow = d.getDay()
      const holiday = hMap[dateStr]
      const isSat = dow === 6
      const isRed = dow === 0 || !!holiday?.isHoliday

      const wrap = document.createElement('div')
      wrap.className = 'fc-day-custom'

      // 공휴일명 왼쪽
      if (holiday?.name) {
        const hol = document.createElement('span')
        hol.className = 'fc-day-hol' + (isSat ? ' sat' : isRed ? ' red' : '')
        hol.textContent = holiday.name
        wrap.appendChild(hol)
      }

      // 날짜 오른쪽
      const num = document.createElement('span')
      num.className = 'fc-day-num' + (isSat ? ' sat' : isRed ? ' red' : '')
      num.textContent = arg.dayNumberText
      wrap.appendChild(num)

      return { domNodes: [wrap] }
    },
    eventContent,
    events: fetchEvents,
    eventClick: handleEventClick,
    dateClick: handleDateClick,
    datesSet: () => { currentTitle.value = calApi.value?.view.title || '' },
  }
})

async function fetchEvents(info, successCallback, failureCallback) {
  try {
    await calendarsReady

    // 일반 캘린더 IDs
    const ids = [...visibleCalendars.value]
    // HOLIDAY 캘린더 IDs (항상 별도로 fetch)
    const holidayIds = showHolidayCalendar.value
      ? calendars.value.filter(c => c.calendarType === 'HOLIDAY').map(c => c.id)
      : []
    const allIds = [...new Set([...ids, ...holidayIds])]
    if (allIds.length === 0) { holidayMap.value = {}; successCallback([]); return }

    const qs = new URLSearchParams()
    qs.append('start', info.startStr)
    qs.append('end', info.endStr)
    allIds.forEach(id => qs.append('calendarIds', String(id)))

    const res = await api.get(`/calendars/events?${qs.toString()}`)
    const allData = res.data.data || []

    // HOLIDAY 캘린더 이벤트 → holidayMap (날짜 셀에 표시)
    const holidayCalIds = new Set(holidayIds)
    const newHolidayMap = {}
    allData.forEach(e => {
      if (holidayCalIds.has(e.calendarId) && e.isAllDay) {
        const dateStr = e.startAt.slice(0, 10)
        const isPublicHoliday = e.description === '공휴일'
        if (!newHolidayMap[dateStr] || isPublicHoliday) {
          newHolidayMap[dateStr] = { name: e.title, isHoliday: isPublicHoliday }
        }
      }
    })
    holidayMap.value = newHolidayMap
    // 날짜 셀 공휴일 표시 갱신 (dayCellContent는 render 시 재호출)
    nextTick(() => calApi.value?.render())

    // HOLIDAY 캘린더 이벤트는 블록으로 표시하지 않음
    const events = allData.filter(e => !holidayCalIds.has(e.calendarId)).map(e => {
      const cal = calendars.value.find(c => c.id === e.calendarId)
      return {
        id: String(e.id),
        title: e.title,
        start: e.startAt,
        // FullCalendar은 all-day end를 exclusive로 처리하므로 하루 추가
        // toISOString()은 UTC 변환으로 KST 자정이 전날이 되므로 로컬 날짜 사용
        end: e.isAllDay && e.endAt ? (() => {
          const d = new Date(e.endAt)
          d.setDate(d.getDate() + 1)
          return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
        })() : e.endAt,
        allDay: e.isAllDay,
        backgroundColor: cal?.color || '#2563EB',
        borderColor: cal?.color || '#2563EB',
        textColor: '#ffffff',
        extendedProps: {
          description: e.description,
          location: e.location,
          calendarId: e.calendarId,
          recurrenceType: e.recurrenceType,
          recurrenceRule: e.recurrenceRule,
          createdBy: e.createdBy,
        }
      }
    })
    successCallback(events)
  } catch (e) {
    console.error('캘린더 일정 로드 실패', e)
    failureCallback(e)
  }
}

function handleEventClick(info) {
  selectedEvent.value = info.event
  showDetail.value = true
}

// FullCalendar startStr/endStr 의 타임존 suffix 제거 → YYYY-MM-DDTHH:mm:ss
function stripTz(str) {
  return str ? str.slice(0, 19) : ''
}

// KST(Asia/Seoul) 기준 현재 시각 → YYYY-MM-DDTHH:mm:ss
function nowKst() {
  return new Date(new Date().toLocaleString('en-US', { timeZone: 'Asia/Seoul' }))
}

// 현재 KST 시각 기준 다음 정시 ~ +1시간 기본값
function defaultEventTimes(baseDate) {
  let base
  if (baseDate) {
    const clicked = new Date(baseDate)
    const kst = nowKst()
    clicked.setHours(kst.getHours(), kst.getMinutes(), 0, 0)
    base = clicked
  } else {
    base = nowKst()
  }
  const start = new Date(base)
  start.setMinutes(0, 0, 0)
  start.setHours(start.getHours() + 1)
  const end = new Date(start)
  end.setHours(end.getHours() + 1)
  const fmt = d =>
    `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}T${String(d.getHours()).padStart(2,'0')}:00:00`
  return { startAt: fmt(start), endAt: fmt(end) }
}

function handleDateClick(info) {
  if (!writableCalendars.value.length) return
  const pad = n => String(n).padStart(2, '0')
  const viewType = info.view.type
  let startAt, endAt, isAllDay

  if (viewType === 'dayGridMonth') {
    // 월간 뷰: 구글 캘린더처럼 종일 이벤트로 생성
    const d = info.date
    const dateStr = `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}`
    startAt = dateStr
    endAt = dateStr
    isAllDay = true
  } else {
    // 주간/일간 뷰: 클릭한 시간 슬롯 기준으로 1시간 블록
    const d = new Date(info.date)
    d.setMinutes(0, 0, 0)
    const end = new Date(d)
    end.setHours(end.getHours() + 1)
    const fmt = dt => `${dt.getFullYear()}-${pad(dt.getMonth()+1)}-${pad(dt.getDate())}T${pad(dt.getHours())}:00:00`
    startAt = fmt(d)
    endAt = fmt(end)
    isAllDay = false
  }

  eventForm.value = {
    calendarId: writableCalendars.value[0]?.id || null,
    title: '', location: '', description: '',
    startAt, endAt,
    isAllDay, recurrenceType: 'NONE',
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
  const { startAt, endAt } = defaultEventTimes()
  eventForm.value = {
    calendarId: writableCalendars.value[0]?.id || null,
    title: '', location: '', description: '',
    startAt, endAt, isAllDay: false, recurrenceType: 'NONE',
  }
  resetRecurrenceRule()
  showForm.value = true
}

function editEvent() {
  showDetail.value = false
  const e = selectedEvent.value
  editingEvent.value = e
  recurrenceEndDate.value = null
  let endAt = stripTz(e.endStr)
  // FullCalendar은 종일 end를 exclusive로 전달하므로 하루 빼서 복원
  if (e.allDay && endAt) {
    const d = new Date(endAt)
    d.setDate(d.getDate() - 1)
    const pad = n => String(n).padStart(2, '0')
    endAt = `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}`
  }
  const recType = e.extendedProps.recurrenceType || 'NONE'
  eventForm.value = {
    calendarId: e.extendedProps.calendarId,
    title: e.title,
    location: e.extendedProps.location || '',
    description: e.extendedProps.description || '',
    startAt: stripTz(e.startStr),
    endAt,
    isAllDay: e.allDay,
    recurrenceType: recType,
  }
  loadRecurrenceRule(e.extendedProps.recurrenceRule || null, recType)
  showForm.value = true
}

// ===== 반복 관련 =====
const dayLabels = ['일', '월', '화', '수', '목', '금', '토']

const intervalUnitLabel = computed(() => {
  switch (eventForm.value.recurrenceType) {
    case 'DAILY':   return '일마다'
    case 'WEEKLY':  return '주마다'
    case 'MONTHLY': return '개월마다'
    case 'YEARLY':  return '년마다'
    default:        return ''
  }
})

const monthlyDateLabel = computed(() => {
  const d = eventForm.value.startAt
  if (!d) return '날짜 기준'
  const date = new Date(d)
  return `매월 ${date.getDate()}일`
})

const monthlyWeekdayLabel = computed(() => {
  const d = eventForm.value.startAt
  if (!d) return '요일 기준'
  const date = new Date(d)
  const nth = Math.ceil(date.getDate() / 7)
  const dayName = dayLabels[date.getDay()]
  return `매월 ${nth}번째 ${dayName}요일`
})

function toggleDay(i) {
  const days = recurrenceRule.value.days
  const idx = days.indexOf(i)
  if (idx === -1) days.push(i)
  else days.splice(idx, 1)
  days.sort((a, b) => a - b)
}

function onRecurrenceTypeChange(type) {
  // 매주: 시작일 요일 기본 선택
  if (type === 'WEEKLY') {
    const d = eventForm.value.startAt
    if (d) {
      const dow = new Date(d).getDay()
      if (!recurrenceRule.value.days.includes(dow)) {
        recurrenceRule.value.days = [dow]
      }
    }
    if (recurrenceRule.value.days.length === 0) recurrenceRule.value.days = [1] // 월요일 기본
  }
}

function resetRecurrenceRule() {
  const d = eventForm.value.startAt
  const defaultDow = d ? new Date(d).getDay() : 1
  recurrenceRule.value = {
    interval: 1,
    days: [defaultDow],
    monthlyType: 'DATE',
    endType: 'NONE',
    until: null,
    count: 10,
  }
}

function buildRecurrenceRule() {
  const type = eventForm.value.recurrenceType
  if (type === 'NONE') return null
  const r = recurrenceRule.value
  const rule = {}
  if (r.interval > 1) rule.interval = r.interval
  if (type === 'WEEKLY' && r.days.length > 0) rule.days = [...r.days]
  if (type === 'MONTHLY' && r.monthlyType === 'WEEKDAY') rule.monthlyType = 'WEEKDAY'
  if (r.endType === 'DATE' && r.until) rule.until = r.until
  if (r.endType === 'COUNT' && r.count > 0) rule.count = r.count
  return Object.keys(rule).length ? JSON.stringify(rule) : null
}

function loadRecurrenceRule(recurrenceRuleStr, recurrenceType) {
  resetRecurrenceRule()
  if (!recurrenceRuleStr || recurrenceType === 'NONE') return
  try {
    const parsed = JSON.parse(recurrenceRuleStr)
    if (parsed.interval) recurrenceRule.value.interval = parsed.interval
    if (parsed.days) recurrenceRule.value.days = parsed.days
    if (parsed.monthlyType) recurrenceRule.value.monthlyType = parsed.monthlyType
    if (parsed.until) { recurrenceRule.value.endType = 'DATE'; recurrenceRule.value.until = parsed.until }
    else if (parsed.count) { recurrenceRule.value.endType = 'COUNT'; recurrenceRule.value.count = parsed.count }
  } catch {}
}

async function saveEvent() {
  if (!eventForm.value.title) return ElMessage.warning('제목을 입력해주세요.')
  saving.value = true
  try {
    const payload = {
      ...eventForm.value,
      recurrenceRule: buildRecurrenceRule(),
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
  const fmt = d => `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')}`
  if (event.allDay) {
    // FullCalendar end는 exclusive → 하루 빼서 표시
    if (!event.endStr) return fmt(s)
    const e = new Date(event.endStr)
    e.setDate(e.getDate() - 1)
    return s.toDateString() === e.toDateString() ? fmt(s) : `${fmt(s)} ~ ${fmt(e)}`
  }
  const e = event.endStr ? new Date(event.endStr) : null
  const timeFmt = d => `${fmt(d)} ${d.toTimeString().slice(0,5)}`
  return e ? `${timeFmt(s)} ~ ${timeFmt(e)}` : timeFmt(s)
}

watch(showHolidayCalendar, val => {
  localStorage.setItem('cal_show_holiday', String(val))
})

// 시작시간 변경 시 종료시간이 시작시간보다 같거나 이르면 종료시간 = 시작시간 + 1시간
watch(() => eventForm.value.startAt, (newStart) => {
  if (!newStart || eventForm.value.isAllDay) return
  const start = new Date(newStart)
  const end = eventForm.value.endAt ? new Date(eventForm.value.endAt) : null
  if (!end || end <= start) {
    const newEnd = new Date(start)
    newEnd.setHours(newEnd.getHours() + 1)
    const fmt = d =>
      `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}T${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}:00`
    eventForm.value.endAt = fmt(newEnd)
  }
})

function onClickOutside(e) {
  if (filterWrap.value && !filterWrap.value.contains(e.target)) showFilter.value = false
}

onMounted(async () => {
  try {
    // 메뉴 데이터가 로드되어 있어야 targetCalendarIds를 정확히 찾을 수 있음
    if (route.params?.menuId) await menuStore.fetchMenus()
    const res = await api.get('/calendars')
    const all = res.data.data || []
    // 메뉴에 연결된 캘린더가 있으면 해당 캘린더만 표시 (다중 지원)
    const activeMenu = menuStore.findMenuById(route.params?.menuId)
    const linkedIds = activeMenu?.targetCalendarIds?.length
      ? activeMenu.targetCalendarIds.map(Number)
      : (activeMenu?.targetId ? [Number(activeMenu.targetId)] : [])
    // HOLIDAY 캘린더는 메뉴 연결 여부와 무관하게 항상 포함 (별도 처리)
    const holidayAll = all.filter(c => c.calendarType === 'HOLIDAY')
    const filtered = linkedIds.length ? all.filter(c => linkedIds.includes(c.id)) : all
    const merged = [...filtered]
    holidayAll.forEach(h => { if (!merged.find(c => c.id === h.id)) merged.push(h) })
    calendars.value = merged
    // 메뉴 설정이 있으면 그 값을 사용, 없으면 localStorage 유지
    if (activeMenu && activeMenu.showHoliday !== undefined) {
      showHolidayCalendar.value = activeMenu.showHoliday !== false
    } else {
      showHolidayCalendar.value = localStorage.getItem('cal_show_holiday') !== 'false'
    }
    // HOLIDAY 캘린더는 visibleCalendars에서 제외 (별도 처리)
    visibleCalendars.value = new Set(calendars.value.filter(c => c.calendarType !== 'HOLIDAY').map(c => c.id))
  } catch {}
  finally {
    // 목록 로드 완료(또는 실패) 후 게이트 해제 → fetchEvents 진행
    resolveCalendarsReady()
  }
  currentTitle.value = calApi.value?.view.title || ''
  document.addEventListener('click', onClickOutside)
})

onUnmounted(() => { document.removeEventListener('click', onClickOutside) })
</script>

<style scoped>
/* ===== 레이아웃 ===== */
.cal-layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 20px;
  align-items: start;
}

/* ===== 사이드바 ===== */
.cal-sidebar {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 16px 18px;
  box-shadow: var(--shadow-sm);
  position: sticky;
  top: 20px;
}

.sidebar-section {}

.sidebar-label {
  font-size: 12.5px;
  font-weight: 800;
  letter-spacing: 0.04em;
  color: var(--t3);
  margin-bottom: 10px;
  text-transform: uppercase;
}

.sidebar-cal-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sidebar-cal-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 4px;
  cursor: pointer;
  font-size: 13.5px;
  font-weight: 500;
  border-radius: 8px;
  transition: background 0.12s;
  user-select: none;
}
.sidebar-cal-item:hover { background: var(--surface2); }

.sidebar-check {
  width: 18px;
  height: 18px;
  border-radius: 5px;
  border: 2px solid;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.12s;
}

.sidebar-cal-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.12s;
}

.sidebar-divider {
  height: 1px;
  background: var(--border);
  margin: 8px 0;
}

/* ===== 메인 ===== */
.cal-main { min-width: 0; display: flex; flex-direction: column; gap: 14px; }

/* ===== 툴바 ===== */
.cal-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
}
.cal-left, .cal-right { display: flex; align-items: center; gap: 8px; }

.cal-title {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.03em;
  color: var(--t1);
}

/* prev / next 연결 버튼 */
.cal-nav-group {
  display: flex;
}

.cal-nav-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--t2);
  cursor: pointer;
  transition: background 0.12s, color 0.12s;
}
.cal-nav-btn:first-child { border-radius: 8px 0 0 8px; }
.cal-nav-btn.next { border-left: none; border-radius: 0 8px 8px 0; }
.cal-nav-btn:hover { background: var(--surface2); color: var(--t1); }

/* 오늘 버튼 */
.cal-today-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 14px;
  height: 32px;
  border: 1px solid var(--border);
  background: var(--surface);
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--t2);
  cursor: pointer;
  font-family: inherit;
  transition: background 0.12s, color 0.12s;
}
.cal-today-btn:hover { background: var(--surface2); color: var(--t1); }

/* 뷰 탭 (segmented control) */
.view-tabs {
  display: flex;
  gap: 2px;
  background: var(--surface2);
  border-radius: 9px;
  padding: 3px;
}

.view-tab-btn {
  border: none;
  background: transparent;
  color: var(--t2);
  font-weight: 600;
  font-size: 13px;
  padding: 6px 17px;
  border-radius: 7px;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.12s, color 0.12s, box-shadow 0.12s;
}
.view-tab-btn.active {
  background: var(--surface);
  color: var(--t1);
  box-shadow: var(--shadow-sm);
}

/* 일정 등록 버튼 */
.cal-add-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  background: var(--primary);
  color: #fff;
  font-weight: 600;
  font-size: 14px;
  padding: 9px 16px;
  border-radius: 9px;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.12s;
}
.cal-add-btn:hover { background: var(--primary-strong); }

/* ===== 모바일 필터 드롭다운 (사이드바 대체) ===== */
.cal-filter-wrap { position: relative; }

.cal-filter-panel {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  min-width: 180px;
  background: var(--surface);
  border: 0.5px solid var(--border);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow);
  padding: 10px 12px;
  z-index: 200;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.filter-title {
  font-size: 11px;
  font-weight: 700;
  color: var(--t3);
  text-transform: uppercase;
  letter-spacing: 0.4px;
  padding-bottom: 6px;
  border-bottom: 0.5px solid var(--border2);
  margin-bottom: 4px;
}

.filter-divider {
  height: 0.5px;
  background: var(--border2);
  margin: 4px 0;
}

.filter-actions {
  display: flex;
  gap: 8px;
  padding-top: 6px;
  border-top: 0.5px solid var(--border2);
  margin-top: 4px;
}

.filter-link {
  font-size: 12px;
  color: var(--accent-t);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  font-family: inherit;
}
.filter-link:hover { text-decoration: underline; }

.filter-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  background: var(--accent);
  color: #fff;
  border-radius: 8px;
  font-size: 10px;
  font-weight: 700;
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

/* ===== 캘린더 카드 ===== */
.cal-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

/* ===== FullCalendar 기본 스타일 ===== */
.cal-card :deep(.fc) { font-family: inherit; }
.cal-card :deep(.fc-theme-standard td),
.cal-card :deep(.fc-theme-standard th),
.cal-card :deep(.fc-theme-standard .fc-scrollgrid),
.cal-card :deep(.fc-theme-standard .fc-scrollgrid-section > td) {
  border-color: var(--border) !important;
}
/* 카드 테두리와 겹치는 scrollgrid 외곽 border 제거 */
.cal-card :deep(.fc-scrollgrid) {
  border-left: none !important;
  border-top: none !important;
  border-right: none !important;
  border-bottom: none !important;
}
.cal-card :deep(.fc-daygrid-day) { background: var(--surface) !important; }
.cal-card :deep(.fc-day-other) { background: var(--surface2) !important; }
.cal-card :deep(.fc-day-today) { background: var(--accent-bg) !important; }
.cal-card :deep(.fc-col-header-cell) {
  background: var(--surface) !important;
  font-size: 12px; font-weight: 700; color: var(--t2); padding: 11px 0;
  border-bottom: 1px solid var(--border) !important;
}
/* 일 헤더: 빨강 */
.cal-card :deep(.fc-day-sun.fc-col-header-cell) { color: var(--danger, #dc2626) !important; }
.cal-card :deep(.fc-day-sun.fc-col-header-cell a) { color: var(--danger, #dc2626) !important; }
.cal-card :deep(.fc-timegrid-slot-label-cushion) {
  font-size: 13px;
  color: var(--t3);
}
.cal-card :deep(.fc-col-header-cell a) { color: var(--t2) !important; }
/* 토/일 컬럼 헤더 색상 (주/일 뷰) */
.cal-card :deep(.fc-day-sat.fc-col-header-cell a) { color: #2563eb !important; }
.cal-card :deep(.fc-day-sun.fc-col-header-cell a) { color: #dc2626 !important; }

/* FullCalendar 날짜 셀 헤더: float 제거 후 flex로 전체 너비 활용 */
.cal-card :deep(.fc-daygrid-day-top) {
  display: flex !important;
  flex-direction: row !important;
  align-items: center !important;
  width: 100% !important;
}
.cal-card :deep(.fc-daygrid-day-number) {
  float: none !important;
  display: flex !important;
  flex: 1 !important;
  width: 100% !important;
  padding: 5px 7px !important;
  box-sizing: border-box !important;
}

/* 커스텀 날짜 셀 내부 */
.cal-card :deep(.fc-day-custom) {
  display: flex;
  align-items: center;
  width: 100%;
}
.cal-card :deep(.fc-day-num) {
  font-size: 13px;
  color: var(--t2);
  line-height: 1.4;
  flex-shrink: 0;
  margin-left: auto;
}
.cal-card :deep(.fc-day-num.sat) { color: #2563eb; }
.cal-card :deep(.fc-day-num.red) { color: #dc2626; }
.cal-card :deep(.fc-day-hol) {
  font-size: 12px;
  color: rgba(100, 100, 100, 0.63);
  font-weight: 500;
  letter-spacing: -0.6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  text-align: left;
}
.cal-card :deep(.fc-day-hol.sat) { color: rgba(37, 99, 235, 0.63); }
.cal-card :deep(.fc-day-hol.red) { color: rgba(220, 38, 38, 0.63); font-weight: 600; }
.cal-card :deep(.fc-daygrid-day-number:hover) { color: var(--accent); }
.cal-card :deep(.fc-event) { border-radius: 4px; border: none; font-size: 12px; cursor: pointer; }
.cal-card :deep(.fc-toolbar) { display: none; }
.cal-card :deep(.fc-more-link) { color: var(--accent-t) !important; }

/* ===== 커스텀 이벤트 카드 ===== */
/* 월 뷰(daygrid)만 투명 배경 처리 */
.cal-card :deep(.fc-daygrid-event) {
  background: transparent !important;
  border: none !important;
  padding: 0 !important;
  margin-bottom: 2px !important;
  border-radius: 0 !important;
  overflow: visible;
}

/* 종일 / 모바일 폴백 스타일 */
.cal-card :deep(.fc-ev-custom) {
  display: flex;
  flex-direction: column;
  border-left: 3px solid var(--ev-color, var(--accent));
  background: color-mix(in srgb, var(--ev-color, var(--accent)) 12%, var(--surface));
  border-radius: 3px;
  padding: 3px 5px;
  width: 100%;
  box-sizing: border-box;
  gap: 1px;
  cursor: pointer;
}
.cal-card :deep(.fc-ev-time) {
  font-size: 11px;
  color: var(--ev-color, var(--accent));
  font-weight: 700;
  line-height: 1.2;
  white-space: nowrap;
}
.cal-card :deep(.fc-ev-title) {
  font-size: 13px;
  color: var(--t1);
  font-weight: 500;
  line-height: 1.35;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 데스크탑 월뷰 구글 캘린더 스타일 */
.cal-card :deep(.fc-ev-goog) {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 1px 4px;
  border-radius: 3px;
  width: 100%;
  box-sizing: border-box;
  cursor: pointer;
  min-width: 0;
}
.cal-card :deep(.fc-ev-goog:hover) {
  background: color-mix(in srgb, var(--ev-color, var(--accent)) 10%, var(--surface));
}
.cal-card :deep(.fc-ev-goog-dot) {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--ev-color, var(--accent));
  flex-shrink: 0;
}
.cal-card :deep(.fc-ev-goog-label) {
  font-size: 13px;
  color: var(--t1);
  font-weight: 400;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.6;
}

/* 주/일 뷰 블록 스타일 */
.cal-card :deep(.fc-ev-block) {
  padding: 3px 6px;
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 1px;
  cursor: pointer;
  overflow: hidden;
}
.cal-card :deep(.fc-ev-block-title) {
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.cal-card :deep(.fc-ev-block-loc) {
  font-size: 11px;
  color: rgba(255,255,255,0.85);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
/* 주/일뷰 이벤트는 backgroundColor가 적용되므로 배경 제거 불필요 */
.cal-card :deep(.fc-timegrid-event) {
  border-radius: 4px !important;
  border-left: 3px solid rgba(255,255,255,0.4) !important;
}

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

/* 반복 설정 */
.rec-interval-row { display: flex; align-items: center; gap: 8px; }
.rec-label { font-size: 13px; color: var(--t2); white-space: nowrap; }
.rec-days { display: flex; gap: 6px; flex-wrap: wrap; }
.rec-day-btn {
  width: 34px; height: 34px;
  border-radius: 50%;
  border: 1.5px solid var(--border2);
  background: var(--surface);
  color: var(--t2);
  font-size: 13px; font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
  display: flex; align-items: center; justify-content: center;
}
.rec-day-btn:hover { border-color: var(--accent); color: var(--accent); }
.rec-day-btn.active { background: var(--accent); border-color: var(--accent); color: #fff; }
.rec-end-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

@media (max-width: 768px) {
  /* 모바일: 사이드바 숨기고 단일 컬럼 */
  .cal-layout {
    grid-template-columns: 1fr;
  }
  .cal-sidebar { display: none; }
  .cal-main { gap: 8px; }
  .form-row { grid-template-columns: 1fr; }

  /* 툴바: 2줄 구조 */
  .cal-toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }

  /* 1줄: 제목 + nav + 오늘 */
  .cal-left {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
  }
  .cal-title {
    flex: 1;
    font-size: 18px !important;
  }

  /* 2줄: 뷰탭 왼쪽 + 필터/추가 오른쪽 */
  .cal-right {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    gap: 6px;
  }
  .view-tabs {
    flex: 1;
  }
  .view-tab-btn {
    flex: 1;
    padding: 6px 10px;
  }

  /* 일정 추가: 텍스트 숨김 */
  .cal-add-btn .btn-text { display: none; }
  .cal-add-btn { padding: 9px 12px; }

  /* 모바일 입력 폼 줌 방지 */
  :deep(.el-input__inner),
  :deep(.el-textarea__inner),
  :deep(.el-date-editor .el-input__inner) { font-size: 16px !important; }

  /* 모바일 다이얼로그 스크롤 */
  :deep(.el-dialog__body) { max-height: calc(80dvh - 120px); overflow-y: auto; }

  /* 모바일 캘린더 */
  .cal-card :deep(.fc-day-num) { font-size: 11px; }
  .cal-card :deep(.fc-day-hol) { font-size: 10px; max-width: 50px; }
  .cal-card :deep(.fc-col-header-cell) { font-size: 10px; padding: 6px 0; }
  .cal-card :deep(.fc-more-link) { font-size: 9px !important; }
  .cal-card :deep(.fc-daygrid-day-frame) { min-height: 50px; }
  .cal-card :deep(.fc-daygrid-day-events) { margin-top: 0; }

  .cal-card :deep(.fc-ev-custom) {
    border-left: none !important;
    padding: 1px 3px !important;
    gap: 0 !important;
    border-radius: 2px !important;
  }
  .cal-card :deep(.fc-ev-time) { font-size: 9px; overflow: hidden; text-overflow: clip; white-space: nowrap; }
  .cal-card :deep(.fc-ev-title) { font-size: 10px; white-space: nowrap; overflow: hidden; text-overflow: clip; }
}
</style>
