<template>
  <div class="board-write">
    <el-form :model="form" label-position="top">
      <el-form-item label="제목">
        <el-input v-model="form.title" placeholder="제목을 입력하세요." size="large" />
      </el-form-item>

      <el-form-item v-if="isAdmin || board?.useNotice" label="공지 설정">
        <el-checkbox v-model="form.isNotice" :disabled="!isAdmin && !board?.useNotice">공지글로 등록</el-checkbox>
      </el-form-item>

      <el-form-item label="내용">
        <RichEditor v-model="form.content" placeholder="내용을 입력하세요." min-height="400px" style="width:100%" />
      </el-form-item>

      <el-form-item label="파일 첨부">
        <el-upload
          v-model:file-list="fileList"
          :auto-upload="false"
          multiple
          :on-change="handleFileChange"
        >
          <el-button size="small">파일 선택</el-button>
          <template #tip>
            <div class="upload-tip">
              파일을 선택하면 저장 시 함께 업로드됩니다.
              <span v-if="board">
                최대 {{ board.fileMaxSizeMb || DEFAULT_MAX_MB }}MB,
                {{ board.fileMaxCount || DEFAULT_MAX_COUNT }}개까지
                <template v-if="board.fileAllowedExtensions"> / 허용: {{ board.fileAllowedExtensions }}</template>
              </span>
            </div>
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
import RichEditor from '@/components/common/RichEditor.vue'
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
const isAdmin = computed(() => authStore.member?.isAdmin)

const loading = ref(false)
const fileList = ref([])
const files = ref([])
const board = ref(null)

const form = ref({ title: '', content: '', isNotice: false })

async function fetchBoard() {
  if (!boardId.value) return
  try {
    const res = await api.get(`/boards/${boardId.value}`)
    board.value = res.data.data
  } catch {}
}

const DEFAULT_MAX_MB = 10
const DEFAULT_MAX_COUNT = 10

function validateFiles(newFiles) {
  if (!board.value) return true
  const maxMb = board.value.fileMaxSizeMb || DEFAULT_MAX_MB
  const maxCount = board.value.fileMaxCount || DEFAULT_MAX_COUNT
  const allowedExts = board.value.fileAllowedExtensions
    ? board.value.fileAllowedExtensions.split(',').map(e => e.trim().toLowerCase())
    : null

  if (files.value.length + newFiles.length > maxCount) {
    ElMessage.warning(`파일은 최대 ${maxCount}개까지 첨부 가능합니다.`)
    return false
  }
  for (const f of newFiles) {
    if (f.size > maxMb * 1024 * 1024) {
      ElMessage.warning(`파일 크기는 ${maxMb}MB 이하여야 합니다. (${f.name})`)
      return false
    }
    if (allowedExts) {
      const ext = f.name.split('.').pop().toLowerCase()
      if (!allowedExts.includes(ext)) {
        ElMessage.warning(`허용되지 않는 파일 형식입니다: .${ext} (허용: ${board.value.fileAllowedExtensions})`)
        return false
      }
    }
  }
  return true
}

function handleFileChange(file) {
  if (file.raw) {
    if (!validateFiles([file.raw])) {
      fileList.value = fileList.value.filter(f => f.uid !== file.uid)
      return
    }
    files.value.push(file.raw)
  }
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
      files.value.forEach(f => formData.append('files', f))
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

async function fetchPost() {
  if (!isEdit.value || !boardId.value) return
  const res = await api.get(`/boards/${boardId.value}/posts/${postId.value}`)
  const p = res.data.data
  form.value = {
    title: p.title,
    content: p.content,
    isNotice: p.isNotice
  }
}

watch([boardId, postId], async () => {
  await fetchBoard()
  fetchPost()
})

onMounted(async () => {
  if (route.params.menuId) await menuStore.fetchMenus()
  await fetchBoard()
  fetchPost()
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

.write-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
}

.upload-tip {
  font-size: 14px;
  color: var(--t3);
  margin-top: 4px;
}

@media (max-width: 768px) {
  .board-write { padding: 20px; }
}
</style>
