<template>
  <div :class="['dashboard', { 'dashboard-menu': dashboardMenuId }]">

    <!-- 위젯 영역 -->
    <div v-if="!layoutLoading && layouts.length" class="widget-area">
      <template v-for="layout in layouts" :key="layout.id">
        <!-- widget = 로드 완료 시 실제 데이터, 아니면 레이아웃 메타 -->
        <!-- 위젯 콘텐츠 로딩 중이면 스켈레톤 카드 -->
        <template v-if="widgetLoading[layout.id]">
          <div :class="parseConfig(layout).size === 'full' || isFullWidget(layout.widgetType) ? 'widget-full' : 'widget-half'">
            <div class="wcard wcard-loading">
              <div class="wcard-head">
                <span class="wcard-title skeleton-text">{{ layout.title || layout.targetBoardName || widgetTypeLabel(layout.widgetType) }}</span>
              </div>
              <div class="wcard-skeleton-body">
                <i class="ti ti-loader-2 spinning" style="font-size:22px;color:var(--t3)"></i>
              </div>
            </div>
          </div>
        </template>
        <!-- v-for 1개 배열로 widget 스코프 변수 생성 -->
        <template v-else v-for="widget in [mergedWidget(layout)]" :key="'d' + widget.id">

        <!-- 이미지 슬라이더 -->
        <div v-if="widget.widgetType === 'IMAGE_SLIDER'" class="widget-full">
          <ImageSlider :slides="parseConfig(widget).slides || []" :title="widget.title" />
        </div>

        <!-- 웰컴 카드 -->
        <div v-else-if="widget.widgetType === 'WELCOME'" class="widget-full">
          <div
            class="welcome-card"
            ref="welcomeRef"
            @mousemove="handleMouseMove"
            @mouseleave="handleMouseLeave"
          >
            <div class="welcome-glow" :style="glowStyle" />
            <div class="welcome-content">
              <div class="welcome-left">
                <div class="welcome-name">안녕하세요, <span class="welcome-highlight">{{ authStore.member?.name || '방문자' }}</span>님</div>
                <div class="welcome-sub">{{ parseConfig(widget).subText ?? '오늘도 좋은 하루 보내세요 ☀️' }}</div>
              </div>
              <div v-if="parseConfig(widget).showClock !== false" class="welcome-right">
                <div class="welcome-date-line">{{ todayDate }}</div>
                <div class="welcome-time-line">{{ todayTime }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 구분선 -->
        <div v-else-if="widget.widgetType === 'DIVIDER'" class="widget-full">
          <div class="widget-divider">
            <span v-if="widget.title" class="divider-label">{{ widget.title }}</span>
            <hr v-else class="divider-line" />
          </div>
        </div>

        <!-- 최신 글 (텍스트 목록) -->
        <div v-else-if="widget.widgetType === 'RECENT_POSTS'"
             :class="parseConfig(widget).size === 'full' ? 'widget-full' : 'widget-half'">
          <div class="wcard">
            <div class="wcard-head">
              <span class="wcard-title">{{ widget.title || widget.targetBoardName || '최신글' }}</span>
              <router-link v-if="widget.targetBoardId" :to="boardListPath(widget)" class="wcard-more">
                더보기 <i class="ti ti-arrow-right"></i>
              </router-link>
            </div>
            <p v-if="widget.description" class="wcard-desc">{{ widget.description }}</p>
            <div v-if="widget.posts?.length" class="post-list">
              <router-link
                v-for="post in widget.posts"
                :key="post.id"
                :to="postPath(widget, post)"
                class="post-row"
              >
                <span class="post-title-wrap">
                  <span v-if="post.boardName" class="post-board-tag">{{ post.boardName }}</span>
                  <span class="post-title-txt">{{ post.title }}</span>
                </span>
                <span class="post-date-txt">{{ formatDate(post.createdAt) }}</span>
              </router-link>
            </div>
            <div v-else class="wcard-empty">등록된 글이 없습니다.</div>
          </div>
        </div>

        <!-- 갤러리 최신 글 (썸네일 그리드) -->
        <div v-else-if="widget.widgetType === 'RECENT_GALLERY'"
             :class="parseConfig(widget).size === 'full' ? 'widget-full' : 'widget-half'">
          <div class="wcard">
            <div class="wcard-head">
              <span class="wcard-title">{{ widget.title || widget.targetBoardName || '갤러리' }}</span>
              <router-link v-if="widget.targetBoardId" :to="boardListPath(widget)" class="wcard-more">
                더보기 <i class="ti ti-arrow-right"></i>
              </router-link>
            </div>
            <p v-if="widget.description" class="wcard-desc">{{ widget.description }}</p>
            <div v-if="widget.posts?.length" class="gallery-grid">
              <router-link
                v-for="post in widget.posts"
                :key="post.id"
                :to="postPath(widget, post)"
                class="gallery-item"
              >
                <div class="gallery-thumb">
                  <template v-if="post.thumbnailUrl">
                    <div class="gw-spinner"><div class="gw-ring"></div></div>
                    <img
                      :src="post.thumbnailUrl"
                      :alt="post.title"
                      class="gw-img"
                      @load="e => { e.target.classList.add('loaded'); e.target.previousElementSibling.style.display='none' }"
                      @error="e => { e.target.style.display='none'; e.target.previousElementSibling.style.display='none' }"
                    />
                  </template>
                  <div v-else class="gallery-no-img"><i class="ti ti-photo"></i></div>
                </div>
                <span class="gallery-title">{{ post.title }}</span>
              </router-link>
            </div>
            <div v-else class="wcard-empty">등록된 글이 없습니다.</div>
          </div>
        </div>

        <!-- 캘린더 주간 위젯 -->
        <div v-else-if="widget.widgetType === 'CALENDAR_WEEKLY'"
             :class="parseConfig(widget).size === 'full' ? 'widget-full' : 'widget-half'">
          <div class="wcard">
            <div class="wcard-head">
              <div class="wcard-head-left">
                <span class="wcard-title">{{ widget.title || '이번 주 일정' }}</span>
                <span v-if="widget.description" class="wcard-desc-inline">{{ widget.description }}</span>
              </div>
              <div class="cal-week-nav">
                <button class="cal-nav-btn" @click.stop="navigateWeekFc(widget, -1)" :disabled="calWeekLoading[widget.id]">
                  <i class="ti ti-chevron-left"></i>
                </button>
                <span class="wcard-week-range">{{ calFcLabels[widget.id] || getWeekRange(widget.id) }}</span>
                <button class="cal-nav-btn" @click.stop="navigateWeekFc(widget, +1)" :disabled="calWeekLoading[widget.id]">
                  <i class="ti ti-chevron-right"></i>
                </button>
              </div>
            </div>
            <div v-if="calWeekLoading[widget.id]" class="cal-loading">
              <i class="ti ti-loader-2 spinning"></i>
            </div>
            <div v-else class="db-cal-wrap">
              <FullCalendar
                :key="`w_${widget.id}_${calWeekOffset[widget.id] ?? 0}`"
                :ref="el => setFcRef('w_' + widget.id, el)"
                :options="getWeekFcOptions(widget)"
              />
            </div>
          </div>
        </div>

        <!-- 캘린더 월간 위젯 -->
        <div v-else-if="widget.widgetType === 'CALENDAR_MONTHLY'"
             :class="parseConfig(widget).size === 'full' ? 'widget-full' : 'widget-half'">
          <div class="wcard">
            <div class="wcard-head">
              <div class="wcard-head-left">
                <span class="wcard-title">{{ widget.title || '이번 달 일정' }}</span>
                <span v-if="widget.description" class="wcard-desc-inline">{{ widget.description }}</span>
              </div>
              <div class="cal-week-nav">
                <button class="cal-nav-btn" @click.stop="navigateMonthFc(widget, -1)" :disabled="calMonthLoading[widget.id]">
                  <i class="ti ti-chevron-left"></i>
                </button>
                <span class="wcard-week-range">{{ calFcLabels[widget.id] || getMonthLabel(widget.id) }}</span>
                <button class="cal-nav-btn" @click.stop="navigateMonthFc(widget, +1)" :disabled="calMonthLoading[widget.id]">
                  <i class="ti ti-chevron-right"></i>
                </button>
              </div>
            </div>
            <div v-if="calMonthLoading[widget.id]" class="cal-loading">
              <i class="ti ti-loader-2 spinning"></i>
            </div>
            <div v-else class="db-cal-wrap">
              <FullCalendar
                :key="`m_${widget.id}_${calMonthOffset[widget.id] ?? 0}`"
                :ref="el => setFcRef('m_' + widget.id, el)"
                :options="getMonthFcOptions(widget)"
              />
            </div>
          </div>
        </div>

        <!-- 링크 모음 -->
        <div v-else-if="widget.widgetType === 'LINK_LIST'"
             :class="parseConfig(widget).size === 'full' ? 'widget-full' : 'widget-half'">
          <div class="wcard">
            <div class="wcard-head">
              <span class="wcard-title">{{ widget.title || '링크 모음' }}</span>
            </div>
            <p v-if="widget.description" class="wcard-desc">{{ widget.description }}</p>
            <div v-if="parseConfig(widget).links?.length" class="link-grid">
              <a
                v-for="(link, i) in parseConfig(widget).links"
                :key="i"
                :href="link.url"
                :target="link.newWindow ? '_blank' : undefined"
                class="link-chip"
              >
                <span>{{ link.label }}</span>
                <i class="ti ti-arrow-up-right link-arrow"></i>
              </a>
            </div>
            <div v-else class="wcard-empty">링크가 없습니다.</div>
          </div>
        </div>

        <!-- 바로가기 -->
        <div v-else-if="widget.widgetType === 'QUICK_LINKS'"
             :class="parseConfig(widget).size === 'full' ? 'widget-full' : 'widget-half'">
          <div class="wcard">
            <div class="wcard-head">
              <span class="wcard-title">{{ widget.title || '바로가기' }}</span>
            </div>
            <p v-if="widget.description" class="wcard-desc">{{ widget.description }}</p>
            <div v-if="parseConfig(widget).links?.length" class="quick-links-grid">
              <component
                :is="link.newWindow ? 'a' : 'router-link'"
                v-for="(link, i) in parseConfig(widget).links"
                :key="i"
                v-bind="link.newWindow ? { href: link.url, target: '_blank' } : { to: link.url }"
                class="quick-link-item"
              >
                <div class="ql-icon-wrap">
                  <i :class="'ti ' + (link.icon || 'ti-link')"></i>
                </div>
                <span class="ql-label">{{ link.label }}</span>
              </component>
            </div>
            <div v-else class="wcard-empty">바로가기가 없습니다.</div>
          </div>
        </div>

        <!-- 이미지 그리드 -->
        <div v-else-if="widget.widgetType === 'IMAGE_GRID'" class="widget-full">
          <div class="wcard">
            <div v-if="widget.title || widget.description" class="wcard-head">
              <span class="wcard-title">{{ widget.title }}</span>
            </div>
            <p v-if="widget.description" class="wcard-desc">{{ widget.description }}</p>
            <div v-if="parseConfig(widget).images?.length" class="img-grid">
              <component
                :is="img.linkUrl ? 'a' : 'div'"
                v-for="(img, i) in parseConfig(widget).images.slice(0,4)"
                :key="i"
                v-bind="img.linkUrl ? { href: img.linkUrl, target: img.newWindow ? '_blank' : undefined } : {}"
                class="img-grid-item"
              >
                <img v-if="img.imageUrl" :src="toSmallThumb(img.imageUrl)" :alt="img.title || ''" loading="lazy" class="img-grid-photo" />
                <div v-else class="img-grid-placeholder"><i class="ti ti-photo"></i></div>
                <div v-if="img.title || img.desc" class="img-grid-overlay">
                  <span v-if="img.title" class="img-grid-title">{{ img.title }}</span>
                  <span v-if="img.desc" class="img-grid-desc">{{ img.desc }}</span>
                </div>
              </component>
            </div>
            <div v-else class="wcard-empty">이미지가 없습니다.</div>
          </div>
        </div>

        <!-- 커스텀 -->
        <div v-else-if="widget.widgetType === 'CUSTOM'"
             :class="parseConfig(widget).size === 'half' ? 'widget-half' : 'widget-full'">
          <div class="wcard">
            <div class="wcard-head">
              <span class="wcard-title">{{ widget.title || '' }}</span>
              <a v-if="parseConfig(widget).moreUrl" :href="parseConfig(widget).moreUrl" class="wcard-more">
                더보기 <i class="ti ti-arrow-right"></i>
              </a>
            </div>
            <p v-if="widget.description" class="wcard-desc">{{ widget.description }}</p>
            <div class="custom-body ql-editor" v-html="toCustomHtml(parseConfig(widget).content || '')" />
          </div>
        </div>

        <!-- 회원 현황 -->
        <div v-else-if="widget.widgetType === 'MEMBER_STATS'" class="widget-half">
          <div class="wcard member-stats-card">
            <div class="wcard-head">
              <span class="wcard-title">{{ widget.title || '회원 현황' }}</span>
            </div>
            <p v-if="widget.description" class="wcard-desc">{{ widget.description }}</p>
            <div class="mstats-grid">
              <div class="mstat-item">
                <div class="mstat-icon" style="background:#EFF6FF;color:#2563EB"><i class="ti ti-users"></i></div>
                <div class="mstat-info">
                  <div class="mstat-num">{{ widget.stats?.memberCount ?? 0 }}</div>
                  <div class="mstat-lbl">전체 회원</div>
                </div>
              </div>
              <div class="mstat-item">
                <div class="mstat-icon" style="background:#ECFDF5;color:#10B981"><i class="ti ti-layout-list"></i></div>
                <div class="mstat-info">
                  <div class="mstat-num">{{ widget.stats?.boardCount ?? 0 }}</div>
                  <div class="mstat-lbl">게시판</div>
                </div>
              </div>
              <div class="mstat-item">
                <div class="mstat-icon" style="background:#FEF2F2;color:#EF4444"><i class="ti ti-mail-question"></i></div>
                <div class="mstat-info">
                  <div class="mstat-num accent-red">{{ widget.stats?.pendingInquiryCount ?? 0 }}</div>
                  <div class="mstat-lbl">미처리 문의</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 접속 통계 -->
        <div v-else-if="widget.widgetType === 'VISIT_STATS'" class="widget-half">
          <div class="wcard visit-stats-card">
            <div class="wcard-head">
              <span class="wcard-title">{{ widget.title || '오늘의 접속' }}</span>
              <span class="wcard-sub-date">{{ todayDateShort }}</span>
            </div>
            <p v-if="widget.description" class="wcard-desc">{{ widget.description }}</p>
            <div class="vstats-row">
              <div class="vstat-item">
                <div class="vstat-num">{{ widget.stats?.todayVisits ?? 0 }}</div>
                <div class="vstat-lbl"><i class="ti ti-eye"></i> 전체 방문</div>
              </div>
              <div class="vstat-divider"></div>
              <div class="vstat-item">
                <div class="vstat-num">{{ widget.stats?.uniqueVisits ?? 0 }}</div>
                <div class="vstat-lbl"><i class="ti ti-user-check"></i> 순 방문자</div>
              </div>
              <div class="vstat-divider"></div>
              <div class="vstat-item">
                <div class="vstat-num accent-blue">{{ widget.stats?.loginVisits ?? 0 }}</div>
                <div class="vstat-lbl"><i class="ti ti-login"></i> 로그인</div>
              </div>
            </div>
          </div>
        </div>

        </template> <!-- end v-else widget v-for -->
      </template> <!-- end v-for layout -->
    </div>

    <div v-else-if="layoutLoading" class="loading-area">
      <i class="ti ti-loader-2 spinning"></i>
    </div>

    <div v-else class="empty-state">
      <i class="ti ti-layout-dashboard"></i>
      <p>표시할 대시보드 위젯이 없습니다.</p>
    </div>

  </div>
</template>

<script setup>
import { onMounted, ref, computed, onBeforeUnmount, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import api from '@/api/axios'
import ImageSlider from '@/components/common/ImageSlider.vue'
import FullCalendar from '@fullcalendar/vue3'
import dayGridPlugin from '@fullcalendar/daygrid'
import interactionPlugin from '@fullcalendar/interaction'
import koLocale from '@fullcalendar/core/locales/ko'

const authStore = useAuthStore()
const menuStore = useMenuStore()
const route = useRoute()

// null = 홈 대시보드, number = 메뉴 연결 대시보드
const dashboardMenuId = computed(() => {
  const id = route.params?.menuId
  return id ? Number(id) : null
})

// layouts: 빠른 메타데이터 (즉시 표시)
const layouts      = ref([])
// widgetData: id → 실제 데이터 (비동기 로드)
const widgetData   = ref({})
// widgetLoading: id → boolean
const widgetLoading = ref({})
const layoutLoading = ref(true)

// 실제 위젯 데이터 = 로드 완료된 경우 widgetData, 아니면 layout 그대로
function mergedWidget(layout) {
  return widgetData.value[layout.id] ?? layout
}

// ===== 웰컴 카드 마우스 glow =====
const welcomeRef = ref(null)
const glowStyle  = ref({})

function handleMouseMove(e) {
  const rect = welcomeRef.value?.getBoundingClientRect()
  if (!rect) return
  const x = ((e.clientX - rect.left) / rect.width) * 100
  const y = ((e.clientY - rect.top) / rect.height) * 100
  glowStyle.value = {
    background: `radial-gradient(circle at ${x}% ${y}%, rgba(255,255,255,0.25) 0%, transparent 55%)`,
    opacity: 1
  }
}
function handleMouseLeave() { glowStyle.value = { opacity: 0 } }

// ===== 날짜/시각 =====
const todayDate = new Date().toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
const todayDateShort = new Date().toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' })
const todayTime = ref(new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' }))
const clockTimer = setInterval(() => {
  todayTime.value = new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
}, 10000)
onBeforeUnmount(() => clearInterval(clockTimer))

// ===== 캘린더 공통 상태 =====
const calWeekOffset  = ref({})  // widgetId → number
const calWeekEvents  = ref({})  // widgetId → events[]
const calWeekLoading = ref({})  // widgetId → boolean
const calMonthOffset  = ref({}) // widgetId → number
const calMonthEvents  = ref({}) // widgetId → events[]
const calMonthLoading = ref({}) // widgetId → boolean

function getWeekRange(widgetId) {
  const offset = calWeekOffset.value[widgetId] ?? 0
  const today = new Date()
  const sunday = new Date(today)
  sunday.setDate(today.getDate() - today.getDay() + offset * 7)
  const sat = new Date(sunday)
  sat.setDate(sunday.getDate() + 6)
  return `${sunday.getMonth()+1}/${sunday.getDate()} – ${sat.getMonth()+1}/${sat.getDate()}`
}

function getMonthLabel(widgetId) {
  const offset = calMonthOffset.value[widgetId] ?? 0
  const d = new Date()
  d.setDate(1)
  d.setMonth(d.getMonth() + offset)
  return `${d.getFullYear()}년 ${d.getMonth() + 1}월`
}

// ===== FullCalendar 위젯 =====
const calHolidayMaps = ref({})  // widgetId → { dateStr → { name, isHoliday } }
const calFcLabels    = ref({})  // widgetId → view title
const fcRefs = {}               // key → FullCalendar instance

function setFcRef(key, el) {
  if (el) fcRefs[key] = el
  else delete fcRefs[key]
}

// raw events → FullCalendar 이벤트 배열 + 공휴일 맵 갱신
function buildFcData(rawEvents, wid) {
  const hMap = {}
  rawEvents.forEach(ev => {
    if (ev.isHoliday || ev.calendarType === 'HOLIDAY') {
      const dateStr = (ev.startAt || '').slice(0, 10)
      if (dateStr) hMap[dateStr] = { name: ev.title, isHoliday: ev.description === '공휴일' || ev.isHoliday }
    }
  })
  calHolidayMaps.value[wid] = hMap
  const pad = n => String(n).padStart(2, '0')
  return rawEvents
    .filter(ev => ev.calendarType !== 'HOLIDAY' && !ev.isHoliday)
    .map(ev => ({
      id: String(ev.id),
      title: ev.title,
      start: ev.startAt,
      // FullCalendar all-day end는 exclusive → 백엔드 inclusive end에 +1일
      end: ev.isAllDay && ev.endAt ? (() => {
        const d = new Date(ev.endAt)
        d.setDate(d.getDate() + 1)
        return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}`
      })() : ev.endAt,
      allDay: ev.isAllDay,
      backgroundColor: ev.calendarColor || '#2563EB',
      borderColor: ev.calendarColor || '#2563EB',
    }))
}

function dbEventContent(arg) {
  const { event } = arg
  const color = event.backgroundColor || '#2563EB'
  const el = document.createElement('div')
  el.style.setProperty('--ev-color', color)
  if (event.allDay) {
    el.className = 'fc-ev-custom'
    const n = document.createElement('div')
    n.className = 'fc-ev-title'
    n.textContent = event.title
    el.appendChild(n)
  } else {
    el.className = 'fc-ev-goog'
    const dot = document.createElement('span')
    dot.className = 'fc-ev-goog-dot'
    el.appendChild(dot)
    const label = document.createElement('span')
    label.className = 'fc-ev-goog-label'
    const h = event.start.getHours(), m = event.start.getMinutes()
    const ap = h < 12 ? '오전' : '오후'
    const hr = h % 12 || 12
    label.textContent = `${ap} ${hr}${m ? ':' + String(m).padStart(2,'0') : '시'} ${event.title}`
    el.appendChild(label)
  }
  return { domNodes: [el] }
}

function dbDayCellContent(wid, arg) {
  const d = arg.date
  const pad = n => String(n).padStart(2, '0')
  const dateStr = `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}`
  const dow = d.getDay()
  const holiday = (calHolidayMaps.value[wid] || {})[dateStr]
  const isSat = dow === 6
  const isRed = dow === 0 || !!holiday?.isHoliday
  const wrap = document.createElement('div')
  wrap.className = 'fc-day-custom'
  if (holiday?.name) {
    const hol = document.createElement('span')
    hol.className = 'fc-day-hol' + (isSat ? ' sat' : isRed ? ' red' : '')
    hol.textContent = holiday.name
    wrap.appendChild(hol)
  }
  const num = document.createElement('span')
  num.className = 'fc-day-num' + (isSat ? ' sat' : isRed ? ' red' : '')
  num.textContent = arg.dayNumberText
  wrap.appendChild(num)
  return { domNodes: [wrap] }
}

function getMonthFcOptions(widget) {
  const wid = widget.id
  const rawEvents = calMonthEvents.value[wid] !== undefined
    ? calMonthEvents.value[wid] : (widget.calendarEvents || [])
  const fcEvents = buildFcData(rawEvents, wid)
  const offset = calMonthOffset.value[wid] ?? 0
  const now = new Date()
  const pad = n => String(n).padStart(2, '0')
  const base = new Date(now.getFullYear(), now.getMonth() + offset, 1)
  return {
    plugins: [dayGridPlugin, interactionPlugin],
    initialView: 'dayGridMonth',
    locale: koLocale,
    headerToolbar: false,
    height: 'auto',
    dayMaxEvents: 3,
    initialDate: `${base.getFullYear()}-${pad(base.getMonth()+1)}-01`,
    events: fcEvents,
    eventContent: dbEventContent,
    dayCellContent: (arg) => dbDayCellContent(wid, arg),
    datesSet: (info) => { calFcLabels.value[wid] = info.view.title },
  }
}

function getWeekFcOptions(widget) {
  const wid = widget.id
  const rawEvents = calWeekEvents.value[wid] !== undefined
    ? calWeekEvents.value[wid] : (widget.calendarEvents || [])
  const fcEvents = buildFcData(rawEvents, wid)
  const offset = calWeekOffset.value[wid] ?? 0
  const today = new Date()
  const sunday = new Date(today)
  sunday.setDate(today.getDate() - today.getDay() + offset * 7)
  const pad = n => String(n).padStart(2, '0')
  const initialDate = `${sunday.getFullYear()}-${pad(sunday.getMonth()+1)}-${pad(sunday.getDate())}`
  return {
    plugins: [dayGridPlugin, interactionPlugin],
    initialView: 'dayGridWeek',
    locale: koLocale,
    headerToolbar: false,
    height: 'auto',
    initialDate,
    events: fcEvents,
    eventContent: dbEventContent,
    dayCellContent: (arg) => dbDayCellContent(wid, arg),
    datesSet: (info) => { calFcLabels.value[wid] = info.view.title },
  }
}

async function navigateWeekFc(widget, delta) {
  const wid = widget.id
  const newOffset = (calWeekOffset.value[wid] ?? 0) + delta
  calWeekOffset.value[wid] = newOffset
  calWeekLoading.value[wid] = true
  try {
    const res = await api.get(`/dashboard/widgets/${wid}`, { params: { weekOffset: newOffset } })
    calWeekEvents.value[wid] = res.data.data?.calendarEvents || []
  } catch {
    calWeekEvents.value[wid] = []
  } finally {
    calWeekLoading.value[wid] = false
  }
}

async function navigateMonthFc(widget, delta) {
  const wid = widget.id
  const newOffset = (calMonthOffset.value[wid] ?? 0) + delta
  calMonthOffset.value[wid] = newOffset
  calMonthLoading.value[wid] = true
  try {
    const res = await api.get(`/dashboard/widgets/${wid}`, { params: { weekOffset: newOffset } })
    calMonthEvents.value[wid] = res.data.data?.calendarEvents || []
  } catch {
    calMonthEvents.value[wid] = []
  } finally {
    calMonthLoading.value[wid] = false
  }
}

const WIDGET_LABELS = {
  WELCOME: '웰컴 카드',
  RECENT_POSTS: '최신 글', RECENT_GALLERY: '갤러리 최신글',
  CALENDAR_WEEKLY: '캘린더 (주간)', CALENDAR_MONTHLY: '캘린더 (월간)',
  IMAGE_SLIDER: '슬라이더',
  IMAGE_GRID: '이미지', LINK_LIST: '링크', QUICK_LINKS: '바로가기',
  MEMBER_STATS: '회원 현황', VISIT_STATS: '접속 통계', CUSTOM: '커스텀',
}
function widgetTypeLabel(t) { return WIDGET_LABELS[t] || t }

function isFullWidget(t) {
  return ['IMAGE_SLIDER', 'IMAGE_GRID', 'WELCOME'].includes(t)
}

function parseCalendarId(widget) {
  try {
    const cfg = widget.extraConfig ? JSON.parse(widget.extraConfig) : {}
    return cfg.calendarId || null
  } catch { return null }
}

// ===== 경로 헬퍼 =====
function boardListPath(widget, post = null) {
  const boardId = widget.targetBoardId || post?.boardId
  if (!boardId) return null
  const menu = menuStore.findBoardMenu(boardId)
  return menu ? `/menu/${menu.id}` : `/board/${boardId}`
}
function postPath(widget, post) {
  const basePath = boardListPath(widget, post)
  return basePath ? `${basePath}/posts/${post.id}` : '/'
}
function parseConfig(widget) {
  try { return widget.extraConfig ? JSON.parse(widget.extraConfig) : {} }
  catch { return {} }
}

/** 커스텀 위젯 HTML: img src를 썸네일로 교체 */
function toCustomHtml(html) {
  if (!html) return html
  return html.replace(/(<img[^>]+src=["'])\/api\/files\/inline\//g, '$1/api/files/inline-thumb/')
}

/** 인라인 이미지 URL → 대시보드용 소형 썸네일 URL */
function toSmallThumb(url) {
  if (!url) return url
  if (url.includes('/api/files/inline/')) return url.replace('/api/files/inline/', '/api/files/inline-thumb/')
  return url
}

function formatDate(d) {
  if (!d) return ''
  const dt  = new Date(d)
  const now = new Date()
  if (dt.toDateString() === now.toDateString()) return dt.toTimeString().slice(0, 5)
  const gap = Math.floor((now - dt) / 86400000)
  if (gap === 1) return '1일 전'
  if (gap <= 3) return `${gap}일 전`
  return `${dt.getMonth() + 1}.${String(dt.getDate()).padStart(2, '0')}`
}

async function loadWidgetData(layout) {
  // DIVIDER, WELCOME은 추가 데이터 불필요
  if (layout.widgetType === 'DIVIDER' || layout.widgetType === 'WELCOME') return
  widgetLoading.value[layout.id] = true
  try {
    const res = await api.get(`/dashboard/widgets/${layout.id}`)
    widgetData.value[layout.id] = res.data.data
  } catch {
    // 데이터 로드 실패해도 레이아웃은 유지
  } finally {
    widgetLoading.value[layout.id] = false
  }
}

onMounted(async () => {
  await menuStore.fetchMenus()
  const params = dashboardMenuId.value ? { menuId: dashboardMenuId.value } : {}
  try {
    // 1단계: 레이아웃(메타데이터) 빠르게 로드 → 즉시 화면 표시
    const res = await api.get('/dashboard/widgets/layout', { params })
    layouts.value = res.data.data || []
  } finally {
    layoutLoading.value = false
  }
  // 2단계: 각 위젯 데이터를 병렬로 로드
  Promise.all(layouts.value.map(loadWidgetData))
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.dashboard-menu {
  padding: 0;
}
/* 메뉴 대시보드: 위젯 그리드 내부 패딩 */
.dashboard-menu .widget-area {
  padding: 16px 20px 20px;
  gap: 12px;
}
/* 메뉴 대시보드: 갤러리 카드처럼 테두리 없이 그림자로 구분 */
.dashboard-menu .wcard {
  background: var(--surface);
  border: 0.5px solid var(--border2);
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
}
.dashboard-menu .wcard:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
  background: var(--surface);
  border: 0.5px solid var(--border2);
}
/* 메뉴 대시보드: 로딩/빈 상태 */
.dashboard-menu .loading-area,
.dashboard-menu .empty-area {
  padding: 60px 20px;
}

/* ===== 웰컴 카드 ===== */
.welcome-card {
  position: relative;
  background: linear-gradient(135deg, #4f6ef7 0%, #7c4ff7 45%, #e05fc4 80%, #f97040 100%);
  border-radius: var(--radius-sm);
  padding: 28px 32px;
  overflow: hidden;
  cursor: default;
  isolation: isolate;
}
.welcome-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 10% 80%, rgba(255,255,255,0.13) 0%, transparent 45%),
    radial-gradient(circle at 90% 15%, rgba(255,255,255,0.1) 0%, transparent 40%);
  z-index: 0;
}
.welcome-glow {
  position: absolute;
  inset: 0;
  pointer-events: none;
  transition: opacity 0.3s;
  z-index: 1;
}
.welcome-content {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.welcome-name {
  font-size: 20px;
  font-weight: 800;
  color: #fff;
  letter-spacing: -0.4px;
}
.welcome-highlight {
  display: inline-block;
  background: rgba(255,255,255,0.22);
  border-radius: 7px;
  padding: 2px 10px;
}
.welcome-sub {
  font-size: 14px;
  color: rgba(255,255,255,0.8);
  margin-top: 7px;
}
.welcome-right {
  text-align: right;
  flex-shrink: 0;
}
.welcome-date-line {
  font-size: 13.5px;
  color: rgba(255,255,255,0.8);
  line-height: 1.6;
}
.welcome-time-line {
  font-size: 28px;
  font-weight: 800;
  color: #fff;
  letter-spacing: -0.5px;
  line-height: 1.2;
}

/* ===== 위젯 그리드 ===== */
.widget-area {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.widget-full  { grid-column: 1 / -1; min-width: 0; }
.widget-half  { grid-column: span 1; min-width: 0; }

/* ===== 공통 위젯 카드 ===== */
.wcard {
  background: var(--surface);
  border-radius: var(--radius-sm);
  border: 0.5px solid var(--border2);
  box-shadow: var(--shadow);
  padding: 20px 22px;
  height: 100%;
  box-sizing: border-box;
  will-change: transform;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}
.wcard:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
  border-color: var(--border);
}
.wcard-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.wcard-title {
  font-size: 20px;
  font-weight: 800;
  color: var(--t1);
  letter-spacing: -0.2px;
}
.wcard-more {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 13px;
  font-weight: 600;
  color: var(--accent-t);
  text-decoration: none;
  transition: opacity 0.15s;
}
.wcard-more:hover { opacity: 0.75; }
.wcard-head-left { display: flex; flex-direction: column; gap: 1px; }
.wcard-desc {
  font-size: 14px;
  color: var(--t3);
  margin: -10px 0 12px;
  line-height: 1.5;
}
.wcard-desc-inline {
  font-size: 13px;
  color: var(--t3);
  font-weight: 400;
}
.widget-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 0;
}
.divider-line {
  flex: 1;
  border: none;
  border-top: 1.5px solid var(--border2);
  margin: 0;
}
.divider-label {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  font-size: 13px;
  font-weight: 700;
  color: var(--t3);
  letter-spacing: 0.5px;
}
.divider-label::before,
.divider-label::after {
  content: '';
  flex: 1;
  border-top: 1.5px solid var(--border2);
}
.wcard-sub-date {
  font-size: 12px;
  color: var(--t4);
  font-weight: 500;
}
.wcard-week-range {
  font-size: 12px;
  color: var(--t3);
  font-weight: 600;
  min-width: 80px;
  text-align: center;
}
.cal-week-nav {
  display: flex;
  align-items: center;
  gap: 2px;
}
.cal-nav-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  border-radius: 6px;
  color: var(--t3);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.14s, color 0.14s;
  padding: 0;
  flex-shrink: 0;
}
.cal-nav-btn:hover:not(:disabled) { background: var(--surface2); color: var(--t1); }
.cal-nav-btn:disabled { opacity: 0.35; cursor: default; }
.cal-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 80px;
  color: var(--t4);
  font-size: 20px;
}
.wcard-empty {
  font-size: 14px;
  color: var(--t3);
  text-align: center;
  padding: 20px 0;
}

/* ===== 최신 글 ===== */
.post-list { display: flex; flex-direction: column; min-width: 0; overflow: hidden; }
.post-title-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}
.post-board-tag {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 700;
  color: var(--accent-t);
  background: var(--accent-bg);
  border-radius: 5px;
  padding: 1px 6px;
  white-space: nowrap;
}
.post-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 0;
  border-bottom: 0.5px solid var(--border2);
  gap: 12px;
  text-decoration: none;
  transition: background 0.12s;
  border-radius: 4px;
  min-width: 0;
}
.post-row:first-child { padding-top: 0; }
.post-row:last-child  { border-bottom: none; padding-bottom: 0; }
.post-row:hover .post-title-txt { color: var(--accent-t); }
.post-title-txt {
  font-size: 14.5px;
  font-weight: 500;
  color: var(--t1);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.15s;
}
.post-date-txt {
  font-size: 12.5px;
  color: var(--t3);
  flex-shrink: 0;
}

/* ===== 갤러리 최신 글 ===== */
.gallery-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.gallery-item {
  text-decoration: none;
  display: flex;
  flex-direction: column;
  gap: 6px;
  border-radius: 10px;
  overflow: hidden;
  will-change: transform;
  transition: transform 0.18s ease;
}
.gallery-item:hover { transform: translateY(-2px); }
.gallery-thumb {
  position: relative;
  aspect-ratio: 4/3;
  background: var(--surface2);
  border-radius: 8px;
  overflow: hidden;
  border: 0.5px solid var(--border2);
  display: flex;
  align-items: center;
  justify-content: center;
}
.gw-spinner {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}
.gw-ring {
  width: 22px;
  height: 22px;
  border: 2.5px solid var(--border2);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  will-change: transform;
}
.gw-img { width: 100%; height: 100%; object-fit: cover; opacity: 0; transition: opacity 0.3s; display: block; }
.gw-img.loaded { opacity: 1; }
.gallery-no-img {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: var(--t4);
}
.gallery-title {
  font-size: 12.5px;
  font-weight: 500;
  color: var(--t2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 0 2px;
}

/* ===== 대시보드 FullCalendar 위젯 ===== */
.db-cal-wrap { padding: 4px 0 6px; }
.db-cal-wrap :deep(.fc) { font-family: inherit; }
.db-cal-wrap :deep(.fc-theme-standard td),
.db-cal-wrap :deep(.fc-theme-standard th),
.db-cal-wrap :deep(.fc-theme-standard .fc-scrollgrid),
.db-cal-wrap :deep(.fc-theme-standard .fc-scrollgrid-section > td) {
  border-color: var(--border) !important;
}
.db-cal-wrap :deep(.fc-daygrid-day) { background: var(--surface) !important; }
.db-cal-wrap :deep(.fc-day-other) { background: var(--surface2) !important; }
.db-cal-wrap :deep(.fc-day-today) { background: var(--accent-bg) !important; }
.db-cal-wrap :deep(.fc-col-header-cell) {
  background: var(--surface2) !important;
  font-size: 12px; font-weight: 500; color: var(--t2); padding: 6px 0;
}
.db-cal-wrap :deep(.fc-col-header-cell a) { color: var(--t2) !important; }
.db-cal-wrap :deep(.fc-day-sat.fc-col-header-cell a) { color: #2563eb !important; }
.db-cal-wrap :deep(.fc-day-sun.fc-col-header-cell a) { color: #dc2626 !important; }
.db-cal-wrap :deep(.fc-toolbar) { display: none; }
.db-cal-wrap :deep(.fc-more-link) { color: var(--accent-t) !important; font-size: 11px; }

/* 날짜 셀 헤더: flex 전체 너비 */
.db-cal-wrap :deep(.fc-daygrid-day-top) {
  display: flex !important;
  flex-direction: row !important;
  align-items: center !important;
  width: 100% !important;
}
.db-cal-wrap :deep(.fc-daygrid-day-number) {
  float: none !important;
  display: flex !important;
  flex: 1 !important;
  width: 100% !important;
  padding: 4px 6px !important;
  box-sizing: border-box !important;
}

/* 커스텀 날짜 셀 내부 */
.db-cal-wrap :deep(.fc-day-custom) {
  display: flex;
  align-items: center;
  width: 100%;
}
.db-cal-wrap :deep(.fc-day-num) {
  font-size: 12px;
  color: var(--t2);
  line-height: 1.4;
  flex-shrink: 0;
  margin-left: auto;
}
.db-cal-wrap :deep(.fc-day-num.sat) { color: #2563eb; }
.db-cal-wrap :deep(.fc-day-num.red) { color: #dc2626; }
.db-cal-wrap :deep(.fc-day-hol) {
  font-size: 11px;
  color: rgba(100, 100, 100, 0.63);
  font-weight: 500;
  letter-spacing: -0.5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  text-align: left;
}
.db-cal-wrap :deep(.fc-day-hol.sat) { color: rgba(37, 99, 235, 0.63); }
.db-cal-wrap :deep(.fc-day-hol.red) { color: rgba(220, 38, 38, 0.63); font-weight: 600; }
.db-cal-wrap :deep(.fc-event) { border-radius: 4px; border: none; font-size: 12px; cursor: pointer; }

/* 이벤트 컨테이너 투명화 */
.db-cal-wrap :deep(.fc-daygrid-event) {
  background: transparent !important;
  border: none !important;
  padding: 0 !important;
  margin-bottom: 2px !important;
  border-radius: 0 !important;
  overflow: visible;
}

/* 종일 이벤트 스타일 */
.db-cal-wrap :deep(.fc-ev-custom) {
  display: flex;
  flex-direction: column;
  border-left: 3px solid var(--ev-color, var(--accent));
  background: color-mix(in srgb, var(--ev-color, var(--accent)) 12%, var(--surface));
  border-radius: 3px;
  padding: 2px 4px;
  width: 100%;
  box-sizing: border-box;
  gap: 1px;
  cursor: pointer;
}
.db-cal-wrap :deep(.fc-ev-title) {
  font-size: 12px;
  color: var(--t1);
  font-weight: 500;
  line-height: 1.35;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 시간 이벤트 구글 스타일 */
.db-cal-wrap :deep(.fc-ev-goog) {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 1px 4px;
  border-radius: 3px;
  width: 100%;
  box-sizing: border-box;
  cursor: pointer;
  min-width: 0;
}
.db-cal-wrap :deep(.fc-ev-goog:hover) {
  background: color-mix(in srgb, var(--ev-color, var(--accent)) 10%, var(--surface));
}
.db-cal-wrap :deep(.fc-ev-goog-dot) {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--ev-color, var(--accent));
  flex-shrink: 0;
}
.db-cal-wrap :deep(.fc-ev-goog-label) {
  font-size: 12px;
  color: var(--t1);
  font-weight: 400;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.6;
}

/* ===== 링크 모음 ===== */
.link-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}
.link-chip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 11px 14px;
  border: 0.5px solid var(--border);
  border-radius: var(--radius-xs);
  background: var(--surface2);
  color: var(--t1);
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  transition: var(--transition);
}
.link-chip:hover {
  border-color: var(--accent);
  background: var(--accent-bg);
  color: var(--accent-t);
}
.link-arrow { font-size: 13px; color: var(--t3); flex-shrink: 0; transition: color 0.15s; }
.link-chip:hover .link-arrow { color: var(--accent-t); }

/* ===== 바로가기 ===== */
.quick-links-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}
.quick-link-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px 14px;
  border-radius: 12px;
  background: var(--surface2);
  border: 0.5px solid var(--border);
  text-decoration: none;
  transition: var(--transition);
  cursor: pointer;
}
.quick-link-item:hover {
  background: var(--accent-bg);
  border-color: var(--accent);
  transform: translateY(-2px);
}
.ql-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: var(--surface);
  border: 0.5px solid var(--border2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: var(--accent-t);
  transition: var(--transition);
}
.quick-link-item:hover .ql-icon-wrap {
  background: var(--accent-t);
  color: #fff;
  border-color: transparent;
}
.ql-label {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--t2);
  text-align: center;
  line-height: 1.3;
  transition: color 0.15s;
}
.quick-link-item:hover .ql-label { color: var(--accent-t); }

/* ===== 이미지 그리드 ===== */
.img-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}
.img-grid-item {
  position: relative;
  aspect-ratio: 3/2;
  border-radius: 10px;
  overflow: hidden;
  display: block;
  text-decoration: none;
  background: var(--surface2);
  border: 0.5px solid var(--border2);
  cursor: pointer;
  will-change: transform;
  transition: transform 0.18s ease;
}
.img-grid-item:hover { transform: scale(1.02); }
.img-grid-photo { width: 100%; height: 100%; object-fit: cover; display: block; }
.img-grid-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: var(--t4);
}
.img-grid-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 28px 10px 10px;
  background: linear-gradient(to top, rgba(0,0,0,0.65) 0%, transparent 100%);
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.img-grid-title {
  font-size: 12.5px;
  font-weight: 700;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.img-grid-desc {
  font-size: 11px;
  color: rgba(255,255,255,0.8);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== 커스텀 위젯 본문 ===== */
.custom-body { font-size: 14.5px; line-height: 1.7; color: var(--t1); }
.custom-body :deep(p)  { margin: 0 0 10px; }
.custom-body :deep(p:last-child) { margin-bottom: 0; }
.custom-body :deep(h1), .custom-body :deep(h2), .custom-body :deep(h3) { font-weight: 700; line-height: 1.3; margin: 14px 0 8px; }
.custom-body :deep(h1) { font-size: 20px; }
.custom-body :deep(h2) { font-size: 17px; }
.custom-body :deep(h3) { font-size: 15px; }
.custom-body :deep(ul), .custom-body :deep(ol) { padding-left: 20px; margin: 8px 0; }
.custom-body :deep(li) { margin-bottom: 4px; }
.custom-body :deep(a)  { color: var(--accent-t); text-decoration: underline; }
.custom-body :deep(blockquote) { border-left: 3px solid var(--accent); padding-left: 12px; margin: 10px 0; color: var(--t2); }
.custom-body :deep(code) { background: var(--surface2); border-radius: 3px; padding: 1px 5px; font-size: 13px; }
.custom-body :deep(strong) { font-weight: 700; }
.custom-body :deep(em) { font-style: italic; }
.custom-body :deep(img) { max-width: 100%; height: auto; display: block; border-radius: 4px; }

/* ===== 회원 현황 ===== */
.mstats-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.mstat-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  background: var(--surface2);
  border-radius: 10px;
  border: 0.5px solid var(--border2);
}
.mstat-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}
.mstat-num {
  font-size: 22px;
  font-weight: 800;
  color: var(--t1);
  line-height: 1;
  letter-spacing: -0.5px;
}
.mstat-num.accent-red { color: #EF4444; }
.mstat-lbl {
  font-size: 13px;
  color: var(--t3);
  margin-top: 3px;
}

/* ===== 접속 통계 ===== */
.vstats-row {
  display: flex;
  align-items: stretch;
  gap: 0;
}
.vstat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 18px 8px;
}
.vstat-divider { width: 1px; background: var(--border2); flex-shrink: 0; }
.vstat-num {
  font-size: 32px;
  font-weight: 800;
  color: var(--t1);
  letter-spacing: -1px;
  line-height: 1;
}
.vstat-num.accent-blue { color: var(--accent-t); }
.vstat-lbl {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12.5px;
  color: var(--t3);
  font-weight: 500;
}
.vstat-lbl i { font-size: 13px; }

/* ===== 위젯 로딩 스켈레톤 ===== */
.wcard-loading {
  pointer-events: none;
}
.wcard-skeleton-body {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 80px;
  color: var(--t4);
}

/* ===== 로딩 / 빈 상태 ===== */
.loading-area {
  display: flex;
  justify-content: center;
  padding: 60px;
  font-size: 28px;
  color: var(--t3);
}
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 60px;
  color: var(--t3);
  font-size: 15px;
}
.empty-state i { font-size: 40px; }

@keyframes spin { to { transform: rotate(360deg); } }
.spinning { animation: spin 0.8s linear infinite; display: inline-block; will-change: transform; }

/* ===== 반응형 (모바일 토스 스타일) ===== */
@media (max-width: 768px) {
  .dashboard { gap: 0; background: var(--bg); }

  .welcome-card {
    padding: 24px 20px 28px;
    border-radius: 0 0 24px 24px;
    margin-bottom: 4px;
  }
  .welcome-content { flex-direction: column; align-items: flex-start; gap: 8px; }
  .welcome-right { text-align: left; }
  .welcome-time-line { font-size: 24px; }
  .welcome-name { font-size: 18px; }

  .widget-area {
    grid-template-columns: minmax(0, 1fr);
    gap: 12px;
    padding: 16px 14px 24px;
  }
  .widget-half { grid-column: 1; }

  .wcard {
    border-radius: 16px;
    border: none;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.07), 0 0 0 0.5px rgba(0, 0, 0, 0.06);
    padding: 18px 18px 16px;
  }
  .wcard:hover { transform: none; box-shadow: 0 2px 12px rgba(0, 0, 0, 0.07), 0 0 0 0.5px rgba(0, 0, 0, 0.06); }

  /* 메뉴 대시보드 모바일 */
  .dashboard-menu .widget-area {
    padding: 12px 14px 16px;
    gap: 10px;
  }
  .dashboard-menu .wcard {
    border-radius: 16px;
    background: var(--surface);
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
    border: 0.5px solid var(--border2);
  }
  .dashboard-menu .wcard:hover {
    transform: none;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
    border: 0.5px solid var(--border2);
  }

  .wcard-head { margin-bottom: 12px; }
  .wcard-title { font-size: 14px; color: var(--t2); font-weight: 700; }
  .wcard-more { font-size: 12px; }

  .post-row { padding: 10px 0; gap: 10px; }
  .post-title-txt { font-size: 14px; }
  .post-date-txt { font-size: 11.5px; }

  .gallery-grid { grid-template-columns: repeat(2, 1fr); gap: 8px; }
  .gallery-title { font-size: 12px; }

  .db-cal-wrap :deep(.fc-col-header-cell) { font-size: 10px; padding: 4px 0; }
  .db-cal-wrap :deep(.fc-day-num) { font-size: 10px; }
  .db-cal-wrap :deep(.fc-day-hol) { font-size: 9px; }
  .db-cal-wrap :deep(.fc-ev-title) { font-size: 10px; }
  .db-cal-wrap :deep(.fc-ev-goog-label) { font-size: 10px; }

.link-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
  .link-chip { padding: 10px 12px; font-size: 13px; border-radius: 12px; }

  .quick-links-grid { grid-template-columns: repeat(3, 1fr); gap: 8px; }
  .quick-link-item { padding: 12px 6px 10px; gap: 6px; }
  .ql-icon-wrap { width: 38px; height: 38px; font-size: 18px; }
  .ql-label { font-size: 11.5px; }

  .img-grid { grid-template-columns: repeat(2, 1fr); gap: 8px; }

  .vstats-row { flex-direction: column; gap: 0; }
  .vstat-divider { width: 100%; height: 0.5px; }
  .vstat-item { padding: 14px 8px; flex-direction: row; justify-content: space-between; }
  .vstat-num { font-size: 24px; }
  .vstat-lbl { font-size: 12px; }

  .mstat-item { padding: 10px 12px; }
  .mstat-num { font-size: 20px; }
}

@media (max-width: 480px) {
  .quick-links-grid { grid-template-columns: repeat(3, 1fr); }
  .img-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
