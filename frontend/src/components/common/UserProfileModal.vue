<template>
  <el-dialog
    v-model="visible"
    width="340px"
    :show-close="false"
    class="user-profile-dialog"
    align-center
    destroy-on-close
    @closed="emit('close')"
  >
    <div v-loading="loading" class="profile-wrap">
      <template v-if="profile">
        <div class="prof-avatar-wrap">
          <img
            v-if="profile.profileImageUrl && !avatarErr"
            :src="resolveFileUrl(profile.profileImageUrl)"
            class="prof-avatar"
            alt=""
            @error="avatarErr = true"
          />
          <span v-else class="prof-avatar-fallback">{{ profile.name?.charAt(0) || 'U' }}</span>
        </div>
        <div class="prof-name">{{ profile.name }}</div>
        <div class="prof-username">@{{ profile.username }}</div>
        <div v-if="profile.groups?.length" class="prof-groups">
          <span v-for="g in profile.groups" :key="g.id" class="prof-badge">{{ g.name }}</span>
        </div>
        <div class="prof-actions">
          <el-button
            v-if="authStore.isLoggedIn && authStore.member?.id !== profile.id"
            type="primary"
            size="small"
            @click="sendMessage"
          >
            <i class="ti ti-send" style="margin-right:4px"></i>쪽지 보내기
          </el-button>
          <el-button size="small" @click="visible = false">닫기</el-button>
        </div>
      </template>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import api from '@/api/axios'
import { resolveFileUrl } from '@/utils/fileUrl'

const props = defineProps({
  memberId: { type: Number, default: null },
  modelValue: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'close'])

const router    = useRouter()
const authStore = useAuthStore()
const visible   = ref(false)
const loading   = ref(false)
const profile   = ref(null)
const avatarErr = ref(false)

watch(() => props.modelValue, (v) => {
  visible.value = v
  if (v && props.memberId) fetchProfile()
})

watch(visible, (v) => emit('update:modelValue', v))

async function fetchProfile() {
  loading.value = true
  avatarErr.value = false
  profile.value = null
  try {
    const res = await api.get(`/members/${props.memberId}/public`)
    profile.value = res.data.data
  } finally {
    loading.value = false
  }
}

function sendMessage() {
  visible.value = false
  router.push({ path: '/messages', query: { to: profile.value.id } })
}
</script>

<style scoped>
.profile-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 8px 0 4px;
  min-height: 160px;
}

.prof-avatar-wrap {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid var(--border);
  background: var(--surface2);
  margin-bottom: 4px;
}

.prof-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.prof-avatar-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #4f6ef7, #7c4ff7);
}

.prof-name {
  font-size: 18px;
  font-weight: 800;
  color: var(--t1);
}

.prof-username {
  font-size: 13px;
  color: var(--t3);
}

.prof-groups {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  justify-content: center;
  margin-top: 2px;
}

.prof-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 20px;
  background: var(--accent-bg);
  color: var(--accent-t);
  font-weight: 600;
}

.prof-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}
</style>
