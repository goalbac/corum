<template>
  <div class="board-write">
    <el-form :model="form" label-position="top">
      <el-form-item label="제목">
        <el-input v-model="form.title" placeholder="제목을 입력하세요." size="large" />
      </el-form-item>

      <el-form-item v-if="isAdmin" label="공지 설정">
        <el-checkbox v-model="form.isNotice">공지글로 등록</el-checkbox>
      </el-form-item>

      <el-form-item :label="board?.boardType === 'DOCUMENT' ? '자료 설명' : '내용'">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="15"
          placeholder="내용을 입력하세요."
          resize="none"
        />
      </el-form-item>

      <el-form-item label="파일 첨부">
        <el-upload
          v-model:file-list="fileList"
          :auto-upload="false"
          multiple
          :accept="acceptExtensions"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
        >
          <el-button size="small">파일 선택</el-button>
          <template #tip>
            <div class="upload-tip">{{ uploadTip }}</div>
          </template>
        </el-upload>
      </el-form-item>

      <div class="write-actions">
        <el-button @click="router.push(basePath)">취소</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">
          {{ isEdit ? '수정' : '등록' }}
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import api from '@/api/axios'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const menuStore = useMenuStore()

const activeMenu = computed(() => menuStore.findMenuById(route.params.menuId))
const boardId = computed(() => route.params.boardId || activeMenu.value?.targetId)
const postId = computed(() => route.params.postId)
const basePath = computed(() => route.params.menuId ? `/menu/${route.params.menuId}` : `/board/${boardId.value}`)
const isEdit = computed(() => !!postId.value)
const isAdmin = computed(() => authStore.member?.admin || authStore.member?.isAdmin)

const board = ref(null)
const loading = ref(false)
const fileList = ref([])
const files = ref([])

const form = ref({ title: '', content: '', isNotice: false })

const allowedExtensions = computed(() =>
  (board.value?.fileAllowedExtensions || '')
    .split(',')
    .map(v => v.trim().replace(/^\./, '').toLowerCase())
    .filter(Boolean)
)
const acceptExtensions = computed(() => allowedExtensions.value.map(ext => `.${ext}`).join(','))
const uploadTip = computed(() => {
  const count = board.value?.fileMaxCount || 5
  const size = board.value?.fileMaxSizeMb ? `${board.value.fileMaxSizeMb}MB` : '전역 설정 용량'
  const ext = allowedExtensions.value.length ? allowedExtensions.value.join(', ') : '전역 설정 확장자'
  return `최대 ${count}개, 파일당 ${size}. 허용 확장자: ${ext}`
})

function handleFileChange(file, nextFileList) {
  fileList.value = nextFileList
  files.value = nextFileList.map(item => item.raw).filter(Boolean)
  if (board.value?.fileMaxCount && files.value.length > board.value.fileMaxCount) {
    ElMessage.warning(`첨부 파일은 최대 ${board.value.fileMaxCount}개까지 선택할 수 있습니다.`)
    fileList.value = nextFileList.slice(0, board.value.fileMaxCount)
    files.value = fileList.value.map(item => item.raw).filter(Boolean)
  }
}

function handleFileRemove(file, nextFileList) {
  fileList.value = nextFileList
  files.value = nextFileList.map(item => item.raw).filter(Boolean)
}

async function handleSubmit() {
  if (!form.value.title.trim()) {
    ElMessage.warning('제목을 입력해주세요.')
    return
  }
  if (!boardId.value) {
    ElMessage.error('연결된 게시판을 찾을 수 없습니다.')
    return
  }

  loading.value = true
  try {
    let savedPostId = postId.value

    if (isEdit.value) {
      await api.put(`/boards/${boardId.value}/posts/${postId.value}`, form.value)
    } else {
      const res = await api.post(`/boards/${boardId.value}/posts`, form.value)
      savedPostId = res.data.data.id
    }

    if (files.value.length > 0) {
      const formData = new FormData()
      files.value.forEach(file => formData.append('files', file))
      await api.post(`/posts/${savedPostId}/files`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
    }

    ElMessage.success(isEdit.value ? '수정되었습니다.' : '등록되었습니다.')
    router.push(`${basePath.value}/posts/${savedPostId}`)
  } finally {
    loading.value = false
  }
}

async function fetchBoard() {
  if (!boardId.value) return
  const res = await api.get(`/boards/${boardId.value}`)
  board.value = res.data.data
}

async function fetchPost() {
  if (!isEdit.value || !boardId.value) return
  const res = await api.get(`/boards/${boardId.value}/posts/${postId.value}`)
  const post = res.data.data
  form.value = { title: post.title, content: post.content, isNotice: post.isNotice }
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
.board-write {
  background: var(--surface);
  border: 0.5px solid var(--border2);
  border-radius: var(--radius-sm);
  padding: 30px;
  color: var(--t1);
  box-shadow: var(--shadow);
}
.write-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 10px; }
.upload-tip { font-size: 14px; color: var(--t3); margin-top: 4px; }
@media (max-width: 768px) {
  .board-write { padding: 20px; }
}
</style>
