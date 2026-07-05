<template>
  <div class="mypage" v-loading="loading">
    <div class="mp-layout">
      <main class="mp-content">
        <!-- 프로필 통계 카드 -->
        <div class="mp-stat-card">
          <div class="mp-stat-avatar-wrap">
            <div class="mp-stat-avatar">
              <img
                v-if="member?.profileImageUrl && !myAvatarError"
                :src="member.profileImageUrl"
                class="avatar-img"
                alt="프로필 사진"
                @error="myAvatarError = true"
              />
              <span v-else>{{ member?.name?.charAt(0) || 'U' }}</span>
            </div>
            <label class="avatar-change-btn" title="사진 변경">
              <input ref="fileInputRef" type="file" accept="image/*" style="display:none" @change="handleFileChange" />
              <i class="ti ti-camera"></i>
            </label>
          </div>

          <div class="mp-stat-info">
            <div class="mp-stat-name-row">
              <span class="mp-stat-name">{{ member?.name || '-' }}</span>
            </div>
            <div class="mp-stat-sub">@{{ member?.username }} · {{ member?.email }}</div>
            <div v-if="selectedFile" class="avatar-upload-row">
              <span class="file-name-small">{{ selectedFile.name }}</span>
              <el-button size="small" type="primary" :loading="photoSaving" @click="handleUploadPhoto">업로드</el-button>
            </div>
          </div>

          <div class="mp-stat-numbers">
            <div class="mp-stat-num-item">
              <div class="mp-stat-num" style="color:var(--accent)">{{ member?.postCount ?? 0 }}</div>
              <div class="mp-stat-num-label">작성글</div>
            </div>
            <div class="mp-stat-num-item">
              <div class="mp-stat-num">{{ member?.commentCount ?? 0 }}</div>
              <div class="mp-stat-num-label">댓글</div>
            </div>
            <div class="mp-stat-num-item">
              <div class="mp-stat-num">{{ joinDuration }}</div>
              <div class="mp-stat-num-label">가입</div>
            </div>
          </div>
        </div>

        <div class="mp-title-row">
          <h1>{{ currentTab?.label }}</h1>
          <p>{{ currentTabSub }}</p>
        </div>

        <!-- 내 정보 -->
        <section v-if="activeTab === 'info'">
          <div class="mp-form-grid">
            <div class="mp-field">
              <label>아이디</label>
              <input :value="member?.username" disabled />
            </div>
            <div class="mp-field">
              <label>이름</label>
              <input v-model="infoForm.name" />
            </div>
            <div class="mp-field span-2">
              <label>이메일</label>
              <input :value="member?.email" disabled />
            </div>
            <div class="mp-field">
              <label>연락처</label>
              <input v-model="infoForm.phone" placeholder="010-0000-0000" />
            </div>
            <div class="mp-field">
              <label>성별</label>
              <select v-model="infoForm.gender">
                <option value="">선택</option>
                <option value="M">남성</option>
                <option value="F">여성</option>
              </select>
            </div>
            <div class="mp-field">
              <label>생년월일</label>
              <el-date-picker
                v-model="infoForm.birthDate"
                type="date"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width:100%"
              />
            </div>
            <div class="mp-field">
              <label>자택전화</label>
              <input v-model="infoForm.homePhone" />
            </div>
            <div class="mp-field span-2">
              <label>집주소</label>
              <input v-model="infoForm.address" />
            </div>
            <div class="mp-field">
              <label>하는 일</label>
              <input v-model="infoForm.occupation" />
            </div>
            <div class="mp-field">
              <label>직장 전화</label>
              <input v-model="infoForm.workPhone" />
            </div>
            <div class="mp-field span-2 mp-newsletter-row">
              <button
                type="button"
                class="mp-toggle"
                :class="{ on: infoForm.newsletterYn }"
                @click="infoForm.newsletterYn = !infoForm.newsletterYn"
              ><span class="mp-toggle-dot"></span></button>
              <span class="mp-newsletter-label">뉴스레터 수신 동의</span>
            </div>
          </div>
          <div class="mp-form-actions">
            <button class="mp-btn-primary" :disabled="saving" @click="handleUpdateInfo">{{ saving ? '저장 중...' : '저장' }}</button>
          </div>
        </section>

        <!-- 비밀번호 변경 -->
        <section v-else-if="activeTab === 'password'">
          <div v-if="isForcedPasswordChange" class="forced-pw-banner">
            <i class="ti ti-shield-lock"></i>
            관리자에 의해 비밀번호가 초기화되었습니다. 임시 비밀번호로 로그인하셨습니다.
            <strong>보안을 위해 지금 바로 새 비밀번호로 변경해주세요.</strong>
          </div>
          <el-form
            ref="pwFormRef"
            :model="pwForm"
            :rules="pwRules"
            label-position="top"
            class="mp-narrow-form"
          >
            <div class="mp-field">
              <label>현재 비밀번호</label>
              <el-input v-model="pwForm.currentPassword" type="password" show-password />
            </div>
            <div class="mp-field">
              <label>새 비밀번호</label>
              <el-input v-model="pwForm.newPassword" type="password" show-password placeholder="8자 이상" />
              <span class="mp-field-hint">영문·숫자·특수문자 조합 8자 이상</span>
            </div>
            <div class="mp-field">
              <label>새 비밀번호 확인</label>
              <el-input v-model="pwForm.newPasswordConfirm" type="password" show-password />
            </div>
            <div class="mp-form-actions">
              <button class="mp-btn-primary" :disabled="pwSaving" @click="handleUpdatePassword">{{ pwSaving ? '변경 중...' : '변경' }}</button>
            </div>
          </el-form>
        </section>

        <!-- 알림 설정 -->
        <section v-else-if="activeTab === 'notif'">
          <div class="mp-table">
            <div class="mp-table-row mp-table-header">
              <div class="mp-table-label"></div>
              <div class="mp-table-channels">
                <span>실시간</span>
                <span>이메일</span>
              </div>
            </div>
            <div v-for="pref in notifPrefs" :key="pref.notifType" class="mp-table-row">
              <div class="mp-table-label">{{ pref.label }}</div>
              <div class="mp-table-channels">
                <el-switch v-model="pref.systemEnabled" />
                <el-switch v-model="pref.emailEnabled" />
              </div>
            </div>
          </div>
          <div class="mp-form-actions">
            <button class="mp-btn-primary" :disabled="notifSaving" @click="saveNotifPrefs">{{ notifSaving ? '저장 중...' : '저장' }}</button>
          </div>

          <!-- 브라우저 푸시 알림 -->
          <div class="push-section">
            <h3>브라우저 푸시 알림</h3>
            <p class="push-desc">사이트를 닫아도 알림을 받을 수 있습니다. 브라우저가 Web Push를 지원하는 경우에만 동작합니다.</p>
            <div class="push-toggle-row">
              <span>{{ notifStore.isPushSubscribed ? '구독 중' : '구독 안 함' }}</span>
              <el-switch
                :model-value="notifStore.isPushSubscribed"
                :loading="pushLoading"
                active-text="켜기"
                inactive-text="끄기"
                @change="togglePush"
              />
            </div>
            <p v-if="pushError" class="push-error">{{ pushError }}</p>
          </div>
        </section>

        <!-- 내 문의 내역 -->
        <section v-else-if="activeTab === 'inquiries'">
          <div class="mp-section-actions">
            <button class="mp-btn-outline" @click="activeTab = 'new-inquiry'">
              <i class="ti ti-plus"></i>문의하기
            </button>
          </div>
          <div v-if="myInquiries.length === 0 && !myInquiryLoading" class="my-empty">
            <i class="ti ti-mail"></i>
            <span>접수한 문의가 없습니다.</span>
          </div>
          <div v-else class="my-inquiry-list" v-loading="myInquiryLoading">
            <div
              v-for="item in myInquiries" :key="item.id"
              class="my-inquiry-item"
              @click="openInquiryDetail(item)"
            >
              <div class="mi-top">
                <span class="mi-title">{{ item.title }}</span>
                <span :class="['mi-badge', statusBadge(item.status)]">{{ statusLabel(item.status) }}</span>
                <i class="ti ti-chevron-right mi-arrow"></i>
              </div>
              <div class="mi-meta">{{ fmtDate(item.createdAt) }}</div>
              <div v-if="item.replyContent" class="mi-reply-preview">
                <i class="ti ti-message-reply"></i>
                <span>{{ item.replyContent.slice(0, 60) }}{{ item.replyContent.length > 60 ? '...' : '' }}</span>
              </div>
              <div v-else class="mi-no-reply">
                <i class="ti ti-clock-hour-3"></i> 아직 답변이 등록되지 않았습니다.
              </div>
            </div>
          </div>
        </section>

        <!-- 내 제보 내역 -->
        <section v-else-if="activeTab === 'reports'">
          <div class="mp-section-actions">
            <button class="mp-btn-outline" @click="activeTab = 'new-report'">
              <i class="ti ti-bug"></i>오류/기능 제보
            </button>
          </div>
          <div v-if="myReports.length === 0 && !myReportLoading" class="my-empty">
            <i class="ti ti-bug"></i>
            <span>접수한 제보가 없습니다.</span>
          </div>
          <div v-else class="my-inquiry-list" v-loading="myReportLoading">
            <div
              v-for="item in myReports" :key="item.id"
              class="my-inquiry-item"
              @click="openInquiryDetail(item)"
            >
              <div class="mi-top">
                <span :class="['mi-type-badge', item.inquiryType === 'BUG_REPORT' ? 'type-bug' : 'type-feature']">
                  <i :class="`ti ${item.inquiryType === 'BUG_REPORT' ? 'ti-bug' : 'ti-sparkles'}`"></i>
                  {{ item.inquiryType === 'BUG_REPORT' ? '오류 제보' : '기능 제안' }}
                </span>
                <span class="mi-title">{{ item.title }}</span>
                <span :class="['mi-badge', statusBadge(item.status)]">{{ statusLabel(item.status) }}</span>
                <i class="ti ti-chevron-right mi-arrow"></i>
              </div>
              <div class="mi-meta">{{ fmtDate(item.createdAt) }}</div>
              <div v-if="item.replyContent" class="mi-reply-preview">
                <i class="ti ti-message-reply"></i>
                <span>{{ item.replyContent.slice(0, 60) }}{{ item.replyContent.length > 60 ? '...' : '' }}</span>
              </div>
              <div v-else class="mi-no-reply">
                <i class="ti ti-clock-hour-3"></i> 아직 답변이 등록되지 않았습니다.
              </div>
            </div>
          </div>
        </section>

        <!-- 문의 작성 -->
        <section v-else-if="activeTab === 'new-inquiry'">
          <div class="mp-section-actions">
            <button class="mp-btn-outline" @click="activeTab = 'inquiries'">
              <i class="ti ti-arrow-left"></i>내 문의 목록
            </button>
          </div>
          <el-form ref="inquiryFormRef" :model="inquiryForm" :rules="inquiryRules" label-position="top" class="mp-narrow-form">
            <div class="mp-field">
              <label>제목</label>
              <el-input v-model="inquiryForm.title" placeholder="문의 제목을 입력하세요." />
            </div>
            <div class="mp-field">
              <label>내용</label>
              <el-input v-model="inquiryForm.content" type="textarea" :rows="7" placeholder="문의 내용을 입력하세요." resize="none" />
            </div>
            <div class="mp-form-grid">
              <div class="mp-field">
                <label>연락처</label>
                <el-input v-model="inquiryForm.contactPhone" placeholder="010-0000-0000" />
              </div>
              <div class="mp-field">
                <label>이메일</label>
                <el-input v-model="inquiryForm.contactEmail" placeholder="example@email.com" />
              </div>
            </div>
            <div class="mp-form-actions">
              <button class="mp-btn-outline" type="button" @click="activeTab = 'inquiries'">취소</button>
              <button class="mp-btn-primary" :disabled="inquirySubmitting" @click="submitInquiry">
                <i class="ti ti-send"></i>{{ inquirySubmitting ? '접수 중...' : '문의 접수' }}
              </button>
            </div>
          </el-form>
        </section>

        <!-- 제보 작성 -->
        <section v-else-if="activeTab === 'new-report'">
          <div class="mp-section-actions">
            <button class="mp-btn-outline" @click="activeTab = 'reports'">
              <i class="ti ti-arrow-left"></i>내 제보 목록
            </button>
          </div>

          <!-- 유형 탭 -->
          <div class="report-type-tabs">
            <button :class="['rtype-btn', { active: reportForm.inquiryType === 'BUG_REPORT' }]" @click="reportForm.inquiryType = 'BUG_REPORT'">
              <i class="ti ti-bug"></i> 오류 제보
            </button>
            <button :class="['rtype-btn', { active: reportForm.inquiryType === 'FEATURE_REQUEST' }]" @click="reportForm.inquiryType = 'FEATURE_REQUEST'">
              <i class="ti ti-sparkles"></i> 기능 제안
            </button>
          </div>

          <!-- 가이드 -->
          <div class="report-guide">
            <div class="rg-title"><i class="ti ti-bulb"></i> {{ currentReportGuide.title }}</div>
            <ul class="rg-list">
              <li v-for="(item, i) in currentReportGuide.items" :key="i" v-html="item"></li>
            </ul>
          </div>

          <el-form ref="reportFormRef" :model="reportForm" :rules="reportRules" label-position="top" class="mp-narrow-form" style="margin-top:16px">
            <div class="mp-field">
              <label>제목</label>
              <el-input v-model="reportForm.title" :placeholder="currentReportGuide.titlePlaceholder" />
            </div>
            <div class="mp-field" v-if="reportForm.inquiryType === 'BUG_REPORT'">
              <label>오류 발생 페이지 / URL</label>
              <el-input v-model="reportForm.pageUrl" placeholder="예: 알려드려요 게시판 (https://...)" />
            </div>
            <div class="mp-field" v-if="reportForm.inquiryType === 'FEATURE_REQUEST'">
              <label>참고 서비스 또는 링크</label>
              <el-input v-model="reportForm.reference" placeholder="예: 구글 캘린더, https://..." />
            </div>
            <div class="mp-field">
              <label>내용</label>
              <el-input v-model="reportForm.content" type="textarea" :rows="8" :placeholder="currentReportGuide.contentPlaceholder" resize="none" />
            </div>
            <div class="mp-form-actions">
              <button class="mp-btn-outline" type="button" @click="activeTab = 'reports'">취소</button>
              <button class="mp-btn-primary" :disabled="reportSubmitting" @click="submitReport">
                <i class="ti ti-send"></i>
                {{ reportSubmitting ? '접수 중...' : (reportForm.inquiryType === 'BUG_REPORT' ? '오류 제보 접수' : '기능 제안 접수') }}
              </button>
            </div>
          </el-form>
        </section>

        <!-- 회원 탈퇴 -->
        <section v-else-if="activeTab === 'withdraw'">
          <div class="mp-danger-box">
            <div class="mp-danger-title"><i class="ti ti-alert-triangle"></i>회원 탈퇴 안내</div>
            <p class="mp-danger-desc">
              탈퇴 시 작성하신 게시글·댓글은 삭제되지 않으며, 동일 아이디로 재가입이 제한될 수 있습니다.
              진행하시려면 비밀번호를 입력해 주세요.
            </p>
            <div class="mp-field" style="margin-bottom:16px">
              <label>비밀번호 확인</label>
              <el-input v-model="withdrawPassword" type="password" show-password placeholder="현재 비밀번호를 입력하세요" />
            </div>
            <div class="mp-form-actions">
              <button class="mp-btn-danger" @click="handleWithdraw">회원 탈퇴</button>
            </div>
          </div>
        </section>
      </main>
    </div>
  </div>

  <!-- 문의 상세 모달 -->
  <el-dialog v-model="showInquiryDetail" width="560px" :show-close="false" destroy-on-close class="inquiry-detail-dlg">
    <template #header>
      <div class="idlg-header">
        <div class="idlg-header-left">
          <span v-if="selectedInquiry?.inquiryType === 'BUG_REPORT'" class="mi-type-badge type-bug">
            <i class="ti ti-bug"></i> 오류 제보
          </span>
          <span v-else-if="selectedInquiry?.inquiryType === 'FEATURE_REQUEST'" class="mi-type-badge type-feature">
            <i class="ti ti-sparkles"></i> 기능 제안
          </span>
          <span v-else class="mi-type-badge" style="background:var(--surface2);color:var(--t3)">
            <i class="ti ti-mail"></i> 문의
          </span>
          <span :class="['mi-badge', statusBadge(selectedInquiry?.status)]">{{ statusLabel(selectedInquiry?.status) }}</span>
        </div>
        <button class="idlg-close" @click="showInquiryDetail = false"><i class="ti ti-x"></i></button>
      </div>
      <div class="idlg-title">{{ selectedInquiry?.title }}</div>
      <div class="idlg-meta">{{ fmtDate(selectedInquiry?.createdAt) }}</div>
    </template>
    <div v-if="selectedInquiry" class="idlg-body">
      <div class="idlg-section-label">내용</div>
      <div class="idlg-content">{{ selectedInquiry.content }}</div>

      <template v-if="selectedInquiry.replyContent">
        <div class="idlg-section-label" style="margin-top:20px">
          <i class="ti ti-message-reply"></i> 답변
          <span class="idlg-reply-meta">{{ selectedInquiry.repliedByName || '담당자' }} · {{ fmtDate(selectedInquiry.repliedAt) }}</span>
        </div>
        <div class="idlg-reply-box">{{ selectedInquiry.replyContent }}</div>
      </template>
      <div v-else class="idlg-no-reply">
        <i class="ti ti-clock-hour-3"></i>
        <div>
          <div class="idlg-no-reply-title">아직 답변이 등록되지 않았습니다.</div>
          <div class="idlg-no-reply-sub">담당자 검토 후 순차적으로 답변드립니다.</div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import api from '@/api/axios'

const router = useRouter()
const route  = useRoute()
const authStore = useAuthStore()
const notifStore = useNotificationStore()
const isForcedPasswordChange = computed(() => !!authStore.member?.mustChangePassword)
const activeTab = ref(isForcedPasswordChange.value ? 'password' : (route.query.tab || 'info'))
const loading = ref(false)
const saving = ref(false)
const pwSaving = ref(false)
const member = ref(null)
const withdrawPassword = ref('')
const fileInputRef = ref(null)
const selectedFile = ref(null)
const photoSaving = ref(false)
const myAvatarError = ref(false)

const tabs = [
  { name: 'info',      label: '내 정보',      sub: '개인 정보를 수정합니다.' },
  { name: 'password',  label: '비밀번호 변경', sub: '보안을 위해 주기적으로 비밀번호를 변경해 주세요.' },
  { name: 'notif',     label: '알림 설정',     sub: '알림 유형별 수신 채널을 설정합니다.' },
  { name: 'inquiries', label: '내 문의 내역',  sub: '내가 남긴 문의와 답변 상태입니다.' },
  { name: 'reports',   label: '내 제보 내역',  sub: '오류 제보·기능 제안 내역입니다.' },
  { name: 'withdraw',  label: '회원 탈퇴',    sub: '계정을 삭제하면 되돌릴 수 없습니다.' }
]
const currentTab = computed(() => tabs.find(t => t.name === activeTab.value))
const currentTabSub = computed(() => {
  if (activeTab.value === 'new-inquiry') return '문의를 새로 작성합니다.'
  if (activeTab.value === 'new-report')  return '오류·제안 내용을 작성합니다.'
  return currentTab.value?.sub || ''
})

// 왼쪽 고정 사이드바(DefaultLayout)에서 탭을 바꾸면 쿼리스트링(route.query.tab)으로 전달됨
watch(() => route.query.tab, (tab) => {
  if (!tab) return
  if (isForcedPasswordChange.value && tab !== 'password') {
    router.replace({ name: 'MyPage', query: { tab: 'password' } })
    return
  }
  activeTab.value = tab
})

function formatJoinDuration(joinedAt) {
  if (!joinedAt) return '-'
  const days = Math.floor((Date.now() - new Date(joinedAt).getTime()) / 86400000)
  if (days < 1) return '오늘'
  if (days < 30) return `${days}일`
  const months = Math.floor(days / 30)
  if (months < 12) return `${months}개월`
  return `${Math.floor(months / 12)}년`
}
const joinDuration = computed(() => formatJoinDuration(member.value?.joinedAt))

// ===== 내 문의 / 제보 =====
const myInquiries = ref([])
const myReports   = ref([])
const myInquiryLoading = ref(false)
const myReportLoading  = ref(false)

async function fetchMyInquiries() {
  myInquiryLoading.value = true
  try {
    const r = await api.get('/inquiries/my', { params: { inquiryType: 'INQUIRY' } })
    myInquiries.value = r.data.data || []
  } finally { myInquiryLoading.value = false }
}

async function fetchMyReports() {
  myReportLoading.value = true
  try {
    const r = await api.get('/inquiries/my')
    myReports.value = r.data.data || []
  } finally { myReportLoading.value = false }
}

const showInquiryDetail = ref(false)
const selectedInquiry   = ref(null)

function openInquiryDetail(item) {
  selectedInquiry.value = item
  showInquiryDetail.value = true
}

// ===== 문의 작성 =====
const inquiryFormRef   = ref()
const inquirySubmitting = ref(false)
const inquiryForm = ref({ title: '', content: '', contactPhone: '', contactEmail: '' })
const inquiryRules = {
  title:   [{ required: true, message: '제목을 입력해주세요.' }],
  content: [{ required: true, message: '내용을 입력해주세요.' }],
}

watch(() => activeTab.value, (tab) => {
  if (tab === 'new-inquiry') {
    inquiryForm.value = {
      title: '', content: '',
      contactPhone: authStore.member?.phone || '',
      contactEmail: authStore.member?.email || '',
    }
  }
  if (tab === 'new-report') {
    reportForm.value = { inquiryType: 'BUG_REPORT', title: '', content: '', pageUrl: '', reference: '' }
  }
})

async function submitInquiry() {
  await inquiryFormRef.value?.validate(async (valid) => {
    if (!valid) return
    inquirySubmitting.value = true
    try {
      await api.post('/inquiries', {
        ...inquiryForm.value,
        inquiryType: 'INQUIRY',
        writerName: authStore.member?.name,
      })
      ElMessage.success('문의가 접수되었습니다.')
      activeTab.value = 'inquiries'
      fetchMyInquiries()
    } catch (e) {
      ElMessage.error(e.response?.data?.message || '접수에 실패했습니다.')
    } finally { inquirySubmitting.value = false }
  })
}

// ===== 제보 작성 =====
const reportFormRef   = ref()
const reportSubmitting = ref(false)
const reportForm = ref({ inquiryType: 'BUG_REPORT', title: '', content: '', pageUrl: '', reference: '' })
const reportRules = {
  title:   [{ required: true, message: '제목을 입력해주세요.' }],
  content: [{ required: true, message: '내용을 입력해주세요.' }],
}

const reportGuides = {
  BUG_REPORT: {
    title: '오류 제보 작성 가이드',
    items: [
      '오류가 발생한 <strong>페이지 또는 URL</strong>을 정확히 알려주세요.',
      '어떤 동작을 했을 때 오류가 발생했는지 <strong>순서대로</strong> 기술해 주세요.',
      '오류 메시지가 있다면 <strong>정확히 복사</strong>해서 붙여넣어 주세요.',
      '어떤 <strong>기기(PC/모바일)</strong>와 <strong>브라우저</strong>를 사용하는지 알려주세요.',
    ],
    titlePlaceholder: '예: 갤러리 게시판에서 사진 업로드 시 오류 발생',
    contentPlaceholder: '재현 순서:\n1. \n2. \n3. \n\n오류 메시지: \n\n사용 환경 (PC/모바일, 브라우저):',
  },
  FEATURE_REQUEST: {
    title: '기능 제안 작성 가이드',
    items: [
      '어떤 문제를 해결하기 위한 기능인지 <strong>배경과 이유</strong>를 설명해 주세요.',
      '원하는 기능을 <strong>구체적으로</strong> 설명해 주세요.',
      '다른 서비스에서 본 기능이라면 <strong>서비스명 또는 링크</strong>를 남겨주세요.',
      '<strong>사용 시나리오</strong>를 예시로 들어주시면 더 잘 이해할 수 있습니다.',
    ],
    titlePlaceholder: '예: 캘린더 일정에 참석 여부 응답 기능 추가',
    contentPlaceholder: '배경/이유:\n\n기능 설명:\n\n참고 서비스/링크:\n\n사용 시나리오:',
  },
}
const currentReportGuide = computed(() => reportGuides[reportForm.value.inquiryType])

async function submitReport() {
  await reportFormRef.value?.validate(async (valid) => {
    if (!valid) return
    reportSubmitting.value = true
    try {
      let content = reportForm.value.content
      if (reportForm.value.pageUrl)  content = `[오류 페이지]\n${reportForm.value.pageUrl}\n\n${content}`
      if (reportForm.value.reference) content = `[참고 서비스/링크]\n${reportForm.value.reference}\n\n${content}`
      await api.post('/inquiries', {
        title: reportForm.value.title,
        content,
        inquiryType: reportForm.value.inquiryType,
        writerName: authStore.member?.name,
        contactEmail: authStore.member?.email,
        contactPhone: authStore.member?.phone,
      })
      ElMessage.success('접수되었습니다.')
      activeTab.value = 'reports'
      fetchMyReports()
    } catch (e) {
      ElMessage.error(e.response?.data?.message || '접수에 실패했습니다.')
    } finally { reportSubmitting.value = false }
  })
}

function statusLabel(s) { return { RECEIVED: '접수', CHECKING: '확인중', DONE: '처리완료' }[s] || s }
function statusBadge(s) { return { RECEIVED: 'badge-received', CHECKING: 'badge-checking', DONE: 'badge-done' }[s] || '' }
function fmtDate(d) { if (!d) return '-'; return new Date(d).toLocaleDateString('ko-KR') }

// ===== 알림 설정 =====
const notifPrefs = ref([])
const notifSaving = ref(false)
const pushLoading = ref(false)
const pushError = ref('')

async function fetchNotifPrefs() {
  const res = await api.get('/notifications/prefs')
  notifPrefs.value = res.data.data || []
}

async function saveNotifPrefs() {
  notifSaving.value = true
  try {
    const payload = {}
    notifPrefs.value.forEach(p => {
      payload[p.notifType] = { system: p.systemEnabled, email: p.emailEnabled }
    })
    await api.put('/notifications/prefs', payload)
    ElMessage.success('알림 설정이 저장되었습니다.')
  } finally {
    notifSaving.value = false
  }
}

async function togglePush(val) {
  pushLoading.value = true
  pushError.value = ''
  try {
    if (val) {
      await notifStore.subscribePush()
      ElMessage.success('브라우저 푸시 알림 구독이 완료되었습니다.')
    } else {
      await notifStore.unsubscribePush()
      ElMessage.success('브라우저 푸시 알림 구독이 취소되었습니다.')
    }
  } catch (e) {
    pushError.value = e.message || '푸시 알림 설정 중 오류가 발생했습니다.'
  } finally {
    pushLoading.value = false
  }
}

const infoForm = ref({
  name: '', phone: '', gender: '', birthDate: '',
  homePhone: '', address: '', occupation: '', workPhone: '', newsletterYn: false
})

const pwForm = ref({ currentPassword: '', newPassword: '', newPasswordConfirm: '' })
const pwFormRef = ref()

const pwRules = {
  currentPassword: [{ required: true, message: '현재 비밀번호를 입력해주세요.' }],
  newPassword: [
    { required: true, message: '새 비밀번호를 입력해주세요.' },
    { min: 8, message: '8자 이상 입력해주세요.' }
  ],
  newPasswordConfirm: [
    { required: true, message: '비밀번호를 한 번 더 입력해주세요.' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwForm.value.newPassword) callback(new Error('비밀번호가 일치하지 않습니다.'))
        else callback()
      }
    }
  ]
}

async function fetchMember() {
  loading.value = true
  try {
    const res = await api.get('/auth/me')
    member.value = res.data.data
    const m = member.value
    infoForm.value = {
      name: m.name || '',
      phone: m.phone || '',
      gender: m.gender || '',
      birthDate: m.birthDate || '',
      homePhone: m.homePhone || '',
      address: m.address || '',
      occupation: m.occupation || '',
      workPhone: m.workPhone || '',
      newsletterYn: m.newsletterYn || false,
    }
  } finally {
    loading.value = false
  }
}

async function handleUpdateInfo() {
  saving.value = true
  try {
    await api.put('/auth/me', infoForm.value)
    await authStore.fetchMe()
    await fetchMember()
    ElMessage.success('정보가 저장되었습니다.')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '저장에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

async function handleUpdatePassword() {
  await pwFormRef.value?.validate(async (valid) => {
    if (!valid) return
    pwSaving.value = true
    try {
      const username = member.value?.username || authStore.member?.username || ''
      const hashPw = async (pw) => {
        const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(username + pw))
        return Array.from(new Uint8Array(buf)).map(b => b.toString(16).padStart(2, '0')).join('')
      }
      await api.put('/auth/me/password', {
        currentPassword: await hashPw(pwForm.value.currentPassword),
        newPassword: await hashPw(pwForm.value.newPassword),
      })
      ElMessage.success('비밀번호가 변경되었습니다.')
      pwForm.value = { currentPassword: '', newPassword: '', newPasswordConfirm: '' }
      // 강제 변경 플래그 해제 후 re-fetch
      await authStore.fetchMe()
      if (!authStore.member?.mustChangePassword) {
        activeTab.value = 'info'
      }
    } catch (e) {
      ElMessage.error(e.response?.data?.message || '변경에 실패했습니다.')
    } finally {
      pwSaving.value = false
    }
  })
}

async function handleWithdraw() {
  if (!withdrawPassword.value) {
    ElMessage.warning('비밀번호를 입력해주세요.')
    return
  }
  await ElMessageBox.confirm(
    '정말로 탈퇴하시겠습니까? 이 작업은 되돌릴 수 없습니다.',
    '회원 탈퇴',
    { type: 'error', confirmButtonText: '탈퇴', cancelButtonText: '취소' }
  )
  try {
    const username = member.value?.username || authStore.member?.username || ''
    const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(username + withdrawPassword.value))
    const hashedPw = Array.from(new Uint8Array(buf)).map(b => b.toString(16).padStart(2, '0')).join('')
    await api.delete('/auth/me', { data: { password: hashedPw } })
    ElMessage.success('탈퇴 처리되었습니다.')
    authStore.clearToken()
    router.push('/login')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '탈퇴에 실패했습니다.')
  }
}

async function handleFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (file.size > 20 * 1024 * 1024) { ElMessage.warning('파일 크기는 20MB 이하여야 합니다.'); return }
  try {
    selectedFile.value = await resizeProfileImage(file, 400, 200 * 1024)
  } catch {
    selectedFile.value = file
  }
  myAvatarError.value = false
}

function resizeProfileImage(file, maxPx, maxBytes) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    const url = URL.createObjectURL(file)
    img.onload = () => {
      URL.revokeObjectURL(url)
      const scale = Math.min(1, maxPx / Math.max(img.width, img.height))
      const w = Math.round(img.width * scale)
      const h = Math.round(img.height * scale)
      const canvas = document.createElement('canvas')
      canvas.width = w
      canvas.height = h
      canvas.getContext('2d').drawImage(img, 0, 0, w, h)
      // quality를 낮춰가며 maxBytes 이하가 될 때까지 시도
      let quality = 0.92
      const tryEncode = () => {
        canvas.toBlob(blob => {
          if (!blob) { reject(new Error('encode failed')); return }
          if (blob.size <= maxBytes || quality <= 0.3) {
            resolve(new File([blob], file.name.replace(/\.[^.]+$/, '.jpg'), { type: 'image/jpeg' }))
          } else {
            quality -= 0.1
            tryEncode()
          }
        }, 'image/jpeg', quality)
      }
      tryEncode()
    }
    img.onerror = reject
    img.src = url
  })
}

async function handleUploadPhoto() {
  if (!selectedFile.value) return
  photoSaving.value = true
  try {
    const form = new FormData()
    form.append('file', selectedFile.value)
    const res = await api.post('/auth/me/profile-image', form, { headers: { 'Content-Type': 'multipart/form-data' } })
    member.value = res.data.data
    myAvatarError.value = false
    await authStore.fetchMe()
    selectedFile.value = null
    ElMessage.success('프로필 사진이 변경되었습니다.')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '업로드에 실패했습니다.')
  } finally {
    photoSaving.value = false
  }
}

onMounted(() => {
  fetchMember()
  fetchNotifPrefs()
  notifStore.checkPushSubscribed()
  if (activeTab.value === 'inquiries') fetchMyInquiries()
  if (activeTab.value === 'reports')   fetchMyReports()
})

// 기존 watch와 통합 (new-inquiry/new-report는 위의 watch에서 처리)
watch(activeTab, (tab) => {
  if (tab === 'notif')     fetchNotifPrefs()
  if (tab === 'inquiries') fetchMyInquiries()
  if (tab === 'reports')   fetchMyReports()
}, { immediate: false })
</script>

<style scoped>
.mypage { color: var(--t1); }

.mp-layout {
  max-width: 920px;
}

/* ===== 프로필 통계 카드 ===== */
.mp-stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 22px;
  margin-bottom: 26px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 16px;
  box-shadow: var(--shadow-sm);
  flex-wrap: wrap;
}

.mp-stat-avatar-wrap { position: relative; flex-shrink: 0; }

.mp-stat-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: var(--accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 24px;
  letter-spacing: -0.03em;
  overflow: hidden;
}

.avatar-img { width: 100%; height: 100%; object-fit: cover; }

.avatar-change-btn {
  position: absolute;
  right: -2px;
  bottom: -2px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--surface);
  border: 1px solid var(--border-strong);
  color: var(--t2);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-sm);
  transition: var(--transition);
}
.avatar-change-btn:hover { color: var(--accent); }

.mp-stat-info { flex: 1; min-width: 180px; }

.mp-stat-name-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.mp-stat-name { font-size: 18px; font-weight: 800; letter-spacing: -0.02em; }

.mp-badge {
  font-size: 11px;
  font-weight: 700;
  color: var(--accent-t);
  background: var(--accent-bg);
  padding: 2px 9px;
  border-radius: 11px;
}

.mp-stat-sub { font-size: 13px; color: var(--t3); margin-top: 3px; }

.avatar-upload-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.file-name-small {
  font-size: 11px;
  color: var(--t3);
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mp-stat-numbers { display: flex; gap: 22px; padding-right: 4px; }
.mp-stat-num-item { text-align: center; }
.mp-stat-num { font-size: 19px; font-weight: 800; }
.mp-stat-num-label { font-size: 11.5px; color: var(--t3); margin-top: 2px; }

/* ===== 타이틀 ===== */
.mp-title-row { margin-bottom: 26px; }
.mp-title-row h1 { margin: 0 0 5px; font-size: 24px; font-weight: 800; letter-spacing: -0.03em; }
.mp-title-row p { margin: 0; font-size: 14px; color: var(--t3); }

/* ===== 폼 공통 ===== */
.mp-form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px 28px;
}

.mp-narrow-form { max-width: 480px; display: flex; flex-direction: column; gap: 18px; }

.mp-field { display: flex; flex-direction: column; gap: 7px; }
.mp-field.span-2 { grid-column: 1 / -1; }

.mp-field label { font-size: 13px; font-weight: 700; color: var(--t2); }
.mp-field-hint { font-size: 12px; color: var(--t3); }

.mp-field input,
.mp-field select {
  font-family: inherit;
  font-size: 14px;
  color: var(--t1);
  background: var(--bg);
  border: 1px solid var(--border-strong);
  border-radius: 9px;
  padding: 11px 13px;
  outline: none;
  transition: var(--transition);
}
.mp-field input:focus,
.mp-field select:focus { border-color: var(--accent); }
.mp-field input:disabled {
  color: var(--t3);
  background: var(--surface2);
  cursor: not-allowed;
}
.mp-field select { cursor: pointer; }

.mp-field :deep(.el-input__wrapper) {
  border-radius: 9px;
  box-shadow: 0 0 0 1px var(--border-strong) inset;
}
.mp-field :deep(.el-textarea__inner) {
  border-radius: 9px;
}

.mp-newsletter-row { flex-direction: row; align-items: center; gap: 9px; margin-top: 2px; }
.mp-newsletter-label { font-size: 14px; color: var(--t2); font-weight: 500; }

.mp-toggle {
  width: 42px;
  height: 24px;
  border-radius: 12px;
  border: none;
  background: var(--border-strong);
  position: relative;
  cursor: pointer;
  padding: 0;
  transition: background 0.15s;
  flex-shrink: 0;
}
.mp-toggle.on { background: var(--accent); }
.mp-toggle-dot {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fff;
  transition: left 0.15s;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.25);
}
.mp-toggle.on .mp-toggle-dot { left: 19px; }

.mp-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 28px;
  padding-top: 22px;
  border-top: 1px solid var(--border);
}
.mp-narrow-form .mp-form-actions,
.write-form .mp-form-actions { margin-top: 6px; padding-top: 0; border-top: none; }

.mp-btn-primary {
  border: none;
  background: var(--accent);
  color: #fff;
  font-weight: 600;
  font-size: 14px;
  padding: 11px 30px;
  border-radius: 9px;
  cursor: pointer;
  transition: var(--transition);
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.mp-btn-primary:hover:not(:disabled) { background: var(--accent-t); }
.mp-btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }

.mp-btn-outline {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid var(--border-strong);
  background: var(--surface);
  color: var(--t2);
  font-weight: 600;
  font-size: 13px;
  padding: 9px 16px;
  border-radius: 9px;
  cursor: pointer;
  transition: var(--transition);
}
.mp-btn-outline:hover { background: var(--surface2); color: var(--t1); }

.mp-btn-danger {
  border: none;
  background: var(--danger);
  color: #fff;
  font-weight: 600;
  font-size: 14px;
  padding: 11px 24px;
  border-radius: 9px;
  cursor: pointer;
}

.mp-section-actions { display: flex; justify-content: flex-end; margin-bottom: 16px; }

.forced-pw-banner {
  display: flex; align-items: flex-start; gap: 10px;
  background: #fff7ed; border: 1px solid #f97316; border-radius: 8px;
  padding: 14px 16px; margin-bottom: 20px; max-width: 480px;
  font-size: 13px; line-height: 1.6; color: #9a3412;
}
.forced-pw-banner i { font-size: 18px; flex-shrink: 0; margin-top: 1px; }
.forced-pw-banner strong { display: block; margin-top: 2px; font-weight: 700; }

/* 제보 유형 탭 */
.report-type-tabs { display: flex; gap: 8px; margin-bottom: 20px; }
.rtype-btn {
  display: flex; align-items: center; gap: 7px;
  padding: 7px 16px; border-radius: 9px;
  border: 1.5px solid var(--border-strong); background: var(--surface);
  color: var(--t2); font-size: 13px; font-weight: 600; cursor: pointer;
  transition: var(--transition);
}
.rtype-btn:hover { border-color: var(--accent); color: var(--accent); }
.rtype-btn.active { border-color: var(--accent); background: var(--accent); color: #fff; }

/* 제보 가이드 */
.report-guide {
  background: var(--surface2); border-radius: 9px;
  padding: 14px 16px; font-size: 13px;
}
.rg-title {
  font-weight: 700; color: var(--t1); font-size: 13px;
  display: flex; align-items: center; gap: 6px;
  margin-bottom: 10px;
}
.rg-title i { color: var(--accent); }
.rg-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 6px; }
.rg-list li { display: flex; align-items: flex-start; gap: 6px; color: var(--t2); line-height: 1.5; }
.rg-list li::before { content: '✓'; color: var(--green); font-weight: 700; flex-shrink: 0; }
.rg-list li :deep(strong) { color: var(--t1); }

/* ===== 내 문의/제보 ===== */
.my-empty {
  display: flex; align-items: center; gap: 10px;
  padding: 40px 0; color: var(--t3); font-size: 14px;
  justify-content: center;
}
.my-empty i { font-size: 28px; }

.my-inquiry-list {
  border: 1px solid var(--border);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}
.my-inquiry-item {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
  background: var(--surface);
  display: flex; flex-direction: column; gap: 6px;
  cursor: pointer;
  transition: var(--transition);
}
.my-inquiry-item:last-child { border-bottom: none; }
.my-inquiry-item:hover { background: var(--surface2); }
.mi-top { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.mi-title { flex: 1; font-size: 14.5px; font-weight: 600; color: var(--t1); min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mi-arrow { font-size: 13px; color: var(--t3); flex-shrink: 0; }
.mi-meta { font-size: 12px; color: var(--t3); }
.mi-reply-preview {
  display: flex; align-items: flex-start; gap: 6px;
  font-size: 12px; color: #1E40AF;
  background: #EFF6FF; border-radius: 4px;
  padding: 5px 8px; margin-top: 2px;
}
.mi-reply-preview i { flex-shrink: 0; margin-top: 1px; }
.mi-badge {
  flex-shrink: 0; padding: 4px 11px; border-radius: 12px;
  font-size: 11.5px; font-weight: 700;
}
.badge-received { background: #FEF3C7; color: #92400E; }
.badge-checking  { background: #DBEAFE; color: #1E40AF; }
.badge-done      { background: #D1FAE5; color: #065F46; }
.mi-no-reply {
  margin-top: 6px;
  display: flex; align-items: center; gap: 5px;
  font-size: 12px; color: var(--t3);
}
.mi-no-reply i { font-size: 13px; }

.mi-type-badge {
  display: inline-flex; align-items: center; gap: 4px;
  flex-shrink: 0; padding: 3px 9px; border-radius: 6px;
  font-size: 11px; font-weight: 700;
}
.type-bug     { background: #FEE2E2; color: #991B1B; }
.type-feature { background: #EDE9FE; color: #5B21B6; }

/* ===== 문의 상세 다이얼로그 ===== */
.idlg-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 8px;
}
.idlg-header-left { display: flex; align-items: center; gap: 8px; }
.idlg-close {
  background: none; border: none; color: var(--t3);
  cursor: pointer; font-size: 16px; padding: 4px; line-height: 1;
  border-radius: 6px; transition: var(--transition);
}
.idlg-close:hover { background: var(--surface2); color: var(--t1); }
.idlg-title {
  font-size: 16px; font-weight: 700; color: var(--t1); line-height: 1.4; margin-bottom: 4px;
}
.idlg-meta { font-size: 12px; color: var(--t3); }

.idlg-body { display: flex; flex-direction: column; gap: 8px; }
.idlg-section-label {
  display: flex; align-items: center; gap: 5px;
  font-size: 12px; font-weight: 700; color: var(--t3); letter-spacing: 0.3px;
  margin-bottom: 4px;
}
.idlg-reply-meta {
  font-weight: 400; color: var(--t3); font-size: 11px; margin-left: 4px;
}
.idlg-content {
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 9px;
  font-size: 14px; color: var(--t1); line-height: 1.7;
  white-space: pre-wrap; min-height: 60px; max-height: 300px; overflow-y: auto;
}
.idlg-reply-box {
  background: #EFF6FF;
  border: 1px solid #BFDBFE;
  border-left: 4px solid #3B82F6;
  border-radius: 9px;
  padding: 12px 14px;
  font-size: 14px; color: #1e3a5f; line-height: 1.7; white-space: pre-wrap;
}
.idlg-no-reply {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 14px; background: var(--surface2);
  border-radius: 9px; color: var(--t3);
}
.idlg-no-reply i { font-size: 18px; flex-shrink: 0; margin-top: 1px; }
.idlg-no-reply-title { font-size: 13px; font-weight: 600; color: var(--t2); }
.idlg-no-reply-sub { font-size: 12px; color: var(--t3); margin-top: 2px; }

/* ===== 탈퇴 ===== */
.mp-danger-box {
  max-width: 560px;
  border: 1px solid var(--danger);
  border-radius: 14px;
  padding: 24px;
  background: var(--danger-bg);
}
.mp-danger-title {
  display: flex; align-items: center; gap: 9px;
  font-size: 16px; font-weight: 800; color: var(--danger);
  margin-bottom: 10px;
}
.mp-danger-desc { margin: 0 0 16px; font-size: 14px; line-height: 1.7; color: var(--t2); }

/* ===== 알림 설정 ===== */
.mp-table {
  border: 1px solid var(--border);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
  margin-bottom: 24px;
}
.mp-table-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 13px 20px;
  border-bottom: 1px solid var(--border);
  background: var(--surface);
}
.mp-table-row:last-child { border-bottom: none; }
.mp-table-header { background: var(--surface2); font-size: 12.5px; font-weight: 700; color: var(--t3); }
.mp-table-label { flex: 1; font-size: 14px; color: var(--t1); }
.mp-table-channels { display: flex; gap: 40px; align-items: center; text-align: center; }
.mp-table-channels span { width: 68px; display: inline-block; }

.push-section {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid var(--border);
}
.push-section h3 { margin: 0 0 6px; font-size: 15px; }
.push-desc { font-size: 13px; color: var(--t3); margin: 0 0 14px; }
.push-toggle-row {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 14px;
}
.push-error {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--danger);
}

@media (max-width: 768px) {
  .mp-stat-card { padding: 16px; }
  .mp-stat-numbers { width: 100%; justify-content: space-around; padding-right: 0; }
  .mp-form-grid { grid-template-columns: 1fr; }
}
</style>
