import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api/axios'
import { resolveFileUrl } from '@/utils/fileUrl'

export const useSiteStore = defineStore('site', () => {
  const siteName = ref('Corum')
  const faviconUrl = ref(null)
  const logoUrl = ref(null)
  const requireLoginSiteWide = ref(false)

  // 라우터 가드가 매 네비게이션마다 호출해도 실제 fetch는 한 번만 일어나도록
  // (동시에 여러 번 불려도 같은 진행중 Promise를 공유)
  let loadPromise = null

  async function fetchSettings() {
    if (loadPromise) return loadPromise
    loadPromise = (async () => {
      try {
        const res = await api.get('/site/public')
        const d = res.data.data
        siteName.value = d.siteName || 'Corum'
        faviconUrl.value = resolveFileUrl(d.faviconUrl) || null
        logoUrl.value = resolveFileUrl(d.logoUrl) || null
        requireLoginSiteWide.value = !!d.requireLoginSiteWide
        applyToDocument()
      } catch {
        loadPromise = null // 실패 시 다음 네비게이션에서 재시도할 수 있게
      }
    })()
    return loadPromise
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

  return { siteName, faviconUrl, logoUrl, requireLoginSiteWide, fetchSettings, applyToDocument }
})
