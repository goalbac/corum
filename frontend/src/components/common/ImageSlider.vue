<template>
  <div
    class="slider-wrap"
    @mouseenter="pauseAuto"
    @mouseleave="resumeAuto"
    @touchstart="onTouchStart"
    @touchend="onTouchEnd"
  >
    <!-- 슬라이드 트랙 -->
    <div class="slider-track-outer">
      <div
        class="slider-track"
        :style="{ transform: `translateX(-${current * 100}%)` }"
      >
        <a
          v-for="(slide, i) in slides"
          :key="i"
          class="slide-item"
          :href="slide.url || undefined"
          :target="slide.newWindow ? '_blank' : undefined"
          :style="{ cursor: slide.url ? 'pointer' : 'default' }"
          @click.prevent="slide.url ? openLink(slide) : null"
        >
          <img :src="resolveFileUrl(slide.imageUrl)" :alt="slide.title || ''" class="slide-img" />
          <div v-if="slide.title" class="slide-caption">
            <span class="slide-caption-text">{{ slide.title }}</span>
          </div>
        </a>
      </div>
    </div>

    <!-- 좌/우 화살표 (슬라이드 2개 이상일 때) -->
    <template v-if="slides.length > 1">
      <button class="slider-arrow left" @click.prevent="prev" aria-label="이전">
        <i class="ti ti-chevron-left"></i>
      </button>
      <button class="slider-arrow right" @click.prevent="next" aria-label="다음">
        <i class="ti ti-chevron-right"></i>
      </button>

      <!-- 점 인디케이터 -->
      <div class="slider-dots">
        <button
          v-for="(_, i) in slides"
          :key="i"
          class="dot"
          :class="{ active: i === current }"
          @click.prevent="goTo(i)"
          :aria-label="`슬라이드 ${i + 1}`"
        />
      </div>

      <!-- 슬라이드 번호 -->
      <div class="slide-counter">{{ current + 1 }} / {{ slides.length }}</div>
    </template>

    <!-- 슬라이드가 없을 때 -->
    <div v-if="!slides.length" class="slider-empty">
      <i class="ti ti-photo"></i>
      <span>슬라이드가 없습니다.</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { resolveFileUrl } from '@/utils/fileUrl'

const props = defineProps({
  slides:   { type: Array, default: () => [] },
  interval: { type: Number, default: 4000 },
  title:    { type: String, default: '' }
})

const current = ref(0)
let timer = null

function goTo(i) {
  current.value = ((i % props.slides.length) + props.slides.length) % props.slides.length
}
function next() { goTo(current.value + 1) }
function prev() { goTo(current.value - 1) }

function startAuto() {
  if (props.slides.length <= 1) return
  timer = setInterval(next, props.interval)
}
function pauseAuto()  { clearInterval(timer) }
function resumeAuto() { startAuto() }

// 터치 스와이프
let touchStartX = 0
function onTouchStart(e) { touchStartX = e.touches[0].clientX }
function onTouchEnd(e) {
  const diff = touchStartX - e.changedTouches[0].clientX
  if (Math.abs(diff) > 40) diff > 0 ? next() : prev()
}

function openLink(slide) {
  if (!slide.url) return
  if (slide.newWindow) window.open(slide.url, '_blank')
  else window.location.href = slide.url
}

onMounted(startAuto)
onBeforeUnmount(pauseAuto)
</script>

<style scoped>
.slider-wrap {
  position: relative;
  width: 100%;
  background: var(--surface);
  border-radius: var(--radius-sm);
  border: 0.5px solid var(--border2);
  box-shadow: var(--shadow);
  overflow: hidden;
  user-select: none;
}

/* 트랙 */
.slider-track-outer {
  overflow: hidden;
  width: 100%;
}
.slider-track {
  display: flex;
  transition: transform 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: transform;
}
.slide-item {
  flex: 0 0 100%;
  width: 100%;
  position: relative;
  display: block;
  text-decoration: none;
}
.slide-img {
  width: 100%;
  height: 300px;
  object-fit: cover;
  display: block;
}

/* 캡션 */
.slide-caption {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 40px 28px 22px;
  background: linear-gradient(to top, rgba(0,0,0,0.58) 0%, transparent 100%);
}
.slide-caption-text {
  font-size: 20px;
  font-weight: 800;
  color: #fff;
  text-shadow: 0 2px 8px rgba(0,0,0,0.4);
  letter-spacing: -0.3px;
}

/* 화살표 */
.slider-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255,255,255,0.88);
  backdrop-filter: blur(8px);
  border: none;
  box-shadow: 0 2px 12px rgba(0,0,0,0.18);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #1a1a2e;
  transition: background 0.15s, opacity 0.15s, transform 0.15s;
  z-index: 10;
  opacity: 0;
}
.slider-wrap:hover .slider-arrow { opacity: 1; }
.slider-arrow.left  { left: 14px; }
.slider-arrow.right { right: 14px; }
.slider-arrow:hover { background: #fff; transform: translateY(-50%) scale(1.08); }

/* 점 인디케이터 */
.slider-dots {
  position: absolute;
  bottom: 14px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 6px;
  z-index: 10;
}
.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  border: none;
  background: rgba(255,255,255,0.55);
  cursor: pointer;
  padding: 0;
  transition: background 0.2s, width 0.2s;
}
.dot.active {
  background: #fff;
  width: 20px;
  border-radius: 4px;
}

/* 카운터 */
.slide-counter {
  position: absolute;
  top: 14px;
  right: 16px;
  font-size: 12px;
  font-weight: 600;
  color: rgba(255,255,255,0.85);
  background: rgba(0,0,0,0.3);
  backdrop-filter: blur(4px);
  border-radius: 20px;
  padding: 3px 10px;
  z-index: 10;
}

/* 빈 상태 */
.slider-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 60px;
  color: var(--t3);
  font-size: 14px;
}
.slider-empty i { font-size: 36px; }

@media (max-width: 768px) {
  .slide-img { height: 200px; }
  .slider-arrow { width: 32px; height: 32px; font-size: 15px; opacity: 1; }
  .slide-caption-text { font-size: 16px; }
}
</style>
