<template>
  <div class="board-detail" v-loading="loading">
    <template v-if="post">
      <div class="post-header">
        <el-tag v-if="post.isNotice" type="danger" effect="plain" size="small">공지</el-tag>
        <h1 class="post-title">{{ post.title }}</h1>
        <div class="post-meta">
          <span>{{ post.writerName }}</span>
          <span class="meta-divider">·</span>
          <span>{{ formatDate(post.createdAt) }}</span>
          <span class="meta-divider">·</span>
          <span>조회 {{ post.viewCount }}</span>
        </div>
      </div>

      <div v-if="post.files?.length" class="attach-area">
        <div class="attach-title">
          <el-icon><Paperclip /></el-icon> 첨부 파일 ({{ post.files.length }})
        </div>
        <div class="attach-list">
          <a v-for="file in post.files" :key="file.id" :href="file.downloadUrl" class="attach-item" target="_blank">
            <el-icon><Document /></el-icon>
            <span>{{ file.originalName }}</span>
            <span class="file-size">({{ formatFileSize(file.fileSize) }})</span>
          </a>
        </div>
      </div>

      <section v-if="isDocumentBoard" class="version-area">
        <div class="version-head">
          <div>
            <strong>문서 버전</strong>
            <span>첨부 파일 기준으로 문서 변경 이력을 기록합니다.</span>
          </div>
          <el-button v-if="canEdit && post.files?.length" size="small" @click="showVersionDialog = true">
            <i class="ti ti-history"></i> 버전 등록
          </el-button>
        </div>
        <div v-if="versions.length" class="version-list">
          <a
            v-for="version in versions"
            :key="version.id"
            class="version-item"
            :href="version.file?.downloadUrl"
            target="_blank"
          >
            <b>v{{ version.versionNumber }}</b>
            <span>{{ version.file?.originalName || '파일 없음' }}</span>
            <small>{{ version.changeNote || '변경 메모 없음' }}</small>
            <em>{{ formatDate(version.createdAt) }}</em>
          </a>
        </div>
        <el-empty v-else description="등록된 문서 버전이 없습니다." :image-size="52" />
      </section>

      <div class="post-content" v-html="post.content" />

      <div class="post-actions">
        <el-button :type="post.liked ? 'primary' : 'default'" size="small" @click="handleLike">
          <el-icon><Star /></el-icon>
          좋아요 {{ post.likeCount }}
        </el-button>
        <el-button size="small" @click="handlePrint">
          <el-icon><Printer /></el-icon> 인쇄
        </el-button>
        <div class="right-actions">
          <el-button v-if="canEdit" size="small" @click="router.push(`${basePath}/posts/${postId}/edit`)">
            수정
          </el-button>
          <el-button v-if="canEdit" size="small" type="danger" @click="handleDelete">
            삭제
          </el-button>
          <el-button size="small" @click="router.push(basePath)">목록</el-button>
        </div>
      </div>

      <CommentSection :board-id="boardId" :post-id="postId" />
    </template>

    <el-dialog v-model="showVersionDialog" title="문서 버전 등록" width="460px">
      <el-form :model="versionForm" label-position="top">
        <el-form-item label="버전 파일">
          <el-select v-model="versionForm.fileId" style="width:100%">
            <el-option
              v-for="file in post?.files || []"
              :key="file.id"
              :label="file.originalName"
              :value="file.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="변경 메모">
          <el-input v-model="versionForm.changeNote" type="textarea" :rows="3" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showVersionDialog = false">취소</el-button>
        <el-button type="primary" @click="saveVersion">저장</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Paperclip, Document, Star, Printer } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import api from '@/api/axios'
import CommentSection from '@/components/board/CommentSection.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const menuStore = useMenuStore()

const activeMenu = computed(() => menuStore.findMenuById(route.params.menuId))
const boardId = computed(() => route.params.boardId || activeMenu.value?.targetId)
const postId = computed(() => route.params.postId)
const basePath = computed(() => route.params.menuId ? `/menu/${route.params.menuId}` : `/board/${boardId.value}`)
const post = ref(null)
const board = ref(null)
const versions = ref([])
const loading = ref(false)
const showVersionDialog = ref(false)
const versionForm = ref({ fileId: null, changeNote: '' })

const isDocumentBoard = computed(() => board.value?.boardType === 'DOCUMENT')
const canEdit = computed(() => {
  if (!authStore.isLoggedIn) return false
  if (authStore.member?.admin || authStore.member?.isAdmin) return true
  return authStore.member?.id === post.value?.memberId
})

async function fetchBoard() {
  if (!boardId.value) return
  const res = await api.get(`/boards/${boardId.value}`)
  board.value = res.data.data
}

async function fetchPost() {
  if (!boardId.value || !postId.value) return
  loading.value = true
  try {
    const res = await api.get(`/boards/${boardId.value}/posts/${postId.value}`)
    post.value = res.data.data
    versionForm.value.fileId = post.value?.files?.[0]?.id || null
    await fetchVersions()
  } finally {
    loading.value = false
  }
}

async function fetchVersions() {
  if (!postId.value || !isDocumentBoard.value) {
    versions.value = []
    return
  }
  const res = await api.get(`/posts/${postId.value}/document-versions`)
  versions.value = res.data.data || []
}

async function saveVersion() {
  if (!versionForm.value.fileId) return ElMessage.warning('버전 파일을 선택해주세요.')
  await api.post(`/posts/${postId.value}/document-versions`, versionForm.value)
  showVersionDialog.value = false
  versionForm.value.changeNote = ''
  await fetchVersions()
  ElMessage.success('문서 버전이 등록되었습니다.')
}

async function handleLike() {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('로그인이 필요합니다.')
    return
  }
  const res = await api.post(`/boards/${boardId.value}/posts/${postId.value}/like`)
  post.value.liked = res.data.data.liked
  post.value.likeCount += res.data.data.liked ? 1 : -1
}

async function handleDelete() {
  await ElMessageBox.confirm('게시글을 삭제할까요?', '삭제 확인', {
    type: 'warning',
    confirmButtonText: '삭제',
    cancelButtonText: '취소'
  })
  await api.delete(`/boards/${boardId.value}/posts/${postId.value}`)
  ElMessage.success('삭제되었습니다.')
  router.push(basePath.value)
}

function handlePrint() {
  window.print()
}

function formatDate(value) {
  if (!value) return ''
  return new Date(value).toLocaleString('ko-KR')
}

function formatFileSize(bytes) {
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / (1024 * 1024)).toFixed(1) + 'MB'
}

watch([boardId, postId], async () => {
  await fetchBoard()
  await fetchPost()
})

onMounted(async () => {
  if (route.params.menuId) await menuStore.fetchMenus()
  await fetchBoard()
  await fetchPost()
})
</script>

<style scoped>
.board-detail {
  background: var(--surface);
  border: 0.5px solid var(--border2);
  border-radius: var(--radius-sm);
  overflow: hidden;
  color: var(--t1);
  box-shadow: var(--shadow);
}
.post-header {
  padding: 28px 30px 22px;
  border-bottom: 1px solid var(--border2);
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.post-title { font-size: 24px; font-weight: 800; line-height: 1.45; color: var(--t1); }
.post-meta { display: flex; align-items: center; gap: 7px; font-size: 15px; color: var(--t2); flex-wrap: wrap; }
.meta-divider { color: var(--t3); }
.attach-area, .version-area { padding: 14px 30px; background: var(--surface2); border-bottom: 1px solid var(--border2); }
.attach-title {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 8px;
  color: var(--t1);
}
.attach-list { display: flex; flex-direction: column; gap: 5px; }
.attach-item, .version-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  color: var(--accent-t);
  padding: 5px 8px;
  border-radius: var(--radius-xs);
  transition: var(--transition);
  width: fit-content;
}
.attach-item:hover, .version-item:hover { background: var(--accent-bg); }
.file-size { color: var(--t3); font-size: 14px; }
.version-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}
.version-head strong { display: block; color: var(--t1); font-size: 15px; }
.version-head span { display: block; color: var(--t3); font-size: 13px; margin-top: 3px; }
.version-list { display: flex; flex-direction: column; gap: 5px; }
.version-item {
  background: var(--surface);
  border: 0.5px solid var(--border2);
  color: var(--t1);
}
.version-item b { color: var(--accent-t); }
.version-item small { color: var(--t2); }
.version-item em { color: var(--t3); font-style: normal; margin-left: 4px; }
.post-content {
  padding: 30px;
  min-height: 220px;
  font-size: 16px;
  line-height: 1.85;
  color: var(--t1);
  border-bottom: 1px solid var(--border2);
}
.post-content :deep(*) { color: inherit; }
.post-content :deep(a) { color: var(--accent-t); }
.post-content :deep(table) { max-width: 100%; border-color: var(--border); }
.post-content :deep(img) { max-width: 100%; height: auto; }
.post-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 30px;
  border-bottom: 1px solid var(--border2);
  background: var(--surface2);
}
.right-actions { margin-left: auto; display: flex; gap: 8px; }
@media (max-width: 768px) {
  .post-header, .attach-area, .version-area, .post-content, .post-actions { padding-left: 18px; padding-right: 18px; }
  .post-title { font-size: 21px; }
  .post-actions, .version-head { flex-wrap: wrap; }
  .right-actions { width: 100%; justify-content: flex-end; }
}
</style>
