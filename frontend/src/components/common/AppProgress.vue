<template>
  <Transition name="progress-fade">
    <div v-if="loading.active" class="progress-bar">
      <div class="progress-track" />
    </div>
  </Transition>
</template>

<script setup>
import { useLoadingStore } from '@/stores/loading'
const loading = useLoadingStore()
</script>

<style scoped>
.progress-bar {
  position: fixed;
  top: var(--header-height);
  left: 0;
  right: 0;
  height: 2px;
  z-index: 99;
  pointer-events: none;
}

.progress-track {
  height: 100%;
  /* 튀지 않도록 --primary를 옅게 섞어서 은은하게 */
  background: color-mix(in srgb, var(--primary) 55%, transparent);
  animation: progress-slide 1.2s cubic-bezier(0.4, 0, 0.2, 1) infinite;
  transform-origin: left center;
}

@keyframes progress-slide {
  0%   { transform: scaleX(0) translateX(0); opacity: 1; }
  60%  { transform: scaleX(0.85) translateX(0); opacity: 1; }
  100% { transform: scaleX(1) translateX(0); opacity: 0; }
}

.progress-fade-enter-active { transition: opacity 0.1s; }
.progress-fade-leave-active { transition: opacity 0.3s 0.08s; }
.progress-fade-enter-from,
.progress-fade-leave-to    { opacity: 0; }
</style>
