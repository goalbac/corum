<template>
  <div class="report-page">
    <!-- 유형 선택 탭 -->
    <div class="report-type-tabs">
      <button
        v-for="t in reportTypes"
        :key="t.value"
        :class="['rtype-btn', { active: reportType === t.value }]"
        @click="switchType(t.value)"
      >
        <i :class="`ti ${t.icon}`"></i>
        {{ t.label }}
      </button>
    </div>

    <!-- 폼 -->
    <div v-if="!submitted" class="report-body">
      <!-- 가이드 패널 -->
      <div class="guide-panel">
        <div class="guide-title">
          <i class="ti ti-bulb"></i>
          {{ currentGuide.title }}
        </div>
        <ul class="guide-list">
          <li v-for="(item, i) in currentGuide.items" :key="i">
            <i class="ti ti-check"></i>
            <span v-html="item"></span>
          </li>
        </ul>
        <div class="guide-example">
          <div class="guide-example-label">작성 예시</div>
          <pre class="guide-example-text">{{ currentGuide.example }}</pre>
        </div>
      </div>

      <!-- 입력 폼 -->
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="report-form">
        <el-form-item label="제목" prop="title">
          <el-input v-model="form.title" :placeholder="currentGuide.titlePlaceholder" />
        </el-form-item>

        <el-form-item v-if="reportType === 'BUG_REPORT'" label="오류 발생 페이지 / URL" prop="pageUrl">
          <el-input v-model="form.pageUrl" placeholder="예: 알려드려요 게시판 (https://...)" />
        </el-form-item>

        <el-form-item v-if="reportType === 'FEATURE_REQUEST'" label="참고한 서비스 또는 링크">
          <el-input v-model="form.reference" placeholder="예: 구글 캘린더, https://..." />
        </el-form-item>

        <el-form-item label="내용" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            :placeholder="currentGuide.contentPlaceholder"
            resize="none"
          />
        </el-form-item>

        <el-form-item label="첨부 파일 (스크린샷, 동영상 등)">
          <div class="file-attach-area">
            <div
              v-for="(f, i) in attachedFiles"
              :key="i"
              class="attached-file"
            >
              <i :class="`ti ${fileIcon(f.name)}`"></i>
              <span class="file-name">{{ f.name }}</span>
              <span class="file-size">{{ formatSize(f.size) }}</span>
              <button type="button" class="file-remove" @click="removeFile(i)">
                <i class="ti ti-x"></i>
              </button>
            </div>
            <label class="file-add-btn">
              <i class="ti ti-paperclip"></i>
              파일 첨부
              <input type="file" multiple style="display:none" @change="handleFiles" />
            </label>
          </div>
          <div class="file-hint">이미지, 동영상, 문서 파일을 첨부할 수 있습니다. (최대 50MB)</div>
        </el-form-item>

        <div class="form-actions">
          <el-button type="primary" size="large" :loading="loading" @click="handleSubmit">
            <i class="ti ti-send"></i>
            {{ reportType === 'BUG_REPORT' ? '오류 제보' : '기능 제안' }} 접수
          </el-button>
        </div>
      </el-form>
    </div>

    <!-- 접수 완료 -->
    <div v-else class="success-state">
      <i class="ti ti-circle-check"></i>
      <h3>{{ reportType === 'BUG_REPORT' ? '오류가 제보되었습니다.' : '기능 제안이 접수되었습니다.' }}</h3>
      <p>마이페이지 → <strong>내 제보 내역</strong>에서 접수 내역과 처리 현황을 확인할 수 있습니다.</p>
      <div class="success-actions">
        <el-button @click="resetForm">새로 작성</el-button>
        <el-button type="primary" @click="router.push('/mypage?tab=reports')">마이페이지에서 확인</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import api from '@/api/axios'

const router = useRouter()
const authStore = useAuthStore()

if (!authStore.isLoggedIn) {
  router.replace('/login?redirect=' + encodeURIComponent(window.location.pathname))
}

const reportType = ref('BUG_REPORT')
const formRef = ref()
const loading = ref(false)
const submitted = ref(false)
const attachedFiles = ref([])

const reportTypes = [
  { value: 'BUG_REPORT',       label: '오류 제보',  icon: 'ti-bug' },
  { value: 'FEATURE_REQUEST',  label: '기능 제안',  icon: 'ti-sparkles' },
]

const form = ref({ title: '', content: '', pageUrl: '', reference: '' })

const rules = {
  title:   [{ required: true, message: '제목을 입력해주세요.' }],
  content: [{ required: true, message: '내용을 입력해주세요.' }],
}

const guides = {
  BUG_REPORT: {
    title: '오류 제보 작성 가이드',
    items: [
      '오류가 발생한 <strong>페이지 또는 URL</strong>을 정확히 알려주세요.',
      '어떤 동작을 했을 때 오류가 발생했는지 <strong>순서대로</strong> 기술해 주세요.',
      '오류 메시지가 있다면 <strong>정확히 복사</strong>해서 붙여넣어 주세요.',
      '가능하면 <strong>스크린샷 또는 화면 녹화</strong>를 첨부해 주세요.',
      '어떤 기기(PC/모바일)와 브라우저(크롬/사파리 등)를 사용하는지 알려주세요.',
    ],
    example: `제목: 알려드려요 게시판 댓글 작성 시 오류 발생

발생 페이지: 알려드려요 게시판 > "2026년 수요법회 안내" 글

재현 순서:
1. 로그인 후 알려드려요 게시판에서 글을 클릭합니다.
2. 하단 댓글 입력창에 "감사합니다"를 입력합니다.
3. "댓글 등록" 버튼을 클릭합니다.

오류 내용:
화면에 "오류가 발생하였습니다"라는 메시지가 나타나고
댓글이 등록되지 않습니다.

환경: PC / Chrome 최신 버전`,
    titlePlaceholder: '예: 갤러리 게시판에서 사진 업로드 시 오류 발생',
    contentPlaceholder: '오류가 발생한 상황을 단계별로 상세히 기술해 주세요.\n\n재현 순서:\n1. \n2. \n3. \n\n오류 메시지: \n\n사용 환경 (PC/모바일, 브라우저):',
  },
  FEATURE_REQUEST: {
    title: '기능 제안 작성 가이드',
    items: [
      '어떤 문제를 해결하기 위한 기능인지 <strong>배경과 이유</strong>를 설명해 주세요.',
      '원하는 기능을 <strong>구체적으로</strong> 설명해 주세요. 막연한 설명보다 구체적인 설명이 도움이 됩니다.',
      '다른 서비스에서 본 기능이라면 <strong>서비스명 또는 링크</strong>를 남겨주세요.',
      '<strong>사용 시나리오</strong>를 예시로 들어주시면 더 잘 이해할 수 있습니다.',
      '스크린샷이나 참고 이미지가 있으면 첨부해 주세요.',
    ],
    example: `제목: 게시판 글에 '좋아요' 외에 반응 이모지 추가 요청

배경:
현재 글에 좋아요만 표시되는데, 다양한 반응을 표현하기 어렵습니다.

제안 기능:
좋아요 외에 ❤️ 😂 😮 😢 👏 등 여러 이모지 반응을
선택할 수 있는 기능을 추가해 주세요.

참고 서비스:
- 페이스북 반응 기능 (https://facebook.com)
- 슬랙 이모지 리액션

사용 시나리오:
"새해 복 많이 받으세요" 글에 ❤️를 클릭하면
글 하단에 반응 수가 표시됩니다.`,
    titlePlaceholder: '예: 캘린더 일정에 참석 여부 응답 기능 추가',
    contentPlaceholder: '제안하는 기능과 사용 시나리오를 구체적으로 설명해 주세요.\n\n배경/이유:\n\n기능 설명:\n\n참고 서비스/링크:\n\n사용 시나리오:',
  },
}

const currentGuide = computed(() => guides[reportType.value])

function switchType(type) {
  reportType.value = type
  formRef.value?.clearValidate()
}

function fileIcon(name) {
  if (/\.(jpg|jpeg|png|gif|webp|svg)$/i.test(name)) return 'ti-photo'
  if (/\.(mp4|mov|avi|webm)$/i.test(name)) return 'ti-video'
  return 'ti-file'
}

function formatSize(bytes) {
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / 1024 / 1024).toFixed(1) + 'MB'
}

function handleFiles(e) {
  const files = Array.from(e.target.files || [])
  const MAX = 50 * 1024 * 1024
  files.forEach(f => {
    if (f.size > MAX) { ElMessage.warning(`${f.name}: 파일 크기가 50MB를 초과합니다.`); return }
    attachedFiles.value.push(f)
  })
  e.target.value = ''
}

function removeFile(i) {
  attachedFiles.value.splice(i, 1)
}

async function handleSubmit() {
  await formRef.value?.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      // 파일 업로드 후 URL 목록 추출
      const uploadedUrls = []
      for (const file of attachedFiles.value) {
        const fd = new FormData()
        fd.append('file', file)
        const res = await api.post('/files/inline-image', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
        uploadedUrls.push({ name: file.name, url: res.data.data?.url })
      }

      // 첨부 파일 정보를 본문에 추가
      let content = form.value.content
      if (form.value.pageUrl) content = `[오류 페이지]\n${form.value.pageUrl}\n\n${content}`
      if (form.value.reference) content = `[참고 서비스/링크]\n${form.value.reference}\n\n${content}`
      if (uploadedUrls.length) {
        content += '\n\n[첨부 파일]\n' + uploadedUrls.map(u => `- ${u.name}: ${u.url}`).join('\n')
      }

      await api.post('/inquiries', {
        title: form.value.title,
        content,
        inquiryType: reportType.value,
        writerName: authStore.member?.name,
        contactEmail: authStore.member?.email,
        contactPhone: authStore.member?.phone,
      })
      submitted.value = true
    } catch (e) {
      ElMessage.error(e.response?.data?.message || '접수에 실패했습니다.')
    } finally { loading.value = false }
  })
}

function resetForm() {
  submitted.value = false
  form.value = { title: '', content: '', pageUrl: '', reference: '' }
  attachedFiles.value = []
}
</script>

<style scoped>
.report-page { display: flex; flex-direction: column; gap: 20px; max-width: 760px; }

/* 유형 탭 */
.report-type-tabs { display: flex; gap: 8px; }
.rtype-btn {
  display: flex; align-items: center; gap: 7px;
  padding: 8px 18px;
  border-radius: var(--radius-xs);
  border: 1.5px solid var(--border2);
  background: var(--surface);
  color: var(--t2);
  font-size: 14px; font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
}
.rtype-btn i { font-size: 15px; }
.rtype-btn:hover { border-color: var(--accent); color: var(--accent); }
.rtype-btn.active { border-color: var(--accent); background: var(--accent); color: #fff; }

/* 가이드 + 폼 레이아웃 */
.report-body { display: grid; grid-template-columns: 280px 1fr; gap: 24px; align-items: start; }

/* 가이드 패널 */
.guide-panel {
  background: var(--surface2);
  border-radius: var(--radius-s);
  padding: 16px;
  font-size: 13px;
  color: var(--t2);
  position: sticky;
  top: 80px;
}
.guide-title {
  font-size: 13px; font-weight: 700; color: var(--t1);
  display: flex; align-items: center; gap: 6px;
  margin-bottom: 12px;
}
.guide-title i { color: var(--accent); }
.guide-list { list-style: none; padding: 0; margin: 0 0 14px; display: flex; flex-direction: column; gap: 7px; }
.guide-list li { display: flex; align-items: flex-start; gap: 6px; line-height: 1.5; }
.guide-list li i { color: var(--green); font-size: 13px; flex-shrink: 0; margin-top: 1px; }
.guide-list li :deep(strong) { color: var(--t1); }

.guide-example { border-top: 0.5px solid var(--border); padding-top: 12px; }
.guide-example-label { font-size: 11px; font-weight: 700; color: var(--t3); letter-spacing: 0.5px; margin-bottom: 6px; text-transform: uppercase; }
.guide-example-text {
  font-size: 11px; color: var(--t3); line-height: 1.6;
  white-space: pre-wrap; font-family: inherit;
  margin: 0; max-height: 200px; overflow-y: auto;
}

/* 폼 */
.report-form { display: flex; flex-direction: column; gap: 4px; }
.form-actions { display: flex; justify-content: flex-end; margin-top: 4px; }
.form-actions .el-button i { margin-right: 4px; }

/* 파일 첨부 */
.file-attach-area { display: flex; flex-direction: column; gap: 6px; }
.attached-file {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 10px; background: var(--surface2);
  border-radius: var(--radius-xs); font-size: 13px;
}
.attached-file i { color: var(--t3); }
.file-name { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--t1); }
.file-size { color: var(--t4); font-size: 11px; flex-shrink: 0; }
.file-remove {
  background: none; border: none; color: var(--t4);
  cursor: pointer; padding: 2px; line-height: 1;
}
.file-remove:hover { color: var(--red); }
.file-add-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 6px 12px;
  border: 1.5px dashed var(--border2);
  border-radius: var(--radius-xs);
  font-size: 13px; color: var(--t3);
  cursor: pointer; transition: var(--transition);
  width: fit-content;
}
.file-add-btn:hover { border-color: var(--accent); color: var(--accent); }
.file-hint { font-size: 11px; color: var(--t4); margin-top: 2px; }

/* 완료 */
.success-state {
  display: flex; flex-direction: column; align-items: center;
  gap: 12px; padding: 60px 0; text-align: center;
}
.success-state i { font-size: 52px; color: var(--green); }
.success-state h3 { font-size: 18px; font-weight: 700; color: var(--t1); }
.success-state p { font-size: 14px; color: var(--t2); line-height: 1.6; }
.success-actions { display: flex; gap: 10px; margin-top: 4px; }

@media (max-width: 700px) {
  .report-body { grid-template-columns: 1fr; }
  .guide-panel { position: static; }
}
</style>
