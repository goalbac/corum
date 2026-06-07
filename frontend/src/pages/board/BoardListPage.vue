<template>
  <div class="board-list">
    <!-- 검색 -->
    <div class="search-bar">
      <el-select v-model="searchType" size="small" style="width:88px">
        <el-option value="title" label="제목" />
        <el-option value="content" label="내용" />
        <el-option value="writer" label="작성자" />
        <el-option value="all" label="전체" />
      </el-select>
      <el-input v-model="keyword" placeholder="검색어 입력" size="small" style="width:200px" clearable @keyup.enter="handleSearch" />
      <el-button size="small" type="primary" @click="handleSearch">검색</el-button>
      <div style="margin-left:auto">
        <el-button size="small" type="primary" @click="$router.push(`/board/${boardId}/write`)">
          <i class="ti ti-pencil" style="margin-right:4px"></i>글쓰기
        </el-button>
      </div>
    </div>

    <!-- 목록 -->
    <el-table :data="posts" v-loading="loading" @row-click="r => $router.push(`/board/${boardId}/posts/${r.id}`)" row-class-name="post-row" style="width:100%">
      <el-table-column width="60" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isNotice" type="danger" size="small" effect="plain">공지</el-tag>
          <span v-else class="row-id">{{ row.id }}</span>
        </template>
      </el-table-column>
      <el-table-column label="제목" min-width="280">
        <template #default="{ row }">
          <div class="title-cell">
            <span :class="['title-text', { notice: row.isNotice }]">{{ row.title }}</span>
            <span v-if="row.hasFile" class="file-icon"><i class="ti ti-paperclip"></i></span>
            <span v-if="row.commentCount > 0" class="comment-cnt">[{{ row.commentCount }}]</span>
            <span v-if="isNew(row.createdAt)" class="new-badge">N</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="작성자" width="90" align="center">
        <template #default="{ row }"><span class="meta-text">{{ row.writerName }}</span></template>
      </el-table-column>
      <el-table-column label="작성일" width="95" align="center">
        <template #default="{ row }"><span class="meta-text">{{ formatDate(row.createdAt) }}</span></template>
      </el-table-column>
      <el-table-column label="조회" width="60" align="center">
        <template #default="{ row }"><span class="meta-text">{{ row.viewCount }}</span></template>
      </el-table-column>
    </el-table>

    <!-- 페이지네이션 -->
    <div class="pagination">
      <el-pagination v-model:current-page="page" :page-size="size" :total="total"
        layout="prev, pager, next" background small @current-change="fetchPosts" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api/axios'

const route = useRoute()
const router = useRouter()
const boardId = ref(route.params.boardId)
const posts = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const searchType = ref('title')

async function fetchPosts() {
  loading.value = true
  try {
    const params = { page: page.value - 1, size: size.value }
    if (keyword.value) { params.keyword = keyword.value; params.searchType = searchType.value }
    const res = await api.get(`/boards/${boardId.value}/posts`, { params })
    posts.value = res.data.data?.content || []
    total.value = res.data.data?.totalElements || 0
  } finally { loading.value = false }
}

function handleSearch() { page.value = 1; fetchPosts() }

function formatDate(d) {
  if (!d) return ''
  const dt = new Date(d)
  const today = new Date()
  if (dt.toDateString() === today.toDateString()) return dt.toTimeString().slice(0,5)
  return `${dt.getFullYear()}.${String(dt.getMonth()+1).padStart(2,'0')}.${String(dt.getDate()).padStart(2,'0')}`
}

function isNew(d) { return d && Date.now() - new Date(d).getTime() < 3 * 86400000 }

watch(() => route.params.boardId, id => { boardId.value = id; page.value = 1; keyword.value = ''; fetchPosts() })
onMounted(fetchPosts)
</script>

<style scoped>
.board-list { display: flex; flex-direction: column; }
.search-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 0 14px;
  border-bottom: 0.5px solid var(--border2);
  margin-bottom: 4px;
  flex-wrap: wrap;
}
.post-row { cursor: pointer; }
.title-cell { display: flex; align-items: center; gap: 5px; }
.title-text { font-size: 13.5px; color: var(--t1); }
.title-text.notice { font-weight: 600; color: var(--accent); }
.file-icon { font-size: 13px; color: var(--t3); }
.comment-cnt { font-size: 12px; color: var(--accent); }
.row-id { font-size: 12px; color: var(--t3); }
.meta-text { font-size: 12.5px; color: var(--t2); }
.pagination { display: flex; justify-content: center; padding: 16px 0 0; }

@media (max-width: 600px) {
  .search-bar > .el-input { width: 150px !important; }
}
</style>
