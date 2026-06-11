<template>
  <div class="board-list">

    <!-- ===== 갤러리 뷰 ===== -->
    <template v-if="isGalleryBoard">
      <div v-loading="loading" class="gallery-grid">
        <div v-for="post in posts" :key="post.id" class="gallery-card" @click="goDetail(post)">
          <div class="gallery-thumb">
            <img v-if="post.thumbnailUrl" :src="post.thumbnailUrl" :alt="post.title"
              @error="e => e.target.style.display='none'" />
            <div v-else class="no-thumb"><i class="ti ti-photo-off"></i></div>
            <div v-if="post.isNotice" class="gallery-notice-badge">공지</div>
          </div>
          <div class="gallery-info">
            <div class="gallery-title">{{ post.title }}</div>
            <div class="gallery-meta-row">
              <span class="gallery-writer">{{ post.writerName }}</span>
              <span class="gallery-dot">·</span>
              <span class="gallery-date">{{ formatDate(post.createdAt) }}</span>
            </div>
            <div class="gallery-stats">
              <span v-if="board?.useLike" class="stat-chip">
                <i class="ti ti-heart"></i> {{ post.likeCount }}
              </span>
              <span class="stat-chip">
                <i class="ti ti-message-circle"></i> {{ post.commentCount }}
              </span>
              <span v-if="post.hasFile" class="stat-chip">
                <i class="ti ti-paperclip"></i>
              </span>
              <span v-if="isNew(post.createdAt)" class="new-badge">N</span>
            </div>
          </div>
        </div>
        <div v-if="!loading && !posts.length" class="gallery-empty">
          <i class="ti ti-photo-off"></i>
          <p>등록된 게시글이 없습니다.</p>
        </div>
      </div>

      <div class="pagination">
        <el-pagination v-model:current-page="page" :page-size="gallerySize" :total="total"
          layout="prev, pager, next" background small @current-change="fetchPosts" />
      </div>
      <div class="list-toolbar">
        <div class="search-group">
          <div class="search-type-wrap">
            <el-select v-model="searchType" size="small" class="search-type">
              <el-option value="title" label="제목" />
              <el-option value="content" label="내용" />
              <el-option value="writer" label="작성자" />
              <el-option value="all" label="전체" />
            </el-select>
          </div>
          <div class="search-input-wrap">
            <i class="ti ti-search search-icon"></i>
            <input v-model="keyword" class="search-input" placeholder="검색어 입력..."
              @keyup.enter="handleSearch" />
            <button v-if="keyword" class="search-clear" @click="keyword=''; handleSearch()">
              <i class="ti ti-x"></i>
            </button>
          </div>
          <button class="search-btn" @click="handleSearch">검색</button>
        </div>
        <button v-if="canWrite" class="write-btn" @click="goWrite">
          <i class="ti ti-pencil-plus"></i>
          <span>글쓰기</span>
        </button>
      </div>
    </template>

    <!-- ===== 웹진 뷰 ===== -->
    <template v-else-if="isWebzineBoard">
      <div v-loading="loading" class="webzine-list">
        <article
          v-for="post in posts"
          :key="post.id"
          :class="['webzine-item', { notice: post.isNotice }]"
          @click="goDetail(post)"
        >
          <div class="webzine-thumb">
            <img
              v-if="post.thumbnailUrl"
              :src="post.thumbnailUrl"
              :alt="post.title"
              @error="e => e.target.style.display='none'"
            />
            <div v-else class="no-thumb"><i class="ti ti-news"></i></div>
          </div>
          <div class="webzine-body">
            <div class="webzine-kicker">
              <span v-if="post.isNotice" class="notice-tag">Notice</span>
              <span>{{ formatDate(post.createdAt) }}</span>
              <span v-if="isNew(post.createdAt)" class="new-badge">N</span>
            </div>
            <h3 class="webzine-title">{{ post.title }}</h3>
            <p v-if="post.excerpt" class="webzine-excerpt">{{ post.excerpt }}</p>
            <div class="webzine-meta">
              <span class="webzine-meta-chip"><i class="ti ti-user"></i>{{ post.writerName }}</span>
              <span v-if="showViewCount" class="webzine-meta-chip"><i class="ti ti-eye"></i>{{ post.viewCount }}</span>
              <span v-if="showLikeCount && board?.useLike" class="webzine-meta-chip"><i class="ti ti-heart"></i>{{ post.likeCount }}</span>
              <span class="webzine-meta-chip"><i class="ti ti-message-circle"></i>{{ post.commentCount }}</span>
              <span v-if="post.hasFile" class="webzine-meta-chip"><i class="ti ti-paperclip"></i></span>
            </div>
          </div>
        </article>
        <div v-if="!loading && !posts.length" class="webzine-empty">
          <i class="ti ti-news-off"></i>
          <p>No posts yet.</p>
        </div>
      </div>

      <div class="pagination">
        <el-pagination v-model:current-page="page" :page-size="gallerySize" :total="total"
          layout="prev, pager, next" background small @current-change="fetchPosts" />
      </div>
      <div class="list-toolbar">
        <div class="search-group">
          <div class="search-type-wrap">
            <el-select v-model="searchType" size="small" class="search-type">
              <el-option value="title" label="제목" />
              <el-option value="content" label="내용" />
              <el-option value="writer" label="작성자" />
              <el-option value="all" label="전체" />
            </el-select>
          </div>
          <div class="search-input-wrap">
            <i class="ti ti-search search-icon"></i>
            <input v-model="keyword" class="search-input" placeholder="검색어 입력..."
              @keyup.enter="handleSearch" />
            <button v-if="keyword" class="search-clear" @click="keyword=''; handleSearch()">
              <i class="ti ti-x"></i>
            </button>
          </div>
          <button class="search-btn" @click="handleSearch">검색</button>
        </div>
        <button v-if="canWrite" class="write-btn" @click="goWrite">
          <i class="ti ti-pencil-plus"></i>
          <span>글쓰기</span>
        </button>
      </div>
    </template>

    <!-- ===== 일반/자료실 테이블 뷰 ===== -->
    <template v-else>
      <div class="post-table" v-loading="loading">
        <!-- 헤더 -->
        <div class="pt-head">
          <div class="pt-col num">번호</div>
          <div class="pt-col title">제목</div>
          <div class="pt-col writer">작성자</div>
          <div class="pt-col date">작성일</div>
          <div v-if="showViewCount" class="pt-col count">조회</div>
          <div v-if="showLikeCount && board?.useLike" class="pt-col count">좋아요</div>
        </div>

        <!-- 공지 행 -->
        <template v-for="post in noticePosts" :key="`notice-${post.id}`">
          <div class="pt-row notice-row" @click="goDetail(post)">
            <div class="pt-col num">
              <span class="notice-tag">공지</span>
            </div>
            <div class="pt-col title">
              <span class="pt-title notice-title">{{ post.title }}</span>
              <span v-if="post.commentCount > 0" class="comment-chip">
                <i class="ti ti-message-2"></i>{{ post.commentCount }}
              </span>
              <span v-if="post.hasFile" class="file-chip"><i class="ti ti-paperclip"></i></span>
              <span v-if="isNew(post.createdAt)" class="new-badge">N</span>
            </div>
            <div class="pt-col writer">{{ post.writerName }}</div>
            <div class="pt-col date">{{ formatDate(post.createdAt) }}</div>
            <div v-if="showViewCount" class="pt-col count">{{ post.viewCount }}</div>
            <div v-if="showLikeCount && board?.useLike" class="pt-col count">{{ post.likeCount }}</div>
          </div>
        </template>

        <!-- 구분선 (공지 있을 때) -->
        <div v-if="noticePosts.length && normalPosts.length" class="notice-divider" />

        <!-- 일반 글 행 -->
        <template v-for="post in normalPosts" :key="post.id">
          <div class="pt-row" @click="goDetail(post)">
            <div class="pt-col num">
              <span class="row-num">{{ post.rowNum }}</span>
            </div>
            <div class="pt-col title">
              <span class="pt-title">{{ post.title }}</span>
              <span v-if="post.commentCount > 0" class="comment-chip">
                <i class="ti ti-message-2"></i>{{ post.commentCount }}
              </span>
              <span v-if="post.hasFile" class="file-chip"><i class="ti ti-paperclip"></i></span>
              <span v-if="isNew(post.createdAt)" class="new-badge">N</span>
            </div>
            <div class="pt-col writer">{{ post.writerName }}</div>
            <div class="pt-col date">{{ formatDate(post.createdAt) }}</div>
            <div v-if="showViewCount" class="pt-col count">{{ post.viewCount }}</div>
            <div v-if="showLikeCount && board?.useLike" class="pt-col count">{{ post.likeCount }}</div>
          </div>
        </template>

        <!-- 빈 상태 -->
        <div v-if="!loading && !posts.length" class="pt-empty">
          <i class="ti ti-inbox"></i>
          <p>등록된 게시글이 없습니다.</p>
        </div>
      </div>

      <div class="pagination">
        <el-pagination v-model:current-page="page" :page-size="size" :total="total"
          layout="prev, pager, next" background small @current-change="fetchPosts" />
      </div>
      <div class="list-toolbar">
        <div class="search-group">
          <div class="search-type-wrap">
            <el-select v-model="searchType" size="small" class="search-type">
              <el-option value="title" label="제목" />
              <el-option value="content" label="내용" />
              <el-option value="writer" label="작성자" />
              <el-option value="all" label="전체" />
            </el-select>
          </div>
          <div class="search-input-wrap">
            <i class="ti ti-search search-icon"></i>
            <input v-model="keyword" class="search-input" placeholder="검색어 입력..."
              @keyup.enter="handleSearch" />
            <button v-if="keyword" class="search-clear" @click="keyword=''; handleSearch()">
              <i class="ti ti-x"></i>
            </button>
          </div>
          <button class="search-btn" @click="handleSearch">검색</button>
        </div>
        <button v-if="canWrite" class="write-btn" @click="goWrite">
          <i class="ti ti-pencil-plus"></i>
          <span>글쓰기</span>
        </button>
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
const size = ref(15)
const gallerySize = ref(15)
const keyword = ref('')
const searchType = ref('title')

const noticePosts = computed(() => posts.value.filter(p => p.isNotice))
const normalPosts = computed(() => posts.value.filter(p => !p.isNotice))
const isGalleryBoard = computed(() => board.value?.boardType === 'GALLERY')
const isWebzineBoard = computed(() => board.value?.boardType === 'WEBZINE')
const isVisualBoard = computed(() => isGalleryBoard.value || isWebzineBoard.value)

const canWrite = computed(() => {
  if (!board.value) return false
  if (authStore.member?.isAdmin) return true
  const perms = board.value.permissions || []
  if (!perms.length) return true
  if (!authStore.isLoggedIn) return false
  const memberGroupIds = authStore.member?.groupIds || []
  return perms.some(p => memberGroupIds.includes(p.groupId) && p.canWrite)
})

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
    const pageSize = isVisualBoard.value ? gallerySize.value : size.value
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

function handleSearch() { page.value = 1; fetchPosts() }
function goWrite() { router.push(`${basePath.value}/write`) }
function goDetail(row) { router.push(`${basePath.value}/posts/${row.id}`) }

function formatDate(d) {
  if (!d) return ''
  const dt = new Date(d)
  const today = new Date()
  if (dt.toDateString() === today.toDateString()) return dt.toTimeString().slice(0, 5)
  return `${dt.getFullYear()}.${String(dt.getMonth() + 1).padStart(2, '0')}.${String(dt.getDate()).padStart(2, '0')}`
}

function isNew(d) { return d && Date.now() - new Date(d).getTime() < 3 * 86400000 }

watch(boardId, async () => {
  page.value = 1; keyword.value = ''
  await fetchBoard(); fetchPosts()
})

onMounted(async () => {
  if (route.params.menuId) await menuStore.fetchMenus()
  await fetchBoard(); fetchPosts()
})
</script>

<style scoped>
.board-list {
  display: flex;
  flex-direction: column;
  color: var(--t1);
}

/* ===== 공통 배지 ===== */
.new-badge {
  font-size: 10px;
  background: var(--new);
  color: #fff;
  border-radius: 3px;
  padding: 1px 4px;
  font-weight: 700;
  flex-shrink: 0;
}

/* ===== 테이블 뷰 ===== */
.post-table {
  width: 100%;
}

.pt-head {
  display: flex;
  align-items: center;
  padding: 0 20px;
  height: 40px;
  background: var(--surface2);
  border-bottom: 1px solid var(--border2);
  border-top: 1px solid var(--border2);
}

.pt-col {
  font-size: 12px;
  font-weight: 700;
  color: var(--t3);
  text-transform: uppercase;
  letter-spacing: 0.3px;
  white-space: nowrap;
}

.pt-col.num   { width: 64px; flex-shrink: 0; text-align: center; }
.pt-col.title { flex: 1; min-width: 0; }
.pt-col.writer{ width: 100px; flex-shrink: 0; text-align: center; }
.pt-col.date  { width: 96px; flex-shrink: 0; text-align: center; }
.pt-col.count { width: 60px; flex-shrink: 0; text-align: center; }

/* 행 */
.pt-row {
  display: flex;
  align-items: center;
  padding: 0 20px;
  min-height: 48px;
  border-bottom: 0.5px solid var(--border2);
  cursor: pointer;
  transition: background 0.12s;
}

.pt-row:hover { background: var(--surface2); }
.pt-row:hover .pt-title { color: var(--accent-t); }

.pt-row .pt-col {
  font-size: 14px;
  font-weight: 400;
  color: var(--t2);
  letter-spacing: 0;
  text-transform: none;
}

.pt-row .pt-col.title {
  display: flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
}

.pt-title {
  color: var(--t1);
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.12s;
}

.notice-row { background: var(--accent-bg); }
.notice-row:hover { background: color-mix(in srgb, var(--accent) 12%, transparent); }
.notice-title { font-weight: 700; color: var(--accent-t); }

.notice-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 7px;
  border-radius: 3px;
  background: var(--accent);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
}

.row-num { font-size: 13px; color: var(--t4); }

.comment-chip {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  color: var(--t3);
  flex-shrink: 0;
}

.comment-chip i { font-size: 12px; }

.file-chip {
  font-size: 13px;
  color: var(--t3);
  flex-shrink: 0;
}

.notice-divider {
  height: 3px;
  background: linear-gradient(90deg, var(--accent) 0%, transparent 100%);
  opacity: 0.15;
}

.pt-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 60px 0;
  color: var(--t4);
  font-size: 14px;
}

.pt-empty i { font-size: 36px; }

/* ===== 갤러리 ===== */
.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1px;
  background: var(--border2);
  border-top: 1px solid var(--border2);
}

.gallery-card {
  background: var(--surface);
  cursor: pointer;
  transition: var(--transition);
  overflow: hidden;
}

.gallery-card:hover { background: var(--surface2); }
.gallery-card:hover .gallery-title { color: var(--accent-t); }

.gallery-thumb {
  position: relative;
  width: 100%;
  aspect-ratio: 4/3;
  background: var(--surface2);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.gallery-thumb img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.3s; }
.gallery-card:hover .gallery-thumb img { transform: scale(1.04); }
.no-thumb { font-size: 28px; color: var(--t4); }

.gallery-notice-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  background: var(--accent);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 3px;
}

.gallery-info { padding: 10px 12px 12px; }

.gallery-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--t1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
  transition: color 0.12s;
}

.gallery-meta-row {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--t3);
  margin-bottom: 6px;
}

.gallery-dot { color: var(--t4); }

.gallery-stats {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.stat-chip {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: var(--t3);
  font-weight: 500;
}

.stat-chip i { font-size: 11px; }

.gallery-empty {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 60px 0;
  color: var(--t4);
  font-size: 14px;
}

.gallery-empty i { font-size: 36px; }

/* ===== Webzine ===== */
.webzine-list {
  display: flex;
  flex-direction: column;
  border-top: 1px solid var(--border2);
}

.webzine-item {
  display: grid;
  grid-template-columns: minmax(160px, 240px) 1fr;
  gap: 18px;
  padding: 18px 20px;
  border-bottom: 0.5px solid var(--border2);
  cursor: pointer;
  background: var(--surface);
  transition: background 0.15s;
}

.webzine-item:hover { background: var(--surface2); }
.webzine-item.notice { background: var(--accent-bg); }

.webzine-thumb {
  width: 100%;
  aspect-ratio: 16/10;
  border-radius: var(--radius-xs);
  overflow: hidden;
  background: var(--surface2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.webzine-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.webzine-item:hover .webzine-thumb img { transform: scale(1.03); }

.webzine-body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.webzine-kicker,
.webzine-meta {
  display: flex;
  align-items: center;
  gap: 7px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--t3);
}

.webzine-title {
  margin: 0;
  color: var(--t1);
  font-size: 20px;
  line-height: 1.35;
  font-weight: 800;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.webzine-item:hover .webzine-title { color: var(--accent-t); }

.webzine-excerpt {
  margin: 0;
  color: var(--t2);
  font-size: 13px;
  line-height: 1.55;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.webzine-meta-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 20px;
}

.webzine-meta-chip i {
  font-size: 13px;
  color: var(--t3);
}

.webzine-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 60px 0;
  color: var(--t4);
  font-size: 14px;
}

.webzine-empty i { font-size: 36px; }

/* ===== 툴바 ===== */
.list-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid var(--border2);
  background: var(--surface2);
  flex-wrap: wrap;
}

.search-group {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.search-type-wrap { flex-shrink: 0; }
.search-type { width: 82px; }

.search-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 10px;
  font-size: 13px;
  color: var(--t3);
  pointer-events: none;
}

.search-input {
  padding: 7px 30px 7px 32px;
  border: 1px solid var(--border);
  border-radius: var(--radius-xs);
  background: var(--surface2);
  font-size: 13px;
  color: var(--t1);
  width: 200px;
  outline: none;
  transition: border-color 0.15s, box-shadow 0.15s;
  font-family: inherit;
}

.search-input:focus {
  border-color: var(--accent);
  background: var(--surface);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--accent) 12%, transparent);
}

.search-input::placeholder { color: var(--t4); }

.search-clear {
  position: absolute;
  right: 8px;
  background: none;
  border: none;
  color: var(--t3);
  font-size: 13px;
  cursor: pointer;
  padding: 2px;
  display: flex;
  align-items: center;
}

.search-btn {
  padding: 7px 14px;
  border-radius: var(--radius-xs);
  border: none;
  background: var(--accent);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.15s;
  white-space: nowrap;
}

.search-btn:hover { opacity: 0.88; }

.write-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 16px;
  border-radius: var(--radius-xs);
  border: 1px solid var(--accent);
  background: var(--accent);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.15s;
  white-space: nowrap;
  flex-shrink: 0;
}

.write-btn:hover { opacity: 0.88; }

/* ===== 페이지네이션 ===== */
.pagination { display: flex; justify-content: center; padding: 24px 0 20px; }

@media (max-width: 600px) {
  /* ===== 테이블 ===== */
  .pt-col.writer, .pt-col.count { display: none; }
  .pt-col.date { width: 72px; font-size: 12px; }
  .search-input { width: 130px; }

  /* ===== 갤러리 : 동글동글 카드 ===== */
  .gallery-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    background: transparent;
    border-top: none;
    padding: 14px;
  }

  .gallery-card {
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
    border: 0.5px solid var(--border2);
  }

  .gallery-thumb {
    border-radius: 0;
  }

  .gallery-notice-badge {
    border-radius: 8px;
  }

  /* ===== 웹진 : 이미지 상단 full-width 카드 ===== */
  .webzine-list {
    gap: 12px;
    padding: 12px;
  }

  .webzine-item {
    grid-template-columns: 1fr;
    gap: 0;
    padding: 0;
    border-radius: 16px;
    overflow: hidden;
    border-bottom: none;
  }

  .webzine-thumb {
    width: 100%;
    aspect-ratio: 16 / 9;
    border-radius: 0;
  }

  .webzine-body {
    padding: 12px 14px 14px;
    gap: 6px;
  }

  .webzine-title {
    font-size: 16px;
    -webkit-line-clamp: 2;
  }

  .webzine-excerpt {
    font-size: 12px;
    -webkit-line-clamp: 2;
  }

  .webzine-meta { gap: 5px; font-size: 11px; }
}
</style>
