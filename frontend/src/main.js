import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import './style.css'
import { useSiteStore } from './stores/site'

const app = createApp(App)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
const pinia = createPinia()
app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.mount('#app')

// 앱 마운트 후 사이트 설정 로드 (title, favicon 적용)
useSiteStore().fetchSettings()

// PWA 설치 가능 조건(manifest + 등록된 서비스 워커) 충족을 위해 푸시 구독 여부와
// 무관하게 항상 등록한다 (푸시 수신은 stores/notification.js에서 구독 시점에 별도 처리)
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {})
  })
}
