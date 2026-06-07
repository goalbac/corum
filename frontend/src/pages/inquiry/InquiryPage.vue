<template>
  <div class="inquiry-page">
    <div class="inquiry-header">
      <h2 class="inquiry-title">문의하기</h2>
      <p class="inquiry-desc">로그인 없이도 문의하실 수 있습니다. 담당자가 확인 후 연락드립니다.</p>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="inquiry-form">
      <el-form-item label="제목" prop="title">
        <el-input v-model="form.title" placeholder="문의 제목을 입력하세요." />
      </el-form-item>
      <el-form-item label="내용" prop="content">
        <el-input v-model="form.content" type="textarea" :rows="7" placeholder="문의 내용을 입력하세요." resize="none" />
      </el-form-item>
      <div class="form-row">
        <el-form-item label="연락처">
          <el-input v-model="form.contactPhone" placeholder="010-0000-0000" />
        </el-form-item>
        <el-form-item label="이메일">
          <el-input v-model="form.contactEmail" placeholder="example@email.com" />
        </el-form-item>
      </div>
      <div class="form-actions">
        <el-button type="primary" size="large" :loading="loading" @click="handleSubmit">
          문의 접수
        </el-button>
      </div>
    </el-form>

    <!-- 접수 완료 -->
    <div v-if="submitted" class="success-state">
      <i class="ti ti-circle-check"></i>
      <h3>문의가 접수되었습니다.</h3>
      <p>담당자 확인 후 이메일 또는 연락처로 답변드리겠습니다.</p>
      <el-button @click="resetForm">새 문의 작성</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import api from '@/api/axios'

const authStore = useAuthStore()
const formRef   = ref()
const loading   = ref(false)
const submitted = ref(false)

const form = ref({
  title: '',
  content: '',
  contactPhone: authStore.member?.phone || '',
  contactEmail: authStore.member?.email || '',
})

const rules = {
  title:   [{ required: true, message: '제목을 입력해주세요.' }],
  content: [{ required: true, message: '내용을 입력해주세요.' }],
}

async function handleSubmit() {
  await formRef.value?.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await api.post('/inquiries', form.value)
      submitted.value = true
    } catch(e) {
      ElMessage.error(e.response?.data?.message || '접수에 실패했습니다.')
    } finally { loading.value = false }
  })
}

function resetForm() {
  submitted.value = false
  form.value = { title: '', content: '', contactPhone: authStore.member?.phone || '', contactEmail: authStore.member?.email || '' }
}
</script>

<style scoped>
.inquiry-page { display: flex; flex-direction: column; gap: 24px; max-width: 640px; }
.inquiry-header {}
.inquiry-title { font-size: 18px; font-weight: 700; color: var(--t1); letter-spacing: -0.4px; }
.inquiry-desc { font-size: 13px; color: var(--t3); margin-top: 6px; }
.inquiry-form { display: flex; flex-direction: column; gap: 4px; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.form-actions { display: flex; justify-content: flex-end; margin-top: 8px; }

.success-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 48px 0;
  text-align: center;
}
.success-state i { font-size: 48px; color: var(--green); }
.success-state h3 { font-size: 18px; font-weight: 700; color: var(--t1); }
.success-state p { font-size: 14px; color: var(--t2); }

@media (max-width: 600px) {
  .form-row { grid-template-columns: 1fr; }
}
</style>
