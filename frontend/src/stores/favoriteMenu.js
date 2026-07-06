import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api/axios'

export const useFavoriteMenuStore = defineStore('favoriteMenu', () => {
  const favoriteIds = ref([])
  const loaded = ref(false)

  const favoriteIdSet = computed(() => new Set(favoriteIds.value))

  async function fetchFavorites(force = false) {
    if (loaded.value && !force) return
    try {
      const res = await api.get('/favorite-menus')
      favoriteIds.value = res.data.data || []
      loaded.value = true
    } catch { /* ignore */ }
  }

  function isFavorite(menuId) {
    return favoriteIdSet.value.has(Number(menuId))
  }

  async function addFavorite(menuId) {
    try {
      await api.post(`/favorite-menus/${menuId}`)
      if (!isFavorite(menuId)) favoriteIds.value.unshift(Number(menuId))
    } catch { /* ignore */ }
  }

  async function removeFavorite(menuId) {
    try {
      await api.delete(`/favorite-menus/${menuId}`)
      favoriteIds.value = favoriteIds.value.filter(id => id !== Number(menuId))
    } catch { /* ignore */ }
  }

  async function toggleFavorite(menuId) {
    if (isFavorite(menuId)) await removeFavorite(menuId)
    else await addFavorite(menuId)
  }

  function reset() {
    favoriteIds.value = []
    loaded.value = false
  }

  return {
    favoriteIds,
    loaded,
    fetchFavorites,
    isFavorite,
    addFavorite,
    removeFavorite,
    toggleFavorite,
    reset,
  }
})
