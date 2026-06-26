<template>
  <div class="reaction-bar">
    <!-- 카운트가 있는 이모지 리액션 -->
    <button
      v-for="emoji in visibleReactions"
      :key="emoji.type"
      :class="['reaction-pill', { reacted: myReactions?.includes(emoji.type) }]"
      :title="emoji.label"
      @click="toggle(emoji.type)"
    >
      <span class="r-emoji">{{ emoji.icon }}</span>
      <span class="r-count">{{ emoji.count }}</span>
    </button>

    <!-- 리액션 추가 버튼 -->
    <div class="reaction-add-wrap" ref="wrapRef">
      <button
        class="reaction-add-btn"
        :class="{ active: pickerOpen }"
        title="리액션 추가"
        @click="pickerOpen = !pickerOpen"
      >
        <i class="ti ti-mood-plus"></i>
      </button>
      <Transition name="picker-fade">
        <div v-if="pickerOpen" class="emoji-picker">
          <button
            v-for="emoji in ALL_EMOJIS"
            :key="emoji.type"
            :class="['picker-emoji', { reacted: myReactions?.includes(emoji.type) }]"
            :title="emoji.label"
            @click="pick(emoji.type)"
          >
            <span>{{ emoji.icon }}</span>
          </button>
        </div>
      </Transition>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  reactions:   { type: Object, default: () => ({}) },
  myReactions: { type: Array,  default: () => [] },
  disabled:    { type: Boolean, default: false },
})
const emit = defineEmits(['toggle'])

const ALL_EMOJIS = [
  { type: 'HEART',     icon: '❤️',  label: '하트'   },
  { type: 'THUMBS_UP', icon: '👍',  label: '좋아요' },
  { type: 'SMILE',     icon: '😄',  label: '웃음'   },
  { type: 'LAUGH_CRY', icon: '😂',  label: '눈물'   },
  { type: 'SAD',       icon: '😢',  label: '슬퍼요' },
  { type: 'ANGRY',     icon: '😠',  label: '화나요' },
]

const pickerOpen = ref(false)
const wrapRef    = ref(null)

// 카운트 > 0 인 이모지만 표시 (순서 유지)
const visibleReactions = computed(() =>
  ALL_EMOJIS
    .map(e => ({ ...e, count: props.reactions[e.type] ?? 0 }))
    .filter(e => e.count > 0)
)

function toggle(emojiType) {
  if (props.disabled) return
  emit('toggle', emojiType)
}

function pick(emojiType) {
  toggle(emojiType)
  pickerOpen.value = false
}

function onClickOutside(e) {
  if (wrapRef.value && !wrapRef.value.contains(e.target)) {
    pickerOpen.value = false
  }
}

onMounted(() => document.addEventListener('click', onClickOutside, true))
onBeforeUnmount(() => document.removeEventListener('click', onClickOutside, true))
</script>

<style scoped>
.reaction-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

/* ===== 리액션 pill ===== */
.reaction-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px 3px 8px;
  border: 1px solid var(--border2);
  border-radius: 99px;
  background: var(--bg2);
  cursor: pointer;
  font-size: 13px;
  transition: border-color .15s, background .15s;
}
.reaction-pill:hover {
  border-color: var(--primary);
  background: var(--primary-bg, #e8f4ff);
}
.reaction-pill.reacted {
  border-color: var(--primary);
  background: var(--primary-bg, #e8f4ff);
  color: var(--primary);
  font-weight: 600;
}
.r-emoji { font-size: 15px; line-height: 1; }
.r-count { color: var(--t2); font-size: 12px; }
.reaction-pill.reacted .r-count { color: var(--primary); }

/* ===== 추가 버튼 ===== */
.reaction-add-wrap { position: relative; }
.reaction-add-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: 1px dashed var(--border2);
  border-radius: 99px;
  background: transparent;
  color: var(--t3);
  cursor: pointer;
  font-size: 16px;
  transition: border-color .15s, color .15s, background .15s;
}
.reaction-add-btn:hover,
.reaction-add-btn.active {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--primary-bg, #e8f4ff);
}

/* ===== 이모지 피커 ===== */
.emoji-picker {
  position: absolute;
  bottom: calc(100% + 6px);
  left: 0;
  display: flex;
  gap: 4px;
  padding: 8px;
  background: var(--bg1);
  border: 1px solid var(--border2);
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,.12);
  z-index: 200;
  white-space: nowrap;
}
.picker-emoji {
  font-size: 22px;
  padding: 4px;
  border-radius: 8px;
  border: 1px solid transparent;
  background: transparent;
  cursor: pointer;
  line-height: 1;
  transition: background .12s, border-color .12s, transform .1s;
}
.picker-emoji:hover { background: var(--bg2); transform: scale(1.2); }
.picker-emoji.reacted {
  background: var(--primary-bg, #e8f4ff);
  border-color: var(--primary);
}

/* ===== 등장 트랜지션 ===== */
.picker-fade-enter-active,
.picker-fade-leave-active { transition: opacity .12s, transform .12s; }
.picker-fade-enter-from,
.picker-fade-leave-to  { opacity: 0; transform: translateY(4px); }
</style>
