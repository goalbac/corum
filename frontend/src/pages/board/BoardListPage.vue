<template>
  <div class="board-list">
    <div class="search-bar">
      <el-select v-model="searchType" size="small" class="search-type">
        <el-option value="title" label="제목" />
        <el-option value="content" label="내용" />
        <el-option value="writer" label="작성자" />
        <el-option value="all" label="전체" />
      </el-select>
      <el-input
        v-model="keyword"
        placeholder="검색어 입력"
        size="small"
        class="search-input"
        clearable
        @keyup.enter="handleSearch"
      />
      <el-button size="small" type="primary" @click="handleSearch">검색</el-button>
      <div class="write-area">
        <el-button size="small" type="primary" @click="goWrite">
          <i class="ti ti-pencil"></i> 글쓰기
        </el-button>
      </div>
    </div>

    <div v-if="board?.boardType === 'GALLERY'" v-loading="loading" class="gallery-grid">
      <button v-for="row in posts" :key="row.id" class="gallery-card" type="button" @click="goDetail(row)">
        <div class="thumb">
          <img v-if="row.thumbnailUrl" :src="row.thumbnailUrl" :alt="row.title" />
          <i v-else class="ti ti-photo"></i>
          <el-tag v-if="row.isNotice" class="notice-tag" type="danger" size="small" effect="plain">공지</el-tag>
        </div>
        <div class="gallery-title">{{ row.title }}</div>
        <div class="gallery-meta">
          <span>{{ row.writerName }}</span>
          <span>{{ formatDate(row.createdAt) }}</span>
          <span>조회 {{ row.viewCount }}</span>
        </div>
      </button>
      <el-empty v-if="!loading && !posts.length" description="등록된 게시글이 없습니다." />
    </div>

    <el-table
      v-else
      :data="posts"
      v-loading="loading"
      row-class-name="post-row"
      style="width:100%"
      @row-click="goDetail"
    >
      <el-table-column width="72" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isNotice" type="danger" size="small" effect="plain">공지</el-tag>
          <span v-else class="row-id">{{ row.id }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="board?.boardType === 'DOCUMENT' ? '자료명' : '제목'" min-width="280">
        <template #default="{ row }">
          <div class="title-cell">
            <i v-if="board?.boardType === 'DOCUMENT'" class="ti ti-file-text document-icon"></i>
            <span :class="['title-text', { notice: row.isNotice }]">{{ row.title }}</span>
            <span v-if="row.hasFile" class="file-icon"><i class="ti ti-paperclip"></i></span>
            <span v-if="row.commentCount > 0" class="comment-cnt">[{{ row.commentCount }}]</span>
            <span v-if="isNew(row.createdAt)" class="new-badge">N</span>
          </div>
          <div v-if="board?.boardType === 'DOCUMENT' && row.firstFileName" class="file-name">
            {{ row.firstFileName }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="작성자" width="120" align="center">
        <template #default="{ row }"><span class="meta-text">{{ row.writerName }}</span></template>
      </el-table-column>
      <el-table-column label="작성일" width="130" align="center">
        <template #default="{ row }"><span class="meta-text">{{ formatDate(row.createdAt) }}</span></template>
      </el-table-column>
      <el-table-column label="조회" width="82" align="center">
        <template #default="{ row }"><span class="meta-text">{{ row.viewCount }}</span></template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        background
        small
        @current-change="fetchPosts"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMenuStore } from '@/stores/menu'
import api from '@/api/axios'

const route = useRoute()
const router = useRouter()
const menuStore = useMenuStore()

const activeMenu = computed(() => menuStore.findMenuById(route.params.menuId))
const boardId = computed(() => route.params.boardId || activeMenu.value?.targetId)
const basePath = computed(() => route.params.menuId ? `/menu/${route.params.menuId}` : `/board/${boardId.value}`)

const board = ref(null)
const posts = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const searchType = ref('title')

async function fetchBoard() {
  if (!boardId.value) return
  const res = await api.get(`/boards/${boardId.value}`)
  board.value = res.data.data
}

async function fetchPosts() {
  if (!boardId.value) return
  loading.value = true
  try {
    const params = { page: page.value - 1, size: size.value }
    if (keyword.value) {
      params.keyword = keyword.value
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

function goWrite() {
  router.push(`${basePath.value}/write`)
}

function goDetail(row) {
  router.push(`${basePath.value}/posts/${row.id}`)
}

function formatDate(value) {
  if (!value) return ''
  const date = new Date(value)
  const today = new Date()
  if (date.toDateString() === today.toDateString()) return date.toTimeString().slice(0, 5)
  return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`
}

function isNew(value) {
  return value && Date.now() - new Date(value).getTime() < 3 * 86400000
}

watch(boardId, async () => {
  page.value = 1
  keyword.value = ''
  await fetchBoard()
  await fetchPosts()
})

onMounted(async () => {
  if (route.params.menuId) await menuStore.fetchMenus()
  await fetchBoard()
  await fetchPosts()
})
</script>

<style scoped>
.board-list { display: flex; flex-direction: column; padding: 0 0 4px; color: var(--t1); }
.search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 0 16px;
  border-bottom: 0.5px solid var(--border2);
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.search-type { width: 100px; }
.search-input { width: 260px; }
.write-area { margin-left: auto; }
.write-area :deep(.el-button i) { margin-right: 4px; }
:deep(.post-row) { cursor: pointer; }
.title-cell { display: flex; align-items: center; gap: 6px; min-width: 0; }
.title-text {
  font-size: 16px;
  color: var(--t1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.title-text.notice { font-weight: 800; color: var(--accent-t); }
.file-icon, .document-icon { font-size: 15px; color: var(--t3); }
.comment-cnt { font-size: 15px; color: var(--accent-t); }
.row-id, .meta-text { font-size: 15px; color: var(--t2); }
.row-id { color: var(--t3); }
.file-name { margin-top: 4px; font-size: 13px; color: var(--t3); }
.pagination { display: flex; justify-content: center; padding: 18px 0 0; }
.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 14px;
  min-height: 180px;
}
.gallery-card {
  border: 0.5px solid var(--border2);
  background: var(--surface);
  border-radius: var(--radius-sm);
  padding: 0;
  text-align: left;
  overflow: hidden;
  cursor: pointer;
  box-shadow: var(--shadow);
  transition: var(--transition);
}
.gallery-card:hover { transform: translateY(-2px); border-color: var(--accent); }
.thumb {
  position: relative;
  aspect-ratio: 4 / 3;
  background: var(--surface2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--t3);
  font-size: 34px;
}
.thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
.notice-tag { position: absolute; left: 10px; top: 10px; }
.gallery-title {
  padding: 12px 12px 6px;
  font-size: 16px;
  font-weight: 800;
  color: var(--t1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.gallery-meta {
  display: flex;
  gap: 8px;
  padding: 0 12px 12px;
  font-size: 13px;
  color: var(--t3);
  flex-wrap: wrap;
}
@media (max-width: 600px) {
  .search-type { width: 92px; }
  .search-input { width: 170px; }
  .write-area { margin-left: 0; width: 100%; display: flex; justify-content: flex-end; }
}
</style>
