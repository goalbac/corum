<template>
  <div v-if="visibleBanners.length" class="app-banners">
    <transition-group name="banner-slide" tag="div">
      <div v-for="banner in visibleBanners" :key="banner.id" class="app-banner">
        <div class="banner-inner">
          <div class="banner-content">
            <a
              v-if="banner.linkUrl"
              :href="banner.linkUrl"
              :target="banner.linkNewWindow ? '_blank' : '_self'"
              class="banner-text"
            >{{ banner.content }}</a>
            <span v-else class="banner-text">{{ banner.content }}</span>
          </div>
          <div class="banner-actions">
            <button class="banner-btn" @click="neverShow(banner.id)">다시 보지 않기</button>
            <button class="banner-btn banner-btn-close" @click="closeSession(banner.id)" aria-label="배너 닫기">
              끄기
            </button>
          </div>
        </div>
      </div>
    </transition-group>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/api/axios'

const allBanners = ref([])
// 이 세션에서만 닫은 배너 ID 집합
const sessionClosed = ref(new Set())

function neverShowKey(id) { return `banner_noshow_${id}` }
function isNeverShow(id) { return !!localStorage.getItem(neverShowKey(id)) }

const visibleBanners = computed(() =>
  allBanners.value.filter(b => !isNeverShow(b.id) && !sessionClosed.value.has(b.id))
)

onMounted(async () => {
  try {
    const res = await api.get('/display/banners/active')
    // 영구 숨김 처리된 배너만 미리 걸러냄
    allBanners.value = (res.data.data || []).filter(b => !isNeverShow(b.id))
  } catch { /* ignore */ }
})

function neverShow(id) {
  localStorage.setItem(neverShowKey(id), '1')
  // computed 재계산을 위해 allBanners에서도 제거
  allBanners.value = allBanners.value.filter(b => b.id !== id)
}

function closeSession(id) {
  sessionClosed.value = new Set([...sessionClosed.value, id])
}
</script>

<style scoped>
.app-banners {
  overflow: hidden;
}

.app-banner {
  background: var(--accent);
  color: #fff;
}

.app-banner + .app-banner {
  border-top: 1px solid rgba(255, 255, 255, 0.2);
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

.banner-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.banner-btn {
  padding: 4px 12px;
  height: 28px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 14px;
  background: transparent;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
  white-space: nowrap;
}

.banner-btn:hover { background: rgba(255, 255, 255, 0.2); }

.banner-btn-close {
  background: rgba(255, 255, 255, 0.15);
}

.banner-btn-close:hover { background: rgba(255, 255, 255, 0.3); }

.banner-slide-enter-active, .banner-slide-leave-active {
  transition: max-height 0.25s ease, opacity 0.2s ease;
  overflow: hidden;
}
.banner-slide-enter-from, .banner-slide-leave-to { max-height: 0; opacity: 0; }
.banner-slide-enter-to, .banner-slide-leave-from { max-height: 60px; opacity: 1; }
</style>
