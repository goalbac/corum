<template>
  <div class="register-wrap">
    <div class="register-card">
      <div class="register-header">
        <router-link to="/login" class="back-link">
          <el-icon><ArrowLeft /></el-icon> 로그인으로
        </router-link>
        <h2 class="register-title">회원가입</h2>
        <p class="register-sub">가입 후 이메일 인증을 완료하면 서비스를 이용할 수 있습니다.</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="register-form"
      >
        <div class="form-row">
          <el-form-item label="아이디" prop="username">
            <el-input v-model="form.username" placeholder="4~50자 영문, 숫자" />
          </el-form-item>
          <el-form-item label="이름" prop="name">
            <el-input v-model="form.name" placeholder="실명을 입력하세요" />
          </el-form-item>
        </div>

        <el-form-item label="이메일" prop="email">
          <el-input v-model="form.email" placeholder="example@email.com" />
        </el-form-item>

        <div class="form-row">
          <el-form-item label="비밀번호" prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="8자 이상" />
          </el-form-item>
          <el-form-item label="비밀번호 확인" prop="passwordConfirm">
            <el-input v-model="form.passwordConfirm" type="password" show-password placeholder="비밀번호 재입력" />
          </el-form-item>
        </div>

        <div class="form-row">
          <el-form-item label="연락처">
            <el-input v-model="form.phone" placeholder="010-0000-0000" />
          </el-form-item>
          <el-form-item label="성별">
            <el-select v-model="form.gender" placeholder="선택" style="width: 100%">
              <el-option value="M" label="남성" />
              <el-option value="F" label="여성" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="생년월일">
          <el-date-picker
            v-model="form.birthDate"
            type="date"
            placeholder="생년월일 선택"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="form.newsletterYn">뉴스레터 수신 동의</el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="agreeTerms">
            <span>
              <a href="/terms" target="_blank" class="link">이용약관</a> 및
              <a href="/privacy" target="_blank" class="link">개인정보처리방침</a>에 동의합니다. (필수)
            </span>
          </el-checkbox>
        </el-form-item>

        <el-button
          type="primary"
          size="large"
          :loading="loading"
          :disabled="!agreeTerms"
          style="width: 100%"
          @click="handleRegister"
        >
          가입하기
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '@/api/axios'

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
.register-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-base);
  padding: 40px 20px;
}

.register-card {
  width: 560px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 40px 40px;
  box-shadow: var(--shadow-md);
}

.register-header {
  margin-bottom: 28px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 16px;
  transition: var(--transition);
}

.back-link:hover { color: var(--color-primary); }

.register-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 6px;
}

.register-sub {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.link {
  color: var(--color-primary);
  text-decoration: underline;
}
</style>
