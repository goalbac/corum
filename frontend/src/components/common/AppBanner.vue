<template>
  <transition name="banner-slide">
    <div v-if="visible && banners.length" class="app-banner">
      <div class="banner-inner">
        <div class="banner-content" v-for="banner in banners" :key="banner.id">
          <a
            v-if="banner.linkUrl"
            :href="banner.linkUrl"
            :target="banner.linkNewWindow ? '_blank' : '_self'"
            class="banner-text"
          >{{ banner.content }}</a>
          <span v-else class="banner-text">{{ banner.content }}</span>
        </div>
        <button class="banner-close" @click="dismiss" aria-label="배너 닫기">
          <i class="ti ti-x"></i>
        </button>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios'

const banners = ref([])
const visible = ref(false)
const STORAGE_KEY = 'banner_dismissed'

onMounted(async () => {
  if (sessionStorage.getItem(STORAGE_KEY)) return
  try {
    const res = await api.get('/display/banners/active')
    banners.value = res.data.data || []
    if (banners.value.length) visible.value = true
  } catch { /* 배너 오류는 무시 */ }
})

function dismiss() {
  visible.value = false
  sessionStorage.setItem(STORAGE_KEY, '1')
}

</script>

<style scoped>
.app-banner {
  background: var(--accent);
  color: #fff;
}

.banner-inner {
  display: flex;
  align-items: center;
  max-width: 1440px;
  margin: 0 auto;
  padding: 10px 20px;
  gap: 12px;
}

.banner-content {
  flex: 1;
  min-width: 0;
}

.banner-text {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  text-decoration: none;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
}

.banner-text:hover { text-decoration: underline; }

.banner-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: rgba(255,255,255,0.2);
  color: #fff;
  border-radius: 50%;
  cursor: pointer;
  font-size: 16px;
  flex-shrink: 0;
  transition: background 0.15s;
}

.banner-close:hover { background: rgba(255,255,255,0.35); }

.banner-slide-enter-active, .banner-slide-leave-active { transition: max-height 0.25s, opacity 0.25s; overflow: hidden; }
.banner-slide-enter-from, .banner-slide-leave-to { max-height: 0; opacity: 0; }
.banner-slide-enter-to, .banner-slide-leave-from { max-height: 60px; opacity: 1; }
</style>
