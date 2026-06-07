<template>
  <div class="board-write">
    <el-form :model="form" label-position="top">
      <el-form-item label="제목">
        <el-input v-model="form.title" placeholder="제목을 입력하세요." size="large" />
      </el-form-item>

      <el-form-item v-if="isAdmin" label="공지 설정">
        <el-checkbox v-model="form.isNotice">공지글로 등록</el-checkbox>
      </el-form-item>

      <el-form-item label="내용">
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
          :on-change="handleFileChange"
        >
          <el-button size="small">파일 선택</el-button>
          <template #tip>
            <div class="upload-tip">파일을 선택하면 저장할 때 함께 업로드됩니다.</div>
          </template>
        </el-upload>
      </el-form-item>

      <div class="write-actions">
        <el-button @click="$router.back()">취소</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">
          {{ isEdit ? '수정' : '등록' }}
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import api from '@/api/axios'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const boardId = computed(() => route.params.boardId)
const postId = computed(() => route.params.postId)
const isEdit = computed(() => !!postId.value)
const isAdmin = computed(() => authStore.member?.admin)

const loading = ref(false)
const fileList = ref([])
const files = ref([])

const form = ref({
  title: '',
  content: '',
  isNotice: false
})

function handleFileChange(file) {
  files.value.push(file.raw)
}

async function handleSubmit() {
  if (!form.value.title.trim()) {
    ElMessage.warning('제목을 입력해주세요.')
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
    router.push(`/board/${boardId.value}/posts/${savedPostId}`)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  if (isEdit.value) {
    const res = await api.get(`/boards/${boardId.value}/posts/${postId.value}`)
    const p = res.data.data
    form.value = {
      title: p.title,
      content: p.content,
      isNotice: p.isNotice
    }
  }
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
