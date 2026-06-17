<template>
  <teleport to="body">
    <template v-for="(popup, index) in visiblePopups" :key="popup.id">
      <!-- 모바일: 맨 위(마지막) 팝업에만 딤 오버레이 -->
      <div
        v-if="index === visiblePopups.length - 1"
        class="popup-dim"
        @click="closePopup(popup.id)"
      />
      <div class="popup-wrap" :class="`pos-${popup.position?.toLowerCase() || 'center'}`">
        <div class="popup-box">
          <!-- 데스크톱 X 닫기 버튼 -->
          <button class="popup-close desktop-only" @click="closePopup(popup.id)" aria-label="팝업 닫기">
            <i class="ti ti-x"></i>
          </button>

          <!-- IMAGE 타입 -->
          <template v-if="popup.contentType === 'IMAGE'">
            <a v-if="popup.linkUrl" :href="popup.linkUrl" :target="popup.linkNewWindow ? '_blank' : '_self'">
              <img :src="popup.imageUrl" :alt="popup.title" class="popup-img" />
            </a>
            <img v-else :src="popup.imageUrl" :alt="popup.title" class="popup-img" />
            <div v-if="popup.content" class="popup-body">{{ popup.content }}</div>
          </template>

          <!-- HTML 타입 -->
          <div v-else class="popup-html" v-html="popup.content" />

          <!-- 데스크톱 footer: 체크박스 스타일 -->
          <div class="popup-footer desktop-footer">
            <label class="no-show-label">
              <input type="checkbox" v-model="noShowChecked[popup.id]" />
              3일간 보지 않기
            </label>
            <button class="popup-close-btn" @click="closePopup(popup.id)">닫기</button>
          </div>

          <!-- 모바일 footer: 텍스트 버튼 스타일 -->
          <div class="popup-footer mobile-footer">
            <button class="mobile-footer-btn no-show-mobile" @click="closeNoShow(popup.id)">다시 보지 않기</button>
            <div class="mobile-footer-divider"></div>
            <button class="mobile-footer-btn close-mobile" @click="closePopup(popup.id)">닫기</button>
          </div>
        </div>
      </div>
    </template>
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
  // /에서만 팝업 표시
  if (route.path !== '/') {
    allPopups.value = []
    return
  }
  try {
    const res = await api.get('/display/popups/active')
    allPopups.value = (res.data.data || []).filter(p => !isNoShowCookie(p.id))
  } catch { /* 팝업 오류 무시 */ }
}

function closePopup(id) {
  if (noShowChecked.value[id]) setNoShowCookie(id)
  closedIds.value = new Set([...closedIds.value, id])
}

function closeNoShow(id) {
  setNoShowCookie(id)
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
/* ===== 딤 오버레이 (모바일 전용) ===== */
.popup-dim {
  display: none;
}
@media (max-width: 768px) {
  .popup-dim {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 498;
  }
}

/* ===== 팝업 위치 래퍼 (데스크톱) ===== */
.popup-wrap {
  position: fixed;
  z-index: 500;
  top: 50%;
  transform: translateY(-50%);
}
.popup-wrap.pos-left   { left: 20px; }
.popup-wrap.pos-right  { right: 20px; }
.popup-wrap.pos-center { left: 50%; transform: translate(-50%, -50%); }

/* ===== 팝업 박스 (데스크톱) ===== */
.popup-box {
  position: relative;
  background: var(--surface);
  border-radius: var(--radius-sm);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.22);
  min-width: 280px;
  max-width: 420px;
  overflow: hidden;
}

/* ===== 데스크톱 X 버튼 ===== */
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
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  border-radius: 50%;
  font-size: 16px;
  cursor: pointer;
}

/* ===== 이미지 / HTML ===== */
.popup-img {
  display: block;
  width: 100%;
  max-height: 400px;
  object-fit: cover;
}

.popup-body {
  padding: 14px 18px;
  font-size: 14px;
  color: var(--t1);
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

.popup-html {
  padding: 24px;
  font-size: 15px;
  color: var(--t1);
  line-height: 1.7;
}

/* ===== Footer ===== */
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

/* 모바일 footer는 데스크톱에서 숨김 */
.mobile-footer { display: none; }

/* ===== 모바일: 하단 시트 ===== */
@media (max-width: 768px) {
  .popup-wrap,
  .popup-wrap.pos-left,
  .popup-wrap.pos-right,
  .popup-wrap.pos-center {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    top: auto;
    transform: none;
    z-index: 499;
  }

  .popup-box {
    width: 100%;
    max-width: 100%;
    min-width: 0;
    border-radius: 20px 20px 0 0;
    box-shadow: 0 -4px 32px rgba(0, 0, 0, 0.2);
    max-height: 80vh;
    overflow-y: auto;
  }

  /* 이미지는 모바일에서 최대 높이 제한 */
  .popup-img {
    max-height: 55vh;
    object-fit: cover;
  }

  /* X 버튼 모바일에서 숨김 */
  .desktop-only { display: none; }

  /* 데스크톱 footer 숨김, 모바일 footer 표시 */
  .desktop-footer { display: none; }
  .mobile-footer {
    display: flex;
    align-items: stretch;
    padding: 0;
    background: var(--surface);
    border-top: 0.5px solid var(--border2);
  }

  .mobile-footer-btn {
    flex: 1;
    padding: 16px 0;
    border: none;
    background: transparent;
    font-size: 15px;
    font-weight: 500;
    color: var(--t2);
    cursor: pointer;
    transition: background 0.15s;
  }
  .mobile-footer-btn:hover { background: var(--surface2); }
  .mobile-footer-btn.close-mobile {
    font-weight: 700;
    color: var(--t1);
  }

  .mobile-footer-divider {
    width: 0.5px;
    background: var(--border2);
    flex-shrink: 0;
  }
}
</style>
