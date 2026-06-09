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
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  z-index: 9999;
  pointer-events: none;
}

.progress-track {
  height: 100%;
  background: linear-gradient(90deg, var(--accent) 0%, #7c4ff7 60%, #e05fc4 100%);
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
