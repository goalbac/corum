<template>
  <div class="mypage">
    <el-tabs v-model="activeTab">

      <!-- 내 정보 -->
      <el-tab-pane label="내 정보" name="info">
        <el-card shadow="never" class="tab-card">
          <el-form
            ref="infoFormRef"
            :model="infoForm"
            label-position="top"
            v-loading="loading"
          >
            <div class="form-row">
              <el-form-item label="아이디">
                <el-input :value="member?.username" disabled />
              </el-form-item>
              <el-form-item label="이름" prop="name">
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
                <el-select v-model="infoForm.gender" placeholder="선택" style="width: 100%">
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
                  style="width: 100%"
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
        </el-card>
      </el-tab-pane>

      <!-- 비밀번호 변경 -->
      <el-tab-pane label="비밀번호 변경" name="password">
        <el-card shadow="never" class="tab-card">
          <el-form
            ref="pwFormRef"
            :model="pwForm"
            :rules="pwRules"
            label-position="top"
            style="max-width: 400px;"
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
        </el-card>
      </el-tab-pane>

      <!-- 회원 탈퇴 -->
      <el-tab-pane label="회원 탈퇴" name="withdraw">
        <el-card shadow="never" class="tab-card">
          <div class="withdraw-section">
            <el-alert
              title="탈퇴 전 꼭 확인해주세요"
              type="warning"
              :closable="false"
              show-icon
              style="margin-bottom: 20px;"
            >
              <template #default>
                <ul class="withdraw-notice">
                  <li>탈퇴 후 작성한 게시글과 댓글은 삭제되지 않습니다.</li>
                  <li>탈퇴 후 동일한 아이디로 재가입이 불가합니다.</li>
                  <li>탈퇴 처리 후 복구가 불가합니다.</li>
                </ul>
              </template>
            </el-alert>
            <el-form label-position="top" style="max-width: 400px;">
              <el-form-item label="비밀번호 확인">
                <el-input v-model="withdrawPassword" type="password" show-password placeholder="현재 비밀번호를 입력하세요" />
              </el-form-item>
              <el-button type="danger" @click="handleWithdraw">회원 탈퇴</el-button>
            </el-form>
          </div>
        </el-card>
      </el-tab-pane>

    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import api from '@/api/axios'

const router = useRouter()
const authStore = useAuthStore()
const activeTab = ref('info')
const loading = ref(false)
const saving = ref(false)
const pwSaving = ref(false)
const member = ref(null)
const withdrawPassword = ref('')

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
        if (value !== pwForm.value.newPassword) {
          callback(new Error('비밀번호가 일치하지 않습니다.'))
        } else callback()
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
      await api.put('/auth/me/password', {
        currentPassword: pwForm.value.currentPassword,
        newPassword: pwForm.value.newPassword,
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
    await api.delete('/auth/me', { data: { password: withdrawPassword.value } })
    ElMessage.success('탈퇴 처리되었습니다.')
    authStore.clearToken()
    router.push('/login')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '탈퇴에 실패했습니다.')
  }
}

onMounted(fetchMember)
</script>

<style scoped>
.mypage { max-width: 700px; }

.tab-card {
  border-radius: var(--radius-lg);
  margin-top: 4px;
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

.withdraw-notice {
  margin-top: 4px;
  padding-left: 16px;
  font-size: 13px;
  line-height: 1.8;
}
</style>
