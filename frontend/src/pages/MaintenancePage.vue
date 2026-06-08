<template>
  <div class="maintenance">
    <div class="maintenance-box">
      <i class="ti ti-tool maintenance-icon"></i>
      <h1>서비스 점검 중</h1>
      <p class="message">{{ message || '더 나은 서비스를 위해 점검 중입니다.' }}</p>
      <p v-if="until" class="until">점검 예정 종료: {{ formatUntil(until) }}</p>
    </div>
  </div>
</template>

<script setup>
defineProps({
  message: String,
  until: String
})

function formatUntil(dt) {
  if (!dt) return ''
  const d = new Date(dt)
  return `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}
</script>

<style scoped>
.maintenance {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg);
}

.maintenance-box {
  text-align: center;
  padding: 60px 40px;
  border-radius: var(--radius-lg);
  background: var(--surface);
  box-shadow: var(--shadow);
  max-width: 480px;
  width: 90%;
}

.maintenance-icon {
  font-size: 56px;
  color: var(--accent);
  margin-bottom: 24px;
  display: block;
}

h1 {
  font-size: 28px;
  font-weight: 800;
  color: var(--t1);
  margin-bottom: 16px;
}

.message {
  font-size: 16px;
  color: var(--t2);
  line-height: 1.7;
  margin-bottom: 12px;
}

.until {
  font-size: 14px;
  color: var(--t3);
}
</style>
