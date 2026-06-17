<template>
  <div class="admin-tools">
    <AdminPageHeader title="시스템 도구" desc="관리자 전용 유지보수 작업을 실행합니다." />

    <div class="tools-grid">
      <!-- 썸네일 일괄 재생성 -->
      <div class="tool-card">
        <div class="tool-head">
          <div class="tool-icon" style="background:#EFF6FF; color:#2563EB">
            <i class="ti ti-photo"></i>
          </div>
          <div>
            <div class="tool-title">썸네일 일괄 재생성</div>
            <div class="tool-desc">thumbnailPath가 없는 첨부 이미지 파일의 썸네일을 새로 생성합니다.</div>
          </div>
        </div>
        <div v-if="thumb.result" class="tool-result" :class="thumb.result.fail > 0 ? 'warn' : 'ok'">
          <div class="result-row">
            <span class="result-label">대상</span><span class="result-val">{{ thumb.result.total }}개</span>
          </div>
          <div class="result-row">
            <span class="result-label">성공</span><span class="result-val success">{{ thumb.result.success }}개</span>
          </div>
          <div class="result-row" v-if="thumb.result.fail > 0">
            <span class="result-label">실패</span><span class="result-val error">{{ thumb.result.fail }}개</span>
          </div>
          <template v-if="thumb.result.failedPaths?.length">
            <div class="result-paths-label">실패 파일</div>
            <div v-for="p in thumb.result.failedPaths" :key="p" class="result-path">{{ p }}</div>
          </template>
        </div>
        <button class="run-btn" :disabled="thumb.loading" @click="runThumb">
          <i v-if="thumb.loading" class="ti ti-loader-2 spinning"></i>
          <i v-else class="ti ti-player-play"></i>
          {{ thumb.loading ? '실행 중...' : '실행' }}
        </button>
      </div>

      <!-- 썸네일 전체 재생성 -->
      <div class="tool-card">
        <div class="tool-head">
          <div class="tool-icon" style="background:#FEF9C3; color:#D97706">
            <i class="ti ti-refresh"></i>
          </div>
          <div>
            <div class="tool-title">썸네일 전체 재생성</div>
            <div class="tool-desc">기존 썸네일 포함 모든 이미지 파일의 썸네일을 새 품질 설정으로 다시 만듭니다.</div>
          </div>
        </div>
        <div v-if="thumbAll.result" class="tool-result" :class="thumbAll.result.fail > 0 ? 'warn' : 'ok'">
          <div class="result-row">
            <span class="result-label">대상</span><span class="result-val">{{ thumbAll.result.total }}개</span>
          </div>
          <div class="result-row">
            <span class="result-label">성공</span><span class="result-val success">{{ thumbAll.result.success }}개</span>
          </div>
          <div class="result-row" v-if="thumbAll.result.fail > 0">
            <span class="result-label">실패</span><span class="result-val error">{{ thumbAll.result.fail }}개</span>
          </div>
          <template v-if="thumbAll.result.failedPaths?.length">
            <div class="result-paths-label">실패 파일</div>
            <div v-for="p in thumbAll.result.failedPaths" :key="p" class="result-path">{{ p }}</div>
          </template>
        </div>
        <button class="run-btn" :disabled="thumbAll.loading" @click="runThumbAll">
          <i v-if="thumbAll.loading" class="ti ti-loader-2 spinning"></i>
          <i v-else class="ti ti-player-play"></i>
          {{ thumbAll.loading ? '실행 중...' : '실행' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminPageHeader from '@/components/admin/AdminPageHeader.vue'
import api from '@/api/axios'

const thumb = reactive({ loading: false, result: null })
const thumbAll = reactive({ loading: false, result: null })

async function runThumbAll() {
  await ElMessageBox.confirm(
    '기존 썸네일을 포함한 모든 이미지 파일의 썸네일을 새 품질(600px/65%, 300px/50%)로 다시 생성합니다.\n파일 수가 많을 경우 시간이 걸릴 수 있습니다. 계속하시겠습니까?',
    '썸네일 전체 재생성',
    { confirmButtonText: '실행', cancelButtonText: '취소', type: 'warning' }
  )
  thumbAll.loading = true
  thumbAll.result = null
  try {
    const res = await api.post('/admin/files/regenerate-thumbnails-all')
    thumbAll.result = res.data.data
    const { total, success, fail } = thumbAll.result
    if (fail === 0) {
      ElMessage.success(`완료: ${total}개 중 ${success}개 성공`)
    } else {
      ElMessage.warning(`완료: ${success}개 성공, ${fail}개 실패`)
    }
  } catch {
    ElMessage.error('실행 중 오류가 발생했습니다.')
  } finally {
    thumbAll.loading = false
  }
}

async function runThumb() {
  await ElMessageBox.confirm(
    'thumbnailPath가 없는 이미지 파일을 대상으로 썸네일을 새로 생성합니다. 계속하시겠습니까?',
    '썸네일 일괄 재생성',
    { confirmButtonText: '실행', cancelButtonText: '취소', type: 'warning' }
  )

  thumb.loading = true
  thumb.result = null
  try {
    const res = await api.post('/admin/files/regenerate-thumbnails')
    thumb.result = res.data.data
    const { total, success, fail } = thumb.result
    if (fail === 0) {
      ElMessage.success(`완료: ${total}개 중 ${success}개 성공`)
    } else {
      ElMessage.warning(`완료: ${success}개 성공, ${fail}개 실패`)
    }
  } catch {
    ElMessage.error('실행 중 오류가 발생했습니다.')
  } finally {
    thumb.loading = false
  }
}
</script>

<style scoped>
.admin-tools { display: flex; flex-direction: column; gap: 20px; }

.tools-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); gap: 16px; }

.tool-card {
  background: var(--surface);
  border: 0.5px solid var(--border2);
  border-radius: var(--radius-sm);
  padding: 20px;
  box-shadow: var(--shadow);
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.tool-card.placeholder { opacity: 0.5; }

.tool-head { display: flex; align-items: flex-start; gap: 14px; }
.tool-icon {
  width: 44px; height: 44px; border-radius: var(--radius-sm);
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; flex-shrink: 0;
}
.tool-title { font-size: 15px; font-weight: 700; color: var(--t1); margin-bottom: 4px; }
.tool-desc { font-size: 13px; color: var(--t3); line-height: 1.5; }

.tool-result {
  background: var(--surface2);
  border-radius: 6px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  border-left: 3px solid var(--border);
}
.tool-result.ok { border-color: #16A34A; }
.tool-result.warn { border-color: #F59E0B; }

.result-row { display: flex; align-items: center; gap: 10px; font-size: 13px; }
.result-label { color: var(--t3); width: 32px; flex-shrink: 0; }
.result-val { color: var(--t1); font-weight: 600; }
.result-val.success { color: #16A34A; }
.result-val.error { color: #EF4444; }
.result-paths-label { font-size: 12px; color: var(--t3); margin-top: 4px; }
.result-path { font-size: 11px; color: var(--t2); font-family: monospace; word-break: break-all; }

.run-btn {
  align-self: flex-start;
  display: inline-flex; align-items: center; gap: 6px;
  padding: 7px 16px;
  border-radius: 6px;
  border: none;
  background: var(--accent);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity .15s;
}
.run-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.run-btn:not(:disabled):hover { opacity: 0.85; }

@keyframes spin { to { transform: rotate(360deg); } }
.spinning { display: inline-block; animation: spin 0.8s linear infinite; }
</style>
