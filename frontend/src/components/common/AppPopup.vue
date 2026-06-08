<template>
  <teleport to="body">
    <div v-for="popup in visiblePopups" :key="popup.id" class="popup-wrap" :class="`pos-${popup.position?.toLowerCase() || 'center'}`">
      <div class="popup-box">
        <button class="popup-close" @click="closePopup(popup.id)" aria-label="팝업 닫기">
          <i class="ti ti-x"></i>
        </button>

        <!-- IMAGE 타입 -->
        <template v-if="popup.contentType === 'IMAGE'">
          <a v-if="popup.linkUrl" :href="popup.linkUrl" :target="popup.linkNewWindow ? '_blank' : '_self'">
            <img :src="popup.imageUrl" :alt="popup.title" class="popup-img" />
          </a>
          <img v-else :src="popup.imageUrl" :alt="popup.title" class="popup-img" />
        </template>

        <!-- HTML 타입 -->
        <div v-else class="popup-html" v-html="popup.content" />

        <div class="popup-footer">
          <label class="no-show-label">
            <input type="checkbox" v-model="noShowChecked[popup.id]" />
            3일간 보지 않기
          </label>
          <button class="popup-close-btn" @click="closePopup(popup.id)">닫기</button>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api/axios'

const route = useRoute()
const allPopups = ref([])
const closedIds = ref(new Set())
const noShowChecked = ref({})

const visiblePopups = computed(() =>
  allPopups.value.filter(p => !closedIds.value.has(p.id))
)

async function fetchPopups() {
  try {
    const menuId = route.params.menuId || null
    const params = menuId ? { menuId } : {}
    const res = await api.get('/display/popups/active', { params })
    allPopups.value = (res.data.data || []).filter(p => !isNoShowCookie(p.id))
  } catch { /* 팝업 오류 무시 */ }
}

function closePopup(id) {
  if (noShowChecked.value[id]) setNoShowCookie(id)
  closedIds.value = new Set([...closedIds.value, id])
}

function cookieKey(id) { return `popup_noshow_${id}` }

function isNoShowCookie(id) {
  return document.cookie.split(';').some(c => c.trim().startsWith(cookieKey(id) + '='))
}

function setNoShowCookie(id) {
  const expires = new Date(Date.now() + 3 * 86400000).toUTCString()
  document.cookie = `${cookieKey(id)}=1; expires=${expires}; path=/`
}

onMounted(fetchPopups)
watch(() => route.path, fetchPopups)
</script>

<style scoped>
.popup-wrap {
  position: fixed;
  z-index: 500;
  top: 50%;
  transform: translateY(-50%);
}

.popup-wrap.pos-left { left: 20px; }
.popup-wrap.pos-right { right: 20px; }
.popup-wrap.pos-center { left: 50%; transform: translate(-50%, -50%); }

.popup-box {
  position: relative;
  background: var(--surface);
  border-radius: var(--radius-sm);
  box-shadow: 0 8px 32px rgba(0,0,0,0.22);
  min-width: 280px;
  max-width: 420px;
  overflow: hidden;
}

.popup-close {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: rgba(0,0,0,0.45);
  color: #fff;
  border-radius: 50%;
  font-size: 16px;
  cursor: pointer;
}

.popup-img {
  display: block;
  width: 100%;
  max-height: 400px;
  object-fit: cover;
}

.popup-html {
  padding: 24px;
  font-size: 15px;
  color: var(--t1);
  line-height: 1.7;
}

.popup-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: var(--surface2);
  border-top: 0.5px solid var(--border);
}

.no-show-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--t2);
  cursor: pointer;
}

.popup-close-btn {
  font-size: 13px;
  padding: 4px 12px;
  border: 0.5px solid var(--border);
  border-radius: var(--radius-xs);
  background: transparent;
  color: var(--t2);
  cursor: pointer;
}

.popup-close-btn:hover { background: var(--surface); color: var(--t1); }
</style>
