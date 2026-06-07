<template>
  <div class="admin-page-header">
    <div class="header-left">
      <h1 class="page-title">{{ title }}</h1>
      <p v-if="desc" class="page-desc">{{ desc }}</p>
    </div>
    <div class="header-right">
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({ title: String, desc: String })
</script>

<style scoped>
.admin-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
  gap: 12px;
  flex-wrap: wrap;
}
.page-title { font-size: 18px; font-weight: 700; color: var(--t1); letter-spacing: -0.4px; }
.page-desc { font-size: 13px; color: var(--t3); margin-top: 4px; }
.header-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
</style>
