<template>
  <div class="mypage" v-loading="loading">
    <div class="mp-layout">
      <!-- 왼쪽: 프로필 카드 -->
      <aside class="mp-profile">
        <div class="profile-avatar-wrap">
          <div class="profile-avatar">
            <img
              v-if="member?.profileImageUrl && !myAvatarError"
              :src="member.profileImageUrl"
              class="avatar-img"
              alt="프로필 사진"
              @error="myAvatarError = true"
            />
            <div v-else class="avatar-placeholder">{{ member?.name?.charAt(0) || 'U' }}</div>
          </div>
          <label class="avatar-change-btn" title="사진 변경">
            <input ref="fileInputRef" type="file" accept="image/*" style="display:none" @change="handleFileChange" />
            <i class="ti ti-camera"></i>
          </label>
        </div>
        <div v-if="selectedFile" class="avatar-upload-row">
          <span class="file-name-small">{{ selectedFile.name }}</span>
          <el-button size="small" type="primary" :loading="photoSaving" @click="handleUploadPhoto">업로드</el-button>
        </div>

        <div class="profile-name">{{ member?.name || '-' }}</div>
        <div class="profile-username">@{{ member?.username }}</div>
        <div class="profile-email">{{ member?.email }}</div>

        <div class="profile-badges">
          <span v-for="g in member?.groups" :key="g.id" class="profile-badge">{{ g.name }}</span>
        </div>

        <div class="profile-nav">
          <button
            v-for="tab in tabs"
            :key="tab.name"
            :class="['pnav-item', { active: activeTab === tab.name }]"
            @click="tab.name === 'messages' ? router.push('/messages') : (activeTab = tab.name)"
          >
            <i :class="`ti ${tab.icon}`"></i>
            {{ tab.label }}
          </button>
        </div>
      </aside>

      <!-- 오른쪽: 컨텐츠 -->
      <main class="mp-content">
        <!-- 내 정보 -->
        <section v-if="activeTab === 'info'">
          <div class="section-head">
            <h2>내 정보</h2>
            <p>개인 정보를 수정합니다.</p>
          </div>
          <el-form :model="infoForm" label-position="top">
            <div class="form-row">
              <el-form-item label="아이디">
                <el-input :value="member?.username" disabled />
              </el-form-item>
              <el-form-item label="이름">
                <el-input v-model="infoForm.name" />
              </el-form-item>
            </div>
            <el-form-item label="이메일">
              <el-input :value="member?.email" disabled />
            </el-form-item>
            <div class="form-row">
              <el-form-item label="연락처">
                <el-input v-model="infoForm.phone" placeholder="010-0000-0000" />
              </el-form-item>
              <el-form-item label="성별">
                <el-select v-model="infoForm.gender" placeholder="선택" style="width:100%">
                  <el-option value="M" label="남성" />
                  <el-option value="F" label="여성" />
                </el-select>
              </el-form-item>
            </div>
            <div class="form-row">
              <el-form-item label="생년월일">
                <el-date-picker
                  v-model="infoForm.birthDate"
                  type="date"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  style="width:100%"
                />
              </el-form-item>
              <el-form-item label="자택전화">
                <el-input v-model="infoForm.homePhone" />
              </el-form-item>
            </div>
            <el-form-item label="집주소">
              <el-input v-model="infoForm.address" />
            </el-form-item>
            <div class="form-row">
              <el-form-item label="하는 일">
                <el-input v-model="infoForm.occupation" />
              </el-form-item>
              <el-form-item label="직장 전화">
                <el-input v-model="infoForm.workPhone" />
              </el-form-item>
            </div>
            <el-form-item>
              <el-checkbox v-model="infoForm.newsletterYn">뉴스레터 수신 동의</el-checkbox>
            </el-form-item>
            <div class="form-actions">
              <el-button type="primary" :loading="saving" @click="handleUpdateInfo">저장</el-button>
            </div>
          </el-form>
        </section>

        <!-- 비밀번호 변경 -->
        <section v-else-if="activeTab === 'password'">
          <div class="section-head">
            <h2>비밀번호 변경</h2>
            <p>현재 비밀번호를 확인한 후 새 비밀번호로 변경합니다.</p>
          </div>
          <el-form
            ref="pwFormRef"
            :model="pwForm"
            :rules="pwRules"
            label-position="top"
            style="max-width:420px"
          >
            <el-form-item label="현재 비밀번호" prop="currentPassword">
              <el-input v-model="pwForm.currentPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="새 비밀번호" prop="newPassword">
              <el-input v-model="pwForm.newPassword" type="password" show-password placeholder="8자 이상" />
            </el-form-item>
            <el-form-item label="새 비밀번호 확인" prop="newPasswordConfirm">
              <el-input v-model="pwForm.newPasswordConfirm" type="password" show-password />
            </el-form-item>
            <div class="form-actions">
              <el-button type="primary" :loading="pwSaving" @click="handleUpdatePassword">변경</el-button>
            </div>
          </el-form>
        </section>

        <!-- 알림 설정 -->
        <section v-else-if="activeTab === 'notif'">
          <div class="section-head">
            <h2>알림 설정</h2>
            <p>수신할 알림 유형과 채널을 선택합니다.</p>
          </div>
          <div class="notif-pref-list">
            <div class="notif-pref-row notif-pref-header">
              <div class="notif-pref-label"></div>
              <div class="notif-pref-channels">
                <span>시스템 알림</span>
                <span>이메일 알림</span>
              </div>
            </div>
            <div v-for="pref in notifPrefs" :key="pref.notifType" class="notif-pref-row">
              <div class="notif-pref-label">{{ pref.label }}</div>
              <div class="notif-pref-channels">
                <el-switch v-model="pref.systemEnabled" />
                <el-switch v-model="pref.emailEnabled" />
              </div>
            </div>
          </div>
          <div class="form-actions">
            <el-button type="primary" :loading="notifSaving" @click="saveNotifPrefs">저장</el-button>
          </div>
        </section>

        <!-- 내 문의 내역 -->
        <section v-else-if="activeTab === 'inquiries'">
          <div class="section-head-row">
            <div>
              <h2>내 문의 내역</h2>
              <p>내가 접수한 문의 목록입니다.</p>
            </div>
            <el-button @click="activeTab = 'new-inquiry'">
              <i class="ti ti-plus" style="margin-right:4px"></i>문의하기
            </el-button>
          </div>
          <div v-if="myInquiries.length === 0 && !myInquiryLoading" class="my-empty">
            <i class="ti ti-mail"></i>
            <span>접수한 문의가 없습니다.</span>
          </div>
          <div v-else class="my-inquiry-list" v-loading="myInquiryLoading">
            <div
              v-for="item in myInquiries" :key="item.id"
              class="my-inquiry-item clickable"
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
          <div class="section-head-row">
            <div>
              <h2>내 제보 내역</h2>
              <p>내가 접수한 오류 제보 및 기능 제안 목록입니다.</p>
            </div>
            <el-button @click="activeTab = 'new-report'">
              <i class="ti ti-bug" style="margin-right:4px"></i>오류/기능 제보
            </el-button>
          </div>
          <div v-if="myReports.length === 0 && !myReportLoading" class="my-empty">
            <i class="ti ti-bug"></i>
            <span>접수한 제보가 없습니다.</span>
          </div>
          <div v-else class="my-inquiry-list" v-loading="myReportLoading">
            <div
              v-for="item in myReports" :key="item.id"
              class="my-inquiry-item clickable"
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
          <div class="section-head-row">
            <div>
              <h2>문의하기</h2>
              <p>로그인 사용자로 문의를 접수합니다.</p>
            </div>
            <el-button @click="activeTab = 'inquiries'">
              <i class="ti ti-arrow-left" style="margin-right:4px"></i>내 문의 목록
            </el-button>
          </div>
          <el-form ref="inquiryFormRef" :model="inquiryForm" :rules="inquiryRules" label-position="top" class="write-form">
            <el-form-item label="제목" prop="title">
              <el-input v-model="inquiryForm.title" placeholder="문의 제목을 입력하세요." />
            </el-form-item>
            <el-form-item label="내용" prop="content">
              <el-input v-model="inquiryForm.content" type="textarea" :rows="7" placeholder="문의 내용을 입력하세요." resize="none" />
            </el-form-item>
            <div class="write-form-row">
              <el-form-item label="연락처">
                <el-input v-model="inquiryForm.contactPhone" placeholder="010-0000-0000" />
              </el-form-item>
              <el-form-item label="이메일">
                <el-input v-model="inquiryForm.contactEmail" placeholder="example@email.com" />
              </el-form-item>
            </div>
            <div class="write-form-actions">
              <el-button @click="activeTab = 'inquiries'">취소</el-button>
              <el-button type="primary" :loading="inquirySubmitting" @click="submitInquiry">
                <i class="ti ti-send" style="margin-right:4px"></i>문의 접수
              </el-button>
            </div>
          </el-form>
        </section>

        <!-- 제보 작성 -->
        <section v-else-if="activeTab === 'new-report'">
          <div class="section-head-row">
            <div>
              <h2>{{ reportForm.inquiryType === 'BUG_REPORT' ? '오류 제보' : '기능 제안' }}</h2>
              <p>{{ reportForm.inquiryType === 'BUG_REPORT' ? '시스템 오류를 제보합니다.' : '새로운 기능을 제안합니다.' }}</p>
            </div>
            <el-button @click="activeTab = 'reports'">
              <i class="ti ti-arrow-left" style="margin-right:4px"></i>내 제보 목록
            </el-button>
          </div>

          <!-- 유형 탭 -->
          <div class="report-type-tabs" style="margin-bottom:20px">
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

          <el-form ref="reportFormRef" :model="reportForm" :rules="reportRules" label-position="top" class="write-form" style="margin-top:16px">
            <el-form-item label="제목" prop="title">
              <el-input v-model="reportForm.title" :placeholder="currentReportGuide.titlePlaceholder" />
            </el-form-item>
            <el-form-item v-if="reportForm.inquiryType === 'BUG_REPORT'" label="오류 발생 페이지 / URL">
              <el-input v-model="reportForm.pageUrl" placeholder="예: 알려드려요 게시판 (https://...)" />
            </el-form-item>
            <el-form-item v-if="reportForm.inquiryType === 'FEATURE_REQUEST'" label="참고 서비스 또는 링크">
              <el-input v-model="reportForm.reference" placeholder="예: 구글 캘린더, https://..." />
            </el-form-item>
            <el-form-item label="내용" prop="content">
              <el-input v-model="reportForm.content" type="textarea" :rows="8" :placeholder="currentReportGuide.contentPlaceholder" resize="none" />
            </el-form-item>
            <div class="write-form-actions">
              <el-button @click="activeTab = 'reports'">취소</el-button>
              <el-button type="primary" :loading="reportSubmitting" @click="submitReport">
                <i class="ti ti-send" style="margin-right:4px"></i>
                {{ reportForm.inquiryType === 'BUG_REPORT' ? '오류 제보 접수' : '기능 제안 접수' }}
              </el-button>
            </div>
          </el-form>
        </section>

        <!-- 회원 탈퇴 -->
        <section v-else-if="activeTab === 'withdraw'">
          <div class="section-head">
            <h2>회원 탈퇴</h2>
            <p>탈퇴 처리 후 복구가 불가합니다.</p>
          </div>
          <el-alert
            title="탈퇴 전 꼭 확인해주세요"
            type="warning"
            :closable="false"
            show-icon
            style="margin-bottom:24px"
          >
            <template #default>
              <ul class="withdraw-notice">
                <li>탈퇴 후 작성한 게시글과 댓글은 삭제되지 않습니다.</li>
                <li>탈퇴 후 동일한 아이디로 재가입이 불가합니다.</li>
                <li>탈퇴 처리 후 복구가 불가합니다.</li>
              </ul>
            </template>
          </el-alert>
          <el-form label-position="top" style="max-width:420px">
            <el-form-item label="비밀번호 확인">
              <el-input v-model="withdrawPassword" type="password" show-password placeholder="현재 비밀번호를 입력하세요" />
            </el-form-item>
            <el-button type="danger" @click="handleWithdraw">회원 탈퇴</el-button>
          </el-form>
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
import api from '@/api/axios'

const router = useRouter()
const route  = useRoute()
const authStore = useAuthStore()
const activeTab = ref(route.query.tab || 'info')
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
  { name: 'info',      icon: 'ti-user',      label: '내 정보' },
  { name: 'password',  icon: 'ti-lock',      label: '비밀번호 변경' },
  { name: 'notif',     icon: 'ti-bell',      label: '알림 설정' },
  { name: 'messages',  icon: 'ti-messages',  label: '쪽지' },
  { name: 'inquiries', icon: 'ti-mail',      label: '내 문의 내역' },
  { name: 'reports',   icon: 'ti-bug',       label: '내 제보 내역' },
  { name: 'withdraw',  icon: 'ti-door-exit', label: '회원 탈퇴' }
]

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
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 0;
  min-height: 600px;
}

/* ===== 왼쪽 프로필 ===== */
.mp-profile {
  padding: 32px 20px 24px;
  border-right: 1px solid var(--border2);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.profile-avatar-wrap {
  position: relative;
  width: 88px;
  height: 88px;
  margin-bottom: 10px;
}

.profile-avatar {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  overflow: hidden;
  border: 2.5px solid var(--border);
  background: var(--surface2);
}

.avatar-img { width: 100%; height: 100%; object-fit: cover; }

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--accent);
  color: #fff;
  font-size: 32px;
  font-weight: 700;
}

.avatar-change-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--accent);
  color: #fff;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 2px solid var(--surface);
  transition: var(--transition);
}

.avatar-change-btn:hover { background: var(--accent-t); }

.avatar-upload-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: center;
}

.file-name-small {
  font-size: 11px;
  color: var(--t3);
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-name {
  font-size: 17px;
  font-weight: 800;
  color: var(--t1);
  text-align: center;
  margin-top: 4px;
}

.profile-username {
  font-size: 13px;
  color: var(--t3);
}

.profile-email {
  font-size: 12px;
  color: var(--t3);
  text-align: center;
  word-break: break-all;
  margin-bottom: 6px;
}

.profile-badges {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 4px;
  margin-bottom: 8px;
}

.profile-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 20px;
  background: var(--accent-bg);
  color: var(--accent-t);
  font-weight: 600;
}

.profile-nav {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 12px;
}

.pnav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 9px 12px;
  border: none;
  border-radius: var(--radius-xs);
  background: transparent;
  color: var(--t2);
  font-size: 14px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
  transition: var(--transition);
}

.pnav-item:hover { background: var(--surface2); color: var(--t1); }
.pnav-item.active { background: var(--accent-bg); color: var(--accent-t); }

/* ===== 오른쪽 컨텐츠 ===== */
.mp-content {
  padding: 32px 30px;
}

.section-head {
  margin-bottom: 24px;
}

.section-head h2 {
  font-size: 18px;
  font-weight: 800;
  color: var(--t1);
  margin-bottom: 4px;
}

.section-head p {
  font-size: 14px;
  color: var(--t3);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

/* 작성 폼 */
.write-form { display: flex; flex-direction: column; gap: 4px; }
.write-form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.write-form-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px; }

/* 제보 유형 탭 */
.report-type-tabs { display: flex; gap: 8px; }
.rtype-btn {
  display: flex; align-items: center; gap: 7px;
  padding: 7px 16px; border-radius: var(--radius-xs);
  border: 1.5px solid var(--border2); background: var(--surface);
  color: var(--t2); font-size: 13px; font-weight: 600; cursor: pointer;
  transition: var(--transition);
}
.rtype-btn:hover { border-color: var(--accent); color: var(--accent); }
.rtype-btn.active { border-color: var(--accent); background: var(--accent); color: #fff; }

/* 제보 가이드 */
.report-guide {
  background: var(--surface2); border-radius: var(--radius-xs);
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

/* 섹션 헤더 (제목 + 버튼) */
.section-head-row {
  display: flex; align-items: flex-start; justify-content: space-between; gap: 12px;
  margin-bottom: 16px;
}
.section-head-row h2 { font-size: 18px; font-weight: 800; color: var(--t1); margin-bottom: 4px; }
.section-head-row p  { font-size: 14px; color: var(--t3); }

/* ===== 내 문의/제보 ===== */
.my-empty {
  display: flex; align-items: center; gap: 10px;
  padding: 40px 0; color: var(--t4); font-size: 14px;
  justify-content: center;
}
.my-empty i { font-size: 28px; }
.my-inquiry-list { display: flex; flex-direction: column; gap: 8px; }
.my-inquiry-item {
  padding: 12px 16px;
  border: 0.5px solid var(--border2);
  border-radius: var(--radius-xs);
  background: var(--surface);
  display: flex; flex-direction: column; gap: 6px;
  transition: var(--transition);
}
.my-inquiry-item.clickable { cursor: pointer; }
.my-inquiry-item.clickable:hover { border-color: var(--accent); background: var(--surface2); }
.mi-top { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.mi-title { flex: 1; font-size: 14px; font-weight: 600; color: var(--t1); min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mi-arrow { font-size: 13px; color: var(--t4); flex-shrink: 0; }
.mi-meta { font-size: 12px; color: var(--t4); }
.mi-reply-preview {
  display: flex; align-items: flex-start; gap: 6px;
  font-size: 12px; color: #1E40AF;
  background: #EFF6FF; border-radius: 4px;
  padding: 5px 8px; margin-top: 2px;
}
.mi-reply-preview i { flex-shrink: 0; margin-top: 1px; }
.mi-badge {
  flex-shrink: 0; padding: 2px 8px; border-radius: 20px;
  font-size: 11px; font-weight: 700;
}
.badge-received { background: #FEF3C7; color: #92400E; }
.badge-checking  { background: #DBEAFE; color: #1E40AF; }
.badge-done      { background: #D1FAE5; color: #065F46; }
/* 답변 표시 */
.mi-reply {
  margin-top: 8px;
  background: #EFF6FF;
  border: 1px solid #BFDBFE;
  border-left: 3px solid #3B82F6;
  border-radius: var(--radius-xs);
  padding: 10px 12px;
}
.mi-reply-head {
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; font-weight: 700; color: #1E40AF;
  margin-bottom: 6px;
}
.mi-reply-head i { font-size: 13px; }
.mi-reply-who { color: #1E40AF; }
.mi-reply-date { font-weight: 400; color: #60A5FA; font-size: 11px; margin-left: 2px; }
.mi-reply-content {
  font-size: 13px; color: #1e3a5f; line-height: 1.6; white-space: pre-wrap;
}
.mi-no-reply {
  margin-top: 6px;
  display: flex; align-items: center; gap: 5px;
  font-size: 12px; color: var(--t4);
}
.mi-no-reply i { font-size: 13px; }

.mi-type-badge {
  display: inline-flex; align-items: center; gap: 4px;
  flex-shrink: 0; padding: 2px 8px; border-radius: 20px;
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
  background: none; border: none; color: var(--t4);
  cursor: pointer; font-size: 16px; padding: 4px; line-height: 1;
  border-radius: var(--radius-xs); transition: var(--transition);
}
.idlg-close:hover { background: var(--surface2); color: var(--t1); }
.idlg-title {
  font-size: 16px; font-weight: 700; color: var(--t1); line-height: 1.4; margin-bottom: 4px;
}
.idlg-meta { font-size: 12px; color: var(--t4); }

.idlg-body { display: flex; flex-direction: column; gap: 8px; }
.idlg-section-label {
  display: flex; align-items: center; gap: 5px;
  font-size: 12px; font-weight: 700; color: var(--t4); letter-spacing: 0.3px;
  margin-bottom: 4px;
}
.idlg-reply-meta {
  font-weight: 400; color: var(--t4); font-size: 11px; margin-left: 4px;
}
.idlg-content {
  padding: 12px 14px;
  border: 0.5px solid var(--border2);
  border-radius: var(--radius-xs);
  font-size: 14px; color: var(--t1); line-height: 1.7;
  white-space: pre-wrap; min-height: 60px; max-height: 300px; overflow-y: auto;
}
.idlg-reply-box {
  background: #EFF6FF;
  border: 1px solid #BFDBFE;
  border-left: 4px solid #3B82F6;
  border-radius: var(--radius-xs);
  padding: 12px 14px;
  font-size: 14px; color: #1e3a5f; line-height: 1.7; white-space: pre-wrap;
}
.idlg-no-reply {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 14px; background: var(--surface2);
  border-radius: var(--radius-xs); color: var(--t3);
}
.idlg-no-reply i { font-size: 18px; flex-shrink: 0; margin-top: 1px; }
.idlg-no-reply-title { font-size: 13px; font-weight: 600; color: var(--t2); }
.idlg-no-reply-sub { font-size: 12px; color: var(--t4); margin-top: 2px; }

.withdraw-notice {
  margin-top: 4px;
  padding-left: 16px;
  font-size: 13px;
  line-height: 1.8;
}

.notif-pref-list {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--border2);
  border-radius: var(--radius-xs);
  overflow: hidden;
  margin-bottom: 24px;
}

.notif-pref-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 13px 18px;
  border-bottom: 1px solid var(--border2);
  background: var(--surface);
}

.notif-pref-row:last-child { border-bottom: none; }

.notif-pref-header {
  background: var(--surface2);
  font-size: 12px;
  font-weight: 700;
  color: var(--t3);
}

.notif-pref-label {
  flex: 1;
  font-size: 14px;
  color: var(--t1);
}

.notif-pref-channels {
  display: flex;
  gap: 40px;
  align-items: center;
  text-align: center;
}

.notif-pref-channels span {
  width: 68px;
  display: inline-block;
}

@media (max-width: 768px) {
  .mp-layout { grid-template-columns: 1fr; }
  .mp-profile { border-right: none; border-bottom: 1px solid var(--border2); padding: 24px 20px 16px; }
  .profile-nav { flex-direction: row; flex-wrap: wrap; }
  .pnav-item { flex: 1; min-width: 100px; justify-content: center; }
  .mp-content { padding: 24px 18px; }
  .form-row { grid-template-columns: 1fr; }
}
</style>
