import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api/axios'
import { resolveFileUrl } from '@/utils/fileUrl'

export const useSiteStore = defineStore('site', () => {
  const siteName = ref('Corum')
  const faviconUrl = ref(null)
  const logoUrl = ref(null)

  async function fetchSettings() {
    try {
      const res = await api.get('/site/public')
      const d = res.data.data
      siteName.value = d.siteName || 'Corum'
      faviconUrl.value = resolveFileUrl(d.faviconUrl) || null
      logoUrl.value = resolveFileUrl(d.logoUrl) || null
      applyToDocument()
    } catch { /* ignore */ }
  }

  function applyToDocument() {
    document.title = siteName.value
    setFavicon(faviconUrl.value || '/favicon.svg')
  }

  function setFavicon(url) {
    // href만 바꾸면 브라우저 캐시가 무시하므로 노드를 제거하고 새로 추가
    document.querySelectorAll('link[rel="icon"], link[rel="shortcut icon"]').forEach(el => el.remove())
    const link = document.createElement('link')
    link.rel = 'icon'
    // 캐시버스팅: 커스텀 파비콘에만 타임스탬프 붙임
    link.href = url === '/favicon.svg' ? url : `${url}${url.includes('?') ? '&' : '?'}_t=${Date.now()}`
    document.head.appendChild(link)
  }

  return { siteName, faviconUrl, logoUrl, fetchSettings, applyToDocument }
})
