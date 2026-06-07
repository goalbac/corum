<template>
  <div :data-theme="themeStore.isDark ? 'dark' : undefined">
    <router-view />
  </div>
</template>

<script setup>
import { useThemeStore } from '@/stores/theme'
const themeStore = useThemeStore()
</script>

<style>
div[data-theme] { min-height: 100vh; background: var(--bg); transition: background 0.25s; }
</style>