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
</template>

<script setup>
import { ref, onMounted } from 'vue'
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
  { name: 'info',     icon: 'ti-user',     label: '내 정보' },
  { name: 'password', icon: 'ti-lock',     label: '비밀번호 변경' },
  { name: 'messages', icon: 'ti-messages', label: '쪽지' },
  { name: 'withdraw', icon: 'ti-door-exit', label: '회원 탈퇴' }
]

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

function handleFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) { ElMessage.warning('파일 크기는 5MB 이하여야 합니다.'); return }
  selectedFile.value = file
  myAvatarError.value = false
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

onMounted(fetchMember)
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

.withdraw-notice {
  margin-top: 4px;
  padding-left: 16px;
  font-size: 13px;
  line-height: 1.8;
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
