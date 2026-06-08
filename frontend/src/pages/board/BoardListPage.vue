<template>
  <div class="board-list">

    <!-- 갤러리 뷰 -->
    <template v-if="board?.boardType === 'GALLERY'">
      <div class="gallery-toolbar">
        <div class="search-area">
          <el-input v-model="keyword" placeholder="검색어 입력" size="small" clearable @keyup.enter="handleSearch" style="width:200px" />
          <el-button size="small" type="primary" @click="handleSearch">검색</el-button>
        </div>
        <el-button v-if="canWrite" size="small" type="primary" @click="goWrite">
          <i class="ti ti-pencil"></i> 글쓰기
        </el-button>
      </div>
      <div v-loading="loading" class="gallery-grid">
        <div v-for="post in posts" :key="post.id" class="gallery-card" @click="goDetail(post)">
          <div class="gallery-thumb">
            <img v-if="post.thumbnailUrl" :src="post.thumbnailUrl" :alt="post.title" @error="e => e.target.style.display='none'" />
            <div v-else class="no-thumb"><i class="ti ti-photo-off"></i></div>
          </div>
          <div class="gallery-info">
            <div class="gallery-title">{{ post.title }}</div>
            <div class="gallery-meta">{{ post.writerName }} · {{ formatDate(post.createdAt) }}</div>
          </div>
        </div>
        <div v-if="!loading && !posts.length" class="gallery-empty">등록된 게시글이 없습니다.</div>
      </div>
      <div class="pagination">
        <el-pagination v-model:current-page="page" :page-size="gallerySize" :total="total"
          layout="prev, pager, next" background small @current-change="fetchPosts" />
      </div>
    </template>

    <!-- 일반/자료실 테이블 뷰 -->
    <template v-else>
      <div class="search-bar">
        <el-select v-model="searchType" size="small" class="search-type">
          <el-option value="title" label="제목" />
          <el-option value="content" label="내용" />
          <el-option value="writer" label="작성자" />
          <el-option value="all" label="전체" />
        </el-select>
        <el-input v-model="keyword" placeholder="검색어 입력" size="small" class="search-input"
          clearable @keyup.enter="handleSearch" />
        <el-button size="small" type="primary" @click="handleSearch">검색</el-button>
        <div class="write-area">
          <el-button v-if="canWrite" size="small" type="primary" @click="goWrite">
            <i class="ti ti-pencil"></i>글쓰기
          </el-button>
        </div>
      </div>

      <el-table :data="posts" v-loading="loading" row-class-name="post-row" style="width:100%" @row-click="goDetail">
        <el-table-column width="72" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isNotice" type="danger" size="small" effect="dark">공지</el-tag>
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
        <el-table-column label="작성자" width="120" align="center">
          <template #default="{ row }"><span class="meta-text">{{ row.writerName }}</span></template>
        </el-table-column>
        <el-table-column label="작성일" width="130" align="center">
          <template #default="{ row }"><span class="meta-text">{{ formatDate(row.createdAt) }}</span></template>
        </el-table-column>
        <el-table-column v-if="showViewCount" label="조회" width="82" align="center">
          <template #default="{ row }"><span class="meta-text">{{ row.viewCount }}</span></template>
        </el-table-column>
        <el-table-column v-if="showLikeCount && board?.useLike" label="좋아요" width="82" align="center">
          <template #default="{ row }"><span class="meta-text">{{ row.likeCount }}</span></template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total"
          layout="prev, pager, next" background small @current-change="fetchPosts" />
      </div>
    </template>

  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMenuStore } from '@/stores/menu'
import { useAuthStore } from '@/stores/auth'
import api from '@/api/axios'

const route = useRoute()
const router = useRouter()
const menuStore = useMenuStore()
const authStore = useAuthStore()

const activeMenu = computed(() => menuStore.findMenuById(route.params.menuId))
const boardId = computed(() => route.params.boardId || activeMenu.value?.targetId)
const basePath = computed(() => route.params.menuId ? `/menu/${route.params.menuId}` : `/board/${boardId.value}`)

const board = ref(null)
const posts = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(20)
const gallerySize = ref(12)
const keyword = ref('')
const searchType = ref('title')

// 권한 체크: board.permissions에서 회원 그룹 매칭
const canWrite = computed(() => {
  if (!board.value) return false
  const perms = board.value.permissions || []
  if (!perms.length) return true // 권한 없음 = 공개
  if (!authStore.isLoggedIn) return false
  const memberGroupIds = authStore.member?.groupIds || []
  return perms.some(p => memberGroupIds.includes(p.groupId) && p.canWrite)
})

// 컬럼 표시 설정 (추후 board 설정 연동 가능)
const showViewCount = ref(true)
const showLikeCount = ref(true)

async function fetchBoard() {
  if (!boardId.value) return
  try {
    const res = await api.get(`/boards/${boardId.value}`)
    board.value = res.data.data
  } catch {}
}

async function fetchPosts() {
  if (!boardId.value) return
  loading.value = true
  try {
    const pageSize = board.value?.boardType === 'GALLERY' ? gallerySize.value : size.value
    const params = { page: page.value - 1, size: pageSize }
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

function formatDate(d) {
  if (!d) return ''
  const dt = new Date(d)
  const today = new Date()
  if (dt.toDateString() === today.toDateString()) return dt.toTimeString().slice(0, 5)
  return `${dt.getFullYear()}.${String(dt.getMonth() + 1).padStart(2, '0')}.${String(dt.getDate()).padStart(2, '0')}`
}

function isNew(d) {
  return d && Date.now() - new Date(d).getTime() < 3 * 86400000
}

watch(boardId, async () => {
  page.value = 1
  keyword.value = ''
  await fetchBoard()
  fetchPosts()
})

onMounted(async () => {
  if (route.params.menuId) await menuStore.fetchMenus()
  await fetchBoard()
  fetchPosts()
})
</script>

<style scoped>
.board-list {
  display: flex;
  flex-direction: column;
  padding: 20px 24px 24px;
  color: var(--t1);
}

/* ===== 검색바 ===== */
.search-bar {
  display: flex;
  gap: 6px;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.search-type { width: 90px; }
.search-input { flex: 1; min-width: 120px; }
.write-area { margin-left: auto; }

/* ===== 갤러리 ===== */
.gallery-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 8px;
}
.search-area { display: flex; gap: 6px; align-items: center; }

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  min-height: 200px;
}
.gallery-card {
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 0.5px solid var(--border);
  background: var(--surface);
  cursor: pointer;
  transition: var(--transition);
}
.gallery-card:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }

.gallery-thumb {
  width: 100%;
  aspect-ratio: 4/3;
  background: var(--surface2);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.gallery-thumb img { width: 100%; height: 100%; object-fit: cover; }
.no-thumb { font-size: 28px; color: var(--t4); }

.gallery-info { padding: 10px 12px; }
.gallery-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--t1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.gallery-meta { font-size: 11px; color: var(--t3); margin-top: 4px; }
.gallery-empty { grid-column: 1 / -1; text-align: center; padding: 60px 0; color: var(--t3); font-size: 14px; }

/* ===== 테이블 ===== */
:deep(.el-table__row) { cursor: pointer; }

.row-id { color: var(--t3); font-size: 13px; }

.title-cell { display: flex; align-items: center; gap: 6px; }
.title-text { font-size: 14px; }
.title-text.notice { font-weight: 600; color: var(--accent); }
.file-icon { font-size: 13px; color: var(--t3); }
.comment-cnt { font-size: 12px; color: var(--accent); font-weight: 600; }
.new-badge {
  font-size: 10px;
  background: var(--accent);
  color: #fff;
  border-radius: 3px;
  padding: 1px 4px;
  font-weight: 700;
}
.meta-text { font-size: 13px; color: var(--t2); }

.pagination { display: flex; justify-content: center; padding: 20px 0 4px; }
</style>
