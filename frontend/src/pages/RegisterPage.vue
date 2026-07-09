<template>
  <div class="auth-page">
    <div class="auth-container">
      <div class="auth-brand">
        <img v-if="logoUrl" :src="logoUrl" :alt="siteName" class="auth-brand-img" />
        <div v-else class="auth-brand-mark">{{ brandLetter }}</div>
        <span class="auth-brand-name">회원가입</span>
      </div>
      <p class="auth-brand-sub">가입 후 이메일 인증을 완료하면 서비스를 이용할 수 있습니다.</p>

      <div class="auth-card">
        <router-link to="/login" class="back-link">
          <el-icon><ArrowLeft /></el-icon> 로그인으로
        </router-link>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="auth-form"
        >
          <el-form-item label="아이디" prop="username">
            <el-input v-model="form.username" placeholder="4~50자 영문, 숫자" size="large" />
          </el-form-item>

          <el-form-item label="비밀번호" prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="8자 이상" size="large" />
          </el-form-item>
          <el-form-item label="비밀번호 확인" prop="passwordConfirm">
            <el-input v-model="form.passwordConfirm" type="password" show-password placeholder="비밀번호 재입력" size="large" />
          </el-form-item>

          <div class="form-row">
            <el-form-item label="이름" prop="name">
              <el-input v-model="form.name" placeholder="홍길동" size="large" />
            </el-form-item>
            <el-form-item label="연락처">
              <el-input v-model="form.phone" placeholder="010-0000-0000" size="large" />
            </el-form-item>
          </div>

          <el-form-item label="이메일" prop="email">
            <el-input v-model="form.email" placeholder="example@email.com" size="large" />
          </el-form-item>

          <div class="form-row">
            <el-form-item label="성별">
              <el-select v-model="form.gender" placeholder="선택" size="large" style="width: 100%">
                <el-option value="M" label="남성" />
                <el-option value="F" label="여성" />
              </el-select>
            </el-form-item>
            <el-form-item label="생년월일">
              <el-date-picker
                v-model="form.birthDate"
                type="date"
                placeholder="생년월일 선택"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                size="large"
                style="width: 100%"
              />
            </el-form-item>
          </div>

          <label class="check-row">
            <el-checkbox v-model="form.newsletterYn" />
            <span>뉴스레터 수신 동의</span>
          </label>

          <label class="check-row terms-row">
            <el-checkbox v-model="agreeTerms" />
            <span>
              <a href="/terms" target="_blank" class="link">이용약관</a> 및
              <a href="/privacy" target="_blank" class="link">개인정보처리방침</a>에 동의합니다.
              <span class="required">(필수)</span>
            </span>
          </label>

          <el-button
            type="primary"
            size="large"
            :loading="loading"
            :disabled="!agreeTerms"
            class="auth-submit"
            @click="handleRegister"
          >
            가입하기
          </el-button>
        </el-form>
      </div>

      <div class="auth-caption">
        이미 계정이 있으신가요? <router-link to="/login" class="link">로그인</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '@/api/axios'
import { useSiteStore } from '@/stores/site'

const siteStore = useSiteStore()
const siteName = computed(() => siteStore.siteName || '코럼')
const logoUrl = computed(() => siteStore.logoUrl)
const brandLetter = computed(() => siteName.value.charAt(0))

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const agreeTerms = ref(false)

const form = ref({
  username: '',
  name: '',
  email: '',
  password: '',
  passwordConfirm: '',
  phone: '',
  gender: '',
  birthDate: '',
  newsletterYn: false,
})

const rules = {
  username: [
    { required: true, message: '아이디를 입력해주세요.' },
    { min: 4, max: 50, message: '4~50자로 입력해주세요.' }
  ],
  name: [{ required: true, message: '이름을 입력해주세요.' }],
  email: [
    { required: true, message: '이메일을 입력해주세요.' },
    { type: 'email', message: '이메일 형식이 올바르지 않습니다.' }
  ],
  password: [
    { required: true, message: '비밀번호를 입력해주세요.' },
    { min: 8, message: '8자 이상 입력해주세요.' }
  ],
  passwordConfirm: [
    { required: true, message: '비밀번호를 한 번 더 입력해주세요.' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.value.password) {
          callback(new Error('비밀번호가 일치하지 않습니다.'))
        } else {
          callback()
        }
      }
    }
  ],
}

async function handleRegister() {
  if (!agreeTerms.value) {
    ElMessage.warning('약관에 동의해주세요.')
    return
  }
  await formRef.value?.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const { passwordConfirm, ...rest } = form.value
      const msgBuffer = new TextEncoder().encode(rest.username + rest.password)
      const hashBuffer = await crypto.subtle.digest('SHA-256', msgBuffer)
      const hashedPassword = Array.from(new Uint8Array(hashBuffer)).map(b => b.toString(16).padStart(2, '0')).join('')
      const payload = {
        ...rest,
        password: hashedPassword,
        // 미선택 시 빈 문자열('')이 아닌 null로 보내야 백엔드 LocalDate 파싱이 깨지지 않는다
        birthDate: rest.birthDate || null,
        gender: rest.gender || null,
      }
      await api.post('/auth/register', payload)
      ElMessage.success('가입이 완료되었습니다. 이메일로 발송된 인증 링크를 클릭해주세요.')
      router.push('/login')
    } catch (e) {
      ElMessage.error(e.response?.data?.message || '가입에 실패했습니다.')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg);
  padding: 40px 20px;
}

.auth-container {
  width: 100%;
  max-width: 460px;
}

/* ===== 브랜드 로고 ===== */
.auth-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 11px;
  margin-bottom: 8px;
}

.auth-brand-img {
  height: 40px;
  width: auto;
  max-width: 100%;
  object-fit: contain;
}

.auth-brand-mark {
  width: 40px;
  height: 40px;
  border-radius: 11px;
  background: var(--accent);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 21px;
  letter-spacing: -0.04em;
  box-shadow: var(--shadow-sm);
  flex-shrink: 0;
}

.auth-brand-name {
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.03em;
  color: var(--t1);
}

.auth-brand-sub {
  text-align: center;
  font-size: 13.5px;
  color: var(--t3);
  margin: 0 0 26px;
}

/* ===== 카드 ===== */
.auth-card {
  background: var(--surface);
  border: 0.5px solid var(--border);
  border-radius: 18px;
  padding: 30px;
  box-shadow: var(--shadow);
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--t3);
  margin-bottom: 18px;
  transition: color 0.15s;
}
.back-link:hover { color: var(--accent); }

.auth-form :deep(.el-form-item) { margin-bottom: 15px; }
.auth-form :deep(.el-form-item__label) {
  font-size: 13px;
  font-weight: 700;
  color: var(--t2);
  padding-bottom: 7px;
  line-height: 1.3;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.check-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--t2);
  cursor: pointer;
  margin-bottom: 12px;
}
.terms-row {
  margin-top: 4px;
  margin-bottom: 20px;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--t1);
}
.required { color: var(--danger); font-weight: 700; }

.auth-submit {
  width: 100%;
  border-radius: 10px;
  font-weight: 700;
  height: 46px;
}

.auth-caption {
  text-align: center;
  font-size: 13px;
  color: var(--t3);
  margin-top: 20px;
}

.link {
  color: var(--accent);
  font-weight: 700;
  text-decoration: none;
}
.link:hover { text-decoration: underline; }

@media (max-width: 768px) {
  :deep(.el-input__inner) { font-size: 16px !important; }
  :deep(.el-input__wrapper) { font-size: 16px !important; }
  .auth-card { padding: 24px 20px; }
  .form-row { grid-template-columns: 1fr; gap: 0; }
}
</style>
