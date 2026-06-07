<template>
  <div class="board-list">

    <!-- 검색 -->
    <div class="search-bar">
      <el-select v-model="searchType" size="small" style="width: 100px;">
        <el-option value="title"   label="제목" />
        <el-option value="content" label="내용" />
        <el-option value="writer"  label="작성자" />
        <el-option value="all"     label="전체" />
      </el-select>
      <el-input
        v-model="keyword"
        placeholder="검색어를 입력하세요"
        size="small"
        style="width: 240px;"
        @keyup.enter="handleSearch"
        clearable
      />
      <el-button size="small" type="primary" @click="handleSearch">검색</el-button>
      <div style="margin-left: auto;">
        <el-button size="small" type="primary" @click="$router.push(`/board/${boardId}/write`)">
          글쓰기
        </el-button>
      </div>
    </div>

    <!-- 목록 테이블 -->
    <el-table
      :data="posts"
      v-loading="loading"
      class="post-table"
      @row-click="handleRowClick"
      row-class-name="post-row"
    >
      <el-table-column width="60" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isNotice" type="danger" size="small" effect="plain">공지</el-tag>
          <span v-else class="row-num">{{ row.id }}</span>
        </template>
      </el-table-column>

      <el-table-column label="제목" min-width="300">
        <template #default="{ row }">
          <div class="title-cell">
            <span :class="{ 'notice-title': row.isNotice }">{{ row.title }}</span>
            <el-icon v-if="row.hasFile" class="file-icon"><Paperclip /></el-icon>
            <span v-if="row.commentCount > 0" class="comment-count">[{{ row.commentCount }}]</span>
            <span v-if="isNew(row.createdAt)" class="new-label">N</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="작성자" width="100" align="center">
        <template #default="{ row }">
          <span class="writer">{{ row.writerName }}</span>
        </template>
      </el-table-column>

      <el-table-column label="작성일" width="100" align="center">
        <template #default="{ row }">
          <span class="date">{{ formatDate(row.createdAt) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="조회" width="70" align="center">
        <template #default="{ row }">
          <span class="count">{{ row.viewCount }}</span>
        </template>
      </el-table-column>

      <el-table-column label="좋아요" width="70" align="center">
        <template #default="{ row }">
          <span class="count">{{ row.likeCount }}</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 페이지네이션 -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="fetchPosts"
      />
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Paperclip } from '@element-plus/icons-vue'
import api from '@/api/axios'

const route    = useRoute()
const router   = useRouter()
const boardId  = ref(route.params.boardId)

const posts    = ref([])
const loading  = ref(false)
const total    = ref(0)
const page     = ref(1)
const size     = ref(20)
const keyword  = ref('')
const searchType = ref('title')

async function fetchPosts() {
  loading.value = true
  try {
    const params = { page: page.value - 1, size: size.value }
    if (keyword.value) {
      params.keyword    = keyword.value
      params.searchType = searchType.value
    }
    const res = await api.get(`/boards/${boardId.value}/posts`, { params })
    posts.value = res.data.data?.content || []
    total.value = res.data.data?.totalElements || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  fetchPosts()
}

function handleRowClick(row) {
  router.push(`/board/${boardId.value}/posts/${row.id}`)
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const today = new Date()
  if (d.toDateString() === today.toDateString()) {
    return d.toTimeString().slice(0, 5)
  }
  return `${d.getFullYear()}.${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')}`
}

function isNew(dateStr) {
  if (!dateStr) return false
  const diff = Date.now() - new Date(dateStr).getTime()
  return diff < 3 * 24 * 60 * 60 * 1000
}

watch(() => route.params.boardId, (id) => {
  boardId.value = id
  page.value = 1
  keyword.value = ''
  fetchPosts()
})

onMounted(fetchPosts)
</script>

<style scoped>
.board-list {
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-base);
}

.post-table {
  width: 100%;
}

.post-table :deep(.post-row) {
  cursor: pointer;
}

.post-table :deep(.post-row:hover td) {
  background: var(--color-primary-bg) !important;
}

.title-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.notice-title {
  font-weight: 600;
  color: var(--color-primary);
}

.file-icon {
  font-size: 13px;
  color: var(--color-text-muted);
}

.comment-count {
  font-size: 12px;
  color: var(--color-primary);
}

.new-label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  background: var(--color-danger);
  color: #fff;
  font-size: 8px;
  font-weight: 700;
  border-radius: 50%;
}

.writer, .date, .count, .row-num {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.pagination {
  display: flex;
  justify-content: center;
  padding: 16px;
  border-top: 1px solid var(--color-border);
}
</style>
